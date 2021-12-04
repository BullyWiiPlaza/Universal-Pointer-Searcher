package com.wiiudev.gecko.pointer.swing;

import lombok.val;
import lombok.var;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.readAllBytes;
import static javax.swing.JOptionPane.showMessageDialog;

public class GUISettingsManager
{
	private final Path settingsFolderPath;

	public GUISettingsManager(final Path settingsFolderPath) throws IOException
	{
		this.settingsFolderPath = settingsFolderPath;
		createDirectories(settingsFolderPath);
	}

	private enum FileChooserOpenDialogType
	{
		OPEN_DIALOG,
		SAVE_DIALOG
	}

	private Path showFileChooser(final Component rootPane,
	                             final FileChooserOpenDialogType fileChooserOpenDialogType)
	{
		val fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setCurrentDirectory(settingsFolderPath.toFile());
		val forcedExtension = "json";
		val fileNameExtensionFilter = new FileNameExtensionFilter(forcedExtension.toUpperCase()
				+ " Files (*." + forcedExtension + ")", forcedExtension);
		fileChooser.setFileFilter(fileNameExtensionFilter);
		val selectedAnswer = fileChooserOpenDialogType.equals(FileChooserOpenDialogType.OPEN_DIALOG)
				? fileChooser.showOpenDialog(rootPane)
				: fileChooser.showSaveDialog(rootPane);
		if (selectedAnswer == JOptionPane.YES_OPTION)
		{
			var filePath = fileChooser.getSelectedFile().toString();
			if (!filePath.toLowerCase().endsWith("." + forcedExtension))
			{
				filePath += "." + forcedExtension;
			}
			return Paths.get(filePath);
		}

		return null;
	}

	// TODO Implement loading settings fully
	public void loadSettings(final Component rootPane) throws IOException
	{
		val filePath = showFileChooser(rootPane, FileChooserOpenDialogType.OPEN_DIALOG);
		if (filePath != null)
		{
			val fileContents = new String(readAllBytes(filePath), StandardCharsets.UTF_8);
			System.out.println("File contents: " + fileContents);
			showMessageDialog(rootPane,
					"The configuration has been restored successfully.",
					"Successfully restored",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	// TODO Implement saving settings fully
	public void saveSettings(final Component rootPane) throws IOException
	{
		val filePath = showFileChooser(rootPane, FileChooserOpenDialogType.SAVE_DIALOG);
		if (filePath != null)
		{
			Files.write(filePath, "".getBytes(StandardCharsets.UTF_8));
			showMessageDialog(rootPane,
					"The configuration has been saved successfully.",
					"Successfully saved",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}
}
