package com.cli.qm.auto.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Modyfing existing test script from a project. Example: testscript-modify -a 'Sample Project' -id 1 -ts 'New Script Name'")
public class TestScriptModify {
	
	@Parameter(names = { "-a", "--projectArea" }, description = "The project area that contains the test script", order=1, required = true)
	public String projectArea;
	
	@Parameter(names = { "-id", "--testScriptId" }, description = "Id of test script that will be modified", order=2, required = true)
	public int testScriptId;
	
	@Parameter(names = { "-ts", "--testScriptName" }, description = "New name for existing test script", order=3, required = false)
	public String testScriptName;
	
	@Parameter(names = { "-td", "--testScriptDescription" }, description = "New description for existing test script", order=4, required = false)
	public String testScriptDescription;
	
	@Parameter(names = { "-o", "--ownerUserId" }, description = "New owner (User id) for existing test script", order=5, required = false)
	public String ownerUserId;
	
	@Parameter(names = { "-st", "--stateName" }, description = "New state (state name) for existing test script", order=7, required = false)
	public String stateName;

}
