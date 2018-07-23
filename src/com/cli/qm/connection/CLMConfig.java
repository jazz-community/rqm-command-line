package com.cli.qm.connection;

public class CLMConfig {
	
	private String jtsURL = new String();
	private String qmURL = new String();
	private String ccmURL = new String();
	private String rmURL = new String();
	private String username = new String();
	private String password = new String();
	
	public CLMConfig(String jtsRepo, String ccmRepo, String rmRepo, String qmRepo, String username, String password)
	{
		this.jtsURL=jtsRepo;
		this.username=username;
		this.password=password;
		this.qmURL=qmRepo;
		this.rmURL = rmRepo;
		this.ccmURL = ccmRepo;
	}
	
	public String getCcmURL() {
		return ccmURL;
	}

	public void setCcmURL(String ccmURL) {
		this.ccmURL = ccmURL;
	}

	public String getRmURL() {
		return rmURL;
	}

	public void setRmURL(String rmURL) {
		this.rmURL = rmURL;
	}

	public CLMConfig()
	{
		
	}
	
	
	



	public String getQmURL() {
		return qmURL;
	}

	public void setQmURL(String qmURL) {
		this.qmURL = qmURL;
	}

	public String getJtsURL() {
		return jtsURL;
	}

	public void setJtsURL(String jtsURL) {
		this.jtsURL = jtsURL;
	}

	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String toString()
	{
		return "JTSrepo="+jtsURL+" QMrepo="+qmURL+" username="+username+" password="+password;
	}
	

}
