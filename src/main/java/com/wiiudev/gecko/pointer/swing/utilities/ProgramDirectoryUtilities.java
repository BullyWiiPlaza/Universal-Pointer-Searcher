package com.wiiudev.gecko.pointer.swing.utilities;

import lombok.val;

import java.io.File;
import java.net.URISyntaxException;

import static com.wiiudev.gecko.pointer.swing.StackTraceUtilities.handleException;

public class ProgramDirectoryUtilities
{
	private static final String JAR_EXTENSION = "jar";

	private static String getJarName()
	{
		return new File(ProgramDirectoryUtilities.class.getProtectionDomain()
				.getCodeSource()
				.getLocation()
				.getPath())
				.getName();
	}

	private static boolean runningFromJAR()
	{
		val jarName = getJarName();
		return jarName.contains("." + JAR_EXTENSION);
	}

	public static String getProgramDirectory()
	{
		if (runningFromJAR())
		{
			return getCurrentJARDirectory();
		} else
		{
			return getCurrentProjectDirectory();
		}
	}

	private static String getCurrentProjectDirectory()
	{
		return new File("").getAbsolutePath();
	}

	private static String getCurrentJARDirectory()
	{
		try
		{
			return new File(ProgramDirectoryUtilities.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
		} catch (URISyntaxException exception)
		{
			handleException(null, exception);
		}

		return null;
	}
}
