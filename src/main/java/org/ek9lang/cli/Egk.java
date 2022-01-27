package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.SigningKeyPair;

/**
 * Generate signing keys.
 */
public class Egk extends E
{
	public Egk(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	@Override
	protected String messagePrefix()
	{
		return "Keys    : ";
	}

	protected boolean doRun()
	{
		//Ensure the .ek9 directory exists in users home directory.
		getFileHandling().validateHomeEK9Directory(commandLine.targetArchitecture);
		if(!getFileHandling().isUsersSigningKeyPairPresent())
		{
			log("Generating new signing keys");

			//Clients only use short key lengths, server uses 2048.
			if(!getFileHandling().saveToHomeEK9Directory(SigningKeyPair.generate(1024)))
			{
				report("Failed to regenerate keys");
				return false;
			}
		}
		else
		{
			log("Already present - not regenerating");
		}

		log("Complete");
		return true;
	}
}
