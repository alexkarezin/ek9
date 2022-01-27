package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;

/**
 * Print the version number of the package.
 */
public class Epv extends E
{
	public Epv(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	@Override
	protected String messagePrefix()
	{
		return "$Version: ";
	}

	protected boolean doRun()
	{
		if(commandLine.isPackagePresent())
		{
			report(commandLine.getVersion());
		}
		else
		{
			report("File " + commandLine.getSourceFileName() + " does not define a package");
			return false;
		}
		log("Complete");

		return true;
	}
}
