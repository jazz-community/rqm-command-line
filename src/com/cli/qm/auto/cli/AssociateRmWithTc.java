package com.cli.qm.auto.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Associating existing requirement with a test case. Example: add-requirement-to-testcase -arm 'Sample RDNG project' -aqm 'Sample QM project' -rm 1 -tc 1")
public class AssociateRmWithTc {
	
	@Parameter(names = { "-arm", "--rmProjectArea" }, description = "Name of project area that is containing a requirement", order=1, required = true)
	public String rmProjectArea;

	@Parameter(names = { "-rm", "--requirementId" }, description = "Id of requirement that will be added to a test case", order=2, required = true)
	public int rmId;
	
	@Parameter(names = { "-aqm", "--qmProjectArea" }, description = "Name of project area that is containing a test case", order=3, required = true)
	public String qmProjectArea;
	
	@Parameter(names = { "-tc", "--testCaseId" }, description = "Id of test case that will be used to add a requirement", order=4, required = true)
	public int testCaseId;

}
