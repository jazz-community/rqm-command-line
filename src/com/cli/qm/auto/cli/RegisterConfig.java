package com.cli.qm.auto.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;


@Parameters(commandDescription = "Creating new config.properties file with username, password and repoURL that will be used to execute accm commands. Example: register-config -rq https://localhost:9443/qm -rr https://localhost:9443/rm -rc https://localhost:9443/ccm -rj https://localhost:9443/jts -u user -p pass")
public class RegisterConfig {
	@Parameter(names = { "-u", "--username" }, description = "The user ID of the user executing the commands", order=0, required = true)
	public String username;
	
	@Parameter(names = { "-p", "--password" }, description = "The password of the user", order=1, required = true)
	public String password;
	
	@Parameter(names = { "-rq", "--repositoryQMURL" }, description = "The repository QM URI - ex. https://localhost:9443/qm", order=2, required = true)
	public String repositoryQM;
	
	@Parameter(names = { "-rj", "--repositoryJTSURL" }, description = "The repository JTS URI - ex. https://localhost:9443/jts", order=3, required = true)
	public String repositoryJTS;
	
	@Parameter(names = { "-rc", "--repositoryCCMURL" }, description = "The repository CCM URI - ex. https://localhost:9443/ccm", order=4, required = true)
	public String repositoryCCM;
	
	@Parameter(names = { "-rr", "--repositoryRMURL" }, description = "The repository RM URI - ex. https://localhost:9443/rm", order=5, required = true)
	public String repositoryRM;
}
