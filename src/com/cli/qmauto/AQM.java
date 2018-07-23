package com.cli.qmauto;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.varia.NullAppender;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.JCommander.Builder;
import com.cli.qm.auto.cli.AssociateRmWithTc;
import com.cli.qm.auto.cli.AssociateRmWithTcByFile;
import com.cli.qm.auto.cli.AssociateRmWithWi;
import com.cli.qm.auto.cli.AssociateRmWithWiByFile;
import com.cli.qm.auto.cli.AssociateTcWithTp;
import com.cli.qm.auto.cli.AssociateTsWithTc;
import com.cli.qm.auto.cli.AssociateWiWithTc;
import com.cli.qm.auto.cli.DeleteConfig;
import com.cli.qm.auto.cli.ExecutionVariableAddToTC;
import com.cli.qm.auto.cli.RegisterConfig;
import com.cli.qm.auto.cli.Settings;
import com.cli.qm.auto.cli.Test;
import com.cli.qm.auto.cli.TestCaseCreate;
import com.cli.qm.auto.cli.TestCaseExecutionRecordCreate;
import com.cli.qm.auto.cli.TestCaseExecutionRecordModify;
import com.cli.qm.auto.cli.TestCaseModify;
import com.cli.qm.auto.cli.TestPlanCreate;
import com.cli.qm.auto.cli.TestPlanModify;
import com.cli.qm.auto.cli.TestScriptCreate;
import com.cli.qm.auto.cli.TestScriptModify;
import com.cli.qm.connection.AdministrationManager;
import com.cli.qm.connection.AuthConfigUtil;
import com.cli.qm.connection.CLMConfig;
import com.cli.qm.connection.TestManager;
import com.beust.jcommander.ParameterException;

public class AQM {

	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure(new NullAppender());
	    Settings settings = new Settings();
	    
	    //Auth and administration commands
	    ExecutionVariableAddToTC evAdd = new ExecutionVariableAddToTC();
	    AssociateTcWithTp associateTcTp = new AssociateTcWithTp();
	    AssociateTsWithTc associateTsTc = new AssociateTsWithTc();
	    AssociateRmWithTc associateRmTc = new AssociateRmWithTc();
	    AssociateRmWithTcByFile associateRmTcFile = new AssociateRmWithTcByFile();
	    AssociateRmWithWiByFile associateRmWiFile = new AssociateRmWithWiByFile();
	    AssociateWiWithTc associateWiTc = new AssociateWiWithTc();
	    RegisterConfig registerConfig = new RegisterConfig();
	    DeleteConfig deleteConfig = new DeleteConfig();
	    AssociateRmWithWi associateRmWi = new AssociateRmWithWi();
	    TestCaseCreate tcCreate = new TestCaseCreate();
	    TestCaseModify tcModify = new TestCaseModify();
	    TestPlanCreate tpCreate = new TestPlanCreate();
	    TestPlanModify tpModify = new TestPlanModify();
	    TestScriptCreate tsCreate = new TestScriptCreate();
	    TestScriptModify tsModify = new TestScriptModify();
	    TestCaseExecutionRecordCreate tcerCreate = new TestCaseExecutionRecordCreate();
	    TestCaseExecutionRecordModify tcerModify = new TestCaseExecutionRecordModify();
	    Test test = new Test();
	    
	    Builder build = JCommander.newBuilder();
	    
	    JCommander jCommander = build.addObject(settings).
	    		addCommand("add-executionvariable-to-testcase", evAdd).
	    		addCommand("add-testcase-to-testplan", associateTcTp).
	    		addCommand("add-testscript-to-testcase", associateTsTc).
	    		addCommand("add-requirement-to-testcase", associateRmTc).
	    		addCommand("add-requirement-to-workitem", associateRmWi).
	    		addCommand("add-requirement-to-workitem-file", associateRmWiFile).
	    		addCommand("add-requirement-to-testcase-file", associateRmTcFile).
	    		addCommand("delete-config", deleteConfig).
	    		addCommand("test", test).
	    		addCommand("testcase-create", tcCreate).
	    		addCommand("testcase-modify", tcModify).
	    		addCommand("tcer-create", tcerCreate).
	    		addCommand("tcer-modify", tcerModify).
	    		addCommand("testplan-create", tpCreate).
	    		addCommand("testplan-modify", tpModify).
	    		addCommand("testscript-create", tsCreate).
	    		addCommand("testscript-modify", tsModify).
	    		addCommand("register-config", registerConfig).
	    		build();
	    jCommander.setProgramName("aqm.bat / aqm.sh");
	    
