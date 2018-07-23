package com.cli.qm.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

/**
 * Factorize some common behaviors shared by the OSLC consumer examples.
 * 
 */
public class HttpUtils {
	static public boolean DEBUG = true;
	
	static String AUTHURL = "X-jazz-web-oauth-url";
	static String AUTHREQUIRED = "X-com-ibm-team-repository-web-auth-msg";
    // name of custom header that authentication messages are stored in
    private static final String FORM_AUTH_HEADER = "X-com-ibm-team-repository-web-auth-msg"; //$NON-NLS-1$
    // auth header value when authentication is required
    private static final String FORM_AUTH_REQUIRED_MSG = "authrequired"; //$NON-NLS-1$
    // auth header value when authentication failed
    private static final String FORM_AUTH_FAILED_MSG = "authfailed"; //$NON-NLS-1$
    // URI the server redirects to when authentication fails
    public static final String FORM_AUTH_FAILED_URI = "/auth/authfailed"; //$NON-NLS-1$
	
	static public void setupLazySSLSupport(HttpClient httpClient) {
		ClientConnectionManager connManager = httpClient.getConnectionManager();
		SchemeRegistry schemeRegistry = connManager.getSchemeRegistry();
		schemeRegistry.unregister("https");
		/** Create a trust manager that does not validate certificate chains */
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public void checkClientTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {
				/** Ignore Method Call */
			}

			public void checkServerTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {
				/** Ignore Method Call */
			}

			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		} };

		SSLContext sc = null;
		try {
			// changed from SSL to TLSv1 to work with CLM 5.0.x
			sc = SSLContext.getInstance("TLSv1"); //$NON-NLS-1$
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException e) {
			/* Fail Silently */
		} catch (KeyManagementException e) {
			/* Fail Silently */
		}

		SSLSocketFactory sf = new SSLSocketFactory(sc);
		sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		Scheme https = new Scheme("https", sf, 443);

		schemeRegistry.register(https);

	}

	/**
	 * Print out the HTTPResponse headers
	 */
	public static void printResponseHeaders(HttpResponse response) {
		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			System.out.println("\t- " + headers[i].getName() + ": " + headers[i].getValue());
		}
	}

	/**
	 * Print out the HTTP Response body
	 */
	public static void printResponseBody(HttpResponse response) {
		HttpEntity entity = response.getEntity();
		if (entity == null) return;
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(entity.getContent()));
			String line = reader.readLine();
			while (line != null) {
				System.out.println(line);
				line = reader.readLine();
			}
			reader.close();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static StringWriter responseBodyToString(HttpResponse response) {
		StringWriter wrter = new StringWriter();
		HttpEntity entity = response.getEntity();
		if (entity == null) return wrter;
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(entity.getContent()));
			String line = reader.readLine();
			while (line != null) {
				wrter.append(line);
				line = reader.readLine();
			}
			reader.close();
		} catch (IllegalStateException e) {
		} catch (IOException e) {
		}
		return wrter;
	}
	
	//Returns true if authentication was required, false otherwise
	private static boolean doRRCOAuth(HttpResponse documentResponse, String login, String password, HttpClient httpClient, String jtsURI)
				throws IOException, InvalidCredentialsException {
		if (documentResponse.getStatusLine().getStatusCode() == 200 || documentResponse.getStatusLine().getStatusCode() == 401) {
			Header header = documentResponse.getFirstHeader(FORM_AUTH_HEADER);
			if (header != null && header.getValue().equals(FORM_AUTH_REQUIRED_MSG)) {
				documentResponse.getEntity().consumeContent();  //closes the connection
				
				//First GET
/*				HttpGet request2 = new HttpGet(header.getValue());
				HttpClientParams.setRedirecting(request2.getParams(), false);
				documentResponse = httpClient.execute(request2);
				documentResponse.getEntity().consumeContent();
				if (DEBUG) {
					System.out.println(">> Response Headers:");
					HttpUtils.printResponseHeaders(documentResponse);
				}
				
				//Second GET
				Header location = documentResponse.getFirstHeader("Location");
				HttpGet request3 = new HttpGet(location.getValue());
				HttpClientParams.setRedirecting(request3.getParams(), false);	
				documentResponse = httpClient.execute(request3);
				documentResponse.getEntity().consumeContent();
				if (DEBUG) {
					System.out.println(">> Response Headers:");
					HttpUtils.printResponseHeaders(documentResponse);
				}*/

				//POST to login form
				// The server requires an authentication: Create the login form
				HttpPost formPost = new HttpPost(jtsURI+"/j_security_check");
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				nvps.add(new BasicNameValuePair("j_username", login));
				nvps.add(new BasicNameValuePair("j_password", password));
				formPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

				// Step (2): The client submits the login form
				if (DEBUG) System.out.println(">> POST " + formPost.getURI());				
				HttpResponse formResponse = httpClient.execute(formPost);
				formResponse.getEntity().consumeContent();
				if (DEBUG) HttpUtils.printResponseHeaders(formResponse);
				
				//Third GET
/*				HttpGet request4 = new HttpGet(location.getValue());
				HttpClientParams.setRedirecting(request4.getParams(), false);
				documentResponse = httpClient.execute(request4);
				documentResponse.getEntity().consumeContent();
				if (DEBUG) {
					System.out.println(">> Response Headers:");
					HttpUtils.printResponseHeaders(documentResponse);
				}
				
				//Second Post
				Header location3 = formResponse.getFirstHeader("Location");
				Map<String,String> oAuthMap = getQueryMap(location3.getValue());
				String oauthToken = oAuthMap.get("oauth_token");
				String oauthCallback = oAuthMap.get("oauth_callback");;

				// The server requires an authentication: Create the login form
				HttpPost formPost2 = new HttpPost(jtsURI+"/j_security_check");
				formPost2.getParams().setParameter("oauth_token", oauthToken);
				formPost2.getParams().setParameter("oauth_callback", oauthCallback);
				formPost2.getParams().setParameter("authorize", "true");
				formPost2.addHeader("Content-Type","application/x-www-form-urlencoded;charset=UTF-8");
				
				if (DEBUG) System.out.println(">> POST "+formPost2.getURI());				
				formResponse = httpClient.execute(formPost2);
				formResponse.getEntity().consumeContent();
				if (DEBUG) HttpUtils.printResponseHeaders(formResponse);
*/				
				int formSc = formResponse.getStatusLine().getStatusCode();
				if (formSc == 200 || formSc == 302){
					header = formResponse.getFirstHeader(FORM_AUTH_HEADER);
                    String redirectURI = formResponse.getFirstHeader("Location").getValue();
					if ((header!=null) && (header.getValue().equals(FORM_AUTH_FAILED_MSG))) {
						// The login failed
						throw new InvalidCredentialsException("Authentication failed");
					} else if (formSc == 302 && redirectURI.contains(FORM_AUTH_FAILED_URI)){
							throw new InvalidCredentialsException("Authentication failed");                    							
					} else {
						// The login succeed
						try {
							formPost.setURI(new URI(redirectURI));
							formResponse = httpClient.execute(formPost);
						} catch (URISyntaxException e) {
							e.printStackTrace();
						}
						// Step (3): Request again the protected resource
						formResponse.getEntity().consumeContent();
						return true; //REDO YOUR REQUEST
					}
				} else {
					throw new InvalidCredentialsException("Authentication failed");					
				}
			}
		}
		return false;
	}

	/**
	 * Access to a Document protected by a Form based authentication
	 * 
	 * @param serverURI			- Server URI
	 * @param request			- HttpGet request
	 * @param login				- Server login
	 * @param password			- Server password
	 * @param httpClient		- HttpClient used for the connection
	 * @param verbose			- if true, trace all server interactions
	 * @return					- HttpResponse
	 * 
	 * @throws IOException
	 * @throws InvalidCredentialsException
	 */
	public static HttpResponse sendGetForSecureDocumentQMCCM(String serverURI, HttpGet request, String login, String password, HttpClient httpClient, String jtsURI)
			throws IOException, InvalidCredentialsException {
		
		// Step (1): Request the protected resource
		if (DEBUG) System.out.println(">> GET(1) "+request.getURI());
		HttpResponse documentResponse = httpClient.execute(request);
		if (DEBUG) {
			System.out.println(">> Response Headers:");
			HttpUtils.printResponseHeaders(documentResponse);
		}
		
		boolean loginWasRequired = doRRCOAuth(documentResponse, login, password, httpClient, serverURI);
		//If we were not authenticated, we need to retry our request
		if(loginWasRequired){
				HttpGet documentGet2;
				try {
					documentGet2 = (HttpGet)(request.clone());
					return httpClient.execute(documentGet2);
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
		}
		return documentResponse;
	}
	
	/**
	 * Update a Document protected by a Form based authentication
	 * 
	 * @param serverURI			- Server URI
	 * @param protectedResource	- Absolute path to the protected document
	 * @param login				- Server login
	 * @param password			- Server password
	 * @param httpClient		- HttpClient used for the connection
	 * @param verbose			- if true, trace all server interactions
	 * @return					- HttpResponse
	 * 
	 * @throws IOException
	 * @throws InvalidCredentialsException
	 */
	public static HttpResponse sendPutForSecureDocumentQMCCM(String serverURI, HttpPut put, String login, String password, HttpClient httpClient, String jtsURI)
			throws IOException, InvalidCredentialsException {
		
		// Step (1): Request the protected resource
		if (DEBUG) System.out.println(">> PUT(1) "+put.getURI());
		HttpResponse documentResponse = httpClient.execute(put);
		if (DEBUG) {
			System.out.println(">> Response Headers:");
			HttpUtils.printResponseHeaders(documentResponse);
		}
		
		boolean loginWasRequired = doRRCOAuth(documentResponse, login, password, httpClient, serverURI);
		//If we were not authenticated, we need to retry our request
		if(loginWasRequired){
			try {
				HttpPut put2 = (HttpPut) put.clone();
				return httpClient.execute(put2);
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		
		
		return  documentResponse;
	}
	
	public static HttpResponse sendPostForSecureDocumentQMCCM(String serverURI, HttpPost post, String login, String password,
			HttpClient httpClient) throws Exception {
		return sendPostForSecureDocumentQMCCM(serverURI, post, login, password, httpClient, 201);
	}
	
	public static HttpResponse sendPostForSecureDocumentQMCCM(String serverURI, HttpPost post, String login, String password,
			HttpClient httpClient, int expectedResponse) throws Exception {

		if (DEBUG)
			System.out.println(">> POST(1) " + post.getURI());
		HttpResponse documentResponse = httpClient.execute(post);
		if (DEBUG) {
			System.out.println(">> Response Headers:");
			HttpUtils.printResponseHeaders(documentResponse);
		}

		if (documentResponse.getStatusLine().getStatusCode() != expectedResponse) {
			throw new Exception("Error occured while posting\n" + documentResponse.getStatusLine().toString());
		}
		return documentResponse;
	}
	
	public static HttpResponse sendGetForSecureDocumentJTS(String serverURI, HttpGet request, String login, String password, HttpClient httpClient, String jtsURI)
			throws IOException, InvalidCredentialsException {
		
		// Step (1): Request the protected resource
		if (DEBUG) System.out.println(">> GET(1) "+request.getURI());
		HttpResponse documentResponse = httpClient.execute(request);
		if (DEBUG) {
			System.out.println(">> Response Headers:");
			HttpUtils.printResponseHeaders(documentResponse);
		}
		
		boolean loginWasRequired = doRRCOAuth(documentResponse, login, password, httpClient, jtsURI);
		//If we were not authenticated, we need to retry our request
		if(loginWasRequired){
				HttpGet documentGet2;
				try {
					documentGet2 = (HttpGet)(request.clone());
					return httpClient.execute(documentGet2);
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
		}
		return documentResponse;
	}
	
	/**
	 * Update a Document protected by a Form based authentication
	 * 
	 * @param serverURI			- Server URI
	 * @param protectedResource	- Absolute path to the protected document
	 * @param login				- Server login
	 * @param password			- Server password
	 * @param httpClient		- HttpClient used for the connection
	 * @param verbose			- if true, trace all server interactions
	 * @return					- HttpResponse
	 * 
	 * @throws IOException
	 * @throws InvalidCredentialsException
	 */
	public static HttpResponse sendPutForSecureDocumentJTS(String serverURI, HttpPut put, String login, String password, HttpClient httpClient, String jtsURI)
			throws IOException, InvalidCredentialsException {
		
		// Step (1): Request the protected resource
		if (DEBUG) System.out.println(">> PUT(1) "+put.getURI());
		HttpResponse documentResponse = httpClient.execute(put);
		if (DEBUG) {
			System.out.println(">> Response Headers:");
			HttpUtils.printResponseHeaders(documentResponse);
		}
		
		boolean loginWasRequired = doRRCOAuth(documentResponse, login, password, httpClient, jtsURI);
		//If we were not authenticated, we need to retry our request
		if(loginWasRequired){
			try {
				HttpPut put2 = (HttpPut) put.clone();
				return httpClient.execute(put2);
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		
		
		return  documentResponse;
	}
	
	public static HttpResponse sendPostForSecureDocumentJTS(String serverURI, HttpPost post, String login, String password,
			HttpClient httpClient) throws Exception {
		return sendPostForSecureDocumentQMCCM(serverURI, post, login, password, httpClient, 201);
	}
	
	public static HttpResponse sendPostForSecureDocumentJTS(String serverURI, HttpPost post, String login, String password,
			HttpClient httpClient, int expectedResponse) throws Exception {

		if (DEBUG)
			System.out.println(">> POST(1) " + post.getURI());
		HttpResponse documentResponse = httpClient.execute(post);
		if (DEBUG) {
			System.out.println(">> Response Headers:");
			HttpUtils.printResponseHeaders(documentResponse);
		}

		if (documentResponse.getStatusLine().getStatusCode() != expectedResponse) {
			throw new Exception("Error occured while posting\n" + documentResponse.getStatusLine().toString());
		}
		return documentResponse;
	}

	public static void printRequest(HttpGet request) {
		System.out.println("\t- Method: " + request.getMethod());
		System.out.println("\t- URL: " + request.getURI());
		System.out.println("\t- Headers: ");
		Header[] headers = request.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			System.out.println("\t\t- " + headers[i].getName() + ": " + headers[i].getValue());
		}
	}
	   
	private static Map<String, String> getQueryMap(String query) {
		Map<String, String> map = new HashMap<String, String>();
		String[] params = query.split("&"); //$NON-NLS-1$

		for (String param : params) {
		String name = param.split("=")[0]; //$NON-NLS-1$
		String value = param.split("=")[1]; //$NON-NLS-1$
		map.put(name, value);
		}

		return map;
	} 
	
    public static DocumentBuilder getDocumentParser() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware( true );
            dbf.setValidating( false );
            return dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new Error(e);
        }
    }
}
