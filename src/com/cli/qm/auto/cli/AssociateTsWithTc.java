package com.cli.qm.auto.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Associating existing test script with a test case. Example: add-testscript-to-testcase -a 'Sample QM project' -tp 1 -tc 1")
public class AssociateTsWithTc {
	
	@Parameter(names = { "-a", "--projectArea" }, description = "Name of project area that is containing a test case and a test script", order=1, required = true)
	public String projectArea;
	
	@Parameter(names = { "-ts", "--testScriptId" }, description = "Id of test script that will be added to a test case", order=2, required = true)
	public int testScriptId;
	
	@Parameter(names = { "-tc", "--testCaseId" }, description = "Id of test case that will be used to add a test script", order=3, required = true)
	public int testCaseId;
	
}
