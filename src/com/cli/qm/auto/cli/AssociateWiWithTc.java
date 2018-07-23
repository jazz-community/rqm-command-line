package com.cli.qm.auto.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Associating existing work item with a test case. Example: add-workitem-to-testcase -awi 'Sample RTC project' -aqm 'Sample QM project' -wi 1 -tc 1")
public class AssociateWiWithTc {
	
	@Parameter(names = { "-awi", "--wiProjectArea" }, description = "Name of project area that is containing a work item", order=1, required = true)
	public String wiProjectArea;

	@Parameter(names = { "-wi", "--workitemId" }, description = "Id of work item that will be added to a test case", order=2, required = true)
	public int wiId;
	
	@Parameter(names = { "-aqm", "--qmProjectArea" }, description = "Name of project area that is containing a test case", order=3, required = true)
	public String qmProjectArea;
	
	@Parameter(names = { "-tc", "--testCaseId" }, description = "Id of test case that will be used to add a work item", order=4, required = true)
	public int testCaseId;

}
