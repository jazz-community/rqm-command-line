package com.cli.qm.auto.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

//, String stateName, String testPlanTemplateName

@Parameters(commandDescription = "Creating new test plan in existing project. Example: testplan-create -a 'Sample Project' -tp 'Sample Test Plan'")
public class TestPlanCreate {
	
	@Parameter(names = { "-a", "--projectArea" }, description = "The project area that will contain created test plan", order=1, required = true)
	public String projectArea;
	
	@Parameter(names = { "-tp", "--testPlanName" }, description = "Name for newly created test plan", order=2, required = true)
	public String testPlanName;
	
	@Parameter(names = { "-td", "--testPlanDescription" }, description = "Description for newly created test plan", order=3, required = false)
	public String testPlanDescription;
	
	@Parameter(names = { "-o", "--ownerUserId" }, description = "Owner (User Id) for newly created test plan", order=4, required = false)
	public String ownerUserId;
	
	@Parameter(names = { "-prio", "--priorityName" }, description = "Priority (priority name) for newly created test script", order=5, required = false)
	public String priorityName;
	
	@Parameter(names = { "-st", "--stateName" }, description = "State (state name) for newly created test script", order=6, required = false)
	public String stateName;
	
	@Parameter(names = { "-t", "--templateName" }, description = "Template name that will be used to create new test plan", order=7, required = false)
	public String testPlanTemplateName;
}
