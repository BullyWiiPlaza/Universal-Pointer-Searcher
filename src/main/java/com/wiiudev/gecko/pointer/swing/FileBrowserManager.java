package com.wiiudev.gecko.pointer.swing;

import com.wiiudev.gecko.pointer.swing.utilities.ProgramDirectoryUtilities;
import lombok.val;
import lombok.var;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.awt.Color.GREEN;
import static java.awt.Color.RED;

public class FileBrowserManager
{
	private final JCheckBox checkBox;
	private final JTextField textField;
	private final JButton browseButton;
	private final OpenDialogType openDialogType;

	public FileBrowserManager(final JCheckBox checkBox, final JTextField textField, final JButton browseButton,
	                          final OpenDialogType openDialogType)
	{
		this.checkBox = checkBox;
		this.textField = textField;
		this.browseButton = browseButton;
		this.openDialogType = openDialogType;
	}

	public enum OpenDialogType
	{
		SAVE,
		OPEN
	}

	public void configure(final Component rootPane)
	{
		val document = textField.getDocument();
		document.addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(final DocumentEvent documentEvent)
			{
				validateFilePath();
			}

			@Override
			public void removeUpdate(final DocumentEvent documentEvent)
			{
				validateFilePath();
			}

			@Override
			public void changedUpdate(final DocumentEvent documentEvent)
			{
				validateFilePath();
			}
		});

		checkBox.addItemListener(itemEvent -> setComponentsAvailability());
		setComponentsAvailability();

		addBrowseButtonActionListener(rootPane);
	}

	private void addBrowseButtonActionListener(final Component rootPane)
	{
		browseButton.addActionListener(actionEvent ->
		{
			val fileChooser = new JFileChooser();
			try
			{
				val storeMemoryPointersFilePath = textField.getText();
				fileChooser.setCurrentDirectory(Paths.get(storeMemoryPointersFilePath).getParent().toFile());
			} catch (final Exception ignored)
			{
				fileChooser.setCurrentDirectory(new File(ProgramDirectoryUtilities.getProgramDirectory()));
				// We don't care if this fails
			}
			val filter = new FileNameExtensionFilter("Text files (.txt)", "txt");
			fileChooser.setFileFilter(filter);
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			val selectedAnswer = openDialogType.equals(OpenDialogType.OPEN)
					? fileChooser.showOpenDialog(rootPane)
					: fileChooser.showSaveDialog(rootPane);
			if (selectedAnswer == JOptionPane.YES_OPTION)
			{
				val selectedFile = fileChooser.getSelectedFile();
				var filePath = selectedFile.toPath().toString();
				val forcedExtension = ".txt";
				if (!filePath.toLowerCase().endsWith(forcedExtension))
				{
					filePath += forcedExtension;
				}
				textField.setText(filePath);
			}
		});
	}

	private void setComponentsAvailability()
	{
		val isSelected = checkBox.isSelected();
		textField.setEnabled(isSelected);
		browseButton.setEnabled(isSelected);
		validateFilePath();
	}

	private void validateFilePath()
	{
		if (checkBox.isSelected())
		{
			val filePath = Paths.get(textField.getText());
			val isValidFilePath = openDialogType.equals(OpenDialogType.OPEN)
					? Files.isRegularFile(filePath)
					: !Files.isDirectory(filePath);
			textField.setBackground(isValidFilePath ? GREEN : RED);
		} else
		{
			textField.setBackground(GREEN);
		}
	}
}
