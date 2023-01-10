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

import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONArray;
import org.scijava.log.Logger;
import org.scijava.menu.MenuConstants;
import org.scijava.options.OptionsPlugin;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;

import ch.fmi.prefect.RequestUtils;

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
public final class PrefectOptions extends OptionsPlugin {

	@Parameter
	private Logger logger;

	@Parameter
	private PrefService prefService;

	@Parameter(label = "Prefect API Key")
	private String apiKey;

	private static final String BASE_URL = "https://api.prefect.cloud/api";
	private static final String ACCOUNTS_URL = BASE_URL + "/me/accounts";
	private static final String WORKSPACES_URL = BASE_URL + "/me/workspaces";

	private static final String ACCOUNT_ID = "accountID";
	private static final String WORKSPACE_ID = "workspaceID";

	public String getApiKey() {
		return apiKey;
	}

	public String getAccountID() {
		return prefService.get(getClass(), ACCOUNT_ID);
	}

	public String getWorkspaceID() {
		return prefService.get(getClass(), WORKSPACE_ID);
	}

	public String getCommonURL() {
		return BASE_URL + "/accounts/" + getAccountID() + "/workspaces/" +
			getWorkspaceID();
	}

	public String getApiURL(String suffix) {
		return getCommonURL() + suffix;
	}

	@Override
	public void run() {
		logger.info("Trying to retrieve account and workspace ID.");
		// Set accountID and workspaceID
		try (CloseableHttpClient client = RequestUtils.getClient()) {
			String accoutsJsonArrayString = RequestUtils.httpGetRequest(client,
				ACCOUNTS_URL, apiKey, logger);
			String accountID = new JSONArray(accoutsJsonArrayString).getJSONObject(0)
				.getString("account_id");
			prefService.put(getClass(), ACCOUNT_ID, accountID);

			String workspacesJsonArrayString = RequestUtils.httpGetRequest(client,
				WORKSPACES_URL, apiKey, logger);
			String workspaceID = new JSONArray(workspacesJsonArrayString).getJSONObject(0)
				.getString("workspace_id");
			prefService.put(getClass(), WORKSPACE_ID, workspaceID);
			logger.info("Successfully retrieved account (" + accountID +
					") and workspace (" + workspaceID + ")");
		} catch (IOException e) {
			logger.error(e);
		}
		super.run();
	}
}