		try {
			jCommander.parse(args);
			if (settings.help || jCommander.getParsedCommand() == null) {
				build.build().usage();
				System.exit(0);
			} else if (jCommander.getParsedCommand().equals("delete-config")) {
		        try {     
		        	AuthConfigUtil.removeConfig();
		        	System.out.println("Config file was successfully removed.");
		        }
		        	catch (Exception e) {
		        	System.out.println("Repo configuration file does not exist or accm was not able to remove it. Please use accm register-config command to register repository");
		        } 
			} 
			else if (jCommander.getParsedCommand().equals("register-config")) {
					CLMConfig conf = new CLMConfig();
		        	conf.setQmURL(registerConfig.repositoryQM);
		        	conf.setRmURL(registerConfig.repositoryRM);
		        	conf.setCcmURL(registerConfig.repositoryCCM);
		        	conf.setJtsURL(registerConfig.repositoryJTS);
		        	conf.setUsername(registerConfig.username);
		        	conf.setPassword(registerConfig.password);
		        	AuthConfigUtil.saveCLMConfig(conf);
		        	System.out.println("Config file was successfully registered.");
			} 


			else if (jCommander.getParsedCommand().equals("add-requirement-to-testcase")) {
				CLMConfig clm = AuthConfigUtil.loadConfig();
				TestManager.associateRequirementToTestCase(clm, associateRmTc.rmProjectArea, associateRmTc.rmId, associateRmTc.qmProjectArea, associateRmTc.testCaseId);
				System.out.println("Disconnecting ...");
			}
			
			else if (jCommander.getParsedCommand().equals("add-requirement-to-testcase-file")) {
				CLMConfig clm = AuthConfigUtil.loadConfig();
				
				String csvFile = associateRmTcFile.file;
		        String line = "";
		        String cvsSplitBy = ",";
		        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
		            while ((line = br.readLine()) != null) {
		                // use comma as separator
		                String[] country = line.split(cvsSplitBy);
		                System.out.println("IDENTIFIERS [Requirement= " + country[0] + " , Test Case=" + country[1] + "]");
		                TestManager.associateRequirementToTestCase(clm, associateRmTcFile.rmProjectArea, Integer.parseInt(country[0]), associateRmTcFile.qmProjectArea, Integer.parseInt(country[1]));
		            }
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
				System.out.println("Disconnecting ...");
			}
			else if (jCommander.getParsedCommand().equals("add-requirement-to-workitem-file")) {
				CLMConfig clm = AuthConfigUtil.loadConfig();
				
				String csvFile = associateRmWiFile.file;
		        String line = "";
		        String cvsSplitBy = ",";
		        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
		            while ((line = br.readLine()) != null) {
		                // use comma as separator
		                String[] country = line.split(cvsSplitBy);
		                System.out.println("IDENTIFIERS [Requirement= " + country[0] + " , Work Item=" + country[1] + "]");
		                TestManager.associateRequirementToWorkItem(clm, associateRmWiFile.rmProjectArea, Integer.parseInt(country[0]), associateRmWiFile.qmProjectArea, Integer.parseInt(country[1]));
		            }
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
				System.out.println("Disconnecting ...");
			}
			
			else if (jCommander.getParsedCommand().equals("add-requirement-to-workitem")) {
				CLMConfig clm = AuthConfigUtil.loadConfig();
				TestManager.associateRequirementToWorkItem(clm, associateRmWi.rmProjectArea, associateRmWi.rmId, associateRmWi.wiProjectArea, associateRmWi.wiId);
				System.out.println("Disconnecting ...");
			}
			
			else if (jCommander.getParsedCommand().equals("delete-config")) {
		        try {     
		        	AuthConfigUtil.removeConfig();
		        	System.out.println("Config file was successfully removed.");
		        }
		        	catch (Exception e) {
		        	System.out.println("Repo configuration file does not exist or accm was not able to remove it. Please use accm register-config command to register repository");
		        } 
			} 
			
			else {
			    System.out.println("Not supported comamnd. Please verify available command list.");
				build.build().usage();
				System.exit(0);
			}
	    }
	    catch (ParameterException ex)
	    {
	    	if (jCommander.getParsedCommand()==null)
	    	{
	    		build.build().usage();
	    	}
	    	else{
	    	build.build().usage(jCommander.getParsedCommand());
	    	}
	    	System.exit(0);
	    }
		
	    catch (NoSuchMethodError ex)
	    {
	    	System.out.println("Missing required parameter(s)");
	    	build.build().usage(jCommander.getParsedCommand());
	    	System.exit(0);
	    }
	}


}
