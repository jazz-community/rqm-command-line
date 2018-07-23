package com.cli.qm.auto.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Modyfing existing test case execution record from a project. Example: tcer-modify -a 'Sample Project' -tcerId 1 -tcer 'New Sample Name'")
public class TestCaseExecutionRecordModify {
	
	@Parameter(names = { "-a", "--projectArea" }, description = "The project area that contains the test case execution record", order=1, required = true)
	public String projectArea;
	
	@Parameter(names = { "-tcerId", "--testCaseExecutionRecordId" }, description = "Id of test case execution record that will be modified", order=2, required = true)
	public int tcerId;

	@Parameter(names = { "-ts", "--testScriptId" }, description = "Id of test script that will be added to a test case execution record", order=3, required = false)
	public int testScriptId;
	
	@Parameter(names = { "-tcer", "--testCaseExecutionRecordName" }, description = "New name for existing test case execution record", order=4, required = false)
	public String tcerName;
	
	@Parameter(names = { "-tcerd", "--testCaseExecutionRecordDescription" }, description = "New description for existing test case execution record", order=5, required = false)
	public String tcerDescription;
	
	@Parameter(names = { "-o", "--ownerUserId" }, description = "New owner (User id) for existing test case execution record", order=6, required = false)
	public String ownerUserId;
	
	@Parameter(names = { "-prio", "--priorityName" }, description = "New priority (Priority name) for existing test case execution record", order=7, required = false)
	public String priorityName;
	
	@Parameter(names = { "-w", "--tcerWeight" }, description = "New state (state name) for existing test case execution record", order=8, required = false)
	public int weight;

}
