package com.wiiudev.gecko.pointer.swing.preprocessed_search;

import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryDump;
import com.wiiudev.gecko.pointer.swing.utilities.JTextAreaLimit;
import com.wiiudev.gecko.pointer.swing.utilities.MemoryDumpsByteOrder;
import lombok.Getter;
import lombok.val;
import lombok.var;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.wiiudev.gecko.pointer.swing.StackTraceUtilities.handleException;
import static com.wiiudev.gecko.pointer.swing.utilities.FrameUtilities.getSelectedItem;
import static com.wiiudev.gecko.pointer.swing.utilities.FrameUtilities.setWindowIconImage;
import static com.wiiudev.gecko.pointer.swing.utilities.MemoryDumpsByteOrder.getMemoryDumpsByteOrder;
import static com.wiiudev.gecko.pointer.swing.utilities.ProgramDirectoryUtilities.getProgramDirectory;
import static com.wiiudev.gecko.pointer.utilities.FileNameUtilities.getBaseFileName;
import static com.wiiudev.gecko.pointer.utilities.FileNameUtilities.getTargetAddressFromFile;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static java.awt.Desktop.getDesktop;
import static java.io.File.separator;
import static java.lang.Long.parseUnsignedLong;
import static java.lang.Long.toHexString;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static javax.swing.JOptionPane.*;
import static javax.swing.SwingUtilities.invokeLater;

public class MemoryDumpDialog extends JDialog
{
	private static final Color INVALID_INPUT_COLOR = RED;
	private static final Color VALID_INPUT_COLOR = GREEN;
	private static final String DUMMY_LABEL_TEXT = "dummy";

	private static final String STARTING_ADDRESS_CHECK_OK = "Starting address check: OK!";
	private static final String STARTING_ADDRESS_INVALID_INTEGER = "Failed parsing starting address";
	private static final String INVALID_FOLDER_MEMORY_DUMPS_STARTING_ADDRESS = "Invalid starting address for at least one of the memory dumps/pointer maps";
	private static final String FILE_PATH_CHECK_OK = "File path check: OK!";
	private static final String INVALID_FILE_PATH = "Invalid file path";

	private JPanel contentPane;

	private JTextField filePathField;
	private JButton browseButtonField;
	private JComboBox<MemoryDumpsByteOrder> byteOrderSelection;
	private JFormattedTextField startingAddressField;
	private JFormattedTextField targetAddressField;
	private JButton confirmMemoryDumpButton;
	private JButton byteOrderInformationButton;
	private JCheckBox parseEntireFolderCheckBox;
	private JLabel folderImporterLabel;
	private JComboBox<FileTypeImport> fileTypeSelection;
	private JLabel startingAddressFieldLabel;
	private JLabel targetAddressFieldLabel;
	private JLabel filePathValidatorLabel;
	private JCheckBox addModuleDumpsFolderCheckBox;
	private JComboBox<InputType> inputTypeSelection;
	private JLabel byteOrderLabel;

	@Getter
	private JSpinner comparisonGroupNumberSpinner;

	private final List<JLabel> statusLabels;

	@Getter
	private boolean memoryDumpAdded;

	@Getter
	private MemoryDump memoryDump;

	@Getter
	private List<MemoryDump> memoryDumps;

	@Getter
	private List<MemoryDump> pointerMaps;

	private final boolean mayParseFolder;

