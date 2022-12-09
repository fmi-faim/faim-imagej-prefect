package ch.fmi.prefect;

import java.io.IOException;
import java.util.Map;

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

public class RequestUtils {

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

	public static String httpRequest(CloseableHttpClient client, HttpRequestBase request, String apiKey, Logger logger) throws IOException {
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

	public static String httpGetRequest(CloseableHttpClient client, String url, String apiKey, Logger logger)
			throws IOException {
		HttpGet get = new HttpGet(url);
		return httpRequest(client, get, apiKey, logger);
	}

	public static String httpPostRequest(CloseableHttpClient client, String url, String apiKey, Logger logger)
			throws IOException {
		HttpPost post = new HttpPost(url);
		return httpRequest(client, post, apiKey, logger);
	}

	public static String httpPostRequest(CloseableHttpClient client, String url, String apiKey,
			JSONObject parameters, Logger logger) throws IOException {
		HttpPost post = new HttpPost(url);
		logger.debug(parameters.toString());
		post.setEntity(new StringEntity(parameters.toString()));
		post.setHeader("Content-type", "application/json");
		return httpRequest(client, post, apiKey, logger);
	}
}
