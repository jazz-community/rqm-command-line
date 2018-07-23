package com.cli.qm.auto.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.cli.qm.connection.CLMConfig;

//int testCaseId, int testScriptId, int testPlanId

@Parameters(commandDescription = "Creating new test case execution record in existing project. Example: tcer-create -a 'Sample Project' -tc 1 -tcer 'Sample Test Case Execution Record'")
public class TestCaseExecutionRecordCreate {
	
	@Parameter(names = { "-a", "--projectArea" }, description = "The project area that will contain created test case execution record", order=1, required = true)
	public String projectArea;
	
	@Parameter(names = { "-tc", "--testCaseId" }, description = "Id of test case for which test case execution record will be created", order=2, required = true)
	public int testCaseId;
	
	@Parameter(names = { "-ts", "--testScriptId" }, description = "Id of test script that will be added to a test case execution record", order=3, required = false)
	public int testScriptId;
	
	@Parameter(names = { "-tp", "--testPlanId" }, description = "Id of test plan that will be added to a test case execution record", order=4, required = false)
	public int testPlanId;
	
	@Parameter(names = { "-tcer", "--testCaseExecutionRecordName" }, description = "Name for newly created test case execution record", order=5, required = false)
	public String tcerName;
	
	@Parameter(names = { "-tcerd", "--testCaseExecutionRecordDescription" }, description = "Description for newly created test case execution record", order=6, required = false)
	public String tcerDescription;
	
	@Parameter(names = { "-o", "--ownerUserId" }, description = "Owner (User Id) for newly created test case execution record", order=7, required = false)
	public String ownerUserId;
	
	@Parameter(names = { "-prio", "--priorityName" }, description = "Priority (priority name) for newly created test case execution record", order=8, required = false)
	public String priorityName;
	
	@Parameter(names = { "-w", "--tcerWeight" }, description = "Weight value for newly created test case execution record", order=9, required = false)
	public int weight = 0;
}
