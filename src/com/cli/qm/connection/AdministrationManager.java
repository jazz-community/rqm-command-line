package com.cli.qm.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


public class AdministrationManager {

	public static void test(CLMConfig clm)
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
		String catalogXPath = "/rdf:Description/oslc_qm:qmServiceProviders/@rdf:resource";
				
		
		
		// Setup the HttClient
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
			System.out.println("Contacting "+QM_server);
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
					System.out.println("AQM status ... [OK]");
				}
				else{
					
					System.out.println("AQM status ... [ERR]");
					System.out.println("Not able to connect to QM server due to connectivity error.");
				}
			}
			else {
				System.out.println("AQM status ... [ERR]");
				System.out.println("Not able to connect to QM server due to connectivity error.");
			}
			rootServicesResponse.getEntity().consumeContent();
		} catch (ClientProtocolException e) {
			System.out.println("AQM status ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println("AQM status ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			System.out.println(e.getMessage());
		} catch (XPathExpressionException e) {
			System.out.println("AQM status ... [ERR]");
			System.out.println("Not able to perform function due to data parsing error");
			System.out.println(e.getMessage());
		} catch (InvalidCredentialsException e) {
			System.out.println("AQM status ... [ERR]");
			System.out.println("Not able to connect to QM server due to authentication error");
			System.out.println(e.getMessage());
		} catch (Exception e) {
			System.out.println("AQM status ... [ERR]");
			System.out.println("Not able to connect to QM server due to connectivity error.");
			System.out.println(e.getMessage());
		}  
		finally {
			// Shutdown the HTTP connection
			httpclient.getConnectionManager().shutdown();
		}
	}
}
