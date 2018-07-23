package com.cli.qm.auto.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Creating new test script in existing project. Example: testscript-create -a 'Sample Project' -ts 'Sample Test Script'")
public class TestScriptCreate {
	
	@Parameter(names = { "-a", "--projectArea" }, description = "The project area that will contain created test script", order=1, required = true)
	public String projectArea;
	
	@Parameter(names = { "-ts", "--testScriptName" }, description = "Name for newly created test script", order=2, required = true)
	public String testScriptName;
	
	@Parameter(names = { "-td", "--testScriptDescription" }, description = "Description for newly created test script", order=3, required = false)
	public String testScriptDescription;
	
	@Parameter(names = { "-o", "--ownerUserId" }, description = "Owner (User Id) for newly created test script", order=4, required = false)
	public String ownerUserId;
		
	@Parameter(names = { "-st", "--stateName" }, description = "State (state name) for newly created test script", order=5, required = false)
	public String stateName;
	
	@Parameter(names = { "-t", "--templateName" }, description = "Template name that will be used to create new test script", order=6, required = false)
	public String testScriptTemplateName;
}
