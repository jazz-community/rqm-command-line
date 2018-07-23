package com.cli.qm.connection;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpResponse;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.omg.CORBA.TCKind;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class TestManager {
	
	public static void createTestCaseExecutionRecord(CLMConfig clm, String projectName,int testCaseId, int testScriptId, int testPlanId, String tcerName, String tcerDescription, String ownerUserId, String priorityName, int weight)
	{
		HttpUtils.DEBUG=false;
		
		if (clm==null)
		{
			System.out.println("AQM status ... [ERR]");
			System.out.println("Aqm was not able to read repo configuration file.");
			System.out.println("Please remove existing repo configuration and create new one using register-config command.");
			return;
		}

		
		String QM_server = clm.getQmURL();	
		String JTS_server = clm.getJtsURL();
		
		
		String projectId = getQMProjectId(clm, projectName);
		
		System.out.println("Connecting to ... "+QM_server);
		
		if (projectId==null)
		{
			System.out.println("Project area " + projectName+ " does not exist.");
			return;
		}
		

		
	
		
		String postUrl = QM_server+"/oslc_qm/contexts/"+projectId+"/resources/com.ibm.rqm.execution.TestcaseExecutionRecord";
				
	
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		
		String rootServices = QM_server + "/whoami";
		
		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, 15000);
	    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
	    // Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		
		try {
			// Request the Root Services document
			String userURL = null;
			if (ownerUserId!=null)
			{
			
				userURL = TestManager.getUserIdURL(clm, ownerUserId);
				if (userURL==null){
			
					System.out.println("User Id " + ownerUserId+ " does not exist.");
					return;
				}
			}	
			if (priorityName!=null)
			{
				String priorityNameTmp=getPriorityURL(clm, URLEncoder.encode(projectName, "UTF-8"), priorityName);
				if (priorityNameTmp==null)
				{
					System.out.println("Priority " + priorityName+ " does not exist in project "+projectName+".");
					return;
				}
				priorityName=priorityNameTmp;
			}
			
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_server);
			
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
				rootServicesResponse.getEntity().consumeContent();
				String xJazzCsrfPrevent = null;
				
				for (Cookie cookie : httpclient.getCookieStore().getCookies())
				{
					if (cookie.getName().equals("JSESSIONID"))
					{
						xJazzCsrfPrevent = cookie.getValue();
					}
				}
				
				String tcURL = getTestCaseURLOSLC(clm, projectName, testCaseId);
				
				
				if (tcURL==null)
				{
					System.out.println("Test Case with identifier " + testCaseId+ " does not exist in project "+projectName+".");
					return;
				}
				
				
				String tsURL = null;
				if(testScriptId>0)
				{
					tsURL = getTestScriptURLOSLC(clm, projectName, testScriptId);
					if (tsURL==null)
					{
						System.out.println("Test Script with identifier " + testScriptId+ " does not exist in project "+projectName+".");
						return;
					}
				}
				
				String tpURL = null;
				if(testPlanId>0)
				{
					tpURL = getTestPlanURLOSLC(clm, projectName, testPlanId);
					if (tpURL==null)
					{
						System.out.println("Test Plan with identifier " + testPlanId+ " does not exist in project "+projectName+".");
						return;
					}
				}
				
				StringWriter sw = generateTestCaseExecutionRecordCreate(tcURL, tsURL,tpURL,tcerName,tcerDescription,userURL,priorityName,weight);
				
				HttpPost post = new HttpPost(postUrl);
				post.addHeader("Accept", "application/xml");
				post.addHeader("Content-Type", "application/rdf+xml");
				if (xJazzCsrfPrevent!=null)
				{
					post.addHeader("X-Jazz-CSRF-Prevent", xJazzCsrfPrevent);
				}
				post.setEntity(new StringEntity(sw.toString(),HTTP.UTF_8));
				
				HttpResponse putResponse = HttpUtils.sendPostForSecureDocumentQMCCM(QM_server, post, login, password, httpclient);
				if (putResponse.getStatusLine().getStatusCode()==201)
				{
					System.out.println("Test Case Execution Record for Test Case "+testCaseId+" was successfully created");
				}
				else {
					System.out.println("AQM ... [ERR]");
					System.out.println("AQM was not able to perform this feature.");
				}
			}
			else {
				System.out.println("AQM ... [ERR]");
				System.out.println("AQM was not able to perform this feature.");
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Test Case Execution Record in this configuration may already exists.");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	public static void modifyTestCaseExecutionRecord(CLMConfig clm, String projectName, int tcerId, int testScriptId, String tcerName, String tcerDescription, String ownerUserId, String priorityName, int weight)
	{
		HttpUtils.DEBUG=false;
		
		if (clm==null)
		{
			System.out.println("AQM status ... [ERR]");
			System.out.println("Aqm was not able to read repo configuration file.");
			System.out.println("Please remove existing repo configuration and create new one using register-config command.");
			return;
		}

		
		String QM_server = clm.getQmURL();	
		String JTS_server = clm.getJtsURL();
		
		
		String projectId = getQMProjectId(clm, projectName);
		
		System.out.println("Connecting to ... "+QM_server);
		
		if (projectId==null)
		{
			System.out.println("Project area " + projectName+ " does not exist.");
			return;
		}
		

		
	
		
		String postUrl = QM_server+"/oslc_qm/contexts/"+projectId+"/resources/com.ibm.rqm.execution.TestcaseExecutionRecord";
				
	
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		
		String rootServices = QM_server + "/whoami";
		
		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, 15000);
	    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
	    // Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		
		try {
			// Request the Root Services document
						
			String userURL = null;
			if (ownerUserId!=null)
			{
			
				userURL = TestManager.getUserIdURL(clm, ownerUserId);
				if (userURL==null){
			
					System.out.println("User Id " + ownerUserId+ " does not exist.");
					return;
				}
			}
			if (priorityName!=null)
			{
				String priorityNameTmp=getPriorityURL(clm, URLEncoder.encode(projectName, "UTF-8"), priorityName);
				if (priorityNameTmp==null)
				{
					System.out.println("Priority " + priorityName+ " does not exist in project "+projectName+".");
					return;
				}
				priorityName=priorityNameTmp;
			}
			
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_server);
			
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
				rootServicesResponse.getEntity().consumeContent();
				String xJazzCsrfPrevent = null;
				
				for (Cookie cookie : httpclient.getCookieStore().getCookies())
				{
					if (cookie.getName().equals("JSESSIONID"))
					{
						xJazzCsrfPrevent = cookie.getValue();
					}
				}
				
				
				String tcerURL = getTestCaseExecutionRecordURLOSLC(clm, projectName, tcerId);
				
				
				if (tcerURL==null)
				{
					System.out.println("Test Case Execution Record with identifier " + tcerId+ " does not exist in project "+projectName+".");
					return;
				}
				
				String tsURL = null;
				if(testScriptId>0)
				{
					tsURL = getTestScriptURLOSLC(clm, projectName, testScriptId);
					if (tsURL==null)
					{
						System.out.println("Test Script with identifier " + testScriptId+ " does not exist in project "+projectName+".");
						return;
					}
				}
				
				StringWriter sw = generateTestCaseExecutionRecordModify(tcerURL,tsURL, tcerName, tcerDescription, userURL, priorityName, weight);
				
				HttpPut put = new HttpPut(tcerURL);
				put.addHeader("Accept", "application/xml");
				put.addHeader("Content-Type", "application/rdf+xml");
				if (xJazzCsrfPrevent!=null)
				{
					put.addHeader("X-Jazz-CSRF-Prevent", xJazzCsrfPrevent);
				}
				put.setEntity(new StringEntity(sw.toString(),HTTP.UTF_8));
				
				HttpResponse putResponse = HttpUtils.sendPutForSecureDocumentQMCCM(QM_server, put, login, password, httpclient,JTS_server);
				if (putResponse.getStatusLine().getStatusCode()==200)
				{
					System.out.println("Test Case Execution Record with identifier "+tcerId+" was successfully modified");
				}
				else {
					System.out.println("AQM ... [ERR]");
					System.out.println("AQM was not able to perform this feature.");
				}
			}
			else {
				System.out.println("AQM ... [ERR]");
				System.out.println("AQM was not able to perform this feature.");
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Test Case Execution Record in this configuration may already exists.");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	
	public static void associateWorkItemToTestCase(CLMConfig clm, int ccmId, String qmProjectName, int tcId)
	{
		HttpUtils.DEBUG=false;
		
		if (clm==null)
		{
			System.out.println("AQM status ... [ERR]");
			System.out.println("Aqm was not able to read repo configuration file.");
			System.out.println("Please remove existing repo configuration and create new one using register-config command.");
			return;
		}

		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		
		String rootServices = QM_server + "/whoami";
		
		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, 15000);
	    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
	    // Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		
		try {
			// Request the Root Services document
			System.out.println("Connecting to ... "+QM_server);
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
			
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
				rootServicesResponse.getEntity().consumeContent();
				String xJazzCsrfPrevent = null;
				
				for (Cookie cookie : httpclient.getCookieStore().getCookies())
				{
					if (cookie.getName().equals("JSESSIONID"))
					{
						xJazzCsrfPrevent = cookie.getValue();
					}
				}
				
				String cURL = getWorkItemURL(clm, ccmId);
				String tcURL = getTestCaseURLOSLC(clm, qmProjectName, tcId);
				
				if (cURL==null)
				{
					System.out.println("Work item with identifier " + ccmId+ " does not exist.");
					return;
				}
				
				if (tcURL==null)
				{
					System.out.println("Test Case with identifier " + tcId+ " does not exist in project "+qmProjectName+".");
					return;
				}
				
				List<String> linki = getTestCaseLinksOSLC(clm, tcURL);
				linki.add(cURL);
				StringWriter sw = generateTestCaseLinksModify(clm, tcURL, linki);
				
				HttpPut put = new HttpPut(tcURL);
				put.addHeader("Accept", "application/xml");
				put.addHeader("Content-Type", "application/rdf+xml");
				if (xJazzCsrfPrevent!=null)
				{
					put.addHeader("X-Jazz-CSRF-Prevent", xJazzCsrfPrevent);
				}
				put.setEntity(new StringEntity(sw.toString(),HTTP.UTF_8));
				
				addTestCaseToWorkItem(clm, cURL, tcURL);
				
				HttpResponse putResponse = HttpUtils.sendPutForSecureDocumentQMCCM(QM_server, put, login, password, httpclient,JTS_Server);
				if (putResponse.getStatusLine().getStatusCode()==200)
				{
					System.out.println("Work Item "+ccmId+ " successfully associated with test case "+tcId );
				}
				else {
					System.out.println("AQM ... [ERR]");
					System.out.println("AQM was not able to perform this feature.");
				}

				

			}
			else {
				System.out.println("AQM ... [ERR]");
				System.out.println("AQM was not able to perform this feature.");
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	public static void associateRequirementToTestCase(CLMConfig clm, String rmProjectName, int rmId, String qmProjectName, int tcId)
	{
		HttpUtils.DEBUG=false;
		
		if (clm==null)
		{
			System.out.println("AQM status ... [ERR]");
			System.out.println("Aqm was not able to read repo configuration file.");
			System.out.println("Please remove existing repo configuration and create new one using register-config command.");
			return;
		}

		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		
		String rootServices = QM_server + "/whoami";
		
		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, 15000);
	    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
	    // Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		
		try {
			// Request the Root Services document
			System.out.println("Connecting to ... "+QM_server);
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
			
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
				rootServicesResponse.getEntity().consumeContent();
				String xJazzCsrfPrevent = null;
				
				for (Cookie cookie : httpclient.getCookieStore().getCookies())
				{
					if (cookie.getName().equals("JSESSIONID"))
					{
						xJazzCsrfPrevent = cookie.getValue();
					}
				}
				String rURL = getRequirementURL(clm, rmProjectName, rmId);
				String tcURL = getTestCaseURLOSLC(clm, qmProjectName, tcId);
				if (rURL==null)
				{
					System.out.println("Requirement with identifier " + rmId+ " does not exist in project "+rmProjectName+".");
					return;
				}
				if (tcURL==null)
				{
					System.out.println("Test Case with identifier " + tcId+ " does not exist in project "+qmProjectName+".");
					return;
				}
				List<String> linki = getTestCaseLinksOSLC(clm, tcURL);
				linki.add(rURL);
				StringWriter sw = generateTestCaseLinksModify(clm, tcURL, linki);
				
				HttpPut put = new HttpPut(tcURL);
				put.addHeader("Accept", "application/xml");
				put.addHeader("Content-Type", "application/rdf+xml");
				if (xJazzCsrfPrevent!=null)
				{
					put.addHeader("X-Jazz-CSRF-Prevent", xJazzCsrfPrevent);
				}
				put.setEntity(new StringEntity(sw.toString(),HTTP.UTF_8));

				addTestCaseToRequirement(clm, rURL, tcURL);
				
				HttpResponse putResponse = HttpUtils.sendPutForSecureDocumentQMCCM(QM_server, put, login, password, httpclient,JTS_Server);
				if (putResponse.getStatusLine().getStatusCode()==200)
				{
					System.out.println("Requirement "+rmId+ " successfully associated with test case "+tcId );
				}
				else {
					System.out.println("AQM ... [ERR]");
					System.out.println("AQM was not able to perform this feature.");
				}

			}
			else {
				System.out.println("AQM ... [ERR]");
				System.out.println("AQM was not able to perform this feature.");
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	public static void associateRequirementToWorkItem(CLMConfig clm, String rmProjectName, int rmId, String wiProjectName, int wiId)
	{
		HttpUtils.DEBUG=false;
		
		if (clm==null)
		{
			System.out.println("AQM status ... [ERR]");
			System.out.println("Aqm was not able to read repo configuration file.");
			System.out.println("Please remove existing repo configuration and create new one using register-config command.");
			return;
		}

		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		
		String rootServices = QM_server + "/whoami";
		
		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, 15000);
	    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
	    // Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		
		try {
			// Request the Root Services document
			System.out.println("Connecting to ... "+QM_server);
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
			
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
				rootServicesResponse.getEntity().consumeContent();
				String xJazzCsrfPrevent = null;
				
				for (Cookie cookie : httpclient.getCookieStore().getCookies())
				{
					if (cookie.getName().equals("JSESSIONID"))
					{
						xJazzCsrfPrevent = cookie.getValue();
					}
				}
				String rURL = getRequirementURL(clm, rmProjectName, rmId);
				String wiURL = getWorkItemURL(clm, wiId);
				if (rURL==null)
				{
					System.out.println("Requirement with identifier " + rmId+ " does not exist in project "+rmProjectName+".");
					return;
				}
				if (wiURL==null)
				{
					System.out.println("Work item with identifier " + wiId+ " does not exist in project "+wiProjectName+".");
					return;
				}
				addWorkItemToRequirement(clm, rURL, wiURL);
				addRequirementToWorkItem(clm, wiURL, rURL);
				
			}
			System.out.println("Requirement "+rmId+ " successfully associated with work item "+wiId );
			rootServicesResponse.getEntity().consumeContent();
			
		} catch (ClientProtocolException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	
    private static URI appendUri(String uri, String appendQuery) throws URISyntaxException {
        URI oldUri = new URI(uri);

        String newQuery = oldUri.getQuery();
        if (newQuery == null) {
            newQuery = appendQuery;
        } else {
            newQuery += "&" + appendQuery;  
        }

        URI newUri = new URI(oldUri.getScheme(), oldUri.getAuthority(),
                oldUri.getPath(), newQuery, oldUri.getFragment());

        return newUri;
    }
	
      
    private static List<String> getTestCaseLinksOSLC(CLMConfig clm, String testCaseUrl)
   	{
   		List<String> lista = new ArrayList<String>();
   		
   		String catalogXPath = "//oslc_qm:validatesRequirement/@rdf:resource|//oslc_qm:testsChangeRequest/@rdf:resource|//oslc_qm:usesTestScript/@rdf:resource";

   		if (clm==null || testCaseUrl==null)
   		{
   			return lista;
   		}
   		else
   		{
   			HttpUtils.DEBUG=false;
   			String QM_server = clm.getQmURL();		
   			String JTS_Server = clm.getJtsURL();
   			String login = clm.getUsername();			 
   			String password = clm.getPassword();	
   			
   			final HttpParams httpParams = new BasicHttpParams();
   			HttpConnectionParams.setSoTimeout(httpParams, 15000);
   			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
   			// Setup the HttClient
   			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
   			HttpUtils.setupLazySSLSupport(httpclient);
   			
   			try {
   			
   						
   			String rootServices = testCaseUrl;
   			
   					
   			// Setup the rootServices request
   			HttpGet rootServiceDoc = new HttpGet(rootServices);
   			rootServiceDoc.addHeader("Accept", "application/xml");
   			rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
   			rootServiceDoc.addHeader("Content-Type", "application/rdf+xml");
   			
   			
   				// Request the Root Services document
   				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
   				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
   					XPathFactory factory = XPathFactory.newInstance();
   					XPath xpath2 = factory.newXPath();
   					xpath2.setNamespaceContext(
   							new NamespaceContextMap(new String[]
   									{	"oslc", "http://open-services.net/ns/core#",
   										"oslc_qm","http://open-services.net/ns/qm#",
   										"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
   										"dcterms","http://purl.org/dc/terms/"}));

   					// Parse the response body to retrieve the Service Provider
   					InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());
   					NodeList nodeTmp = (NodeList) (xpath2.evaluate(catalogXPath, source, XPathConstants.NODESET));
   					if (nodeTmp!=null)
   					{
   						for (int i = 0; i<nodeTmp.getLength();i++)
   						{
   							lista.add(nodeTmp.item(i).getTextContent());
   						}
   						return lista;
   					}
   					else{
   						return lista;
   					}
   				}
   				else
   				{
   					return lista;
   				}
   				}
   			catch (ClientProtocolException e) {
   				System.out.println("AQM ... [ERR]");
   				System.out.println("Not able to connect to RM server due to connectivity error.");
   				
   				return lista;
   			} catch (IOException e) {
   				System.out.println("AQM ... [ERR]");
   				System.out.println("Not able to connect to RM server due to connectivity error.");
   				
   				return lista;
   			} catch (InvalidCredentialsException e) {
   				System.out.println("AQM ... [ERR]");
   				System.out.println("Not able to connect to RM server due to authentication error");
   				
   				return lista;
   			} catch (Exception e) {
   				System.out.println("AQM ... [ERR]");
   				System.out.println("Not able to connect to RM server due to connectivity error.");
   				
   				return lista;
   			}  
   			finally {
   				httpclient.getConnectionManager().shutdown();
   			}
   		}
   	}
    
   
    private static String getTestCaseURLOSLC(CLMConfig clm, String projectName, int id)
	{
		
		String catalogXPath = "//oslc_qm:TestCase/@rdf:about";
		
		if (clm==null || id <= 0 || projectName==null)
		{
			return null;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = clm.getQmURL();		
			String JTS_Server = clm.getJtsURL();
			String login = clm.getUsername();			 
			String password = clm.getPassword();
			
			String projectAreaId = getQMProjectId(clm, projectName);
			
			if (projectAreaId==null)
			{
				System.out.println("Project area " + projectName+ " does not exist.");
				return null;
			}
		
			
			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
			
			try {
			
						
			String rootServices = QM_server + "/oslc_qm/contexts/"+projectAreaId+"/resources/com.ibm.rqm.planning.VersionedTestCase";//+id;
			rootServices=appendUri(rootServices, "oslc.where=oslc:shortId="+id).toString();
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
			rootServiceDoc.addHeader("Content-Type", "application/rdf+xml");
			
			
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
					XPathFactory factory = XPathFactory.newInstance();
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"oslc_qm","http://open-services.net/ns/qm#",
										"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());
					Node nodeTmp = (Node) (xpath2.evaluate(catalogXPath, source, XPathConstants.NODE));
					if (nodeTmp!=null)
					{
						return nodeTmp.getTextContent();	
					}
					else{
						return null;
					}
				}
				else
				{
					return null;
				}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
		}
	}
    private static String getTestPlanURLOSLC(CLMConfig clm, String projectName, int id)
	{
		
		String catalogXPath = "//oslc_qm:TestPlan/@rdf:about";
		
		if (clm==null || id <= 0 || projectName==null)
		{
			return null;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = clm.getQmURL();		
			String JTS_Server = clm.getJtsURL();
			String login = clm.getUsername();			 
			String password = clm.getPassword();
			
			String projectAreaId = getQMProjectId(clm, projectName);
			
			if (projectAreaId==null)
			{
				System.out.println("Project area " + projectName+ " does not exist.");
				return null;
			}
		
			
			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
			
			try {
			
						
			String rootServices = QM_server + "/oslc_qm/contexts/"+projectAreaId+"/resources/com.ibm.rqm.planning.VersionedTestPlan";//+id;
			rootServices=appendUri(rootServices, "oslc.where=oslc:shortId="+id).toString();
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
			rootServiceDoc.addHeader("Content-Type", "application/rdf+xml");
			
			
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
					XPathFactory factory = XPathFactory.newInstance();
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"oslc_qm","http://open-services.net/ns/qm#",
										"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());
					Node nodeTmp = (Node) (xpath2.evaluate(catalogXPath, source, XPathConstants.NODE));
					if (nodeTmp!=null)
					{
						return nodeTmp.getTextContent();	
					}
					else{
						return null;
					}
				}
				else
				{
					return null;
				}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
		}
	}
    
    private static String getTestScriptURLOSLC(CLMConfig clm, String projectName, int id)
	{
		
		String catalogXPath = "//oslc_qm:TestScript/@rdf:about";
		
		if (clm==null || id <= 0 || projectName==null)
		{
			return null;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = clm.getQmURL();		
			String JTS_Server = clm.getJtsURL();
			String login = clm.getUsername();			 
			String password = clm.getPassword();
			
			String projectAreaId = getQMProjectId(clm, projectName);
			
			if (projectAreaId==null)
			{
				System.out.println("Project area " + projectName+ " does not exist.");
				return null;
			}
		
			
			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
			
			try {
			
						
			String rootServices = QM_server + "/oslc_qm/contexts/"+projectAreaId+"/resources/com.ibm.rqm.planning.VersionedExecutionScript";//+id;
			rootServices=appendUri(rootServices, "oslc.where=oslc:shortId="+id).toString();
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
			rootServiceDoc.addHeader("Content-Type", "application/rdf+xml");
			
			
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
					XPathFactory factory = XPathFactory.newInstance();
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"oslc_qm","http://open-services.net/ns/qm#",
										"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());
					Node nodeTmp = (Node) (xpath2.evaluate(catalogXPath, source, XPathConstants.NODE));
					if (nodeTmp!=null)
					{
						return nodeTmp.getTextContent();	
					}
					else{
						return null;
					}
				}
				else
				{
					return null;
				}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
		}
	}
    
    private static String getTestCaseExecutionRecordURLOSLC(CLMConfig clm, String projectName, int id)
   	{
   		
   		String catalogXPath = "//oslc_qm:TestExecutionRecord/@rdf:about";
   		
   		if (clm==null || id <= 0 || projectName==null)
   		{
   			return null;
   		}
   		else
   		{
   			HttpUtils.DEBUG=false;
   			String QM_server = clm.getQmURL();		
   			String JTS_Server = clm.getJtsURL();
   			String login = clm.getUsername();			 
   			String password = clm.getPassword();
   			
   			String projectAreaId = getQMProjectId(clm, projectName);
   			
   			if (projectAreaId==null)
   			{
   				System.out.println("Project area " + projectName+ " does not exist.");
   				return null;
   			}
   		
   			
   			final HttpParams httpParams = new BasicHttpParams();
   			HttpConnectionParams.setSoTimeout(httpParams, 15000);
   			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
   			// Setup the HttClient
   			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
   			HttpUtils.setupLazySSLSupport(httpclient);
   			
   			try {
   			
   						
   			String rootServices = QM_server + "/oslc_qm/contexts/"+projectAreaId+"/resources/com.ibm.rqm.execution.TestcaseExecutionRecord";//+id;
   			rootServices=appendUri(rootServices, "oslc.where=oslc:shortId="+id).toString();
   					
   			// Setup the rootServices request
   			HttpGet rootServiceDoc = new HttpGet(rootServices);
   			rootServiceDoc.addHeader("Accept", "application/xml");
   			rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
   			rootServiceDoc.addHeader("Content-Type", "application/rdf+xml");
   			
   			
   				// Request the Root Services document
   				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
   				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
   					XPathFactory factory = XPathFactory.newInstance();
   					XPath xpath2 = factory.newXPath();
   					xpath2.setNamespaceContext(
   							new NamespaceContextMap(new String[]
   									{	"oslc", "http://open-services.net/ns/core#",
   										"oslc_qm","http://open-services.net/ns/qm#",
   										"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
   										"dcterms","http://purl.org/dc/terms/"}));

   					// Parse the response body to retrieve the Service Provider
   					InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());
   					Node nodeTmp = (Node) (xpath2.evaluate(catalogXPath, source, XPathConstants.NODE));
   					if (nodeTmp!=null)
   					{
   						return nodeTmp.getTextContent();	
   					}
   					else{
   						return null;
   					}
   				}
   				else
   				{
   					return null;
   				}
   				}
   			catch (ClientProtocolException e) {
   				System.out.println("AQM ... [ERR]");
   				System.out.println("Not able to connect to RM server due to connectivity error.");
   				
   				return null;
   			} catch (IOException e) {
   				System.out.println("AQM ... [ERR]");
   				System.out.println("Not able to connect to RM server due to connectivity error.");
   				
   				return null;
   			} catch (InvalidCredentialsException e) {
   				System.out.println("AQM ... [ERR]");
   				System.out.println("Not able to connect to RM server due to authentication error");
   				
   				return null;
   			} catch (Exception e) {
   				System.out.println("AQM ... [ERR]");
   				System.out.println("Not able to connect to RM server due to connectivity error.");
   				
   				return null;
   			}  
   			finally {
   				httpclient.getConnectionManager().shutdown();
   			}
   		}
   	}
    
	private static String getRequirementURL(CLMConfig clm, String projectName, int id)
	{
		
		String catalogXPath = "//oslc_rm:Requirement/@rdf:about";
		
		if (clm==null || id <= 0 || projectName==null)
		{
			return null;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String RM_server = clm.getRmURL();		
			String JTS_Server = clm.getJtsURL();
			String login = clm.getUsername();			 
			String password = clm.getPassword();

			String projectAreaId = getRMProjectId(clm, projectName);

			if (projectAreaId==null)
			{
				System.out.println("Project area " + projectName+ " does not exist.");
				return null;
			}
			
			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
			
			try {
			String projectString = URLEncoder.encode(clm.getRmURL()+"/process/project-areas/")+projectAreaId;
			
			
			//String query = "&oslc.prefix=dcterms=<http://purl.org/dc/terms/>&oslc.where=dcterms:identifier="+id;
			

			String rootServices = RM_server + "/views?"+"oslc.query=true&projectURL="+projectString;//+id;
			rootServices=appendUri(rootServices, "oslc.prefix=dcterms=<http://purl.org/dc/terms/>").toString();
			rootServices=appendUri(rootServices, "oslc.where=dcterms:identifier="+id).toString();
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
			rootServiceDoc.addHeader("Content-Type", "application/rdf+xml");
			
			
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentJTS(RM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
					XPathFactory factory = XPathFactory.newInstance();
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"oslc_rm","http://open-services.net/ns/rm#",
										"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());
					Node nodeTmp = (Node) (xpath2.evaluate(catalogXPath, source, XPathConstants.NODE));
					if (nodeTmp!=null)
					{
						return nodeTmp.getTextContent();	
					}
					else{
						return null;
					}
				}
				else
				{
					return null;
				}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to RM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
		}
	}
	
	private static String getWorkItemURL(CLMConfig clm, int id)
	{
		if (clm==null || id <= 0 )
		{
			return null;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String CCM_server = clm.getCcmURL();		
			String JTS_Server = clm.getJtsURL();
			String login = clm.getUsername();			 
			String password = clm.getPassword();
			String rootServices = CCM_server + "/resource/itemName/com.ibm.team.workitem.WorkItem/"+id;

			
			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(CCM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
					return rootServices;
				}
				else
				{
					return null;
				}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to CCM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to CCM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to CCM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to CCM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
		}
	}
	
	private static String getQMProjectId(CLMConfig clm, String projectName)
	{
		HttpUtils.DEBUG=false;
			
		String rootServices = clm.getQmURL() + "/rootservices";
		String catalogXPath = "/rdf:Description/oslc_qm:qmServiceProviders/@rdf:resource";
		String serviceProviderTitleXPath = "//oslc:ServiceProvider";
		
		// Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		
		try {
			// Request the Root Services document
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(clm.getQmURL(), rootServiceDoc, clm.getUsername(), clm.getPassword(), httpclient,clm.getJtsURL());
			
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
	    		// Define the XPath evaluation environment
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				xpath.setNamespaceContext(
						new NamespaceContextMap(new String[]
								{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
									"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"dc","http://purl.org/dc/terms/",
									"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
									"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
									"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
									"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
									"ju","http://jazz.net/ns/ui#",
									"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
									"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
									"qm_rqm","http://jazz.net/ns/qm/rqm#",
									"oslc","http://open-services.net/ns/core#",
									"trs","http://jazz.net/ns/trs#",
									"trs2","http://open-services.net/ns/core/trs#"
								}));

				// Parse the response body to retrieve the catalog URI
			
				InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());		
				Node attribute = (Node) (xpath.evaluate(catalogXPath, source, XPathConstants.NODE));
				String serviceProvidersCatalog = attribute.getTextContent();
				rootServicesResponse.getEntity().consumeContent();
				
				// Setup the catalog request
				HttpGet catalogDoc = new HttpGet(serviceProvidersCatalog);
				catalogDoc.addHeader("Accept", "application/xml");
				catalogDoc.addHeader("OSLC-Core-Version", "2.0");
				
				// Access to the Service Providers catalog

				HttpResponse catalogResponse = HttpUtils.sendGetForSecureDocumentQMCCM(clm.getQmURL(), catalogDoc, clm.getUsername(), clm.getPassword(), httpclient,clm.getJtsURL());
				if (catalogResponse.getStatusLine().getStatusCode() == 200) {
		    		// Define the XPath evaluation environment
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					source = new InputSource(catalogResponse.getEntity().getContent());
					NodeList titleNodes = (NodeList) (xpath2.evaluate(serviceProviderTitleXPath, source, XPathConstants.NODESET));
					
					// Print out the title of each Service Provider
					int length = titleNodes.getLength();
					for (int i = 0; i < length; i++) {
						Node tmpNode = titleNodes.item(i);
						if (tmpNode instanceof Element)
						{
							Element eleTmp = (Element)tmpNode;
							if (eleTmp.getElementsByTagName("dcterms:title").getLength()>0)
							{
								String str = eleTmp.getElementsByTagName("dcterms:title").item(0).getTextContent();
								if (str.equals(projectName))
								{
									String tmpStr = eleTmp.getAttribute("rdf:about"); 
									if (tmpStr!=null)
									{
										tmpStr = tmpStr.replaceAll(clm.getQmURL(), "").replace("/services.xml","").replace("/oslc_qm/contexts/", "");
										return tmpStr;
									}
								}
							
							}
						}
					}
					return null;
				}
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
		return null;
	}
	
	private static String getRMProjectId(CLMConfig clm, String projectName)
	{
		HttpUtils.DEBUG=false;
			
		String rootServices = clm.getRmURL() + "/rootservices";
		String catalogXPath = "/rdf:Description/oslc_rm:rmServiceProviders/@rdf:resource";
		String serviceProviderTitleXPath = "//oslc:ServiceProvider";
		
		// Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		try {
			// Request the Root Services document
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentJTS(clm.getRmURL(), rootServiceDoc, clm.getUsername(), clm.getPassword(), httpclient,clm.getJtsURL());
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
	    		// Define the XPath evaluation environment
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				xpath.setNamespaceContext(
						new NamespaceContextMap(new String[]
								{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
									"oslc_rm","http://open-services.net/xmlns/rm/1.0/",
									"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"dc","http://purl.org/dc/terms/",
									"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
									"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
									"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
									"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
									"ju","http://jazz.net/ns/ui#",
									"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
									"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
									"qm_rqm","http://jazz.net/ns/qm/rqm#",
									"oslc","http://open-services.net/ns/core#",
									"trs","http://jazz.net/ns/trs#",
									"trs2","http://open-services.net/ns/core/trs#"
								}));

				
				// Parse the response body to retrieve the catalog URI

				InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());

				Node attribute = (Node) (xpath.evaluate(catalogXPath, source, XPathConstants.NODE));

				String serviceProvidersCatalog = attribute.getTextContent();

				rootServicesResponse.getEntity().consumeContent();

				// Setup the catalog request
				HttpGet catalogDoc = new HttpGet(serviceProvidersCatalog);
				catalogDoc.addHeader("Accept", "application/xml");
				catalogDoc.addHeader("OSLC-Core-Version", "2.0");
				
				// Access to the Service Providers catalog
				HttpResponse catalogResponse = HttpUtils.sendGetForSecureDocumentJTS(clm.getQmURL(), catalogDoc, clm.getUsername(), clm.getPassword(), httpclient,clm.getJtsURL());
				if (catalogResponse.getStatusLine().getStatusCode() == 200) {
		    		// Define the XPath evaluation environment
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"oslc_rm","http://open-services.net/xmlns/rm/1.0/",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					source = new InputSource(catalogResponse.getEntity().getContent());
					NodeList titleNodes = (NodeList) (xpath2.evaluate(serviceProviderTitleXPath, source, XPathConstants.NODESET));
					// Print out the title of each Service Provider
					int length = titleNodes.getLength();
					for (int i = 0; i < length; i++) {
						Node tmpNode = titleNodes.item(i);
						if (tmpNode instanceof Element)
						{
							Element eleTmp = (Element)tmpNode;
							if (eleTmp.getElementsByTagName("dcterms:title").getLength()>0)
							{
								String str = eleTmp.getElementsByTagName("dcterms:title").item(0).getTextContent();
								if (str.equals(projectName))
								{
									String tmpStr = eleTmp.getAttribute("rdf:about"); 
									if (tmpStr!=null)
									{
										tmpStr = tmpStr.replaceAll(clm.getRmURL(), "").replace("/services.xml","").replace("/oslc_rm/", "");
										return tmpStr;
									}
								}
							
							}
						}
					}
					return null;
				}
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
		return null;
	}
	private static void addWorkItemToRequirement(CLMConfig clm, String requirementURL, String workItemUrl) throws Exception
	{
		HttpUtils.DEBUG=false;
		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		// Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(requirementURL);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
			// Request the Root Services document
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentJTS(clm.getRmURL(), rootServiceDoc, clm.getUsername(), clm.getPassword(), httpclient,clm.getJtsURL());
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
	    		// Define the XPath evaluation environment
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				xpath.setNamespaceContext(
						new NamespaceContextMap(new String[]
								{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
									"oslc_rm","http://open-services.net/xmlns/rm/1.0/",
									"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"dc","http://purl.org/dc/terms/",
									"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
									"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
									"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
									"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
									"ju","http://jazz.net/ns/ui#",
									"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
									"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
									"qm_rqm","http://jazz.net/ns/qm/rqm#",
									"oslc","http://open-services.net/ns/core#",
									"trs","http://jazz.net/ns/trs#",
									"trs2","http://open-services.net/ns/core/trs#"
								}));

				
				// Parse the response body to retrieve the catalog URI


				String etag = null;
				String xJazzCsrfPrevent = null;
				
				for (Cookie cookie : httpclient.getCookieStore().getCookies())
				{
					if (cookie.getName().equals("JSESSIONID"))
					{
						xJazzCsrfPrevent = cookie.getValue();
					}
				}
				
				if (rootServicesResponse.getFirstHeader("Etag")!=null)
				{
					etag = rootServicesResponse.getFirstHeader("Etag").getValue();
				}
				
				String test = HttpUtils.responseBodyToString(rootServicesResponse).toString();
				
				String[] output = test.split("</dcterms:identifier>");

				test = output[0]+"</dcterms:identifier>"+"<oslc_rm:implementedBy rdf:resource='"+workItemUrl+"'/>"+output[1];
				
		
				rootServicesResponse.getEntity().consumeContent();

				HttpPut put = new HttpPut(requirementURL);
				put.addHeader("Accept", "application/xml");
				put.addHeader("Content-Type", "application/rdf+xml");
				put.addHeader("OSLC-Core-Version", "2.0");
				
				if (xJazzCsrfPrevent!=null)
				{
					put.addHeader("X-Jazz-CSRF-Prevent", xJazzCsrfPrevent);
				}
				if (etag!=null)
				{
					put.addHeader("If-Match", etag);
				}
		
				put.setEntity(new StringEntity(test.toString(),HTTP.UTF_8));


				HttpResponse putResponse = HttpUtils.sendPutForSecureDocumentJTS(clm.getRmURL(), put, login, password, httpclient, JTS_Server);
				if (putResponse.getStatusLine().getStatusCode()!=200)
				{
					throw new Exception();
				}
				}
	}
	private static void addTestCaseToRequirement(CLMConfig clm, String requirementURL, String testCaseUrl) throws Exception
	{
		HttpUtils.DEBUG=false;
		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		// Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(requirementURL);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
			// Request the Root Services document
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentJTS(clm.getRmURL(), rootServiceDoc, clm.getUsername(), clm.getPassword(), httpclient,clm.getJtsURL());
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
	    		// Define the XPath evaluation environment
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				xpath.setNamespaceContext(
						new NamespaceContextMap(new String[]
								{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
									"oslc_rm","http://open-services.net/xmlns/rm/1.0/",
									"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"dc","http://purl.org/dc/terms/",
									"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
									"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
									"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
									"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
									"ju","http://jazz.net/ns/ui#",
									"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
									"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
									"qm_rqm","http://jazz.net/ns/qm/rqm#",
									"oslc","http://open-services.net/ns/core#",
									"trs","http://jazz.net/ns/trs#",
									"trs2","http://open-services.net/ns/core/trs#"
								}));

				
				// Parse the response body to retrieve the catalog URI


				String etag = null;
				String xJazzCsrfPrevent = null;
				
				for (Cookie cookie : httpclient.getCookieStore().getCookies())
				{
					if (cookie.getName().equals("JSESSIONID"))
					{
						xJazzCsrfPrevent = cookie.getValue();
					}
				}
				
				if (rootServicesResponse.getFirstHeader("Etag")!=null)
				{
					etag = rootServicesResponse.getFirstHeader("Etag").getValue();
				}
				
				String test = HttpUtils.responseBodyToString(rootServicesResponse).toString();
				
				String[] output = test.split("</dcterms:identifier>");

				test = output[0]+"</dcterms:identifier>"+"<oslc_rm:validatedBy rdf:resource='"+testCaseUrl+"'/>"+output[1];
				
		
				rootServicesResponse.getEntity().consumeContent();

				HttpPut put = new HttpPut(requirementURL);
				put.addHeader("Accept", "application/xml");
				put.addHeader("Content-Type", "application/rdf+xml");
				put.addHeader("OSLC-Core-Version", "2.0");
				
				if (xJazzCsrfPrevent!=null)
				{
					put.addHeader("X-Jazz-CSRF-Prevent", xJazzCsrfPrevent);
				}
				if (etag!=null)
				{
					put.addHeader("If-Match", etag);
				}
		
				put.setEntity(new StringEntity(test.toString(),HTTP.UTF_8));


				HttpResponse putResponse = HttpUtils.sendPutForSecureDocumentJTS(clm.getRmURL(), put, login, password, httpclient, JTS_Server);
				if (putResponse.getStatusLine().getStatusCode()!=200)
				{
					throw new Exception();
				}
				}
	}
	
	private static void addTestCaseToWorkItem(CLMConfig clm, String workItemURL, String testCaseUrl) throws Exception
	{
		HttpUtils.DEBUG=false;
		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		// Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(workItemURL);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
			// Request the Root Services document
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(clm.getCcmURL(), rootServiceDoc, clm.getUsername(), clm.getPassword(), httpclient,clm.getJtsURL());
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
	    		// Define the XPath evaluation environment
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				xpath.setNamespaceContext(
						new NamespaceContextMap(new String[]
								{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
									"oslc_rm","http://open-services.net/xmlns/rm/1.0/",
									"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"dc","http://purl.org/dc/terms/",
									"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
									"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
									"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
									"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
									"ju","http://jazz.net/ns/ui#",
									"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
									"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
									"qm_rqm","http://jazz.net/ns/qm/rqm#",
									"oslc","http://open-services.net/ns/core#",
									"trs","http://jazz.net/ns/trs#",
									"trs2","http://open-services.net/ns/core/trs#"
								}));

				
				// Parse the response body to retrieve the catalog URI

			
				
				String test = HttpUtils.responseBodyToString(rootServicesResponse).toString();
				
				String[] output = test.split("</dcterms:identifier>");

				test = output[0]+"</dcterms:identifier>"+"<oslc_cm:relatedTestCase rdf:resource='"+testCaseUrl+"'/>"+output[1];
				
		
				rootServicesResponse.getEntity().consumeContent();

				HttpPut put = new HttpPut(workItemURL);
				put.addHeader("Accept", "application/xml");
				put.addHeader("Content-Type", "application/rdf+xml");
				put.addHeader("OSLC-Core-Version", "2.0");

		
				put.setEntity(new StringEntity(test.toString(),HTTP.UTF_8));


				HttpResponse putResponse = HttpUtils.sendPutForSecureDocumentQMCCM(clm.getCcmURL(), put, login, password, httpclient, JTS_Server);
				if (putResponse.getStatusLine().getStatusCode()!=200)
				{
					throw new Exception();
				}
				}
	}
	
	private static void addRequirementToWorkItem(CLMConfig clm, String workItemURL, String requirementUrl) throws Exception
	{
		HttpUtils.DEBUG=false;
		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		// Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(workItemURL);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
			// Request the Root Services document
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(clm.getCcmURL(), rootServiceDoc, clm.getUsername(), clm.getPassword(), httpclient,clm.getJtsURL());
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
	    		// Define the XPath evaluation environment
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				xpath.setNamespaceContext(
						new NamespaceContextMap(new String[]
								{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
									"oslc_rm","http://open-services.net/xmlns/rm/1.0/",
									"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"dc","http://purl.org/dc/terms/",
									"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
									"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
									"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
									"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
									"ju","http://jazz.net/ns/ui#",
									"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
									"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
									"qm_rqm","http://jazz.net/ns/qm/rqm#",
									"oslc","http://open-services.net/ns/core#",
									"trs","http://jazz.net/ns/trs#",
									"trs2","http://open-services.net/ns/core/trs#",
									"calm","http://jazz.net/xmlns/prod/jazz/calm/1.0/"
								}));

				
				// Parse the response body to retrieve the catalog URI

			
				
				String test = HttpUtils.responseBodyToString(rootServicesResponse).toString();
				
				String[] output = test.split("</dcterms:identifier>");

				test = output[0]+"</dcterms:identifier>"+"<oslc_cm:implementsRequirement rdf:resource='"+requirementUrl+"'/>"+output[1];
				
		
				rootServicesResponse.getEntity().consumeContent();

				HttpPut put = new HttpPut(workItemURL);
				put.addHeader("Accept", "application/xml");
				put.addHeader("Content-Type", "application/rdf+xml");
				put.addHeader("OSLC-Core-Version", "2.0");

		
				put.setEntity(new StringEntity(test.toString(),HTTP.UTF_8));


				HttpResponse putResponse = HttpUtils.sendPutForSecureDocumentQMCCM(clm.getCcmURL(), put, login, password, httpclient, JTS_Server);
				if (putResponse.getStatusLine().getStatusCode()!=200)
				{
					System.out.println(putResponse.getStatusLine().getStatusCode());
					throw new Exception();
				}
				}
	}
	public static void createTestScript(CLMConfig clm, String projectName, String testCaseName, String testCaseDescription, String ownerUserId, String stateName, String testScritpTemplateName)
	{
		HttpUtils.DEBUG=false;
		
		if (clm==null)
		{
			System.out.println("AQM status ... [ERR]");
			System.out.println("Aqm was not able to read repo configuration file.");
			System.out.println("Please remove existing repo configuration and create new one using register-config command.");
			return;
		}

		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		
		String rootServices = QM_server + "/rootservices";
		String serviceProviderTitleXPath = "//oslc:ServiceProvider/dcterms:title";
		String catalogXPath = "/rdf:Description/oslc_qm:qmServiceProviders/@rdf:resource";
		
		String encodedPRojectAreaName = null;

		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, 15000);
	    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
	    // Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		
		try {
			// Request the Root Services document
			System.out.println("Connecting to ... "+QM_server);
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
			
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
	    		// Define the XPath evaluation environment
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				xpath.setNamespaceContext(
						new NamespaceContextMap(new String[]
								{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
									"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"dc","http://purl.org/dc/terms/",
									"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
									"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
									"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
									"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
									"ju","http://jazz.net/ns/ui#",
									"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
									"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
									"qm_rqm","http://jazz.net/ns/qm/rqm#",
									"oslc","http://open-services.net/ns/core#",
									"trs","http://jazz.net/ns/trs#",
									"trs2","http://open-services.net/ns/core/trs#"
								}));

				// Parse the response body to retrieve the catalog URI
			
				InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());		
				Node attribute = (Node) (xpath.evaluate(catalogXPath, source, XPathConstants.NODE));
				String serviceProvidersCatalog = attribute.getTextContent();
				rootServicesResponse.getEntity().consumeContent();
				
				// Setup the catalog request
				HttpGet catalogDoc = new HttpGet(serviceProvidersCatalog);
				catalogDoc.addHeader("Accept", "application/xml");
				catalogDoc.addHeader("OSLC-Core-Version", "2.0");
				
				// Access to the Service Providers catalog

				HttpResponse catalogResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, catalogDoc, login, password, httpclient,JTS_Server);
				if (catalogResponse.getStatusLine().getStatusCode() == 200) {
		    		// Define the XPath evaluation environment
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					source = new InputSource(catalogResponse.getEntity().getContent());
					NodeList titleNodes = (NodeList) (xpath2.evaluate(serviceProviderTitleXPath, source, XPathConstants.NODESET));
					
					// Print out the title of each Service Provider
					int length = titleNodes.getLength();
					for (int i = 0; i < length; i++) {
						if(titleNodes.item(i).getTextContent().equals(projectName))
						{
							encodedPRojectAreaName=URLEncoder.encode(projectName, "UTF-8");
						}
					}
					
					if (encodedPRojectAreaName==null)
					{
						System.out.println("Project area " + projectName+ " does not exist.");
						return;
					}
					
					if (ownerUserId!=null)
					{
						if (!veriftUserId(clm, ownerUserId)){
							System.out.println("User Id " + ownerUserId+ " does not exist.");
							return;
						}
					}
					
					
					if (stateName!=null)
					{
						String stateNameTmp=getStateId(clm, encodedPRojectAreaName, stateName,"testscript");
						if (stateNameTmp==null)
						{
							System.out.println("State " + stateName+ " does not exist for artifact test script in project "+projectName+".");
							return;
						}
						stateName=stateNameTmp;
					}
					
					if (testScritpTemplateName!=null)
					{
						String testScriptTemplateNameTmp=getTemplateId(clm, encodedPRojectAreaName, testScritpTemplateName,"testscript");
						if (testScriptTemplateNameTmp==null)
						{
							System.out.println("Template " + testScritpTemplateName+ " does not exist for artifact test script in project "+projectName+".");
							return;
						}
						testScritpTemplateName=testScriptTemplateNameTmp;
					}
					
					String postUrl = QM_server+"/service/com.ibm.rqm.integration.service.IIntegrationService/resources/"+encodedPRojectAreaName+"/testscript/";
					HttpPost post = new HttpPost(postUrl);
					post.addHeader("Accept", "application/xml");
					
					StringWriter wrter = generateTestScriptCreate(testCaseName,testCaseDescription,ownerUserId,stateName,testScritpTemplateName);

					post.setEntity(new StringEntity(wrter.toString(),HTTP.UTF_8));
					
					String xJazzCsrfPrevent = null;
					
					for (Cookie cookie : httpclient.getCookieStore().getCookies())
					{
						if (cookie.getName().equals("JSESSIONID"))
						{
							xJazzCsrfPrevent = cookie.getValue();
						}
					}
					
					if (xJazzCsrfPrevent!=null)
					{
						post.addHeader("X-Jazz-CSRF-Prevent", xJazzCsrfPrevent);
					}
		
					HttpResponse postResponse = HttpUtils.sendPostForSecureDocumentQMCCM(QM_server, post, login, password, httpclient);
					if (postResponse.getStatusLine().getStatusCode()==201)
					{
						System.out.println("Test Script "+testCaseName+" was successfully created in project "+projectName);
					}
					else {
						System.out.println("AQM ... [ERR]");
						System.out.println("AQM was not able to perform this feature.");
					}
				}
			}
			else {
				System.out.println("AQM ... [ERR]");
				System.out.println("AQM was not able to perform this feature.");
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	public static void createTestCase(CLMConfig clm, String projectName, String testCaseName, String testCaseDescription, String ownerUserId, String priorityName, String stateName, String testCaseTemplateName)
	{
		HttpUtils.DEBUG=false;
		
		if (clm==null)
		{
			System.out.println("AQM status ... [ERR]");
			System.out.println("Aqm was not able to read repo configuration file.");
			System.out.println("Please remove existing repo configuration and create new one using register-config command.");
			return;
		}

		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		
		String rootServices = QM_server + "/rootservices";
		String serviceProviderTitleXPath = "//oslc:ServiceProvider/dcterms:title";
		String catalogXPath = "/rdf:Description/oslc_qm:qmServiceProviders/@rdf:resource";
		
		String encodedPRojectAreaName = null;

		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, 15000);
	    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
	    // Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		
		try {
			// Request the Root Services document
			System.out.println("Connecting to ... "+QM_server);
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
			
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
	    		// Define the XPath evaluation environment
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				xpath.setNamespaceContext(
						new NamespaceContextMap(new String[]
								{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
									"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"dc","http://purl.org/dc/terms/",
									"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
									"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
									"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
									"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
									"ju","http://jazz.net/ns/ui#",
									"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
									"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
									"qm_rqm","http://jazz.net/ns/qm/rqm#",
									"oslc","http://open-services.net/ns/core#",
									"trs","http://jazz.net/ns/trs#",
									"trs2","http://open-services.net/ns/core/trs#"
								}));

				// Parse the response body to retrieve the catalog URI
			
				InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());		
				Node attribute = (Node) (xpath.evaluate(catalogXPath, source, XPathConstants.NODE));
				String serviceProvidersCatalog = attribute.getTextContent();
				rootServicesResponse.getEntity().consumeContent();
				
				// Setup the catalog request
				HttpGet catalogDoc = new HttpGet(serviceProvidersCatalog);
				catalogDoc.addHeader("Accept", "application/xml");
				catalogDoc.addHeader("OSLC-Core-Version", "2.0");
				
				// Access to the Service Providers catalog

				HttpResponse catalogResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, catalogDoc, login, password, httpclient,JTS_Server);
				if (catalogResponse.getStatusLine().getStatusCode() == 200) {
		    		// Define the XPath evaluation environment
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					source = new InputSource(catalogResponse.getEntity().getContent());
					NodeList titleNodes = (NodeList) (xpath2.evaluate(serviceProviderTitleXPath, source, XPathConstants.NODESET));
					
					// Print out the title of each Service Provider
					int length = titleNodes.getLength();
					for (int i = 0; i < length; i++) {
						if(titleNodes.item(i).getTextContent().equals(projectName))
						{
							encodedPRojectAreaName=URLEncoder.encode(projectName, "UTF-8");
						}
					}
					
					if (encodedPRojectAreaName==null)
					{
						System.out.println("Project area " + projectName+ " does not exist.");
						return;
					}
					
					if (ownerUserId!=null)
					{
						if (!veriftUserId(clm, ownerUserId)){
							System.out.println("User Id " + ownerUserId+ " does not exist.");
							return;
						}
					}
					
					if (priorityName!=null)
					{
						String priorityNameTmp=getPriorityId(clm, URLEncoder.encode(projectName, "UTF-8"), priorityName);
						if (priorityNameTmp==null)
						{
							System.out.println("Priority " + priorityName+ " does not exist in project "+projectName+".");
							return;
						}
						priorityName=priorityNameTmp;
					}
					
					if (stateName!=null)
					{
						String stateNameTmp=getStateId(clm, encodedPRojectAreaName, stateName,"testcase");
						if (stateNameTmp==null)
						{
							System.out.println("State " + stateName+ " does not exist for artifact test case in project "+projectName+".");
							return;
						}
						stateName=stateNameTmp;
					}
					
					if (testCaseTemplateName!=null)
					{
						String testCaseTemplateNameTmp=getTemplateId(clm, encodedPRojectAreaName, testCaseTemplateName,"testcase");
						if (testCaseTemplateNameTmp==null)
						{
							System.out.println("Template " + testCaseTemplateName+ " does not exist for artifact test case in project "+projectName+".");
							return;
						}
						testCaseTemplateName=testCaseTemplateNameTmp;
					}
					
					String postUrl = QM_server+"/service/com.ibm.rqm.integration.service.IIntegrationService/resources/"+encodedPRojectAreaName+"/testcase/";
					HttpPost post = new HttpPost(postUrl);
					post.addHeader("Accept", "application/xml");
					
					StringWriter wrter = generateTestCaseCreate(testCaseName,testCaseDescription,ownerUserId,priorityName,stateName,testCaseTemplateName);

					post.setEntity(new StringEntity(wrter.toString(),HTTP.UTF_8));
					
					String xJazzCsrfPrevent = null;
					
					for (Cookie cookie : httpclient.getCookieStore().getCookies())
					{
						if (cookie.getName().equals("JSESSIONID"))
						{
							xJazzCsrfPrevent = cookie.getValue();
						}
					}
					
					if (xJazzCsrfPrevent!=null)
					{
						post.addHeader("X-Jazz-CSRF-Prevent", xJazzCsrfPrevent);
					}
		
					HttpResponse postResponse = HttpUtils.sendPostForSecureDocumentQMCCM(QM_server, post, login, password, httpclient);
					if (postResponse.getStatusLine().getStatusCode()==201)
					{
						System.out.println("Test Case "+testCaseName+" was successfully created in project "+projectName);
					}
					else {
						System.out.println("AQM ... [ERR]");
						System.out.println("AQM was not able to perform this feature.");
					}					
				}
			}
			else {
				System.out.println("AQM ... [ERR]");
				System.out.println("AQM was not able to perform this feature.");
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	public static void createTestPlan(CLMConfig clm, String projectName, String testPlanName, String testPlanDescription, String ownerUserId, String priorityName, String stateName, String testPlanTemplateName)
	{
		HttpUtils.DEBUG=false;
		
		if (clm==null)
		{
			System.out.println("AQM status ... [ERR]");
			System.out.println("Aqm was not able to read repo configuration file.");
			System.out.println("Please remove existing repo configuration and create new one using register-config command.");
			return;
		}

		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		
		String rootServices = QM_server + "/rootservices";
		String serviceProviderTitleXPath = "//oslc:ServiceProvider/dcterms:title";
		String catalogXPath = "/rdf:Description/oslc_qm:qmServiceProviders/@rdf:resource";
		
		String encodedPRojectAreaName = null;

		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, 15000);
	    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
	    // Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		
		try {
			// Request the Root Services document
			System.out.println("Connecting to ... "+QM_server);
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
			
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
	    		// Define the XPath evaluation environment
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				xpath.setNamespaceContext(
						new NamespaceContextMap(new String[]
								{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
									"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"dc","http://purl.org/dc/terms/",
									"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
									"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
									"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
									"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
									"ju","http://jazz.net/ns/ui#",
									"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
									"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
									"qm_rqm","http://jazz.net/ns/qm/rqm#",
									"oslc","http://open-services.net/ns/core#",
									"trs","http://jazz.net/ns/trs#",
									"trs2","http://open-services.net/ns/core/trs#"
								}));

				// Parse the response body to retrieve the catalog URI
			
				InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());		
				Node attribute = (Node) (xpath.evaluate(catalogXPath, source, XPathConstants.NODE));
				String serviceProvidersCatalog = attribute.getTextContent();
				rootServicesResponse.getEntity().consumeContent();
				
				// Setup the catalog request
				HttpGet catalogDoc = new HttpGet(serviceProvidersCatalog);
				catalogDoc.addHeader("Accept", "application/xml");
				catalogDoc.addHeader("OSLC-Core-Version", "2.0");
				
				// Access to the Service Providers catalog

				HttpResponse catalogResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, catalogDoc, login, password, httpclient,JTS_Server);
				if (catalogResponse.getStatusLine().getStatusCode() == 200) {
		    		// Define the XPath evaluation environment
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					source = new InputSource(catalogResponse.getEntity().getContent());
					NodeList titleNodes = (NodeList) (xpath2.evaluate(serviceProviderTitleXPath, source, XPathConstants.NODESET));
					
					// Print out the title of each Service Provider
					int length = titleNodes.getLength();
					for (int i = 0; i < length; i++) {
						if(titleNodes.item(i).getTextContent().equals(projectName))
						{
							encodedPRojectAreaName=URLEncoder.encode(projectName, "UTF-8");
						}
					}
					
					if (encodedPRojectAreaName==null)
					{
						System.out.println("Project area " + projectName+ " does not exist.");
						return;
					}
					
					if (ownerUserId!=null)
					{
						if (!veriftUserId(clm, ownerUserId)){
							System.out.println("User Id " + ownerUserId+ " does not exist.");
							return;
						}
					}
					
					if (priorityName!=null)
					{
						String priorityNameTmp=getPriorityId(clm, URLEncoder.encode(projectName, "UTF-8"), priorityName);
						if (priorityNameTmp==null)
						{
							System.out.println("Priority " + priorityName+ " does not exist in project "+projectName+".");
							return;
						}
						priorityName=priorityNameTmp;
					}
					
					if (stateName!=null)
					{
						String stateNameTmp=getStateId(clm, encodedPRojectAreaName, stateName,"testplan");
						if (stateNameTmp==null)
						{
							System.out.println("State " + stateName+ " does not exist for artifact test plan in project "+projectName+".");
							return;
						}
						stateName=stateNameTmp;
					}
					
					if (testPlanTemplateName!=null)
					{
						String testPlanTemplateNameTmp=getTemplateId(clm, encodedPRojectAreaName, testPlanTemplateName,"testplan");
						if (testPlanTemplateNameTmp==null)
						{
							System.out.println("Template " + testPlanTemplateName+ " does not exist for artifact test plan in project "+projectName+".");
							return;
						}
						testPlanTemplateName=testPlanTemplateNameTmp;
					}
					
					String postUrl = QM_server+"/service/com.ibm.rqm.integration.service.IIntegrationService/resources/"+encodedPRojectAreaName+"/testplan/";
					HttpPost post = new HttpPost(postUrl);
					post.addHeader("Accept", "application/xml");
					
					StringWriter wrter = generateTestPlanCreate(testPlanName,testPlanDescription,ownerUserId,priorityName,stateName,testPlanTemplateName);

					post.setEntity(new StringEntity(wrter.toString(),HTTP.UTF_8));
					
					String xJazzCsrfPrevent = null;
					
					for (Cookie cookie : httpclient.getCookieStore().getCookies())
					{
						if (cookie.getName().equals("JSESSIONID"))
						{
							xJazzCsrfPrevent = cookie.getValue();
						}
					}
					
					if (xJazzCsrfPrevent!=null)
					{
						post.addHeader("X-Jazz-CSRF-Prevent", xJazzCsrfPrevent);
					}
		
					HttpResponse postResponse = HttpUtils.sendPostForSecureDocumentQMCCM(QM_server, post, login, password, httpclient);
					if (postResponse.getStatusLine().getStatusCode()==201)
					{
						System.out.println("Test Plan "+testPlanName+" was successfully created in project "+projectName);
					}
					else {
						System.out.println("AQM ... [ERR]");
						System.out.println("AQM was not able to perform this feature.");
					}
				}
			}
			else {
				System.out.println("AQM ... [ERR]");
				System.out.println("AQM was not able to perform this feature.");
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	public static void modifyTestPlan(CLMConfig clm, String projectName, int testPlanId, String testPlanName, String testPlanDescription, String ownerUserId, String priorityName, String stateName)
	{
		HttpUtils.DEBUG=false;
		
		if (clm==null)
		{
			System.out.println("AQM status ... [ERR]");
			System.out.println("Aqm was not able to read repo configuration file.");
			System.out.println("Please remove existing repo configuration and create new one using register-config command.");
			return;
		}

		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		
		String rootServices = QM_server + "/rootservices";
		String serviceProviderTitleXPath = "//oslc:ServiceProvider/dcterms:title";
		String catalogXPath = "/rdf:Description/oslc_qm:qmServiceProviders/@rdf:resource";
		
		String encodedPRojectAreaName = null;

		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, 15000);
	    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
	    // Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		
		try {
			// Request the Root Services document
			System.out.println("Connecting to ... "+QM_server);
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
			
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
	    		// Define the XPath evaluation environment
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				xpath.setNamespaceContext(
						new NamespaceContextMap(new String[]
								{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
									"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"dc","http://purl.org/dc/terms/",
									"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
									"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
									"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
									"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
									"ju","http://jazz.net/ns/ui#",
									"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
									"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
									"qm_rqm","http://jazz.net/ns/qm/rqm#",
									"oslc","http://open-services.net/ns/core#",
									"trs","http://jazz.net/ns/trs#",
									"trs2","http://open-services.net/ns/core/trs#"
								}));

				// Parse the response body to retrieve the catalog URI
			
				InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());		
				Node attribute = (Node) (xpath.evaluate(catalogXPath, source, XPathConstants.NODE));
				String serviceProvidersCatalog = attribute.getTextContent();
				rootServicesResponse.getEntity().consumeContent();
				
				// Setup the catalog request
				HttpGet catalogDoc = new HttpGet(serviceProvidersCatalog);
				catalogDoc.addHeader("Accept", "application/xml");
				catalogDoc.addHeader("OSLC-Core-Version", "2.0");
				
				// Access to the Service Providers catalog

				HttpResponse catalogResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, catalogDoc, login, password, httpclient,JTS_Server);
				if (catalogResponse.getStatusLine().getStatusCode() == 200) {
		    		// Define the XPath evaluation environment
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					source = new InputSource(catalogResponse.getEntity().getContent());
					NodeList titleNodes = (NodeList) (xpath2.evaluate(serviceProviderTitleXPath, source, XPathConstants.NODESET));
					
					// Print out the title of each Service Provider
					int length = titleNodes.getLength();
					for (int i = 0; i < length; i++) {
						if(titleNodes.item(i).getTextContent().equals(projectName))
						{
							encodedPRojectAreaName=URLEncoder.encode(projectName, "UTF-8");
						}
					}
					
					if (encodedPRojectAreaName==null)
					{
						System.out.println("Project area " + projectName+ " does not exist.");
						return;
					}
					String testPlanURL = null;
					testPlanURL = TestManager.getTestPlanURL(clm, encodedPRojectAreaName, testPlanId);
					if (testPlanURL==null)
					{
						System.out.println("Test Plan with Id " + testPlanId+ " does not exist in project "+projectName+".");
						return;
					}
					
					if (ownerUserId!=null)
					{
						if (!veriftUserId(clm, ownerUserId)){
							System.out.println("User Id " + ownerUserId+ " does not exist.");
							return;
						}
					}
					
					if (priorityName!=null)
					{
						String priorityNameTmp=getPriorityId(clm, URLEncoder.encode(projectName, "UTF-8"), priorityName);
						if (priorityNameTmp==null)
						{
							System.out.println("Priority " + priorityName+ " does not exist in project "+projectName+".");
							return;
						}
						priorityName=priorityNameTmp;
					}
					
					if (stateName!=null)
					{
						String stateNameTmp=getStateId(clm, encodedPRojectAreaName, stateName,"testplan");
						if (stateNameTmp==null)
						{
							System.out.println("State " + stateName+ " does not exist for artifact test plan in project "+projectName+".");
							return;
						}
						stateName=stateNameTmp;
					}
					
					String putUrl = testPlanURL;
					HttpPut put = new HttpPut(putUrl);
					put.addHeader("Accept", "application/xml");
					
					StringWriter wrter = TestManager.generateTestPlanModify(testPlanName,testPlanDescription,ownerUserId,priorityName,stateName);

					put.setEntity(new StringEntity(wrter.toString(),HTTP.UTF_8));
					
					String xJazzCsrfPrevent = null;
					
					for (Cookie cookie : httpclient.getCookieStore().getCookies())
					{
						if (cookie.getName().equals("JSESSIONID"))
						{
							xJazzCsrfPrevent = cookie.getValue();
						}
					}
					
					if (xJazzCsrfPrevent!=null)
					{
						put.addHeader("X-Jazz-CSRF-Prevent", xJazzCsrfPrevent);
					}
		
					HttpResponse postResponse = HttpUtils.sendPutForSecureDocumentQMCCM(QM_server, put, login, password, httpclient,JTS_Server);
					
					
					if (postResponse.getStatusLine().getStatusCode()==200)
					{
						System.out.println("Test Plan "+testPlanName+" was successfully modifed in project "+projectName);
					}
					else {
						System.out.println("AQM ... [ERR]");
						System.out.println("AQM was not able to perform this feature.");
					}

					
				}
			}
			else {
				System.out.println("AQM ... [ERR]");
				System.out.println("AQM was not able to perform this feature.");
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	public static void associateTestScriptWithTestCase(CLMConfig clm, String projectName, int testCaseId,  int testScriptId)
	{
		HttpUtils.DEBUG=false;
		
		if (clm==null)
		{
			System.out.println("AQM status ... [ERR]");
			System.out.println("Aqm was not able to read repo configuration file.");
			System.out.println("Please remove existing repo configuration and create new one using register-config command.");
			return;
		}

		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		
		String rootServices = QM_server + "/rootservices";
		String serviceProviderTitleXPath = "//oslc:ServiceProvider/dcterms:title";
		String catalogXPath = "/rdf:Description/oslc_qm:qmServiceProviders/@rdf:resource";
		
		String encodedPRojectAreaName = null;

		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, 15000);
	    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
	    // Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		
		try {
			// Request the Root Services document
			System.out.println("Connecting to ... "+QM_server);
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
			
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
	    		// Define the XPath evaluation environment
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				xpath.setNamespaceContext(
						new NamespaceContextMap(new String[]
								{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
									"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"dc","http://purl.org/dc/terms/",
									"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
									"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
									"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
									"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
									"ju","http://jazz.net/ns/ui#",
									"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
									"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
									"qm_rqm","http://jazz.net/ns/qm/rqm#",
									"oslc","http://open-services.net/ns/core#",
									"trs","http://jazz.net/ns/trs#",
									"trs2","http://open-services.net/ns/core/trs#"
								}));

				// Parse the response body to retrieve the catalog URI
			
				InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());		
				Node attribute = (Node) (xpath.evaluate(catalogXPath, source, XPathConstants.NODE));
				String serviceProvidersCatalog = attribute.getTextContent();
				rootServicesResponse.getEntity().consumeContent();
				
				// Setup the catalog request
				HttpGet catalogDoc = new HttpGet(serviceProvidersCatalog);
				catalogDoc.addHeader("Accept", "application/xml");
				catalogDoc.addHeader("OSLC-Core-Version", "2.0");
				
				// Access to the Service Providers catalog

				HttpResponse catalogResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, catalogDoc, login, password, httpclient,JTS_Server);
				if (catalogResponse.getStatusLine().getStatusCode() == 200) {
		    		// Define the XPath evaluation environment
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					source = new InputSource(catalogResponse.getEntity().getContent());
					NodeList titleNodes = (NodeList) (xpath2.evaluate(serviceProviderTitleXPath, source, XPathConstants.NODESET));
					
					// Print out the title of each Service Provider
					int length = titleNodes.getLength();
					for (int i = 0; i < length; i++) {
						if(titleNodes.item(i).getTextContent().equals(projectName))
						{
							encodedPRojectAreaName=URLEncoder.encode(projectName, "UTF-8");
						}
					}
					
					if (encodedPRojectAreaName==null)
					{
						System.out.println("Project area " + projectName+ " does not exist.");
						return;
					}
	
					String testCaseURL = null;
					testCaseURL = TestManager.getTestCaseURL(clm, encodedPRojectAreaName, testCaseId);
					if (testCaseURL==null)
					{
						System.out.println("Test Case with Id " + testCaseId+ " does not exist in project "+projectName+".");
						return;
					}
					
					String testScriptURL = null;
					testScriptURL = TestManager.getTestScriptURL(clm, encodedPRojectAreaName, testScriptId);
					
					if (testScriptURL==null)
					{
						System.out.println("Test Script with Id " + testScriptId+ " does not exist in project "+projectName+".");
						return;
					}
					
					
					String putUrl = testCaseURL;
					HttpPut put = new HttpPut(putUrl);
					put.addHeader("Accept", "application/xml");
					
					// Existing Test Cases
					List<String> lista = new ArrayList<String>();
					lista.add(testScriptURL);
					
					List<String> fromTC = getTestCaseScriptsREST(clm, projectName, testCaseId);
					lista.addAll(fromTC);
					
					//Execution variables
					
					Map<String, String> parametrs = getTestCaseExecutionVariablesREST(clm, projectName, testCaseId);
					
					StringWriter wrter = TestManager.generateTestCaseAssociateTestScript(lista, parametrs );

					put.setEntity(new StringEntity(wrter.toString(),HTTP.UTF_8));
					
					String xJazzCsrfPrevent = null;
					
					for (Cookie cookie : httpclient.getCookieStore().getCookies())
					{
						if (cookie.getName().equals("JSESSIONID"))
						{
							xJazzCsrfPrevent = cookie.getValue();
						}
					}
					
					if (xJazzCsrfPrevent!=null)
					{
						put.addHeader("X-Jazz-CSRF-Prevent", xJazzCsrfPrevent);
					}
		
					HttpResponse postResponse = HttpUtils.sendPutForSecureDocumentQMCCM(QM_server, put, login, password, httpclient,JTS_Server);
					
					
					if (postResponse.getStatusLine().getStatusCode()==200)
					{
						System.out.println("Test Script id "+testScriptId+" was successfully assigned to test case id "+testCaseId+" in project "+projectName);
					}
					else {
						System.out.println("AQM ... [ERR]");
						System.out.println("AQM was not able to perform this feature.");
					}

					
				}
			}
			else {
				System.out.println("AQM ... [ERR]");
				System.out.println("AQM was not able to perform this feature.");
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	public static void addExecutionVariableToTestCase(CLMConfig clm, String projectName, int testCaseId,  List<String> keywords)
	{
		Map<String, String> exec_var = new HashMap<String, String>();
		if (keywords!=null)
		{
			for (String keyword:keywords)
			{
				String [] keyAndValue = keyword.split("=");
				if (keyAndValue.length!=2)
				{
					System.out.println("Variables are not properly defined. Example: -v \"(variable name)=value with spaces\" or variable=value. For multiple variables -v \"variable=value\" -v \"variable2=value2\"");
					return;
				}
				else{
					exec_var.put(keyAndValue[0], keyAndValue[1]);
				}
			}
		}
		
		HttpUtils.DEBUG=false;
		
		if (clm==null)
		{
			System.out.println("AQM status ... [ERR]");
			System.out.println("Aqm was not able to read repo configuration file.");
			System.out.println("Please remove existing repo configuration and create new one using register-config command.");
			return;
		}

		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		
		String rootServices = QM_server + "/rootservices";
		String serviceProviderTitleXPath = "//oslc:ServiceProvider/dcterms:title";
		String catalogXPath = "/rdf:Description/oslc_qm:qmServiceProviders/@rdf:resource";
		
		String encodedPRojectAreaName = null;

		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, 15000);
	    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
	    // Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		
		try {
			// Request the Root Services document
			System.out.println("Connecting to ... "+QM_server);
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
			
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
	    		// Define the XPath evaluation environment
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				xpath.setNamespaceContext(
						new NamespaceContextMap(new String[]
								{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
									"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"dc","http://purl.org/dc/terms/",
									"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
									"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
									"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
									"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
									"ju","http://jazz.net/ns/ui#",
									"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
									"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
									"qm_rqm","http://jazz.net/ns/qm/rqm#",
									"oslc","http://open-services.net/ns/core#",
									"trs","http://jazz.net/ns/trs#",
									"trs2","http://open-services.net/ns/core/trs#"
								}));

				// Parse the response body to retrieve the catalog URI
			
				InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());		
				Node attribute = (Node) (xpath.evaluate(catalogXPath, source, XPathConstants.NODE));
				String serviceProvidersCatalog = attribute.getTextContent();
				rootServicesResponse.getEntity().consumeContent();
				
				// Setup the catalog request
				HttpGet catalogDoc = new HttpGet(serviceProvidersCatalog);
				catalogDoc.addHeader("Accept", "application/xml");
				catalogDoc.addHeader("OSLC-Core-Version", "2.0");
				
				// Access to the Service Providers catalog

				HttpResponse catalogResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, catalogDoc, login, password, httpclient,JTS_Server);
				if (catalogResponse.getStatusLine().getStatusCode() == 200) {
		    		// Define the XPath evaluation environment
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					source = new InputSource(catalogResponse.getEntity().getContent());
					NodeList titleNodes = (NodeList) (xpath2.evaluate(serviceProviderTitleXPath, source, XPathConstants.NODESET));
					
					// Print out the title of each Service Provider
					int length = titleNodes.getLength();
					for (int i = 0; i < length; i++) {
						if(titleNodes.item(i).getTextContent().equals(projectName))
						{
							encodedPRojectAreaName=URLEncoder.encode(projectName, "UTF-8");
						}
					}
					
					if (encodedPRojectAreaName==null)
					{
						System.out.println("Project area " + projectName+ " does not exist.");
						return;
					}
	
					String testCaseURL = null;
					testCaseURL = TestManager.getTestCaseURL(clm, encodedPRojectAreaName, testCaseId);
					if (testCaseURL==null)
					{
						System.out.println("Test Case with Id " + testCaseId+ " does not exist in project "+projectName+".");
						return;
					}
					
				
					
					String putUrl = testCaseURL;
					HttpPut put = new HttpPut(putUrl);
					put.addHeader("Accept", "application/xml");
					
					// Existing Test Cases
					
					List<String> fromTC = getTestCaseScriptsREST(clm, projectName, testCaseId);
					Map<String, String> parametrs = getTestCaseExecutionVariablesREST(clm, projectName, testCaseId);
					exec_var.putAll(parametrs);
					
					
					StringWriter wrter = TestManager.generateTestCaseAssociateTestScript(fromTC, exec_var);

					put.setEntity(new StringEntity(wrter.toString(),HTTP.UTF_8));
					
					String xJazzCsrfPrevent = null;
					
					for (Cookie cookie : httpclient.getCookieStore().getCookies())
					{
						if (cookie.getName().equals("JSESSIONID"))
						{
							xJazzCsrfPrevent = cookie.getValue();
						}
					}
					
					if (xJazzCsrfPrevent!=null)
					{
						put.addHeader("X-Jazz-CSRF-Prevent", xJazzCsrfPrevent);
					}
		
					HttpResponse postResponse = HttpUtils.sendPutForSecureDocumentQMCCM(QM_server, put, login, password, httpclient,JTS_Server);
					
					
					if (postResponse.getStatusLine().getStatusCode()==200)
					{
						System.out.println("Execution variable(s) successfully assigned to test case id "+testCaseId+" in project "+projectName);
					}
					else {
						System.out.println("AQM ... [ERR]");
						System.out.println("AQM was not able to perform this feature.");
					}

					
				}
			}
			else {
				System.out.println("AQM ... [ERR]");
				System.out.println("AQM was not able to perform this feature.");
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	public static void associateTestCaseWithTestPlan(CLMConfig clm, String projectName, int testPlanId,  int testCaseId)
	{
		HttpUtils.DEBUG=false;
		
		if (clm==null)
		{
			System.out.println("AQM status ... [ERR]");
			System.out.println("Aqm was not able to read repo configuration file.");
			System.out.println("Please remove existing repo configuration and create new one using register-config command.");
			return;
		}

		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		
		String rootServices = QM_server + "/rootservices";
		String serviceProviderTitleXPath = "//oslc:ServiceProvider/dcterms:title";
		String catalogXPath = "/rdf:Description/oslc_qm:qmServiceProviders/@rdf:resource";
		
		String encodedPRojectAreaName = null;

		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, 15000);
	    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
	    // Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		
		try {
			// Request the Root Services document
			System.out.println("Connecting to ... "+QM_server);
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
			
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
	    		// Define the XPath evaluation environment
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				xpath.setNamespaceContext(
						new NamespaceContextMap(new String[]
								{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
									"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"dc","http://purl.org/dc/terms/",
									"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
									"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
									"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
									"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
									"ju","http://jazz.net/ns/ui#",
									"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
									"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
									"qm_rqm","http://jazz.net/ns/qm/rqm#",
									"oslc","http://open-services.net/ns/core#",
									"trs","http://jazz.net/ns/trs#",
									"trs2","http://open-services.net/ns/core/trs#"
								}));

				// Parse the response body to retrieve the catalog URI
			
				InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());		
				Node attribute = (Node) (xpath.evaluate(catalogXPath, source, XPathConstants.NODE));
				String serviceProvidersCatalog = attribute.getTextContent();
				rootServicesResponse.getEntity().consumeContent();
				
				// Setup the catalog request
				HttpGet catalogDoc = new HttpGet(serviceProvidersCatalog);
				catalogDoc.addHeader("Accept", "application/xml");
				catalogDoc.addHeader("OSLC-Core-Version", "2.0");
				
				// Access to the Service Providers catalog

				HttpResponse catalogResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, catalogDoc, login, password, httpclient,JTS_Server);
				if (catalogResponse.getStatusLine().getStatusCode() == 200) {
		    		// Define the XPath evaluation environment
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					source = new InputSource(catalogResponse.getEntity().getContent());
					NodeList titleNodes = (NodeList) (xpath2.evaluate(serviceProviderTitleXPath, source, XPathConstants.NODESET));
					
					// Print out the title of each Service Provider
					int length = titleNodes.getLength();
					for (int i = 0; i < length; i++) {
						if(titleNodes.item(i).getTextContent().equals(projectName))
						{
							encodedPRojectAreaName=URLEncoder.encode(projectName, "UTF-8");
						}
					}
					
					if (encodedPRojectAreaName==null)
					{
						System.out.println("Project area " + projectName+ " does not exist.");
						return;
					}
	
					String testPlanURL = null;
					testPlanURL = TestManager.getTestPlanURL(clm, encodedPRojectAreaName, testPlanId);
					if (testPlanURL==null)
					{
						System.out.println("Test Plan with Id " + testPlanId+ " does not exist in project "+projectName+".");
						return;
					}
					
					String testCaseURL = null;
					testCaseURL = TestManager.getTestCaseURL(clm, encodedPRojectAreaName, testCaseId);
					
					if (testCaseURL==null)
					{
						System.out.println("Test Case with Id " + testCaseId+ " does not exist in project "+projectName+".");
						return;
					}
					
					
					String putUrl = testPlanURL;
					HttpPut put = new HttpPut(putUrl);
					put.addHeader("Accept", "application/xml");
					
					
					List<String> lista = new ArrayList<String>();
					lista.add(testCaseURL);
					
					List<String> fromTC = getTestPlanTestCaseREST(clm, projectName, testPlanId);
					lista.addAll(fromTC);
					
					StringWriter wrter = TestManager.generateTestPlanAssociateTestCase(lista);

					put.setEntity(new StringEntity(wrter.toString(),HTTP.UTF_8));
					
					String xJazzCsrfPrevent = null;
					
					for (Cookie cookie : httpclient.getCookieStore().getCookies())
					{
						if (cookie.getName().equals("JSESSIONID"))
						{
							xJazzCsrfPrevent = cookie.getValue();
						}
					}
					
					if (xJazzCsrfPrevent!=null)
					{
						put.addHeader("X-Jazz-CSRF-Prevent", xJazzCsrfPrevent);
					}
		
					HttpResponse postResponse = HttpUtils.sendPutForSecureDocumentQMCCM(QM_server, put, login, password, httpclient,JTS_Server);
					
					
					if (postResponse.getStatusLine().getStatusCode()==200)
					{
						System.out.println("Test Case id "+testCaseId+" was successfully assigned to test plan id "+testPlanId+" in project "+projectName);
					}
					else {
						System.out.println("AQM ... [ERR]");
						System.out.println("AQM was not able to perform this feature.");
					}

					
				}
			}
			else {
				System.out.println("AQM ... [ERR]");
				System.out.println("AQM was not able to perform this feature.");
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	public static void modifyTestCase(CLMConfig clm, String projectName, int testCaseId, String testCaseName, String testCaseDescription, String ownerUserId, String priorityName, String stateName)
	{
		HttpUtils.DEBUG=false;
		
		if (clm==null)
		{
			System.out.println("AQM status ... [ERR]");
			System.out.println("Aqm was not able to read repo configuration file.");
			System.out.println("Please remove existing repo configuration and create new one using register-config command.");
			return;
		}

		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		
		String rootServices = QM_server + "/rootservices";
		String serviceProviderTitleXPath = "//oslc:ServiceProvider/dcterms:title";
		String catalogXPath = "/rdf:Description/oslc_qm:qmServiceProviders/@rdf:resource";
		
		String encodedPRojectAreaName = null;

		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, 15000);
	    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
	    // Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		
		try {
			// Request the Root Services document
			System.out.println("Connecting to ... "+QM_server);
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
			
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
	    		// Define the XPath evaluation environment
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				xpath.setNamespaceContext(
						new NamespaceContextMap(new String[]
								{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
									"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"dc","http://purl.org/dc/terms/",
									"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
									"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
									"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
									"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
									"ju","http://jazz.net/ns/ui#",
									"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
									"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
									"qm_rqm","http://jazz.net/ns/qm/rqm#",
									"oslc","http://open-services.net/ns/core#",
									"trs","http://jazz.net/ns/trs#",
									"trs2","http://open-services.net/ns/core/trs#"
								}));

				// Parse the response body to retrieve the catalog URI
			
				InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());		
				Node attribute = (Node) (xpath.evaluate(catalogXPath, source, XPathConstants.NODE));
				String serviceProvidersCatalog = attribute.getTextContent();
				rootServicesResponse.getEntity().consumeContent();
				
				// Setup the catalog request
				HttpGet catalogDoc = new HttpGet(serviceProvidersCatalog);
				catalogDoc.addHeader("Accept", "application/xml");
				catalogDoc.addHeader("OSLC-Core-Version", "2.0");
				
				// Access to the Service Providers catalog

				HttpResponse catalogResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, catalogDoc, login, password, httpclient,JTS_Server);
				if (catalogResponse.getStatusLine().getStatusCode() == 200) {
		    		// Define the XPath evaluation environment
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					source = new InputSource(catalogResponse.getEntity().getContent());
					NodeList titleNodes = (NodeList) (xpath2.evaluate(serviceProviderTitleXPath, source, XPathConstants.NODESET));
					
					// Print out the title of each Service Provider
					int length = titleNodes.getLength();
					for (int i = 0; i < length; i++) {
						if(titleNodes.item(i).getTextContent().equals(projectName))
						{
							encodedPRojectAreaName=URLEncoder.encode(projectName, "UTF-8");
						}
					}
					
					if (encodedPRojectAreaName==null)
					{
						System.out.println("Project area " + projectName+ " does not exist.");
						return;
					}
					String testCaseURL = null;
					testCaseURL = TestManager.getTestCaseURL(clm, encodedPRojectAreaName, testCaseId);
					if (testCaseURL==null)
					{
						System.out.println("Test Case with Id " + testCaseId+ " does not exist in project "+projectName+".");
						return;
					}
					
					if (ownerUserId!=null)
					{
						if (!veriftUserId(clm, ownerUserId)){
							System.out.println("User Id " + ownerUserId+ " does not exist.");
							return;
						}
					}
					
					if (priorityName!=null)
					{
						String priorityNameTmp=getPriorityId(clm, URLEncoder.encode(projectName, "UTF-8"), priorityName);
						if (priorityNameTmp==null)
						{
							System.out.println("Priority " + priorityName+ " does not exist in project "+projectName+".");
							return;
						}
						priorityName=priorityNameTmp;
					}
					
					if (stateName!=null)
					{
						String stateNameTmp=getStateId(clm, encodedPRojectAreaName, stateName,"testcase");
						if (stateNameTmp==null)
						{
							System.out.println("State " + stateName+ " does not exist for artifact test case in project "+projectName+".");
							return;
						}
						stateName=stateNameTmp;
					}
					
					String putUrl = testCaseURL;
					HttpPut put = new HttpPut(putUrl);
					put.addHeader("Accept", "application/xml");
					
					StringWriter wrter = TestManager.generateTestCaseModify(testCaseName,testCaseDescription,ownerUserId,priorityName,stateName);

					put.setEntity(new StringEntity(wrter.toString(),HTTP.UTF_8));
					
					String xJazzCsrfPrevent = null;
					
					for (Cookie cookie : httpclient.getCookieStore().getCookies())
					{
						if (cookie.getName().equals("JSESSIONID"))
						{
							xJazzCsrfPrevent = cookie.getValue();
						}
					}
					
					if (xJazzCsrfPrevent!=null)
					{
						put.addHeader("X-Jazz-CSRF-Prevent", xJazzCsrfPrevent);
					}
		
					HttpResponse postResponse = HttpUtils.sendPutForSecureDocumentQMCCM(QM_server, put, login, password, httpclient,JTS_Server);
					
					
					if (postResponse.getStatusLine().getStatusCode()==200)
					{
						System.out.println("Test Case "+testCaseName+" was successfully modifed in project "+projectName);
					}
					else {
						System.out.println("AQM ... [ERR]");
						System.out.println("AQM was not able to perform this feature.");
					}
				}
			}
			else {
				System.out.println("AQM ... [ERR]");
				System.out.println("AQM was not able to perform this feature.");
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	public static void modifyTestScript(CLMConfig clm, String projectName, int testCaseId, String testScriptName, String testScriptDescription, String ownerUserId, String stateName)
	{
		HttpUtils.DEBUG=false;
		
		if (clm==null)
		{
			System.out.println("AQM status ... [ERR]");
			System.out.println("Aqm was not able to read repo configuration file.");
			System.out.println("Please remove existing repo configuration and create new one using register-config command.");
			return;
		}

		
		String QM_server = clm.getQmURL();		
		String JTS_Server = clm.getJtsURL();
		String login = clm.getUsername();			 
		String password = clm.getPassword();
		
		
		String rootServices = QM_server + "/rootservices";
		String serviceProviderTitleXPath = "//oslc:ServiceProvider/dcterms:title";
		String catalogXPath = "/rdf:Description/oslc_qm:qmServiceProviders/@rdf:resource";
		
		String encodedPRojectAreaName = null;

		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParams, 15000);
	    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
	    // Setup the HttClient
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the rootServices request
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		
		try {
			// Request the Root Services document
			System.out.println("Connecting to ... "+QM_server);
			HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
			
			if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
	    		// Define the XPath evaluation environment
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				xpath.setNamespaceContext(
						new NamespaceContextMap(new String[]
								{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
									"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
									"dc","http://purl.org/dc/terms/",
									"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
									"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
									"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
									"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
									"ju","http://jazz.net/ns/ui#",
									"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
									"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
									"qm_rqm","http://jazz.net/ns/qm/rqm#",
									"oslc","http://open-services.net/ns/core#",
									"trs","http://jazz.net/ns/trs#",
									"trs2","http://open-services.net/ns/core/trs#"
								}));

				// Parse the response body to retrieve the catalog URI
			
				InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());		
				Node attribute = (Node) (xpath.evaluate(catalogXPath, source, XPathConstants.NODE));
				String serviceProvidersCatalog = attribute.getTextContent();
				rootServicesResponse.getEntity().consumeContent();
				
				// Setup the catalog request
				HttpGet catalogDoc = new HttpGet(serviceProvidersCatalog);
				catalogDoc.addHeader("Accept", "application/xml");
				catalogDoc.addHeader("OSLC-Core-Version", "2.0");
				
				// Access to the Service Providers catalog

				HttpResponse catalogResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, catalogDoc, login, password, httpclient,JTS_Server);
				if (catalogResponse.getStatusLine().getStatusCode() == 200) {
		    		// Define the XPath evaluation environment
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					source = new InputSource(catalogResponse.getEntity().getContent());
					NodeList titleNodes = (NodeList) (xpath2.evaluate(serviceProviderTitleXPath, source, XPathConstants.NODESET));
					
					// Print out the title of each Service Provider
					int length = titleNodes.getLength();
					for (int i = 0; i < length; i++) {
						if(titleNodes.item(i).getTextContent().equals(projectName))
						{
							encodedPRojectAreaName=URLEncoder.encode(projectName, "UTF-8");
						}
					}
					
					if (encodedPRojectAreaName==null)
					{
						System.out.println("Project area " + projectName+ " does not exist.");
						return;
					}
					String testCaseURL = null;
					testCaseURL = TestManager.getTestScriptURL(clm, encodedPRojectAreaName, testCaseId);
					if (testCaseURL==null)
					{
						System.out.println("Test Script with Id " + testCaseId+ " does not exist in project "+projectName+".");
						return;
					}
					
					if (ownerUserId!=null)
					{
						if (!veriftUserId(clm, ownerUserId)){
							System.out.println("User Id " + ownerUserId+ " does not exist.");
							return;
						}
					}

					
					if (stateName!=null)
					{
						String stateNameTmp=getStateId(clm, encodedPRojectAreaName, stateName,"testscript");
						if (stateNameTmp==null)
						{
							System.out.println("State " + stateName+ " does not exist for artifact test script in project "+projectName+".");
							return;
						}
						stateName=stateNameTmp;
					}
					
					String putUrl = testCaseURL;
					HttpPut put = new HttpPut(putUrl);
					put.addHeader("Accept", "application/xml");
					
					StringWriter wrter = TestManager.generateTestScriptModify(testScriptName,testScriptDescription,ownerUserId,stateName);

					put.setEntity(new StringEntity(wrter.toString(),HTTP.UTF_8));
					
					String xJazzCsrfPrevent = null;
					
					for (Cookie cookie : httpclient.getCookieStore().getCookies())
					{
						if (cookie.getName().equals("JSESSIONID"))
						{
							xJazzCsrfPrevent = cookie.getValue();
						}
					}
					
					if (xJazzCsrfPrevent!=null)
					{
						put.addHeader("X-Jazz-CSRF-Prevent", xJazzCsrfPrevent);
					}
		
					HttpResponse postResponse = HttpUtils.sendPutForSecureDocumentQMCCM(QM_server, put, login, password, httpclient,JTS_Server);
					
					if (postResponse.getStatusLine().getStatusCode()==200)
					{
						System.out.println("Test Script "+testScriptName+" was successfully modifed in project "+projectName);
					}
					else {
						System.out.println("AQM ... [ERR]");
						System.out.println("AQM was not able to perform this feature.");
					}
				}
			}
			else {
				System.out.println("AQM ... [ERR]");
				System.out.println("AQM was not able to perform this feature.");
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (IOException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			
		} catch (Exception e) {
			System.out.println("AQM ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	
	private static List<String> getTestCaseScriptsREST(CLMConfig config, String projectName, int testcaseId)
	{
		String testScriptPath = "//ns2:testscript/@href";
		List<String> lista = new ArrayList<String>();
		if (config==null || projectName==null || testcaseId <= 0)
		{
			return lista;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = config.getQmURL();		
			String JTS_Server = config.getJtsURL();
			String login = config.getUsername();			 
			String password = config.getPassword();
			String rootServices = getTestCaseURL(config, URLEncoder.encode(projectName), 1);
			
			if (rootServices==null)
			{
				return lista;
			}
		
			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
					XPathFactory factory = XPathFactory.newInstance();
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"oslc_rm","http://open-services.net/ns/rm#",
										"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
										"ns2","http://jazz.net/xmlns/alm/qm/v0.1/",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());
					NodeList nodeTmp = (NodeList) (xpath2.evaluate(testScriptPath, source, XPathConstants.NODESET));
					if (nodeTmp!=null)
					{
						for (int i = 0; i<nodeTmp.getLength();i++)
						{
							String s = nodeTmp.item(i).getTextContent();
							lista.add(s);
						}
					}
				}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return lista;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return lista;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to authentication error");
				
				return lista;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return lista;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
				
			}
			return lista;
		}
	}
	
	private static List<String> getTestPlanTestCaseREST(CLMConfig config, String projectName, int testPlanId)
	{
		String testScriptPath = "//ns2:testcase/@href";
		List<String> lista = new ArrayList<String>();
		if (config==null || projectName==null || testPlanId <= 0)
		{
			return lista;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = config.getQmURL();		
			String JTS_Server = config.getJtsURL();
			String login = config.getUsername();			 
			String password = config.getPassword();
			String rootServices = getTestPlanURL(config, URLEncoder.encode(projectName), 1);
			
			if (rootServices==null)
			{
				return lista;
			}
		
			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
					XPathFactory factory = XPathFactory.newInstance();
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"oslc_rm","http://open-services.net/ns/rm#",
										"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
										"ns2","http://jazz.net/xmlns/alm/qm/v0.1/",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());
					NodeList nodeTmp = (NodeList) (xpath2.evaluate(testScriptPath, source, XPathConstants.NODESET));
					if (nodeTmp!=null)
					{
						for (int i = 0; i<nodeTmp.getLength();i++)
						{
							String s = nodeTmp.item(i).getTextContent();
							lista.add(s);
						}
					}
				}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return lista;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return lista;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to authentication error");
				
				return lista;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return lista;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
				
			}
			return lista;
		}
	}
	
	private static Map<String,String> getTestCaseExecutionVariablesREST(CLMConfig config, String projectName, int testcaseId)
	{
		String testScriptPath = "//ns2:variable";
		Map<String, String> parameters = new HashMap<String, String>();
		if (config==null || projectName==null || testcaseId <= 0)
		{
			return parameters;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = config.getQmURL();		
			String JTS_Server = config.getJtsURL();
			String login = config.getUsername();			 
			String password = config.getPassword();
			String rootServices = getTestCaseURL(config, URLEncoder.encode(projectName), 1);
			
			if (rootServices==null)
			{
				return parameters;
			}
		
			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
					XPathFactory factory = XPathFactory.newInstance();
					XPath xpath2 = factory.newXPath();
					xpath2.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc", "http://open-services.net/ns/core#",
										"oslc_rm","http://open-services.net/ns/rm#",
										"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
										"ns2","http://jazz.net/xmlns/alm/qm/v0.1/",
										"dcterms","http://purl.org/dc/terms/"}));

					// Parse the response body to retrieve the Service Provider
					InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());
					NodeList nodeTmp = (NodeList) (xpath2.evaluate(testScriptPath, source, XPathConstants.NODESET));
					if (nodeTmp!=null)
					{
						for (int i = 0; i<nodeTmp.getLength();i++)
						{
							Node nTmp = nodeTmp.item(i);
							if (nTmp instanceof Element)
							{
								Element ele = (Element)nTmp;
								Node name = ele.getElementsByTagName("ns2:name").item(0);
								Node value = ele.getElementsByTagName("ns2:value").item(0);
								parameters.put(name.getTextContent(), value.getTextContent());
							}
						}
					}
				}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return parameters;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return parameters;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to authentication error");
				
				return parameters;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return parameters;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
				
			}
			return parameters;
		}
	}
	
	
	private static String getTestCaseURL(CLMConfig config,String projectName, int testcaseId)
	{
		if (config==null || testcaseId <= 0 )
		{
			return null;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = config.getQmURL();		
			String JTS_Server = config.getJtsURL();
			String login = config.getUsername();			 
			String password = config.getPassword();
			String rootServices = QM_server + "/service/com.ibm.rqm.integration.service.IIntegrationService/resources/"+projectName+"/testcase/urn:com.ibm.rqm:testcase:"+testcaseId;

			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
					return rootServices;
				}
				else
				{
					return null;
				}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
		}
	}
	
	private static String getTestSuiteExecutionRecordURL(CLMConfig config,String projectName, int testSuiteExecutionRecordId)
	{
		if (config==null || testSuiteExecutionRecordId <= 0 )
		{
			return null;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = config.getQmURL();		
			String JTS_Server = config.getJtsURL();
			String login = config.getUsername();			 
			String password = config.getPassword();
			String rootServices = QM_server + "/service/com.ibm.rqm.integration.service.IIntegrationService/resources/"+projectName+"/suiteexecutionrecord/urn:com.ibm.rqm:suiteexecutionrecord:"+testSuiteExecutionRecordId;
			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
					return rootServices;
				}
				else
				{
					return null;
				}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
		}
	}
	
	private static String getTestCaseExecutionRecordURL(CLMConfig config,String projectName, int testCaseExecutionRecordId)
	{
		if (config==null || testCaseExecutionRecordId <= 0 )
		{
			return null;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = config.getQmURL();		
			String JTS_Server = config.getJtsURL();
			String login = config.getUsername();			 
			String password = config.getPassword();
			String rootServices = QM_server + "/service/com.ibm.rqm.integration.service.IIntegrationService/resources/"+projectName+"/executionworkitem/urn:com.ibm.rqm:executionworkitem:"+testCaseExecutionRecordId;

			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
					return rootServices;
				}
				else
				{
					return null;
				}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
		}
	}
	
	private static String getTestScriptURL(CLMConfig config,String projectName, int testScriptId)
	{
		if (config==null || testScriptId <= 0 )
		{
			return null;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = config.getQmURL();		
			String JTS_Server = config.getJtsURL();
			String login = config.getUsername();			 
			String password = config.getPassword();
			String rootServices = QM_server + "/service/com.ibm.rqm.integration.service.IIntegrationService/resources/"+projectName+"/testscript/urn:com.ibm.rqm:testscript:"+testScriptId;

			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
					return rootServices;
				}
				else
				{
					return null;
				}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
		}
	}
	
	private static String getTestSuiteURL(CLMConfig config,String projectName, int testSuiteid)
	{
		if (config==null || testSuiteid <= 0 )
		{
			return null;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = config.getQmURL();		
			String JTS_Server = config.getJtsURL();
			String login = config.getUsername();			 
			String password = config.getPassword();
			String rootServices = QM_server + "/service/com.ibm.rqm.integration.service.IIntegrationService/resources/"+projectName+"/testsuite/urn:com.ibm.rqm:testsuite:"+testSuiteid;

			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
					return rootServices;
				}
				else
				{
					return null;
				}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
		}
	}
	
	private static String getTestPlanURL(CLMConfig config,String projectName, int testplanId)
	{
		if (config==null || testplanId <= 0 )
		{
			return null;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = config.getQmURL();		
			String JTS_Server = config.getJtsURL();
			String login = config.getUsername();			 
			String password = config.getPassword();
			String rootServices = QM_server + "/service/com.ibm.rqm.integration.service.IIntegrationService/resources/"+projectName+"/testplan/urn:com.ibm.rqm:testplan:"+testplanId;

			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
					return rootServices;
				}
				else
				{
					return null;
				}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
		}
	}
	
	private static String getTemplateId(CLMConfig config,String projectName,String templateName, String typeName)
	{
		if (config==null || templateName == null || typeName == null || projectName == null)
		{
			return null;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = config.getQmURL();		
			String JTS_Server = config.getJtsURL();
			String login = config.getUsername();			 
			String password = config.getPassword();
			String rootServices = QM_server + "/service/com.ibm.rqm.integration.service.IIntegrationService/resources/"+projectName+"/template/";
			String projectAreaTmp = null;

			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
			    		// Define the XPath evaluation environment
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					DocumentBuilder db = dbf.newDocumentBuilder();
					Document doc = db.parse(rootServicesResponse.getEntity().getContent());
					NodeList list = doc.getElementsByTagName("entry");
					int length = list.getLength();
					for (int i = 0; i < length; i++) {
						Node node = list.item(i);
						if (node instanceof Element)
						{
							Element elem = (Element)node;
							NodeList title = elem.getElementsByTagName("title");
							if (title.getLength()>0)
							{
								if(title.item(0).getTextContent().equals(templateName))
								{
									NodeList id = elem.getElementsByTagName("link");
									if (id.getLength()>0)
									{
										Node nodeId = id.item(0);
										if (node instanceof Element)
										{
											Element idEle = (Element)nodeId;
											String href = idEle.getAttribute("href");
											String tmp=href.replace(rootServices, "");
											if (tmp.split("/")[0].contains(typeName))
											{
												return href;
											}
										}
									}
								}
								
							}
							}

					}
					rootServicesResponse.getEntity().consumeContent();
					return null;
					}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
			return null;
		}
	}
	
	private static String getPriorityId(CLMConfig config,String projectName, String priorityName)
	{
		if (config==null || priorityName == null)
		{
			return null;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = config.getQmURL();		
			String JTS_Server = config.getJtsURL();
			String login = config.getUsername();			 
			String password = config.getPassword();

			String rootServices = QM_server + "/service/com.ibm.rqm.integration.service.IIntegrationService/process-info/"+projectName+"/priority/";
			String serviceProviderTitleXPath = "//qm_process:Priority";		
			String projectAreaTmp = null;
			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);

				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {

		    		// Define the XPath evaluation environment
					XPathFactory factory = XPathFactory.newInstance();
					XPath xpath = factory.newXPath();
					xpath.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
										"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
										"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
										"dc","http://purl.org/dc/terms/",
										"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
										"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
										"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
										"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
										"ju","http://jazz.net/ns/ui#",
										"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
										"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
										"qm_rqm","http://jazz.net/ns/qm/rqm#",
										"oslc","http://open-services.net/ns/core#",
										"trs","http://jazz.net/ns/trs#",
										"trs2","http://open-services.net/ns/core/trs#",
										"rdfs","http://www.w3.org/2000/01/rdf-schema#",
										"qm_process","http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/"
									}));

					// Parse the response body to retrieve the catalog URI
				
					InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());
					

					NodeList titleNodes = (NodeList) (xpath.evaluate(serviceProviderTitleXPath, source, XPathConstants.NODESET));

					int length = titleNodes.getLength();
					for (int i = 0; i < length; i++) {
						Node node = titleNodes.item(i);
						if (node instanceof Element)
						{

							Element elem = (Element)node;
							NodeList title = elem.getElementsByTagName("dcterms:title");
							if (title.getLength()>0)
							{

								if(title.item(0).getTextContent().equals(priorityName))
								{
									NodeList id = elem.getElementsByTagName("dcterms:identifier");
									if (id.getLength()>0) return id.item(0).getTextContent();
								}
							}
						}
					}
					rootServicesResponse.getEntity().consumeContent();
					return null;
					}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
			return null;
		}
	}
	
	public static String getPriorityURL(CLMConfig config,String projectName, String priorityName)
	{
		if (config==null || priorityName == null)
		{
			return null;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = config.getQmURL();		
			String JTS_Server = config.getJtsURL();
			String login = config.getUsername();			 
			String password = config.getPassword();

			String rootServices = QM_server + "/service/com.ibm.rqm.integration.service.IIntegrationService/process-info/"+projectName+"/priority/";
			String serviceProviderTitleXPath = "//qm_process:Priority";		
			String projectAreaTmp = null;
			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);

				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {

		    		// Define the XPath evaluation environment
					XPathFactory factory = XPathFactory.newInstance();
					XPath xpath = factory.newXPath();
					xpath.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
										"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
										"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
										"dc","http://purl.org/dc/terms/",
										"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
										"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
										"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
										"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
										"ju","http://jazz.net/ns/ui#",
										"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
										"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
										"qm_rqm","http://jazz.net/ns/qm/rqm#",
										"oslc","http://open-services.net/ns/core#",
										"trs","http://jazz.net/ns/trs#",
										"trs2","http://open-services.net/ns/core/trs#",
										"rdfs","http://www.w3.org/2000/01/rdf-schema#",
										"qm_process","http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/"
									}));

					// Parse the response body to retrieve the catalog URI
				
					InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());
					

					NodeList titleNodes = (NodeList) (xpath.evaluate(serviceProviderTitleXPath, source, XPathConstants.NODESET));

					int length = titleNodes.getLength();
					for (int i = 0; i < length; i++) {
						Node node = titleNodes.item(i);
						if (node instanceof Element)
						{

							Element elem = (Element)node;
							NodeList title = elem.getElementsByTagName("dcterms:title");
							if (title.getLength()>0)
							{

								if(title.item(0).getTextContent().equals(priorityName))
								{
									return elem.getAttribute("rdf:about");
								}
							}
						}
					}
					rootServicesResponse.getEntity().consumeContent();
					return null;
					}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
			return null;
		}
	}
	
	private static String getStateId(CLMConfig config, String projectName, String stateName, String typeName)
	{
		if (config==null || stateName == null || typeName==null || projectName==null)
		{
			return null;
		}
		else
		{
			HttpUtils.DEBUG=false;
			String QM_server = config.getQmURL();		
			String JTS_Server = config.getJtsURL();
			String login = config.getUsername();			 
			String password = config.getPassword();
			String rootServices = QM_server + "/service/com.ibm.rqm.integration.service.IIntegrationService/process-info/"+projectName+"/workflowstate/";
			String serviceProviderTitleXPath = "//qm_process:WorkflowState";		
			String projectAreaTmp = null;

			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(rootServices);
			rootServiceDoc.addHeader("Accept", "application/xml");
			rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentQMCCM(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);

				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {

		    		// Define the XPath evaluation environment
					XPathFactory factory = XPathFactory.newInstance();
					XPath xpath = factory.newXPath();
					xpath.setNamespaceContext(
							new NamespaceContextMap(new String[]
									{	"oslc_qm","http://open-services.net/xmlns/qm/1.0/",
										"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
										"rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",
										"dc","http://purl.org/dc/terms/",
										"jfs","http://jazz.net/xmlns/prod/jazz/jfs/1.0/",
										"jtp","http://jazz.net/xmlns/prod/jazz/jtp/0.6/",
										"jd","http://jazz.net/xmlns/prod/jazz/discovery/1.0/",
										"jp06","http://jazz.net/xmlns/prod/jazz/process/0.6/",
										"ju","http://jazz.net/ns/ui#",
										"jdb","http://jazz.net/xmlns/prod/jazz/dashboard/1.0/",
										"rqm","http://jazz.net/xmlns/prod/jazz/rqm/qm/1.0/",
										"qm_rqm","http://jazz.net/ns/qm/rqm#",
										"oslc","http://open-services.net/ns/core#",
										"trs","http://jazz.net/ns/trs#",
										"trs2","http://open-services.net/ns/core/trs#",
										"rdfs","http://www.w3.org/2000/01/rdf-schema#",
										"qm_process","http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/"
									}));

					// Parse the response body to retrieve the catalog URI
				
					InputSource source = new InputSource(rootServicesResponse.getEntity().getContent());
					
					
					NodeList titleNodes = (NodeList) (xpath.evaluate(serviceProviderTitleXPath, source, XPathConstants.NODESET));
					
					int length = titleNodes.getLength();
					for (int i = 0; i < length; i++) {
						Node node = titleNodes.item(i);
						if (node instanceof Element)
						{
							Element elem = (Element)node;
							NodeList title = elem.getElementsByTagName("dcterms:title");
							NodeList artifact = elem.getElementsByTagName("qm_process:artifactType");
							if (title.getLength()>0)
							{
								//System.out.println(title.item(0).getTextContent() + " - "+artifact.item(0).getTextContent());
								if(title.item(0).getTextContent().equals(stateName) && typeName.equals(artifact.item(0).getTextContent().toLowerCase()))
								{	
									NodeList id = elem.getElementsByTagName("dcterms:identifier");
									if (id.getLength()>0) return id.item(0).getTextContent();
								
								}
							}
						}
					}
					rootServicesResponse.getEntity().consumeContent();
					return null;
					}
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
			return null;
		}
	}
	
	
	
	private static boolean veriftUserId(CLMConfig config, String userId) throws UnsupportedEncodingException
	{
		if (config==null || userId == null)
		{
			return false;
		}
		else
			{
			HttpUtils.DEBUG=false;
			String QM_server = config.getQmURL();		
			String JTS_Server = config.getJtsURL();
			String login = config.getUsername();			 
			String password = config.getPassword();

			userId = URLEncoder.encode(userId, "UTF-8");
			String jtsUsers = JTS_Server+"/users/"+userId;		
			

			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(jtsUsers);
			rootServiceDoc.addHeader("Accept", "application/rdf+xml");
			rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentJTS(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
		    		// Define the XPath evaluation environment
			
					// Parse the response body to retrieve the catalog URI
					rootServicesResponse.getEntity().consumeContent();
					return true;
					}else
					{
						return false;
					}
					
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return false;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return false;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to authentication error");
				
				return false;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return false;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
		}
	}
	
	private static String getUserIdURL(CLMConfig config, String userId) throws UnsupportedEncodingException
	{
		if (config==null || userId == null)
		{
			return null;
		}
		else
			{
			HttpUtils.DEBUG=false;
			String QM_server = config.getQmURL();		
			String JTS_Server = config.getJtsURL();
			String login = config.getUsername();			 
			String password = config.getPassword();

			userId = URLEncoder.encode(userId, "UTF-8");
			String jtsUsers = JTS_Server+"/users/"+userId;		
			

			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParams, 15000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
			// Setup the HttClient
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpUtils.setupLazySSLSupport(httpclient);
					
			// Setup the rootServices request
			HttpGet rootServiceDoc = new HttpGet(jtsUsers);
			rootServiceDoc.addHeader("Accept", "application/rdf+xml");
			rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
			
			try {
				// Request the Root Services document
				HttpResponse rootServicesResponse = HttpUtils.sendGetForSecureDocumentJTS(QM_server, rootServiceDoc, login, password, httpclient,JTS_Server);
				if (rootServicesResponse.getStatusLine().getStatusCode() == 200) {
		    		// Define the XPath evaluation environment
			
					// Parse the response body to retrieve the catalog URI
					rootServicesResponse.getEntity().consumeContent();

					return jtsUsers;
					}else
					{
						return null;
					}
					
				}
			catch (ClientProtocolException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (IOException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			} catch (InvalidCredentialsException e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to authentication error");
				
				return null;
			} catch (Exception e) {
				System.out.println("AQM ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
				
				return null;
			}  
			finally {
				httpclient.getConnectionManager().shutdown();
			}
		}
	}
	
	
	private static StringWriter generateTestCaseExecutionRecordCreate(String testCaseUrl, String testScriptUrl, String testPlanUrl, String tcerName, String tcerDescription, String contributor, String priorityURL,int weight)
	{
		StringWriter wrter = new StringWriter();
		wrter.append("<?xml version='1.0' encoding='UTF-8'?>");
		wrter.append("<rdf:RDF ");
		wrter.append("xmlns:rqm_qm='http://jazz.net/ns/qm/rqm#' ");
		wrter.append("xmlns:skos='http://www.w3.org/2004/02/skos/core#' ");
		wrter.append("xmlns:oslc='http://open-services.net/ns/core#' ");
		wrter.append("xmlns:oslc_rm='http://open-services.net/ns/rm#' ");
		wrter.append("xmlns:acp='http://jazz.net/ns/acp#' ");
		wrter.append("xmlns:oslc_qm='http://open-services.net/ns/qm#' ");
		wrter.append("xmlns:oslc_config='http://open-services.net/ns/config#' ");
		wrter.append("xmlns:oslc_auto='http://open-services.net/ns/auto#' ");
		wrter.append("xmlns:process='http://jazz.net/ns/process#' ");
		wrter.append("xmlns:rqm_auto='http://jazz.net/ns/auto/rqm#' ");
		wrter.append("xmlns:bp='http://open-services.net/ns/basicProfile#' ");
		wrter.append("xmlns:calm='http://jazz.net/xmlns/prod/jazz/calm/1.0/' ");
		wrter.append("xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' ");
		wrter.append("xmlns:foaf='http://xmlns.com/foaf/0.1/' ");
		wrter.append("xmlns:cmx='http://open-services.net/ns/cm-x#' ");
		wrter.append("xmlns:owl='http://www.w3.org/2002/07/owl#' ");
		wrter.append("xmlns:rqm_process='http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/' ");
		wrter.append("xmlns:dcterms='http://purl.org/dc/terms/' ");
		wrter.append("xmlns:rqm_lm='http://jazz.net/ns/qm/rqm/labmanagement#' ");
		wrter.append("xmlns:xsd='http://www.w3.org/2001/XMLSchema#' ");
		wrter.append("xmlns:acc='http://open-services.net/ns/core/acc#' ");
		wrter.append("xmlns:oslc_cm='http://open-services.net/ns/cm#' ");
		wrter.append("xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#' ");
		wrter.append("xmlns:xml='http://www.w3.org/XML/1998/namespace' ");
		wrter.append(">");
		  
		wrter.append("<oslc_qm:TestExecutionRecord>");
		
			wrter.append("<oslc_qm:runsTestCase rdf:resource='"+testCaseUrl+"'/>");
			
			
			if (testScriptUrl!=null)
			{
				wrter.append("<oslc_qm:executesTestScript rdf:resource='"+testScriptUrl+"'/>");
			}
			
			if (testPlanUrl!=null)
			{
				wrter.append("<oslc_qm:reportsOnTestPlan rdf:resource='"+testPlanUrl+"'/>");
			}
			
			if (contributor!=null){
				wrter.append("<dcterms:contributor rdf:resource='"+contributor+"'/>");
			}
			
			
			if (priorityURL!=null)
			{
				wrter.append("<rqm_process:hasPriority  rdf:resource='"+priorityURL+"'/>");
			}
			
			if (weight>0)
			{
				wrter.append("<rqm_qm:weight>"+weight+"</rqm_qm:weight>");
			}
			
			if(tcerName!=null)
			{
				wrter.append("<dcterms:title>"+tcerName+"</dcterms:title>");
			}
			
			if(tcerName!=null)
			{
				wrter.append("<dcterms:title>"+tcerName+"</dcterms:title>");
			}
			
			
			if(tcerDescription!=null)
			{
				wrter.append("<dcterms:description>"+tcerDescription+"</dcterms:description>");
			}
		
		wrter.append("</oslc_qm:TestExecutionRecord>");
		wrter.append("</rdf:RDF>");
		
		 
		
		return wrter;
	}
	
	private static StringWriter generateTestCaseExecutionRecordModify(String tcerURL, String testScriptUrl, String tcerName, String tcerDescription, String contributor, String priorityURL,int weight)
	{
		StringWriter wrter = new StringWriter();
		wrter.append("<?xml version='1.0' encoding='UTF-8'?>");
		wrter.append("<rdf:RDF ");
		wrter.append("xmlns:rqm_qm='http://jazz.net/ns/qm/rqm#' ");
		wrter.append("xmlns:skos='http://www.w3.org/2004/02/skos/core#' ");
		wrter.append("xmlns:oslc='http://open-services.net/ns/core#' ");
		wrter.append("xmlns:oslc_rm='http://open-services.net/ns/rm#' ");
		wrter.append("xmlns:acp='http://jazz.net/ns/acp#' ");
		wrter.append("xmlns:oslc_qm='http://open-services.net/ns/qm#' ");
		wrter.append("xmlns:oslc_config='http://open-services.net/ns/config#' ");
		wrter.append("xmlns:oslc_auto='http://open-services.net/ns/auto#' ");
		wrter.append("xmlns:process='http://jazz.net/ns/process#' ");
		wrter.append("xmlns:rqm_auto='http://jazz.net/ns/auto/rqm#' ");
		wrter.append("xmlns:bp='http://open-services.net/ns/basicProfile#' ");
		wrter.append("xmlns:calm='http://jazz.net/xmlns/prod/jazz/calm/1.0/' ");
		wrter.append("xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' ");
		wrter.append("xmlns:foaf='http://xmlns.com/foaf/0.1/' ");
		wrter.append("xmlns:cmx='http://open-services.net/ns/cm-x#' ");
		wrter.append("xmlns:owl='http://www.w3.org/2002/07/owl#' ");
		wrter.append("xmlns:rqm_process='http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/' ");
		wrter.append("xmlns:dcterms='http://purl.org/dc/terms/' ");
		wrter.append("xmlns:rqm_lm='http://jazz.net/ns/qm/rqm/labmanagement#' ");
		wrter.append("xmlns:xsd='http://www.w3.org/2001/XMLSchema#' ");
		wrter.append("xmlns:acc='http://open-services.net/ns/core/acc#' ");
		wrter.append("xmlns:oslc_cm='http://open-services.net/ns/cm#' ");
		wrter.append("xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#' ");
		wrter.append("xmlns:xml='http://www.w3.org/XML/1998/namespace' ");
		wrter.append(">");
		  
		wrter.append("<oslc_qm:TestExecutionRecord rdf:about='"+tcerURL+"'>");
				
			if (testScriptUrl!=null)
			{
				wrter.append("<oslc_qm:executesTestScript rdf:resource='"+testScriptUrl+"'/>");
			}
			
			if (contributor!=null){
				wrter.append("<dcterms:contributor rdf:resource='"+contributor+"'/>");
			}
			
			
			if (priorityURL!=null)
			{
				wrter.append("<rqm_process:hasPriority  rdf:resource='"+priorityURL+"'/>");
			}
			
			if (weight>0)
			{
				wrter.append("<rqm_qm:weight>"+weight+"</rqm_qm:weight>");
			}
			
			if(tcerName!=null)
			{
				wrter.append("<dcterms:title>"+tcerName+"</dcterms:title>");
			}
			
			
			
			if(tcerDescription!=null)
			{
				wrter.append("<dcterms:description>"+tcerDescription+"</dcterms:description>");
			}
		
		wrter.append("</oslc_qm:TestExecutionRecord>");
		wrter.append("</rdf:RDF>");
		
		 
		
		return wrter;
	}
	
	private static StringWriter generateTestCaseCreate(String name,String description, String ownerUserId, String priorityUrl, String stateName, String testPlanTemplateName)
	{
		StringWriter wrter = new StringWriter();
		wrter.append("<?xml version='1.0' encoding='UTF-8'?>");
		wrter.append("<ns2:testcase xmlns:ns2='http://jazz.net/xmlns/alm/qm/v0.1/' xmlns:ns1='http://www.w3.org/1999/02/22-rdf-syntax-ns#' xmlns:ns3='http://schema.ibm.com/vega/2008/' xmlns:ns4='http://purl.org/dc/elements/1.1/' xmlns:ns5='http://jazz.net/xmlns/prod/jazz/process/0.6/' xmlns:ns6='http://jazz.net/xmlns/alm/v0.1/' xmlns:ns7='http://purl.org/dc/terms/' xmlns:ns8='http://jazz.net/xmlns/alm/qm/v0.1/testscript/v0.1/' xmlns:ns9='http://jazz.net/xmlns/alm/qm/v0.1/executionworkitem/v0.1' xmlns:ns10='http://open-services.net/ns/core#' xmlns:ns11='http://open-services.net/ns/qm#' xmlns:ns12='http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/' xmlns:ns13='http://www.w3.org/2002/07/owl#' xmlns:ns14='http://jazz.net/xmlns/alm/qm/qmadapter/v0.1' xmlns:ns15='http://jazz.net/xmlns/alm/qm/qmadapter/task/v0.1' xmlns:ns16='http://jazz.net/xmlns/alm/qm/v0.1/executionresult/v0.1' xmlns:ns17='http://jazz.net/xmlns/alm/qm/v0.1/catalog/v0.1' xmlns:ns18='http://jazz.net/xmlns/alm/qm/v0.1/tsl/v0.1/' xmlns:ns20='http://jazz.net/xmlns/alm/qm/styleinfo/v0.1/' xmlns:ns21='http://www.w3.org/1999/XSL/Transform'>");
		wrter.append("<ns4:title>"+name+"</ns4:title>");
		if (description!=null)
		{
			wrter.append("<ns4:description>"+description+"</ns4:description>");
		}
		if (ownerUserId!=null)
		{
			wrter.append("<ns6:owner>"+ownerUserId+"</ns6:owner>");
		}
		if (priorityUrl!=null)
		{
			wrter.append("<ns2:priority>"+priorityUrl+"</ns2:priority>");
		}
		if(stateName!=null)
		{
			wrter.append("<ns6:state>"+stateName+"</ns6:state>");
		}
		if (testPlanTemplateName!=null)
		{
			wrter.append("<ns2:template href=\""+testPlanTemplateName+"\"/>");
		}		
		wrter.append("</ns2:testcase>");
		
		return wrter;
	}
	
	private static StringWriter generateTestPlanCreate(String name,String description, String ownerUserId, String priorityUrl, String stateName, String testCaseTemplateName)
	{
		StringWriter wrter = new StringWriter();
		wrter.append("<?xml version='1.0' encoding='UTF-8'?>");
		wrter.append("<ns2:testplan xmlns:ns2='http://jazz.net/xmlns/alm/qm/v0.1/' xmlns:ns4='http://purl.org/dc/elements/1.1/' xmlns:ns6='http://jazz.net/xmlns/alm/v0.1/'>");
		wrter.append("<ns4:title>"+name+"</ns4:title>");
		if (description!=null)
		{
			wrter.append("<ns4:description>"+description+"</ns4:description>");
		}
		if (ownerUserId!=null)
		{
			wrter.append("<ns6:owner>"+ownerUserId+"</ns6:owner>");
		}
		if (priorityUrl!=null)
		{
			wrter.append("<ns2:priority>"+priorityUrl+"</ns2:priority>");
		}
		if(stateName!=null)
		{
			wrter.append("<ns6:state>"+stateName+"</ns6:state>");
		}
		if (testCaseTemplateName!=null)
		{
			wrter.append("<ns2:template href=\""+testCaseTemplateName+"\"/>");
		}		
		wrter.append("</ns2:testplan>");
		
		return wrter;
	}
	private static StringWriter generateTestScriptCreate(String name,String description, String ownerUserId, String stateName, String testScriptTemplateName)
	{
		StringWriter wrter = new StringWriter();
		wrter.append("<?xml version='1.0' encoding='UTF-8'?>");
		wrter.append("<ns2:testscript xmlns:ns2='http://jazz.net/xmlns/alm/qm/v0.1/' xmlns:ns4='http://purl.org/dc/elements/1.1/' xmlns:ns6='http://jazz.net/xmlns/alm/v0.1/'>");
		wrter.append("<ns4:title>"+name+"</ns4:title>");
		if (description!=null)
		{
			wrter.append("<ns4:description>"+description+"</ns4:description>");
		}
		if (ownerUserId!=null)
		{
			wrter.append("<ns6:owner>"+ownerUserId+"</ns6:owner>");
		}
		if(stateName!=null)
		{
			wrter.append("<ns6:state>"+stateName+"</ns6:state>");
		}
		if (testScriptTemplateName!=null)
		{
			wrter.append("<ns2:template href=\""+testScriptTemplateName+"\"/>");
		}		
		wrter.append("</ns2:testscript>");
		
		return wrter;
	}
	
	private static StringWriter generateTestCaseModify(String name,String description, String ownerUserId, String priorityUrl, String stateName)
	{
		StringWriter wrter = new StringWriter();
		wrter.append("<?xml version='1.0' encoding='UTF-8'?>");
		wrter.append("<ns2:testcase xmlns:ns2='http://jazz.net/xmlns/alm/qm/v0.1/' xmlns:ns4='http://purl.org/dc/elements/1.1/' xmlns:ns6='http://jazz.net/xmlns/alm/v0.1/'>");
		if (name!=null)
		{
			wrter.append("<ns4:title>"+name+"</ns4:title>");
		}
		if (description!=null)
		{
			wrter.append("<ns4:description>"+description+"</ns4:description>");
		}
		if (ownerUserId!=null)
		{
			wrter.append("<ns6:owner>"+ownerUserId+"</ns6:owner>");
		}
		if (priorityUrl!=null)
		{
			wrter.append("<ns2:priority>"+priorityUrl+"</ns2:priority>");
		}
		if(stateName!=null)
		{
			wrter.append("<ns6:state>"+stateName+"</ns6:state>");
		}
		wrter.append("</ns2:testcase>");
		
		return wrter;
	}
	
	private static StringWriter generateTestPlanModify(String name,String description, String ownerUserId, String priorityUrl, String stateName)
	{
		StringWriter wrter = new StringWriter();
		wrter.append("<?xml version='1.0' encoding='UTF-8'?>");
		wrter.append("<ns2:testplan xmlns:ns2='http://jazz.net/xmlns/alm/qm/v0.1/' xmlns:ns4='http://purl.org/dc/elements/1.1/' xmlns:ns6='http://jazz.net/xmlns/alm/v0.1/'>");
		if (name!=null)
		{
			wrter.append("<ns4:title>"+name+"</ns4:title>");
		}
		if (description!=null)
		{
			wrter.append("<ns4:description>"+description+"</ns4:description>");
		}
		if (ownerUserId!=null)
		{
			wrter.append("<ns6:owner>"+ownerUserId+"</ns6:owner>");
		}
		if (priorityUrl!=null)
		{
			wrter.append("<ns2:priority>"+priorityUrl+"</ns2:priority>");
		}
		if(stateName!=null)
		{
			wrter.append("<ns6:state>"+stateName+"</ns6:state>");
		}
		wrter.append("</ns2:testplan>");
		
		return wrter;
	}

	private static StringWriter generateTestPlanAssociateTestCase(List <String> testCaseURLList)
	{
		StringWriter wrter = new StringWriter();
		wrter.append("<?xml version='1.0' encoding='UTF-8'?>");
		wrter.append("<ns2:testplan xmlns:ns2='http://jazz.net/xmlns/alm/qm/v0.1/' xmlns:ns4='http://purl.org/dc/elements/1.1/' xmlns:ns6='http://jazz.net/xmlns/alm/v0.1/'>");
		for (String str: testCaseURLList)
		{
			wrter.append("<ns2:testcase href='"+str+"'/>");
		}
		wrter.append("</ns2:testplan>");
		
		return wrter;
	}
	
	
	

	private static StringWriter generateTestCaseAssociateTestScript(List<String> testScriptURL, Map<String, String> parametrs)
	{
		StringWriter wrter = new StringWriter();
		wrter.append("<?xml version='1.0' encoding='UTF-8'?>");
		wrter.append("<ns2:testcase xmlns:ns2='http://jazz.net/xmlns/alm/qm/v0.1/' xmlns:ns4='http://purl.org/dc/elements/1.1/' xmlns:ns6='http://jazz.net/xmlns/alm/v0.1/'>");
		for (String str : testScriptURL)
		{
			wrter.append("<ns2:testscript href='"+str+"'/>");
		}
		if (parametrs!=null)
		{
		if (parametrs.size()>0)
		{
		wrter.append("<ns2:variables>");
		for (Map.Entry<String, String> entry : parametrs.entrySet())
		{
			wrter.append("<ns2:variable>");
			wrter.append("<ns2:name>"+entry.getKey()+"</ns2:name>");
			wrter.append("<ns2:value>"+entry.getValue()+"</ns2:value>");
			wrter.append("</ns2:variable>");
		}
		wrter.append("</ns2:variables>");
		}}
		wrter.append("</ns2:testcase>");
		
		return wrter;
	}
	
	private static StringWriter generateTestScriptModify(String name,String description, String ownerUserId, String stateName)
	{
		StringWriter wrter = new StringWriter();
		wrter.append("<?xml version='1.0' encoding='UTF-8'?>");
		wrter.append("<ns2:testscript xmlns:ns2='http://jazz.net/xmlns/alm/qm/v0.1/' xmlns:ns4='http://purl.org/dc/elements/1.1/' xmlns:ns6='http://jazz.net/xmlns/alm/v0.1/'>");
		if (name!=null)
		{
			wrter.append("<ns4:title>"+name+"</ns4:title>");
		}
		if (description!=null)
		{
			wrter.append("<ns4:description>"+description+"</ns4:description>");
		}
		if (ownerUserId!=null)
		{
			wrter.append("<ns6:owner>"+ownerUserId+"</ns6:owner>");
		}
		if(stateName!=null)
		{
			wrter.append("<ns6:state>"+stateName+"</ns6:state>");
		}
		wrter.append("</ns2:testscript>");
		
		return wrter;
	}
	
	private static StringWriter generateTestCaseLinksModify(CLMConfig clm, String testCaseUrl, List<String> links)
	{
		StringWriter wrter = new StringWriter();
		wrter.append("<?xml version='1.0' encoding='UTF-8'?>");
		wrter.append("<rdf:RDF ");
		wrter.append("xmlns:rqm_qm='http://jazz.net/ns/qm/rqm#' ");
		wrter.append("xmlns:skos='http://www.w3.org/2004/02/skos/core#' ");
		wrter.append("xmlns:oslc='http://open-services.net/ns/core#' ");
		wrter.append("xmlns:oslc_rm='http://open-services.net/ns/rm#' ");
		wrter.append("xmlns:acp='http://jazz.net/ns/acp#' ");
		wrter.append("xmlns:oslc_qm='http://open-services.net/ns/qm#' ");
		wrter.append("xmlns:oslc_config='http://open-services.net/ns/config#' ");
		wrter.append("xmlns:oslc_auto='http://open-services.net/ns/auto#' ");
		wrter.append("xmlns:process='http://jazz.net/ns/process#' ");
		wrter.append("xmlns:rqm_auto='http://jazz.net/ns/auto/rqm#' ");
		wrter.append("xmlns:bp='http://open-services.net/ns/basicProfile#' ");
		wrter.append("xmlns:calm='http://jazz.net/xmlns/prod/jazz/calm/1.0/' ");
		wrter.append("xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' ");
		wrter.append("xmlns:foaf='http://xmlns.com/foaf/0.1/' ");
		wrter.append("xmlns:cmx='http://open-services.net/ns/cm-x#' ");
		wrter.append("xmlns:owl='http://www.w3.org/2002/07/owl#' ");
		wrter.append("xmlns:rqm_process='http://jazz.net/xmlns/prod/jazz/rqm/process/1.0/' ");
		wrter.append("xmlns:dcterms='http://purl.org/dc/terms/' ");
		wrter.append("xmlns:rqm_lm='http://jazz.net/ns/qm/rqm/labmanagement#' ");
		wrter.append("xmlns:xsd='http://www.w3.org/2001/XMLSchema#' ");
		wrter.append("xmlns:acc='http://open-services.net/ns/core/acc#' ");
		wrter.append("xmlns:oslc_cm='http://open-services.net/ns/cm#' ");
		wrter.append("xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#' ");
		wrter.append("xmlns:xml='http://www.w3.org/XML/1998/namespace' ");
		wrter.append(">");
		  
		wrter.append("<oslc_qm:TestCase rdf:about='"+testCaseUrl+"'>");
		
		for (String str :links)
		{
			if (str.contains("/resource/itemName/com.ibm.team.workitem.WorkItem/")){
				wrter.append("<oslc_qm:testsChangeRequest rdf:resource='"+str+"'/>");
			}
			else if (str.contains("/resources/com.ibm.rqm.planning.VersionedExecutionScript/"))
			{
				wrter.append("<oslc_qm:usesTestScript rdf:resource='"+str+"'/>");
			}
			else if (str.contains(clm.getRmURL()+"/resources/"))
			{
				wrter.append("<oslc_qm:validatesRequirement rdf:resource='"+str+"'/>");
			}
		}
		
		wrter.append("</oslc_qm:TestCase>");
		wrter.append("</rdf:RDF>");
		
		 
		
		return wrter;
	}
}
