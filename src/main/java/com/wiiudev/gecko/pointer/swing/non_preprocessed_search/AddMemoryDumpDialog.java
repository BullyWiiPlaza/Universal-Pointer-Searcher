package com.wiiudev.gecko.pointer.swing.non_preprocessed_search;

import com.wiiudev.gecko.pointer.non_preprocessed_search.MemoryDump;
import com.wiiudev.gecko.pointer.swing.utilities.JTextAreaLimit;
import lombok.val;
import lombok.var;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.wiiudev.gecko.pointer.swing.StackTraceUtilities.handleException;
import static com.wiiudev.gecko.pointer.swing.utilities.FrameUtilities.setWindowIconImage;
import static com.wiiudev.gecko.pointer.swing.utilities.JTextAreaLimit.isHexadecimal;
import static java.nio.file.Files.*;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import static javax.swing.JFileChooser.FILES_ONLY;

public class AddMemoryDumpDialog extends JDialog
{
	private JPanel contentPane;
	private JButton confirmButton;
	private JButton cancelButton;
	private JButton browseMemoryDumpButton;
	private JTextField pathField;
	private JTextField targetAddressField;
	private JCheckBox equalsFilenameCheckBox;
	private JCheckBox parseFolderCheckBox;

	private boolean confirmed;

	public AddMemoryDumpDialog(JRootPane rootPane)
	{
		equalsFilenameCheckBox.addChangeListener(changeEvent -> setTargetAddressFieldAvailability());
		parseFolderCheckBox.addChangeListener(changeEvent -> setEqualsFilenameCheckBoxAvailability());

		setLocationRelativeTo(rootPane);
		setDialogProperties();

		setContentPane(contentPane);
		setModal(true);
		getRootPane().setDefaultButton(confirmButton);

		confirmButton.addActionListener(actionEvent -> onOK());

		cancelButton.addActionListener(actionEvent -> onCancel());

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent windowEvent)
			{
				onCancel();
			}
		});

		contentPane.registerKeyboardAction(actionEvent -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

		targetAddressField.setDocument(new JTextAreaLimit());

		targetAddressField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent documentEvent)
			{
				setConfirmButtonAvailability();
			}

			@Override
			public void removeUpdate(DocumentEvent documentEvent)
			{
				setConfirmButtonAvailability();
			}

			@Override
			public void changedUpdate(DocumentEvent documentEvent)
			{
				setConfirmButtonAvailability();
			}
		});

		browseMemoryDumpButton.addActionListener(actionEvent ->
		{
			val memoryDumpChooser = new JFileChooser();
			val memoryDumpPath = pathField.getText();

			if (new File(memoryDumpPath).exists())
			{
				memoryDumpChooser.setCurrentDirectory(new File(memoryDumpPath));
			} else
			{
				val programDirectory = System.getProperty("user.dir");
				val programDirectoryFolder = new File(programDirectory);
				memoryDumpChooser.setCurrentDirectory(programDirectoryFolder);
			}

			int fileSelectionMode;
			val shouldParseFolder = parseFolderCheckBox.isSelected();

			if (shouldParseFolder)
			{
				fileSelectionMode = DIRECTORIES_ONLY;
			} else
			{
				fileSelectionMode = FILES_ONLY;
			}

			memoryDumpChooser.setFileSelectionMode(fileSelectionMode);

			val selectedAnswer = memoryDumpChooser.showOpenDialog(rootPane);

			if (selectedAnswer == JOptionPane.YES_OPTION)
			{
				val selectedFile = memoryDumpChooser.getSelectedFile();
				pathField.setText(selectedFile.getAbsolutePath());
				setConfirmButtonAvailability();
			}
		});

		setTargetAddressFieldAvailability();
		setConfirmButtonAvailability();
		setEqualsFilenameCheckBoxAvailability();
	}

	private void setEqualsFilenameCheckBoxAvailability()
	{
		val forceEqualsFilename = parseFolderCheckBox.isSelected();

		if (forceEqualsFilename)
		{
			equalsFilenameCheckBox.setSelected(true);
			equalsFilenameCheckBox.setEnabled(false);
		} else
		{
			equalsFilenameCheckBox.setEnabled(true);
		}
	}

	private void setTargetAddressFieldAvailability()
	{
		val shouldEnableTargetAddressField = !equalsFilenameCheckBox.isSelected();
		targetAddressField.setEnabled(shouldEnableTargetAddressField);
		setConfirmButtonAvailability();
	}

	private void setConfirmButtonAvailability()
	{
		val pathString = pathField.getText();
		val path = Paths.get(pathString);
		val pathExists = exists(path);
		val parseFolderChecked = parseFolderCheckBox.isSelected();
		var pathValid = false;

		restoreMemoryDumpStartingAddress(path);

		if (parseFolderChecked)
		{
			if (isDirectory(path))
			{
				try
				{
					if (!MemoryDump.getMemoryDumps(pathString).isEmpty())
					{
						pathValid = true;
					}
				} catch (Exception ignored)
				{
					// At least one invalid file name found
				}
			}
		} else if (isRegularFile(path))
		{
			if (pathString.toLowerCase().endsWith(".bin"))
			{
				pathValid = true;
			}
		}

		pathValid = pathExists && pathValid;

		val targetAddressValue = targetAddressField.getText();
		val equalsFilenameChecked = equalsFilenameCheckBox.isSelected();
		var targetAddressValid = false;

		if (equalsFilenameChecked)
		{
			if (parseFolderChecked)
			{
				targetAddressValid = true;
			} else if (isHexadecimal(path.getFileName().toString().split("\\.(?=[^\\.]+$)")[0]))
			{
				targetAddressValid = true;
			}
		} else if (isHexadecimal(targetAddressValue))
		{
			targetAddressValid = true;
		}

		val shouldEnableConfirmButton = pathValid && targetAddressValid;
		confirmButton.setEnabled(shouldEnableConfirmButton);
	}

	private void restoreMemoryDumpStartingAddress(Path path)
	{
		try
		{
			val isDirectory = isDirectory(path);

			long dumpStartAddress;
			if (isDirectory)
			{
				dumpStartAddress = MemoryDump.getDumpStartAddress(path.toString());
			} else
			{
				val parentPath = path.getParent();
				dumpStartAddress = MemoryDump.getDumpStartAddress(parentPath.toString());
			}
		} catch (IOException exception)
		{
			handleException(rootPane, exception);
		}
	}

	private void onOK()
	{
		confirmed = true;
		dispose();
	}

	private void onCancel()
	{
		confirmed = false;
		dispose();
	}

	private void setDialogProperties()
	{
		setTitle("Add memory dump(s)");
		setSize(400, 400);
		setWindowIconImage(this);
	}
}
