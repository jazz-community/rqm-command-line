package com.cli.qm.auto.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Modyfing existing test case from a project. Example: testcase-modify -a 'Sample Project' -id 1 -tc 'New Case Name'")
public class TestCaseModify {
	
	@Parameter(names = { "-a", "--projectArea" }, description = "The project area that contains the test case", order=1, required = true)
	public String projectArea;
	
	@Parameter(names = { "-id", "--testCaseId" }, description = "Id of test case that will be modified", order=2, required = true)
	public int testCaseId;
	
	@Parameter(names = { "-tc", "--testCaseName" }, description = "New name for existing test case", order=3, required = false)
	public String testCaseName;
	
	@Parameter(names = { "-td", "--testCaseDescription" }, description = "New description for existing test case", order=4, required = false)
	public String testCaseDescription;
	
	@Parameter(names = { "-o", "--ownerUserId" }, description = "New owner (User id) for existing test case", order=5, required = false)
	public String ownerUserId;
	
	@Parameter(names = { "-prio", "--priorityName" }, description = "New priority (Priority name) for existing test case", order=6, required = false)
	public String priorityName;
	
	@Parameter(names = { "-st", "--stateName" }, description = "New state (state name) for existing test case", order=7, required = false)
	public String stateName;

}
