package com.cli.qm.auto.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Modyfing existing test plan from a project. Example: testplan-modify -a 'Sample Project' -id 1 -tp 'New Plan Name'")
public class TestPlanModify {
	
	@Parameter(names = { "-a", "--projectArea" }, description = "The project area that contains the test plan", order=1, required = true)
	public String projectArea;
	
	@Parameter(names = { "-id", "--testPlanId" }, description = "Id of test plan that will be modified", order=2, required = true)
	public int testPlanId;
	
	@Parameter(names = { "-tp", "--testPlanName" }, description = "New name for existing test plan", order=3, required = false)
	public String testPlanName;
	
	@Parameter(names = { "-td", "--testPlanDescription" }, description = "New description for existing test plan", order=4, required = false)
	public String testPlanDescription;
	
	@Parameter(names = { "-o", "--ownerUserId" }, description = "New owner (User id) for existing test plan", order=5, required = false)
	public String ownerUserId;
	
	@Parameter(names = { "-prio", "--priorityName" }, description = "New priority (Priority name) for existing test plan", order=6, required = false)
	public String priorityName;
	
	@Parameter(names = { "-st", "--stateName" }, description = "New state (state name) for existing test plan", order=7, required = false)
	public String stateName;

}
