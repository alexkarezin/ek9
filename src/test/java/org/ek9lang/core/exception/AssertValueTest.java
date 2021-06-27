package org.ek9lang.core.exception;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class AssertValueTest
{

	@Test(expected = IllegalArgumentException.class)
	public void testNullRange()
	{
		Date toCheck = null;
		AssertValue.checkRange("Outside range", null, 1, 10);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLowerRange()
	{
		Date toCheck = null;
		AssertValue.checkRange("Outside range", -1, 1, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUpperRange()
	{
		Date toCheck = null;
		AssertValue.checkRange("Outside range", 11, null, 10);
	}

	@Test
	public void testRange()
	{
		Date toCheck = null;
		AssertValue.checkRange("Outside range", 1, 0, 10);
		//No exception
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOptionNotEmpty()
	{
		Date toCheck = null;
		AssertValue.checkNotEmpty("Should not be empty", Optional.empty());
	}

	@Test
	public void testOption()
	{
		Date toCheck = null;
		AssertValue.checkNotEmpty("Should not be empty", Optional.of(5));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCollectionNotEmpty()
	{
		Date toCheck = null;
		AssertValue.checkNotEmpty("Should not be empty", new ArrayList<String>());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullDate()
	{
		Date toCheck = null;
		AssertValue.checkNotNull("Some Message", toCheck);
	}

	@Test
	public void testNoneDate()
	{
		Date toCheck = new Date();
		AssertValue.checkNotNull("Some Message", toCheck);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCheckTrue()
	{
		AssertValue.checkTrue("Cannot be false", false);
	}

	@Test
	public void testTrue()
	{
		AssertValue.checkTrue("Cannot be false", true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullString()
	{
		String toCheck = null;
		AssertValue.checkNotEmpty("Some Message", toCheck);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyString()
	{
		String toCheck = "";
		AssertValue.checkNotEmpty("Some Message", toCheck);
	}

	@Test
	public void testNoneEmptyString()
	{
		String toCheck = "Some Text";
		AssertValue.checkNotEmpty("Some Message", toCheck);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullStrings()
	{
		String[] toCheck = null;
		AssertValue.checkNotEmpty("Some Message", toCheck);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyStrings()
	{
		String[] toCheck = new String[0];
		AssertValue.checkNotEmpty("Some Message", toCheck);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPartEmptyStrings()
	{
		String[] toCheck = { "Some Text", null };
		AssertValue.checkNotEmpty("Some Message", toCheck);
	}

	@Test
	public void testNoneEmptyStrings()
	{
		String[] toCheck = { "Some Text", "other text" };
		AssertValue.checkNotEmpty("Some Message", toCheck);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testNullNotFoundFile()
	{
		AssertValue.checkCanReadFile("File Cannot be found", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNotFoundFile()
	{
		AssertValue.checkCanReadFile("File Cannot be found", "nosuchfile");
	}

	@Test
	public void testAccessibleDirectory() throws IOException
	{
		String tempDir = System.getProperty("java.io.tmpdir");

		AssertValue.checkDirectoryReadable("Must be able to read from", tempDir);
		AssertValue.checkDirectoryWritable("Must be able to write to", tempDir);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonReadableDirectory() throws IOException
	{
		String tempDir = " no such directory/";
		AssertValue.checkDirectoryReadable("Must be able to read", tempDir);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonWritableDirectory() throws IOException
	{
		String tempDir = " no such directory/";

		AssertValue.checkDirectoryWritable("Must be able to write", tempDir);
	}
	@Test
	public void testFoundFile() throws IOException
	{
		String tempDir = System.getProperty("java.io.tmpdir");
		
		File newFile = new File(tempDir, "assertTest.txt");
		//Remove it if already there
		if(newFile.exists())
			newFile.delete();
		
		//make the file check can access
		newFile.createNewFile();
		AssertValue.checkCanReadFile("File Cannot be found", newFile.getAbsolutePath());
		if(newFile.exists())
			newFile.delete();
	}
}
