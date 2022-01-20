package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.OsSupport;

public class Esv extends Eve
{	
	public Esv(CommandLineDetails commandLine, FileCache sourceFileCache, OsSupport osSupport)
	{
		super(commandLine, sourceFileCache, osSupport);
	}
	
	public boolean run()
	{
		log("Version=: Prepare");

		if(commandLine.isPackagePresent())
		{
			//Need to get from command line.
			String newVersionParameter = commandLine.getOptionParameter("-SV");
			Version newVersion = Version.withNoBuildNumber(newVersionParameter);
			if(!super.setVersionNewNumber(newVersion))
			{
				report("Failed to set version in " + commandLine.getSourceFileName());
				return false;
			}
		}
		else
		{
			report("File " + super.commandLine.getSourceFileName() + " does not define a package");
			return false;
		}
		log("Version=: Complete");

		return true;
	}
}
