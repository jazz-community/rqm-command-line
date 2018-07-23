package com.cli.qm.connection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.ObjectInputStream.GetField;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.xml.bind.DatatypeConverter;



public class AuthConfigUtil {
	
	private static String clearURL(String url)
	{
		if (url==null){
			return null;
		}
		else{
			if (url.charAt(url.length()-1)=='/')
			{
				url = url.substring(0, url.length()-1);
			}
			return url;
		}
	}
	
	public static void saveCLMConfig(CLMConfig config)
	{
		Properties props = new Properties();
		FileOutputStream output = null;
		try {
			String key = getServerName(null);
			props.setProperty("jtsURL",encodeString(config.getJtsURL(), key));
			props.setProperty("qmURL",encodeString(config.getQmURL(), key));
			props.setProperty("ccmURL",encodeString(config.getCcmURL(), key));
			props.setProperty("rmURL",encodeString(config.getRmURL(), key));
			props.setProperty("username", encodeString(config.getUsername(), key));
			props.setProperty("password", encodeString(config.getPassword(), key));
			output = new FileOutputStream("config.properties");
			props.store(output,null);
		}
		catch (Exception io)
		{
			System.out.println("Aqm was not able to create repo configuration file.");
		}finally
		{
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					System.exit(1);
				}
			}
		}
	}
	
	public static CLMConfig loadConfig()
	{
		Properties prop = new Properties();
		InputStream input = null;
		
		try {
			String key = getServerName(null);
			input = new FileInputStream("config.properties");
			prop.load(input);

			// get the property value and print it out
			String jtsRepo = clearURL(decodeString(prop.getProperty("jtsURL"),key));
			String qmRepo = clearURL(decodeString(prop.getProperty("qmURL"),key));
			String rmRepo = clearURL(decodeString(prop.getProperty("rmURL"),key));
			String ccmRepo = clearURL(decodeString(prop.getProperty("ccmURL"),key));
			String username = decodeString(prop.getProperty("username"),key);
			String password = decodeString(prop.getProperty("password"),key);
			
			if (jtsRepo == null || username==null || password == null || qmRepo==null || rmRepo==null || ccmRepo==null)
			{
				throw new Exception();
			}
			return new CLMConfig(jtsRepo,ccmRepo, rmRepo, qmRepo,username,password);
			
		} catch (Exception io) {
			System.out.println("Aqm was not able to read repo configuration file.\nPlease remove existing repo configuration and create new one using register-config command.");
			System.exit(1);
		}
		
		return null;
	}
	
	
	public static void removeConfig() throws Exception
	{
		File file = new File("config.properties");
		if(file.exists())
		{
			file.delete();
		}
		else
		{
			throw new Exception();
		}
	}
	
	private static String encodeString(String text, String sKey)
	{
		if (text==null || sKey==null)
		{
			return null;
		}
		else
		{
			DESKeySpec keySpec;
			try {
				keySpec = new DESKeySpec(sKey.getBytes("UTF8"));
				SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
				SecretKey key = keyFactory.generateSecret(keySpec);
				
				byte[] cleartext = text.getBytes("UTF8");      

				Cipher cipher = Cipher.getInstance("DES"); // cipher is not thread safe
				cipher.init(Cipher.ENCRYPT_MODE, key);
				
				//https://stackoverflow.com/questions/19743851/base64-java-encode-and-decode-a-string?noredirect=1&lq=1
				String encryptedPwd = DatatypeConverter.printBase64Binary(cipher.doFinal(cleartext));
				return encryptedPwd;
			} catch (InvalidKeyException e) {
				System.out.println(e.getMessage());
				return null;
			} catch (UnsupportedEncodingException e) {
				return null;
			} catch (NoSuchAlgorithmException e) {
				return null;
			} catch (InvalidKeySpecException e) {
				return null;
			} catch (NoSuchPaddingException e) {
				return null;
			} catch (IllegalBlockSizeException e) {
				return null;
			} catch (BadPaddingException e) {
				return null;
			}
		}
	}
	
	private static String decodeString(String text, String sKey)
	{
		if (text==null || sKey==null)
		{
			return null;
		}
		else
		{
			DESKeySpec keySpec;
			try {
				keySpec = new DESKeySpec(sKey.getBytes("UTF8"));
				SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
				SecretKey key = keyFactory.generateSecret(keySpec);
				
				byte[] encrypedPwdBytes = DatatypeConverter.parseBase64Binary(text);

				Cipher cipher = Cipher.getInstance("DES");// cipher is not thread safe
				cipher.init(Cipher.DECRYPT_MODE, key);
				
				byte[] plainTextPwdBytes = (cipher.doFinal(encrypedPwdBytes));
				
				return new String (plainTextPwdBytes);
				
			} catch (InvalidKeyException e) {
				return null;
			} catch (UnsupportedEncodingException e) {
				return null;
			} catch (NoSuchAlgorithmException e) {
				return null;
			} catch (InvalidKeySpecException e) {
				return null;
			} catch (NoSuchPaddingException e) {
				return null;
			} catch (IllegalBlockSizeException e) {
				return null;
			} catch (BadPaddingException e) {
				return null;
			}
		}
	}
	
	private static String getServerName(String txt)
	{
		try
		{
			String hostname = null;
			InetAddress addr;
		    addr = InetAddress.getLocalHost();
		    if(txt!=null)
		    {
		    	hostname = addr.getHostName()+txt;
		    }else{
		    	hostname = addr.getHostName();
		    }
		    if (hostname.length()>7)return hostname;
		    else return getServerName(hostname);
		}
		
		catch (UnknownHostException ex)
		{
			return null;
		}
	}
	

		
}
