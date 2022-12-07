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

package ch.fmi.prefect.config;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.scijava.log.Logger;
import org.scijava.menu.MenuConstants;
import org.scijava.options.OptionsPlugin;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Runs the Edit::Options::Prefect dialog.
 * 
 * @author Jan Eglinger
 */
@Plugin(type = OptionsPlugin.class, menu = {
		@Menu(label = MenuConstants.EDIT_LABEL, weight = MenuConstants.EDIT_WEIGHT,
				mnemonic = MenuConstants.EDIT_MNEMONIC),
		@Menu(label = "Options", mnemonic = 'o'),
		@Menu(label = "Prefect...", weight = 100, mnemonic = 'p') })
public class PrefectOptions extends OptionsPlugin {

	@Parameter
	private Logger logger;

	@Parameter(label = "Prefect API Key")
	private String apiKey;

	private String accountID;
	private String workspaceID;

	private static final String ACCOUNTS_URL = "https://api.prefect.cloud/api/me/accounts";
	private static final String WORKSPACES_URL = "https://api.prefect.cloud/api/me/workspaces";

	public String getApiKey() {
		return apiKey;
	}

	public String getAccountID() {
		return accountID;
	}

	public String getWorkspaceID() {
		return workspaceID;
	}

	@Override
	public void run() {
		logger.info("Trying to retrieve account and workspace ID.");
		// Set accountID and workspaceID
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		String proxyHost = System.getProperty("https.proxyHost");
		if (proxyHost != null && proxyHost != "") {
			clientBuilder.setProxy(new HttpHost(proxyHost,
												Integer.parseInt(System.getProperty("https.proxyPort")),
												HttpHost.DEFAULT_SCHEME_NAME)
												);
		}
		try (CloseableHttpClient client = clientBuilder.build()) {
			HttpGet accountQuery = new HttpGet(ACCOUNTS_URL);
			accountQuery.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
			try (CloseableHttpResponse response = client.execute(accountQuery)) {
				HttpEntity entity = response.getEntity();
				String jsonArrayString = EntityUtils.toString(entity);
				JSONArray jsonArray = new JSONArray(jsonArrayString);
				JSONObject firstEntry = jsonArray.getJSONObject(0);
				accountID = firstEntry.getString("account_id");
				EntityUtils.consume(entity);
			}
			HttpGet workspaceQuery = new HttpGet(WORKSPACES_URL);
			workspaceQuery.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
			try (CloseableHttpResponse response = client.execute(workspaceQuery)) {
				HttpEntity entity = response.getEntity();
				String jsonArrayString = EntityUtils.toString(entity);
				JSONArray jsonArray = new JSONArray(jsonArrayString);
				JSONObject firstEntry = jsonArray.getJSONObject(0);
				workspaceID = firstEntry.getString("workspace_id");
				EntityUtils.consume(entity);
			}			
		} catch (ClientProtocolException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		}
		logger.info("Successfully retrieved account (" + accountID + ") and workspace (" + workspaceID + ")");
		super.run();
	}
}
