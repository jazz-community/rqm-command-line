package com.cli.qm.auto.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Associating existing requirement with a test case. Example: add-requirement-to-testcase -arm 'Sample RDNG project' -aqm 'Sample QM project' -rm 1 -tc 1")
public class AssociateRmWithWi {
	
	@Parameter(names = { "-arm", "--rmProjectArea" }, description = "Name of project area that is containing a requirement", order=1, required = true)
	public String rmProjectArea;

	@Parameter(names = { "-rm", "--requirementId" }, description = "Id of requirement that will be added to a test case", order=2, required = true)
	public int rmId;
	
	@Parameter(names = { "-awi", "--wiProjectArea" }, description = "Name of project area that is containing a work item", order=1, required = true)
	public String wiProjectArea;

	@Parameter(names = { "-wi", "--workitemId" }, description = "Id of work item that will be added to a test case", order=2, required = true)
	public int wiId;

}
