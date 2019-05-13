package com.wiiudev.gecko.pointer.swing;

import lombok.val;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wiiudev.gecko.pointer.NativePointerSearcherManager.giveAllPosixFilePermissions;
import static com.wiiudev.gecko.pointer.NativePointerSearcherManager.readFromProcess;
import static com.wiiudev.gecko.pointer.swing.utilities.DefaultContextMenu.addDefaultContextMenu;
import static com.wiiudev.gecko.pointer.swing.utilities.FrameUtilities.setWindowIconImage;
import static java.lang.System.getenv;
import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.write;
import static org.apache.commons.lang3.SystemUtils.*;

public class NativePointerSearcherOutputDialog extends JDialog
{
	private static final String WINDOWS_COMMAND_LINE = "Command Prompt";
	private static final String UNIX_COMMAND_LINE = "Terminal";
	private static final int DROPPED_COMMAND_HEADER_LENGTH = 1;
	private static final String SCRIPT_FILE_EXTENSION = IS_OS_WINDOWS ? ".bat" : ".sh";
	private static final String SCRIPT_FILE_HEADER = IS_OS_UNIX ? "#!/usr/bin/env bash"
			+ lineSeparator() + lineSeparator() : "";
	private String SCRIPT_FILE_POSTFIX = IS_OS_LINUX ? ";read" : "";

	private JPanel contentPane;
	private JTextArea outputArea;
	private JButton executeOnCommandPromptButton;

	NativePointerSearcherOutputDialog()
	{
		setFrameProperties();
		configureCommandLineExecutor();
		addDefaultContextMenu(outputArea);
	}

	private void setFrameProperties()
	{
		setContentPane(contentPane);
		setModal(true);
		val defaultToolkit = Toolkit.getDefaultToolkit();
		val screenSize = defaultToolkit.getScreenSize();
		val width = (int) (screenSize.getWidth() * 0.9);
		val height = (int) (screenSize.getHeight() * 0.9);
		setSize(width, height);
		setWindowIconImage(this);
	}

	private void configureCommandLineExecutor()
	{
		val commandLineExecutorTextHeader = "Execute on";
		if (IS_OS_WINDOWS)
		{
			executeOnCommandPromptButton.setText(commandLineExecutorTextHeader + " " + WINDOWS_COMMAND_LINE);
		} else if (IS_OS_UNIX)
		{
			executeOnCommandPromptButton.setText(commandLineExecutorTextHeader + " " + UNIX_COMMAND_LINE);
		}

		executeOnCommandPromptButton.addActionListener(actionEvent ->
		{
			val outputAreaText = outputArea.getText();
			val lines = outputAreaText.split("\n");
			if (lines.length > 0)
			{
				var command = lines[0];
				val commandLength = command.length();
				if (commandLength > DROPPED_COMMAND_HEADER_LENGTH)
				{
					command = command.substring(DROPPED_COMMAND_HEADER_LENGTH);
				}

				val finalCommand = command;
				val thread = new Thread(() ->
				{
					try
					{
						executeCommandOnCommandLine(finalCommand);
					} catch (Exception exception)
					{
						exception.printStackTrace();
					}
				});

				thread.setName("Command Line Pointer Searcher");
				thread.start();
			}
		});
	}

	private void executeCommandOnCommandLine(String command) throws Exception
	{
		// Create and write the script file
		val pointerSearchScriptFile = createTempFile("prefix", SCRIPT_FILE_EXTENSION);
		val scriptFileContent = SCRIPT_FILE_HEADER + command + SCRIPT_FILE_POSTFIX;
		write(pointerSearchScriptFile, scriptFileContent.getBytes(UTF_8));

		// Give execute permissions
		if (IS_OS_UNIX)
		{
			giveAllPosixFilePermissions(pointerSearchScriptFile);
		}

		val launcherCommand = buildLauncherCommand(pointerSearchScriptFile);
		val processBuilder = new ProcessBuilder(launcherCommand);
		processBuilder.redirectErrorStream(true);
		val process = processBuilder.start();
		val processOutput = readFromProcess(process);
		if (!processOutput.isEmpty())
		{
			System.out.println(processOutput);
		}
	}

	private List<String> buildLauncherCommand(Path scriptFile)
	{
		val launcherCommand = new ArrayList<String>();

		if (IS_OS_WINDOWS)
		{
			val windowsSystemDirectory = getenv("WINDIR") + "\\system32";
			launcherCommand.add(windowsSystemDirectory + "\\" + "cmd");
			launcherCommand.add("/c");
			launcherCommand.add("start");
		} else if (IS_OS_MAC)
		{
			launcherCommand.add("open");
			launcherCommand.add("-a");
			launcherCommand.add("Terminal.app");
		} else if (IS_OS_LINUX)
		{
			launcherCommand.add("gnome-terminal");
			launcherCommand.add("--");
		}

		launcherCommand.add(scriptFile.toString());
		return launcherCommand;
	}

	public void setText(String output)
	{
		outputArea.setText(output);
		outputArea.setCaretPosition(0);
	}
}
