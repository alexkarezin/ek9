package org.ek9lang.cli.support;

import org.ek9lang.cli.CommandLineDetails;
import org.ek9lang.cli.EK9SourceVisitor;
import org.ek9lang.cli.support.EK9ProjectProperties;
import org.ek9lang.compiler.parsing.JustParser;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;

import java.io.File;
import java.util.Optional;
import java.util.Properties;

/**
 * Once the EK9 'Edp' dependency module has determined that a packaged module now needs to be resolved
 * it will call upon this resolver to make sure that it is available.
 * <p>
 * This resolver will initially look in the users $HOME/.ek9/lib directory for a zip file
 * matching the vector for the packages module ie 'ekopen.network.support.utils-1.6.1-9.zip'
 * <p>
 * If that is not present then it will make an https request to repo.ek9lang.com
 * to obtain that zip file, and hash of zip fingerprint.
 * <p>
 * The manual steps for this are:
 * curl https://repo.ek9lang.org/ekopen.net.handy.tools-3.2.1-0.zip -o ekopen.net.handy.tools-3.2.1-0.zip
 * curl https://repo.ek9lang.org/ekopen.net.handy.tools-3.2.1-0.zip.sha256 -o ekopen.net.handy.tools-3.2.1-0.zip.sha256
 * cat ekopen.net.handy.tools-3.2.1-0.zip | shasum -a 256 -c ekopen.net.handy.tools-3.2.1-0.zip.sha256
 * <p>
 * Note when the publisher of the package uploaded the zip, they did a couple of extra bits to be able to provide
 * a secure copy of the hash of the zip.
 * <p>
 * See SigningKeyPair.doubleEncryption as an example of this.
 * They used their private key to encrypt the hash of the zip they created.
 * They then used the public key of the repo server to encrypt that data.
 * <p>
 * So that ensures that only the server with its private key can decrypt that payload, but then
 * only by using the publishers public key can the inner payload be decrypted to reveal the hash.
 * That hash can then be checked against a re-run of hashing of the zip file.
 * <p>
 * On the repo server, the zip is taken and put to one side for virus scanning and later processing.
 * The first layer of encryption is decrypted by the repo serer using its own private key. This ensures that the
 * data decrypted (the still encrypted hash and un-encrypted client public key) has not been tampered with.
 * <p>
 * Now the provided client public key can be used to decrypt the encrypted hash. That hash value can be checked
 * against the hash calculated against the zip.
 * <p>
 * If the zip is virus free, then the zip, the encrypted (with the client private key) hash and the clients public
 * key are all stored on the S3 server.
 * <p>
 * So when the zip is downloaded, this resolver will use the same hashing routine to calculate the fingerprint.
 * It will also get the clients public key to decrypt the encrypted hash and check that the hash values match.
 * <p>
 * Anyway that's the general idea.
 */
public class PackageResolver
{
	private final CommandLineDetails commandLine;
	private final FileHandling fileHandling;
	private final OsSupport osSupport;

	public PackageResolver(CommandLineDetails commandLine, FileHandling fileHandling, OsSupport osSupport)
	{
		this.commandLine = commandLine;
		this.fileHandling = fileHandling;
		this.osSupport = osSupport;
	}

	public Optional<EK9SourceVisitor> resolve(String dependencyVector)
	{
		EK9SourceVisitor visitor = null;
		String zipFileName = fileHandling.makePackagedModuleZipFileName(dependencyVector);
		File homeEK9Lib = fileHandling.getUsersHomeEK9LibDirectory();
		//Lets check if it is unpacked already, if not we can unpack it.
		File unpackedDir = new File(homeEK9Lib, dependencyVector);

		File zipFile = new File(homeEK9Lib, zipFileName);
		if(commandLine.isVerbose())
			System.err.println("Resolve : Checking '" + dependencyVector + "'");

		if(osSupport.isFileReadable(zipFile))
		{
			if(commandLine.isVerbose())
				System.err.println("Resolve : Found '" + zipFile.toString() + "'");

			if(osSupport.isDirectoryReadable(unpackedDir))
			{
				if(commandLine.isVerbose())
					System.err.println("Resolve : Already unpacked '" + dependencyVector + "'");
				visitor = processPackageProperties(unpackedDir);

			}
			else
			{
				if(commandLine.isVerbose())
					System.err.println("Resolve : Unpacking '" + zipFile.toString() + "'");
				if(unZip(zipFile, unpackedDir))
					visitor = processPackageProperties(unpackedDir);
			}
		}
		else
		{
			if(downloadDependency(dependencyVector))
			{
				if(commandLine.isVerbose())
					System.err.println("Resolve : Unpacking '" + zipFile.toString() + "'");
				if(unZip(zipFile, unpackedDir))
					visitor = processPackageProperties(unpackedDir);
			}
			else
			{
				System.err.println("Resolve : '" + dependencyVector + "' cannot be resolved!");
			}
		}
		return Optional.ofNullable(visitor);
	}

	private boolean downloadDependency(String dependencyVector)
	{
		//TODO the download part
		return true;
	}

	/**
	 * Load .package.prperties file from the unpacked dir and fet the property 'sourceFile'
	 * This will tell us which of the sources of ek9 files is the one containing the package directive to use.
	 *
	 * @param unpackedDir The directory where the zip is unpacked to.
	 * @return A Visitor with all the details of the package from the source file.
	 */
	private EK9SourceVisitor processPackageProperties(File unpackedDir)
	{
		File propertiesFile = new File(unpackedDir, ".package.properties");
		EK9SourceVisitor visitor = null;
		if(commandLine.isVerbose())
			System.err.println("Resolve : Loading '" + propertiesFile.toString() + "'");

		EK9ProjectProperties projectProperties = new EK9ProjectProperties(propertiesFile);
		Properties properties = projectProperties.loadProperties();
		String srcToAccess = properties.getProperty("sourceFile");
		if(commandLine.isVerbose())
			System.err.println("Resolve : SourceFile '" + srcToAccess + "'");

		File src = new File(unpackedDir, srcToAccess);
		if(osSupport.isFileReadable(src))
		{
			visitor = loadFileAndVisit(src);
		}
		else
		{
			System.err.println("Resolve : Unable to read sourceFile '" + srcToAccess + "'");
		}
		return visitor;
	}

	private boolean unZip(File zipFile, File unpackedDir)
	{
		if(!fileHandling.unZipFileTo(zipFile, unpackedDir))
		{
			System.err.println("Resolve : Failed to unzip '" + zipFile.toString() + "'");
			return false;
		}
		return true;

	}

	private EK9SourceVisitor loadFileAndVisit(File sourceFile)
	{
		EK9SourceVisitor visitor = new EK9SourceVisitor();
		if(!new JustParser().readSourceFile(sourceFile, visitor))
		{
			System.err.println("Unable to Parse source file [" + sourceFile.getAbsolutePath() + "]");
			return null;
		}
		return visitor;
	}
}