	public MemoryDumpDialog(MemoryDump memoryDump, boolean mayParseFolder)
	{
		this.memoryDump = memoryDump;
		this.mayParseFolder = mayParseFolder;


		if (memoryDump != null)
		{
			val isAddedAsFolder = memoryDump.isAddedAsFolder();
			addModuleDumpsFolderCheckBox.setSelected(isAddedAsFolder);
		}

		statusLabels = new ArrayList<>();
		statusLabels.add(folderImporterLabel);
		statusLabels.add(filePathValidatorLabel);
		statusLabels.add(startingAddressFieldLabel);
		statusLabels.add(targetAddressFieldLabel);

		configureFrameProperties();
		populateByteOrders();
		byteOrderLabel.setVisible(false);
		byteOrderInformationButton.setVisible(false);
		byteOrderSelection.setVisible(false);
		addFilePathFieldDocumentListener();
		addBrowseMemoryDumpButtonListener();
		addAddMemoryDumpButtonListener();
		addByteOrderInformationButtonListener();
		runComponentAvailabilitySetter();
		addTextFieldInputRestrictions();
		considerPopulatingFields();
		val comparisonGroupSpinnerNumberModel = new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1);
		comparisonGroupNumberSpinner.setModel(comparisonGroupSpinnerNumberModel);
		fileTypeSelection.setModel(new DefaultComboBoxModel<>(FileTypeImport.values()));
		inputTypeSelection.setModel(new DefaultComboBoxModel<>(InputType.values()));
	}

	private void addTextFieldInputRestrictions()
	{
		startingAddressField.setDocument(new JTextAreaLimit());
		targetAddressField.setDocument(new JTextAreaLimit());
	}

	public boolean isAddModuleDumpsFolderSelected()
	{
		return addModuleDumpsFolderCheckBox.isSelected();
	}

	public boolean shouldParseEntireFolder()
	{
		return parseEntireFolderCheckBox.isSelected();
	}

	private void considerPopulatingFields()
	{
		if (memoryDump != null)
		{
			filePathField.setText(memoryDump.getFilePath().toString());
			val startingAddress = memoryDump.getStartingAddress();
			startingAddressField.setText(startingAddress == null ? "" : toHexString(startingAddress).toUpperCase());
			val targetAddress = memoryDump.getTargetAddress();
			targetAddressField.setText(targetAddress == null ? "" : toHexString(targetAddress).toUpperCase());
			val byteOrder = memoryDump.getByteOrder();
			val memoryDumpsByteOrder = getMemoryDumpsByteOrder(byteOrder);
			byteOrderSelection.setSelectedItem(memoryDumpsByteOrder);
		}
	}

	private void addByteOrderInformationButtonListener()
	{
		byteOrderInformationButton.addActionListener(actionEvent ->
		{
			val selectedAnswer = showConfirmDialog(
					rootPane,
					"The order of bytes in memory can differ between systems.\n" +
							"The Wii and Wii U for example use big endian while the 3DS uses little endian.\n" +
							"Do you want to read more about byte orders?",
					"Read more?",
					YES_NO_OPTION);

			if (selectedAnswer == YES_OPTION)
			{
				try
				{
					val desktop = getDesktop();
					desktop.browse(new URI("https://en.wikipedia.org/wiki/Endianness"));
				} catch (Exception exception)
				{
					handleException(rootPane, exception);
				}
			}
		});
	}

	private void addFilePathFieldDocumentListener()
	{
		val document = filePathField.getDocument();

		document.addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent documentEvent)
			{
				tryPopulatingFields();
			}

			@Override
			public void removeUpdate(DocumentEvent documentEvent)
			{
				tryPopulatingFields();
			}

			@Override
			public void changedUpdate(DocumentEvent documentEvent)
			{
				tryPopulatingFields();
			}
		});
	}

	private void tryPopulatingFields()
	{
		tryPopulatingTargetAddress();
		tryPopulatingStartingAddress();
	}

	private void tryPopulatingTargetAddress()
	{
		val filePath = getFilePath();

		if (new File(filePath).isDirectory())
		{
			return;
		}

		try
		{
			val targetAddress = getTargetAddressFromFile(filePath);
			targetAddressField.setText(toHexString(targetAddress).toUpperCase());
		} catch (NumberFormatException ignored)
		{

		}
	}

	private void tryPopulatingStartingAddress()
	{
		val filePath = getFilePath();

		// Beginning of Wii U memory dump
		try
		{
			val randomAccessFile = new RandomAccessFile(filePath, "r");
			val firstValue = randomAccessFile.readInt();
			if (firstValue == 0x3E8)
			{
				startingAddressField.setText("10000000");
			}
		} catch (Exception ignored)
		{

		}
	}

	private void populateByteOrders()
	{
		byteOrderSelection.setModel(new DefaultComboBoxModel<>(MemoryDumpsByteOrder.values()));
	}

	private void addBrowseMemoryDumpButtonListener()
	{
		browseButtonField.addActionListener(actionEvent ->
		{
			val shouldParseFolder = parseEntireFolderCheckBox.isSelected() || addModuleDumpsFolderCheckBox.isSelected();
			val fileType = getSelectedItem(fileTypeSelection);
			val memoryDumpChooser = new MemoryDumpChooser(filePathField, shouldParseFolder, fileType);
			val approved = memoryDumpChooser.select(this);

			val background = startingAddressField.getBackground();
			if (approved && background.equals(INVALID_INPUT_COLOR))
			{
				startingAddressField.requestFocusInWindow();
			}
		});
	}

	private void runComponentAvailabilitySetter()
	{
		val thread = new Thread(() ->
		{
			// Wait till the dialog is visible
			while (!isShowing())
			{
				try
				{
					//noinspection BusyWait
					Thread.sleep(10);
				} catch (InterruptedException exception)
				{
					exception.printStackTrace();
				}
			}

			// Keep setting the availability and background colors
			while (isShowing())
			{
				// Verify the file path
				val filePath = getFilePath();
				var isFilePathValid = false;

				val shouldParseEntireFolder = parseEntireFolderCheckBox.isSelected();
				String filePathValidatorText;

				try
				{
					val path = Paths.get(filePath);
					val shouldParseFolder = shouldParseEntireFolder || addModuleDumpsFolderCheckBox.isSelected();
					isFilePathValid = shouldParseFolder ? isDirectory(path) : isRegularFile(path);
					filePathValidatorText = "The file path is not a " + (shouldParseFolder ? "directory" : "file");
				} catch (InvalidPathException ignored)
				{
					filePathValidatorText = INVALID_FILE_PATH;
				}

				val isImportableFilesOkay = new boolean[]{true};
				val finalIsFilePathValid = isFilePathValid;
				val finalFilePathValidatorText = filePathValidatorText;
				val selectedFileType = getSelectedItem(fileTypeSelection);
				invokeLater(() ->
				{
					val isParsingEntireFolderAllowed = mayParseFolder && !addModuleDumpsFolderCheckBox.isSelected();
					parseEntireFolderCheckBox.setEnabled(isParsingEntireFolderAllowed);
					inputTypeSelection.setEnabled(selectedFileType.equals(FileTypeImport.MEMORY_DUMP));
					val isComparisonGroupNumberEnabled = getSelectedItem(inputTypeSelection).equals(InputType.COMPARISON)
							&& getSelectedItem(fileTypeSelection).equals(FileTypeImport.MEMORY_DUMP);
					comparisonGroupNumberSpinner.setEnabled(isComparisonGroupNumberEnabled);
					addModuleDumpsFolderCheckBox.setEnabled(mayParseFolder && !parseEntireFolderCheckBox.isSelected());
					filePathField.setBackground(finalIsFilePathValid ? VALID_INPUT_COLOR : INVALID_INPUT_COLOR);
					setValidationLabel(finalIsFilePathValid, FILE_PATH_CHECK_OK,
							finalFilePathValidatorText, filePathValidatorLabel);
					targetAddressField.setEnabled(true);
					// targetAddressField.setEnabled(!shouldParseEntireFolder || addModuleDumpsFolderCheckBox.isSelected());
					fileTypeSelection.setEnabled(!shouldParseEntireFolder && !addModuleDumpsFolderCheckBox.isSelected());

					val detectedFileTypeImport = FileTypeImport.parseFileTypeImport(filePath);
					if (detectedFileTypeImport != null)
					{
						// fileTypeSelection.setSelectedItem(detectedFileTypeImport);
						// fileTypeSelection.setEnabled(false);
					}

					if (shouldParseEntireFolder && finalIsFilePathValid)
					{
						val memoryDumps = new ArrayList<File>();
						val pointerMaps = new ArrayList<File>();
						findFiles(memoryDumps, pointerMaps);
						val folderImporterLabelText = getFolderImporterLabelText(memoryDumps, pointerMaps);
						folderImporterLabel.setText(folderImporterLabelText);
						int importableFilesCount = memoryDumps.size() + pointerMaps.size();
						isImportableFilesOkay[0] = importableFilesCount > 0;
					} else if (addModuleDumpsFolderCheckBox.isSelected())
					{
						val moduleDumps = new ArrayList<File>();
						val pointerMaps = new ArrayList<File>();
						findFiles(moduleDumps, pointerMaps);
						val moduleDumpsLabelText = getModulesMemoryDumpLabelText(moduleDumps);
						folderImporterLabel.setText(moduleDumpsLabelText);
					} else
					{
						folderImporterLabel.setText("");
					}
				});

				// Verify the starting address
				var startingAddress = -1L;
				boolean isStartingAddressFieldValid = true;
				String parsingExceptionMessage = null;

				if (!selectedFileType.equals(FileTypeImport.POINTER_MAP))
				{
					try
					{
						startingAddress = parseUnsignedLong(startingAddressField.getText(), 16);
					} catch (NumberFormatException exception)
					{
						parsingExceptionMessage = exception.getMessage();
						isStartingAddressFieldValid = false;
					}
				}

				var areAllMemoryDumpsOkay = false;
				if (shouldParseEntireFolder && finalIsFilePathValid)
				{
					val memoryDumps = new ArrayList<File>();
					val pointerMaps = new ArrayList<File>();
					findFiles(memoryDumps, pointerMaps);

					try
					{
						areAllMemoryDumpsOkay = areAllMemoryDumpsOkay(startingAddress, memoryDumps);
						areAllMemoryDumpsOkay = true;

						/* if (areAllMemoryDumpsOkay)
						{
							areAllMemoryDumpsOkay = areAllMemoryDumpsOkay(startingAddress, pointerMaps);
						} */
					} catch (IOException exception)
					{
						exception.printStackTrace();
					}

					val finalAreAllMemoryDumpsOkay = areAllMemoryDumpsOkay;
					invokeLater(() -> confirmMemoryDumpButton.setEnabled(finalAreAllMemoryDumpsOkay));
				}

				val finalIsStartingAddressFieldValid = isStartingAddressFieldValid;
				val finalParsingExceptionMessage = parsingExceptionMessage;
				invokeLater(() ->
				{
					val background = finalIsStartingAddressFieldValid ? VALID_INPUT_COLOR : INVALID_INPUT_COLOR;
					setValidationLabel(finalIsStartingAddressFieldValid,
							STARTING_ADDRESS_CHECK_OK,
							STARTING_ADDRESS_INVALID_INTEGER + ": " + finalParsingExceptionMessage,
							startingAddressFieldLabel);
					startingAddressField.setBackground(background);
					val isPointerMapSelected = isPointerMapSelected();
					byteOrderSelection.setEnabled(!isPointerMapSelected);
				});

				if (targetAddressField.isEnabled())
				{
					// Verify the target address
					val isTargetAddressFieldValid = isTargetAddressFieldValid(filePath, startingAddress, isStartingAddressFieldValid);
					val isAddingMemoryDumpAllowed = isFilePathValid && isStartingAddressFieldValid && isTargetAddressFieldValid;
					invokeLater(() -> confirmMemoryDumpButton.setEnabled(isAddingMemoryDumpAllowed));
				} else
				{
					val finalAreAllMemoryDumpsOkay = areAllMemoryDumpsOkay;
					invokeLater(() ->
					{
						setValidationLabel(finalAreAllMemoryDumpsOkay, "", "", targetAddressFieldLabel);
						setValidationLabel(finalAreAllMemoryDumpsOkay,
								STARTING_ADDRESS_CHECK_OK,
								INVALID_FOLDER_MEMORY_DUMPS_STARTING_ADDRESS,
								startingAddressFieldLabel);
						targetAddressField.setText("");
						targetAddressField.setBackground(VALID_INPUT_COLOR);
						startingAddressField.setBackground(finalAreAllMemoryDumpsOkay ? VALID_INPUT_COLOR : INVALID_INPUT_COLOR);
						confirmMemoryDumpButton.setEnabled(finalAreAllMemoryDumpsOkay && isImportableFilesOkay[0]);
					});
				}

				try
				{
					//noinspection BusyWait
					Thread.sleep(50);
				} catch (InterruptedException exception)
				{
					exception.printStackTrace();
				}
			}
		});

		thread.start();
	}

	private String getModulesMemoryDumpLabelText(List<File> memoryDumps)
	{
		val memoryDumpsSize = memoryDumps.size();
		return memoryDumpsSize + " module dump(s) found";
	}

	private boolean areAllMemoryDumpsOkay(long startingAddress, List<File> memoryDumps) throws IOException
	{
		for (val memoryDump : memoryDumps)
		{
			val absolutePath = memoryDump.getAbsolutePath();
			val targetAddress = getTargetAddressFromFile(absolutePath);
			val offset = targetAddress - startingAddress;
			val fileSize = getFileSize(absolutePath);
			val isAddingMemoryDumpAllowed = targetAddress >= startingAddress && offset <= fileSize;
			if (!isAddingMemoryDumpAllowed)
			{
				return false;
			}
		}
		return true;
	}

	private void setValidationLabel(boolean validationPassed,
	                                String validationPassedText,
	                                String validationFailedText,
	                                JLabel validationLabel)
	{
		if (validationPassed)
		{
			validationLabel.setForeground(VALID_INPUT_COLOR);
			validationLabel.setText(validationPassedText);
		} else
		{
			validationLabel.setForeground(INVALID_INPUT_COLOR);
			validationLabel.setText(validationFailedText);
		}
	}

	private boolean isPointerMapSelected()
	{
		val selectedItem = getSelectedItem(fileTypeSelection);
		return FileTypeImport.POINTER_MAP.equals(selectedItem);
	}

	private boolean isMemoryDumpSelected()
	{
		val selectedItem = getSelectedItem(fileTypeSelection);
		return FileTypeImport.MEMORY_DUMP.equals(selectedItem);
	}

	private String getFolderImporterLabelText(ArrayList<File> memoryDumps, ArrayList<File> pointerMaps)
	{
		val memoryDumpsSize = memoryDumps.size();
		val pointerMapsSize = pointerMaps.size();
		return memoryDumpsSize + " memory dump(s) and "
				+ pointerMapsSize + " pointer map(s) found";
	}

	private void findFiles(ArrayList<File> memoryDumps, ArrayList<File> pointerMaps)
	{
		val directoryFilePath = getFilePath();
		val directory = new File(directoryFilePath);
		val listedFiles = directory.listFiles();
		if (listedFiles != null)
		{
			for (val listedFile : listedFiles)
			{
				val listedFilePath = listedFile.toString();
				if (listedFilePath.endsWith("." + FileTypeImport.MEMORY_DUMP.getExtension())
						|| listedFilePath.endsWith("." + FileTypeImport.MEMORY_DUMP_EXTENSION_DMP)
				        || listedFilePath.endsWith("." + FileTypeImport.MEMORY_DUMP_EXTENSION_RAW))
				{
					memoryDumps.add(listedFile);
				} else if (listedFilePath.endsWith("." + FileTypeImport.POINTER_MAP.getExtension()))
				{
					pointerMaps.add(listedFile);
				}
			}
		}
	}

	private String getFilePath()
	{
		val filePath = filePathField.getText();
		return toRelativeFilePath(filePath);
	}

	static String toRelativeFilePath(String filePath)
	{
		val programDirectory = getProgramDirectory();

		val programDirectoryPrefix = programDirectory + separator;
		if (filePath.startsWith(programDirectoryPrefix))
		{
			return filePath.replace(programDirectoryPrefix, "");
		}

		return filePath;
	}

	private boolean isTargetAddressFieldValid(String filePath, long startingAddress, boolean isStartingAddressFieldValid)
	{
		invokeLater(() ->
		{
			val fileTypeImport = getSelectedItem(fileTypeSelection);
			if (addModuleDumpsFolderCheckBox.isSelected())
			{
				if (!fileTypeImport.equals(FileTypeImport.MEMORY_DUMP))
				{
					fileTypeSelection.setSelectedItem(FileTypeImport.MEMORY_DUMP);
				}

				if (parseEntireFolderCheckBox.isSelected())
				{
					parseEntireFolderCheckBox.setSelected(false);
				}

				startingAddressField.setText("0");
				startingAddressField.setEnabled(false);
			} else
			{
				startingAddressField.setEnabled(fileTypeImport.equals(FileTypeImport.MEMORY_DUMP));
			}
		});

		var targetAddress = -1L;
		var isTargetAddressFieldValid = true;
		var targetAddressValidationFailedText = "";

		try
		{
			val targetAddressFieldText = targetAddressField.getText();
			targetAddress = parseUnsignedLong(targetAddressFieldText, 16);
		} catch (NumberFormatException exception)
		{
			targetAddressValidationFailedText = "Failed parsing target address: " + exception.getMessage();
			isTargetAddressFieldValid = false;
		}

		if (isTargetAddressFieldValid)
		{
			if (isStartingAddressFieldValid)
			{
				val targetOffset = targetAddress - startingAddress;
				if (isMemoryDumpSelected())
				{
					/* try
					{
						val memoryDumpFileSize = getFileSize(filePath);
						if (memoryDumpFileSize == -1)
						{
							isTargetAddressFieldValid = true;
						} else
						{
							isTargetAddressFieldValid = targetOffset >= 0 && targetOffset <= memoryDumpFileSize;
						}
						targetAddressValidationFailedText = "Target address outside of memory dump bounds";
					} catch (IOException | InvalidPathException exception)
					{
						isTargetAddressFieldValid = false;
						targetAddressValidationFailedText = "Failed validating target address inclusiveness";
					} */
				} else if (targetOffset < 0)
				{
					isTargetAddressFieldValid = false;
					targetAddressValidationFailedText = "The target address cannot be before the starting address";
				}
			}
		}

		if (parseEntireFolderCheckBox.isSelected() && !addModuleDumpsFolderCheckBox.isSelected())
		{
			isTargetAddressFieldValid = true;
		}

		val finalIsTargetAddressFieldValid = isTargetAddressFieldValid;
		val finalTargetAddressValidationFailedText = targetAddressValidationFailedText;
		invokeLater(() ->
		{
			val background = finalIsTargetAddressFieldValid ? VALID_INPUT_COLOR : INVALID_INPUT_COLOR;
			targetAddressField.setBackground(background);

			if (targetAddressField.isEnabled())
			{
				setValidationLabel(finalIsTargetAddressFieldValid, "Target address check: OK!",
						finalTargetAddressValidationFailedText, targetAddressFieldLabel);
			} else
			{
				targetAddressFieldLabel.setText("");
			}
		});

		return isTargetAddressFieldValid;
	}

	private long getFileSize(String filePath) throws IOException
	{
		var memoryDumpFileSize = -1L;
		val filePathObject = Paths.get(filePath);

		if (isRegularFile(filePathObject))
		{
			memoryDumpFileSize = Files.size(Paths.get(filePath));
		}

		return memoryDumpFileSize;
	}

	private void configureFrameProperties()
	{
		setContentPane(contentPane);
		setModal(true);
		setWindowIconImage(this);
		packDialog();
	}

	private void packDialog()
	{
		for (val statusLabel : statusLabels)
		{
			statusLabel.setText(DUMMY_LABEL_TEXT);
		}

		pack();

		for (val statusLabel : statusLabels)
		{
			statusLabel.setText("");
		}
	}

	private void addAddMemoryDumpButtonListener()
	{
		confirmMemoryDumpButton.addActionListener(actionEvent ->
		{
			dispose();

			val filePath = getFilePath();
			val fileType = getSelectedItem(fileTypeSelection);
			val startingAddress = fileType.equals(FileTypeImport.POINTER_MAP)
					? null : parseUnsignedLong(startingAddressField.getText(), 16);
			val selectedByteOrder = getSelectedItem(byteOrderSelection);
			val byteOrder = selectedByteOrder == null ? null : selectedByteOrder.getByteOrder();
			val targetAddressFieldText = targetAddressField.getText();
			val targetAddress = targetAddressFieldText.equals("")
					? null : (Long) parseUnsignedLong(targetAddressFieldText, 16);
			memoryDump = new MemoryDump(filePath, startingAddress, targetAddress, byteOrder);
			memoryDump.setInputType(getSelectedItem(inputTypeSelection));
			val comparisonGroupNumber = Integer.parseInt(String.valueOf(comparisonGroupNumberSpinner.getValue()));
			memoryDump.setComparisonGroupNumber(comparisonGroupNumber);
			memoryDump.setAddedAsFolder(addModuleDumpsFolderCheckBox.isSelected());

			val parseEntireFolderCheckBoxSelected = parseEntireFolderCheckBox.isSelected();
			if (parseEntireFolderCheckBoxSelected && !addModuleDumpsFolderCheckBox.isSelected())
			{
				parseFolder(startingAddress, byteOrder);
			} else
			{
				memoryDumpAdded = true;
				memoryDump.setFileType(fileType);
			}
		});
	}

	private void parseFolder(long startingAddress, ByteOrder byteOrder)
	{
		val memoryDumpPaths = new ArrayList<File>();
		val pointerMapPaths = new ArrayList<File>();
		findFiles(memoryDumpPaths, pointerMapPaths);

		memoryDumpAdded = true;
		memoryDumps = new ArrayList<>();
		for (val memoryDumpPath : memoryDumpPaths)
		{
			val absolutePath = memoryDumpPath.getAbsolutePath();

			try
			{
				val targetAddressFromFile = getTargetAddressFromFile(absolutePath);
				val memoryDump = new MemoryDump(absolutePath, startingAddress,
						targetAddressFromFile, byteOrder);
				memoryDumps.add(memoryDump);
			} catch (Exception exception)
			{
				handleException(rootPane, exception);
			}
		}

		pointerMaps = new ArrayList<>();
		for (val pointerMapPath : pointerMapPaths)
		{
			val pointerMapAbsolutePath = pointerMapPath.getAbsolutePath();
			val pointerMapBaseFileName = getBaseFileName(pointerMapAbsolutePath);

			for (val memoryDump : memoryDumps)
			{
				val memoryDumpFilePath = memoryDump.getFilePath().toString();
				val memoryDumpBaseFileName = getBaseFileName(memoryDumpFilePath);
				if (pointerMapBaseFileName.equals(memoryDumpBaseFileName))
				{
					val targetAddress = getTargetAddressFromFile(memoryDumpFilePath);
					val pointerMapMemoryDump = new MemoryDump(pointerMapAbsolutePath,
							startingAddress, targetAddress, null);
					pointerMapMemoryDump.setFileType(FileTypeImport.POINTER_MAP);
					pointerMaps.add(pointerMapMemoryDump);
				}
			}
		}
	}

	public void setFilePath(Path filePath)
	{
		if (filePath != null)
		{
			filePathField.setText(filePath.toString());
		}
	}

	public void setLastAddedStartingAddress(Long startingAddress)
	{
		if (startingAddress != null)
		{
			startingAddressField.setText(toHexString(startingAddress).toUpperCase());
		}
	}

	public boolean isParseEntireFolderSelected()
	{
		return parseEntireFolderCheckBox.isSelected();
	}

	public void setParseEntireFolder(boolean selected)
	{
		parseEntireFolderCheckBox.setSelected(selected);
	}

	public void setByteOrder(ByteOrder byteOrder)
	{
		if (byteOrder != null)
		{
			val memoryDumpsByteOrder = getMemoryDumpsByteOrder(byteOrder);
			byteOrderSelection.setSelectedItem(memoryDumpsByteOrder);
		}
	}

	public void setLastAddedTargetAddress(Long targetAddress)
	{
		if (targetAddress != null)
		{
			targetAddressField.setText(toHexString(targetAddress).toUpperCase());
		}
	}

	public void setAddModuleDumpsFolder(boolean addModuleDumpsFolder)
	{
		addModuleDumpsFolderCheckBox.setSelected(addModuleDumpsFolder);
	}

	public void setFileType(final FileTypeImport fileType)
	{
		fileTypeSelection.setSelectedItem(fileType);
	}

	public void setInputType(final InputType inputType)
	{
		inputTypeSelection.setSelectedItem(inputType);
	}
}
