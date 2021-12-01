package com.wiiudev.gecko.pointer.utilities;

import lombok.val;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static com.wiiudev.gecko.pointer.utilities.JVMArgumentEnforcer.JARUtilities.runningFromJARFile;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

public class JVMArgumentEnforcer
{
	private final String argument;

	public JVMArgumentEnforcer(String argument)
	{
		this.argument = argument;
	}

	public static void assert64BitJavaInstallation()
	{
		val bitVersion = System.getProperty("sun.arch.data.model");

		if(!bitVersion.equals("64"))
		{
			showMessageDialog(null,
					"Only 64-bit Java installations are supported!",
					"Fatal Error",
					ERROR_MESSAGE);

			System.exit(0);
		}
	}

	private boolean hasTargetArgument()
	{
		val runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		val inputArguments = runtimeMXBean.getInputArguments();

		return inputArguments.contains(argument);
	}

	public void forceArgument() throws URISyntaxException, IOException
	{
		if (!hasTargetArgument())
		{
			// This won't work from IDEs
			if (runningFromJARFile())
			{
				// Supply the desired argument
				restartApplication();
			}
		}
	}

	private void restartApplication() throws URISyntaxException, IOException
	{
		val javaBinary = getJavaBinaryPath();
		val command = new ArrayList<String>();
		command.add(javaBinary);
		command.add("-jar");
		command.add(argument);
		val currentJARFilePath = JARUtilities.getCurrentJARFilePath();
		command.add(currentJARFilePath);

		val processBuilder = new ProcessBuilder(command);
		processBuilder.start();
		System.exit(0);
	}

	private String getJavaBinaryPath()
	{
		return System.getProperty("java.home")
				+ File.separator + "bin"
				+ File.separator + "java";
	}

	public static class JARUtilities
	{
		public static boolean runningFromJARFile() throws URISyntaxException
		{
			val currentJarFile = getCurrentJARFile();

			return currentJarFile.getName().endsWith(".jar");
		}

		public static String getCurrentJARFilePath() throws URISyntaxException
		{
			val currentJarFile = getCurrentJARFile();

			return currentJarFile.getPath();
		}

		private static File getCurrentJARFile() throws URISyntaxException
		{
			return new File(JVMArgumentEnforcer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		}
	}
}
