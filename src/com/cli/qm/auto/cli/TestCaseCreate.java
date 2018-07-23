package com.cli.qm.auto.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Creating new test case in existing project. Example: testcase-create -a 'Sample Project' -tc 'Sample Test Case'")
public class TestCaseCreate {
	
	@Parameter(names = { "-a", "--projectArea" }, description = "The project area that will contain created test case", order=1, required = true)
	public String projectArea;
	
	@Parameter(names = { "-tc", "--testCaseName" }, description = "Name for newly created test case", order=2, required = true)
	public String testCaseName;
	
	@Parameter(names = { "-td", "--testCaseDescription" }, description = "Description for newly created test case", order=3, required = false)
	public String testCaseDescription;
	
	@Parameter(names = { "-o", "--ownerUserId" }, description = "Owner (User Id) for newly created test case", order=4, required = false)
	public String ownerUserId;
	
	@Parameter(names = { "-prio", "--priorityName" }, description = "Priority (priority name) for newly created test case", order=5, required = false)
	public String priorityName;
	
	@Parameter(names = { "-st", "--stateName" }, description = "State (state name) for newly created test case", order=6, required = false)
	public String stateName;
	
	@Parameter(names = { "-t", "--templateName" }, description = "Template name that will be used to create new test case", order=7, required = false)
	public String testCaseTemplateName;
}
