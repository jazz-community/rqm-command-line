package com.cli.qm.connection;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;


public class QMConnectionUtils {

	public static HttpClient connect (CLMConfig config)
	{
    	String server = "http://spserver/rm";		// Set the Public URI of your RRC server
        
        String rootServices = server + "/rootservices";
		System.out.println(">> Example01: Accessing Root Services document with HttpClient");
		System.out.println("	- Root Services URI: "+rootServices);
		
		// Setup the HttpClient
		HttpClient httpclient = new DefaultHttpClient();
		
		// Disabling SSL Certificate Validation
		HttpUtils.setupLazySSLSupport(httpclient);
		
		// Setup the HTTP GET method
		HttpGet rootServiceDoc = new HttpGet(rootServices);
		rootServiceDoc.addHeader("Accept", "application/rdf+xml");
		rootServiceDoc.addHeader("OSLC-Core-Version", "2.0");
		return httpclient;
		
	}
}
