package com.wiiudev.gecko.pointer.swing.preprocessed_search;

import lombok.val;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

import static com.wiiudev.gecko.pointer.swing.preprocessed_search.FileTypeImport.*;
import static com.wiiudev.gecko.pointer.swing.preprocessed_search.MemoryDumpDialog.toRelativeFilePath;

class MemoryDumpChooser extends JFileChooser
{
	private static final String APPLICATION_DIRECTORY = System.getProperty("user.dir");

	private final JTextField filePathField;
	private final boolean selectFolders;
	private final FileTypeImport fileType;

	MemoryDumpChooser(JTextField filePathField, boolean selectFolders, FileTypeImport fileType)
	{
		this.filePathField = filePathField;
		this.selectFolders = selectFolders;
		this.fileType = fileType;
	}

	boolean select(Component parent)
	{
		val fileSelectionMode = selectFolders ? DIRECTORIES_ONLY : FILES_ONLY;
		setFileSelectionMode(fileSelectionMode);
		setCurrentDirectory(this);

		if (selectFolders)
		{
			setDialogTitle("Add Folder");
		} else
		{
			setDialogTitle("Add File");
		}

		if (!selectFolders)
		{
			val memoryDumpsFilter = new FileNameExtensionFilter("Memory Dumps", MEMORY_DUMP.getExtension(),
					MEMORY_DUMP_EXTENSION_DMP, MEMORY_DUMP_EXTENSION_RAW);
			val pointerMapsFilter = new FileNameExtensionFilter("Pointer Maps", POINTER_MAP.getExtension());
			addChoosableFileFilter(memoryDumpsFilter);
			addChoosableFileFilter(pointerMapsFilter);

			switch (fileType)
			{
				case MEMORY_DUMP:
					setFileFilter(memoryDumpsFilter);
					break;

				case POINTER_MAP:
					setFileFilter(pointerMapsFilter);
					break;
			}
		}

		val selectedOption = showOpenDialog(parent);
		val approved = selectedOption == APPROVE_OPTION;

		if (approved)
		{
			val selectedFile = getSelectedFile();
			val selectedFilePath = selectedFile.getAbsolutePath();
			val relativeFilePath = toRelativeFilePath(selectedFilePath);
			filePathField.setText(relativeFilePath);
		}

		return approved;
	}

	private void setCurrentDirectory(JFileChooser fileChooser)
	{
		val applicationDirectoryFile = new File(APPLICATION_DIRECTORY);
		val currentDirectory = filePathField.getText();
		val currentDirectoryFile = new File(currentDirectory);

		if (currentDirectoryFile.exists())
		{
			fileChooser.setCurrentDirectory(currentDirectoryFile);
		} else
		{
			fileChooser.setCurrentDirectory(applicationDirectoryFile);
		}
	}
}
