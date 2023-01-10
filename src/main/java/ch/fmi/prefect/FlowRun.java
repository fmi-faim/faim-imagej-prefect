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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.log.Logger;
import org.scijava.module.MutableModuleItem;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ch.fmi.prefect.config.PrefectOptions;

@Plugin(type = Command.class, menuPath = "FMI>Prefect>Start Flow Run...")
public final class FlowRun extends DynamicCommand implements Initializable {

	private String FLOWS_SUFFIX = "/flows/filter";
	private String DEPLOYMENTS_SUFFIX = "/deployments/filter";

	private Map<String, String> deployment_ids;

	@Parameter
	private CommandService commandService;

	@Parameter
	private OptionsService optionsService;

	@Parameter
	private Logger logger;

	@Parameter(choices = {})
	private String deployment;

	@Override
	public void run() {
		logger.info("Trying to run deployment ID: " + deployment_ids.get(
			deployment));
		commandService.run(FlowRunner.class, true, "deploymentID", deployment_ids
			.get(deployment));
	}

	@Override
	public void initialize() {
		PrefectOptions prefectOptions = optionsService.getOptions(
			PrefectOptions.class);
		Map<String, String> flows = new HashMap<>();
		deployment_ids = new HashMap<>();
		// Get list of existing deployments
		try (CloseableHttpClient client = RequestUtils.getClient()) {
			String flows_url = prefectOptions.getApiURL(FLOWS_SUFFIX);
			logger.debug(flows_url);
			String flows_response = RequestUtils.httpPostRequest(client, flows_url,
				prefectOptions.getApiKey(), logger);
			logger.debug(flows_response);
			JSONArray flows_array = new JSONArray(flows_response);
			for (int i = 0; i < flows_array.length(); i++) {
				JSONObject flow = flows_array.getJSONObject(i);
				flows.put(flow.getString("id"), flow.getString("name"));
			}
			// filter by tag(s)
			JSONObject request_body = null;
			if (prefectOptions.getTags().length > 0) {
				request_body = new JSONObject().put("deployments", new JSONObject().put("tags",
						new JSONObject().put("all_", new JSONArray().putAll(prefectOptions.getTags()))));
			}
			String deployments_response = RequestUtils.httpPostRequest(client,
				prefectOptions.getApiURL(DEPLOYMENTS_SUFFIX), prefectOptions
					.getApiKey(), request_body, logger);
			logger.debug(deployments_response);
			JSONArray deployments_array = new JSONArray(deployments_response);
			if (deployments_array.length() == 0) {
				cancel("No deployments found matching the configured tags. Unable to start any flow run.");
			}
			for (int i = 0; i < deployments_array.length(); i++) {
				JSONObject deployment = deployments_array.getJSONObject(i);
				deployment_ids.put(flows.get(deployment.getString("flow_id")) + ": " +
					deployment.getString("name"), deployment.getString("id"));
			}

		} catch (IOException e) {
			logger.error(e);
		}
		MutableModuleItem<String> deploymentInput = getInfo().getMutableInput(
			"deployment", String.class);
		deploymentInput.setChoices(new ArrayList<>(deployment_ids.keySet()));
	}

}
