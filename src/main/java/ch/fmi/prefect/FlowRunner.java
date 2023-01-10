/*-
 * #%L
 * A SciJava plugin to interact with Prefect Cloud, developed at the FMI Basel.
 * %%
 * Copyright (C) 2022 Friedrich Miescher Institute for Biomedical Research (FMI), Basel (Switzerland)
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

package ch.fmi.prefect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.convert.ConvertService;
import org.scijava.log.Logger;
import org.scijava.module.DefaultMutableModuleItem;
import org.scijava.module.MutableModuleItem;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;

import ch.fmi.prefect.config.PrefectOptions;

public final class FlowRunner extends DynamicCommand implements Initializable {

	private String DEPLOYMENTS_API = "/deployments/";
	private String CREATE_API = "/create_flow_run";
	private List<String> prefectInputs = new ArrayList<>();

	@Parameter
	private OptionsService optionsService;

	@Parameter
	private ConvertService convertService;

	@Parameter
	private Logger logger;

	@Parameter
	private String deploymentID;

	@Override
	public void run() {
		JSONObject inputs = new JSONObject();
		logger.info("Run deployment " + deploymentID +
			"with the following inputs:");
		for (String input_name : prefectInputs) {
			inputs.put(input_name, getInput(input_name));
			logger.info(input_name + ": " + inputs.get(input_name));
		}
		JSONObject parameters = new JSONObject();
		parameters.put("parameters", inputs);
		logger.debug(parameters.toString());

		PrefectOptions prefectOptions = optionsService.getOptions(
			PrefectOptions.class);
		try (CloseableHttpClient client = RequestUtils.getClient()) {
			String create_url = prefectOptions.getApiURL(DEPLOYMENTS_API +
				deploymentID + CREATE_API);
			logger.debug(create_url);
			String create_response = RequestUtils.httpPostRequest(client, create_url,
				prefectOptions.getApiKey(), parameters, logger);
			logger.debug(create_response);
			JSONObject response = new JSONObject(create_response);
			String flow_run_name = response.getString("name");
			String work_queue_name = response.getString("work_queue_name");
			logger.info("Successfully created flow run '" + flow_run_name +
				"' on work queue '" + work_queue_name + "'.");
		} catch (Exception e) {
			logger.error("Creating flow run failed.", e);
		}
	}

	@Override
	public void initialize() {
		if (deploymentID == null) {
			throw new IllegalArgumentException(
				"Please set deploymentID programmatically to correctly initialize this command.");
		}
		if (optionsService == null) {
			throw new IllegalStateException("OptionsService not initialized.");
		}
		// Get parameters for deploymentID
		PrefectOptions prefectOptions = optionsService.getOptions(
			PrefectOptions.class);
		JSONObject parameter_openapi_schema;
		JSONObject optional_defaults;
		try (CloseableHttpClient client = RequestUtils.getClient()) {
			String deployment_url = prefectOptions.getApiURL(DEPLOYMENTS_API +
				deploymentID);
			logger.debug(deployment_url);
			String deployment_response = RequestUtils.httpGetRequest(client,
				deployment_url, prefectOptions.getApiKey(), logger);
			JSONObject deployment = new JSONObject(deployment_response);
			parameter_openapi_schema = deployment.getJSONObject(
				"parameter_openapi_schema");
			optional_defaults = deployment.getJSONObject("parameters");
		} catch (IOException e) {
			logger.error("Initialization failed.", e);
			return;
		} catch (JSONException e) {
			logger.error("Initialization failed.", e);
			return;
		}
		JSONObject properties = parameter_openapi_schema.getJSONObject(
			"properties");
		for (String name : JSONObject.getNames(properties)) {
			this.addInput(getModuleItem(name, properties.getJSONObject(name),
				optional_defaults.optString(name, null)));
			prefectInputs.add(name);
		}
		logger.info(properties);
	}

	@SuppressWarnings("unchecked")
	private <T> MutableModuleItem<T> getModuleItem(String name, JSONObject json,
		String optional_default)
	{
		Class<T> clazz;
		logger.info("Name: " + name);
		String type = json.optString("type", null);
		logger.info("  Type: " + type);
		if (type != null) {
			switch (type) {
				case "number":
					clazz = (Class<T>) Double.class;
					break;
				case "integer":
					clazz = (Class<T>) Integer.class;
					break;
				case "boolean":
					clazz = (Class<T>) Boolean.class;
					break;
				case "string":
					if ("path".equals(json.optString("format"))) {
						clazz = (Class<T>) File.class;
						break;
					}
				default:
					clazz = (Class<T>) String.class;
			}
		} else {
			// type is null, so we look for enum, allOf, anyOf.
			clazz = (Class<T>) String.class; // fallback
		}

		String defaultValue = optional_default != null ? optional_default : json
			.optString("default", null);

		logger.info("  Default: " + defaultValue);

		MutableModuleItem<T> moduleItem = new DefaultMutableModuleItem<>(this, name,
			clazz);
		moduleItem.setDefaultValue(convertService.convert(defaultValue, clazz));
		moduleItem.setPersisted(false); // for debugging purposes
		moduleItem.setChoices((List<? extends T>) getChoices(name, json, clazz));

		logger.info("  Enum: " + json.optString("enum", null));
		logger.info("  allOf: " + json.optJSONArray("allOf"));
		logger.info("  anyOf: " + json.optJSONArray("anyOf"));
		logger.info("  ==> " + clazz);
		return moduleItem;
	}

	private <T> List<? extends T> getChoices(String name, JSONObject json,
		Class<T> clazz)
	{
		try {
			List<T> list = new ArrayList<>();
			JSONArray anyOf = json.optJSONArray("anyOf");
			if (anyOf != null) {
				for (int i = 0; i < anyOf.length(); i++) {
					JSONArray enum_object = anyOf.getJSONObject(i).optJSONArray("enum");
					if (enum_object != null) {
						for (int j = 0; j < enum_object.length(); j++) {
							list.add(convertService.convert(enum_object.getString(j), clazz));
						}
					}
				}
			}
			return list;
		} catch (JSONException e) {
			logger.error(e);
			return null;
		}
	}

}
