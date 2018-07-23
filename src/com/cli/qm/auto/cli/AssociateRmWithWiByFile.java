package com.cli.qm.auto.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters
public class AssociateRmWithWiByFile {
	
	@Parameter(names = { "-arm", "--rmProjectArea" }, order=1, required = true)
	public String rmProjectArea;

	@Parameter(names = { "-rmc", "--requirementIdCol" },  order=2, required = true)
	public int rmId;
	
	@Parameter(names = { "-awi", "--wiProjectArea" }, order=3, required = true)
	public String qmProjectArea;
	
	@Parameter(names = { "-wic", "--workItemIdCol" }, order=4, required = true)
	public int testCaseId;
	
	@Parameter(names = { "-file", "--File" }, order=5, required = true)
	public String file;

}