package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.OsSupport;

import java.io.File;

/**
 * Install a package to your own $HOME/.ek9/lib directory.
 */
public class Ei extends E
{
	public Ei(CommandLineDetails commandLine, FileCache sourceFileCache, OsSupport osSupport)
	{
		super(commandLine, sourceFileCache, osSupport);
	}

	public boolean run()
	{
		log("Install: - Package");

		Ep ep = new Ep(commandLine, sourceFileCache, osSupport);
		if(ep.run())
		{
			//Now do deployment.
			log("Install: Prepare");

			String zipFileName = fileHandling.makePackagedModuleZipFileName(commandLine.getModuleName(), commandLine.getVersion().toString());
			File fromDir = new File(fileHandling.getDotEK9Directory(commandLine.getSourceFileDirectory()));
			File destinationDir = fileHandling.getUsersHomeEK9LibDirectory();

			log("Install: Copying '" + zipFileName + "' from '" + fromDir.toString() + "' to '" + destinationDir + "'");

			if(!fileHandling.copy(fromDir, destinationDir, zipFileName))
			{
				report("Install: Failed");
				return false;
			}
			else
			{
				report("Install: " + new File(destinationDir, zipFileName).toString() + " installed");
			}
			String sha256FileName = zipFileName + ".sha256";
			if(!fileHandling.copy(fromDir, destinationDir, sha256FileName))
			{
				report("Install: Failed");
				return false;
			}
			else
			{
				report("Install: " + new File(destinationDir, sha256FileName).toString() + " provided");
			}

			log("Install: Complete");

			return true;
		}
		return false;
	}
}
