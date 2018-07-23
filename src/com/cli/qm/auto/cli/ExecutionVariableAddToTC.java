package com.cli.qm.auto.cli;

import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Addining execution variable(s) to existing test case. Example: add-executionvariable-to-testcase -a 'Sample project' -tc 1 -k va1=val1")
public class ExecutionVariableAddToTC {
	
	@Parameter(names = { "-a", "--projectArea" }, description = "Name of project area that is containing a test case", order=1, required = true)
	public String projectArea;
	
	@Parameter(names = { "-tc", "--testCaseId" }, description = "Id of test case that will be used to add variable(s)", order=2, required = true)
	public int testCaseId;
	
	@Parameter(names = { "-e", "--executionVariable" }, description = "Execution variable(s) that will be added or changed. Execution variable can be added multiple times. Exmaple: -k va11=value1 -k va2=value2 -k var3=value3", order=5)
	public List<String> executionVariables;
	
}
