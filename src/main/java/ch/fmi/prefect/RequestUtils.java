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

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.scijava.log.Logger;

public final class RequestUtils {

	private RequestUtils() {
		// prevent instantiation of static utility class
	}

	public static CloseableHttpClient getClient() {
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		String proxyHost = System.getProperty("https.proxyHost");
		if (proxyHost != null && proxyHost != "") {
			clientBuilder.setProxy(new HttpHost(proxyHost,
												Integer.parseInt(System.getProperty("https.proxyPort")),
												HttpHost.DEFAULT_SCHEME_NAME)
												);
		}
		return clientBuilder.build();
	}

	public static String httpRequest(CloseableHttpClient client,
		HttpRequestBase request, String apiKey, Logger logger) throws IOException
	{
		request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
		String result = null;
		try (CloseableHttpResponse response = client.execute(request)) {
			HttpEntity entity = response.getEntity();
			result = EntityUtils.toString(entity);
			EntityUtils.consume(entity);
		} catch (ClientProtocolException e) {
			logger.error(e);
		}
		return result;
	}

	public static String httpGetRequest(CloseableHttpClient client, String url,
		String apiKey, Logger logger) throws IOException
	{
		HttpGet get = new HttpGet(url);
		return httpRequest(client, get, apiKey, logger);
	}

	public static String httpPostRequest(CloseableHttpClient client, String url,
		String apiKey, Logger logger) throws IOException
	{
		HttpPost post = new HttpPost(url);
		return httpRequest(client, post, apiKey, logger);
	}

	public static String httpPostRequest(CloseableHttpClient client, String url,
		String apiKey, JSONObject parameters, Logger logger) throws IOException
	{
		HttpPost post = new HttpPost(url);
		logger.debug(parameters.toString());
		post.setEntity(new StringEntity(parameters.toString()));
		post.setHeader("Content-type", "application/json");
		return httpRequest(client, post, apiKey, logger);
	}
}
