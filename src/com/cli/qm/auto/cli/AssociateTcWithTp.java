package com.cli.qm.auto.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Associating existing test case with a test plan. Example: add-testcase-to-testplan -a 'Sample QM project' -ts 1 -tc 1")
public class AssociateTcWithTp {
	
	@Parameter(names = { "-a", "--projectArea" }, description = "Name of project area that is containing a test case and a test plan", order=1, required = true)
	public String projectArea;
	
	@Parameter(names = { "-tc", "--testCaseId" }, description = "Id of test case that will be added to a test plan", order=2, required = true)
	public int testCaseId;
	
	@Parameter(names = { "-tp", "--testPlanId" }, description = "Id of test plan that will be used to add a test case", order=3, required = true)
	public int testPlanId;
	
}
