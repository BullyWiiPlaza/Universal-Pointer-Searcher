package com.wiiudev.gecko.pointer.swing;

import com.wiiudev.gecko.pointer.NativePointerSearcherManager;
import com.wiiudev.gecko.pointer.NativePointerSearcherOutput;
import com.wiiudev.gecko.pointer.preprocessed_search.MemoryPointerList;
import com.wiiudev.gecko.pointer.preprocessed_search.MemoryPointerSearcher;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.*;
import com.wiiudev.gecko.pointer.swing.preprocessed_search.MemoryDumpDialog;
import com.wiiudev.gecko.pointer.swing.utilities.JTextAreaLimit;
import com.wiiudev.gecko.pointer.swing.utilities.PersistentSettingsManager;
import com.wiiudev.gecko.pointer.swing.utilities.WindowsTaskBarProgress;
import com.wiiudev.gecko.pointer.utilities.Benchmark;
import lombok.val;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.wiiudev.gecko.pointer.SingleMemoryDumpPointersFinder.findPotentialPointerLists;
import static com.wiiudev.gecko.pointer.SingleMemoryDumpPointersFinder.toOutputString;
import static com.wiiudev.gecko.pointer.preprocessed_search.MemoryPointerSearcher.MINIMUM_POINTER_SEARCH_DEPTH;
import static com.wiiudev.gecko.pointer.preprocessed_search.MemoryPointerSearcher.getSGenitive;
import static com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryPointer.parseMemoryPointer;
import static com.wiiudev.gecko.pointer.preprocessed_search.data_structures.OffsetPrintingSetting.SIGNED;
import static com.wiiudev.gecko.pointer.swing.PersistentSetting.*;
import static com.wiiudev.gecko.pointer.swing.utilities.DefaultContextMenu.addDefaultContextMenu;
import static com.wiiudev.gecko.pointer.swing.utilities.FileSizePrinting.readableFileSize;
import static com.wiiudev.gecko.pointer.swing.utilities.FrameUtilities.getSelectedItem;
import static com.wiiudev.gecko.pointer.swing.utilities.FrameUtilities.setWindowIconImage;
import static com.wiiudev.gecko.pointer.swing.utilities.HTMLDialogUtilities.addHyperLinkListener;
import static com.wiiudev.gecko.pointer.swing.utilities.ResourceUtilities.resourceToString;
import static com.wiiudev.gecko.pointer.utilities.DataConversions.parseInt;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static java.awt.event.ItemEvent.SELECTED;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseUnsignedInt;
import static java.lang.Long.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.write;
import static java.util.Collections.singletonList;
import static javax.swing.JOptionPane.*;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.text.DefaultCaret.NEVER_UPDATE;
import static org.apache.commons.io.FilenameUtils.separatorsToSystem;

public class UniversalPointerSearcherGUI extends JFrame
{
	public static final String APPLICATION_NAME = "Universal Pointer Searcher";
	private static final String APPLICATION_VERSION = "v3.02";
	private static final String STORED_POINTERS_FILE_NAME = "Pointers.txt";

	// Invalid JOptionPane option as default for recognition
	private static final int SINGLE_MEMORY_DUMP_METHOD_DEFAULT_SELECTED_ANSWER = -2;
	private static final int DEFAULT_POINTER_RESULTS_PAGE_SIZE = 10_000;
	private static final int DEFAULT_RESULTS_PAGE_SIZE = 1;

	private JPanel rootPanel;

	private JCheckBox allowNegativeOffsetsCheckBox;
	private JTextField maximumPointerOffsetField;
	private JTextArea foundPointersOutputArea;
	private JButton searchPointersButton;
	private JButton addMemoryDumpButton;
	private JButton resetMemoryDumpsButton;
	private JLabel pointerSearchStatisticsLabel;
	private JFormattedTextField pointerValueAlignmentField;
	private JFormattedTextField maximumMemoryChunkSizeField;
	private JFormattedTextField pointerSearchDepthField;
	private JComboBox<OffsetPrintingSetting> offsetPrintingSettingSelection;
	private JTable addedMemoryDumpsTable;
	private JButton cancelSearchButton;
	private JTextPane aboutTextPane;
	private JProgressBar pointerSearchProgressBar;
	private JTable ignoredMemoryRangesTable;
	private JButton editMemoryDumpButton;
	private JFormattedTextField startingBaseAddressField;
	private JFormattedTextField endBaseAddressField;
	private JCheckBox baseOffsetRangeSelection;
	private JLabel ignoredMemoryRangesLabel;
	private JScrollPane ignoredMemoryRangesTableScrollPane;
	private JComboBox<MemoryPointerSorting> sortingSelection;
	private JButton tutorialButton;
	private JTabbedPane tabbedPane;
	private JPanel aboutTab;
	private JCheckBox singleMemoryDumpMethodCheckBox;
	private JButton removeMemoryDumpButton;
	private JButton singleMemoryDumpMethodInformationButton;
	private JCheckBox generatePointerMapsCheckBox;
	private JProgressBar innerPointerSearchProgressBar;
	private JCheckBox writePointersToFileSystemCheckBox;
	private JCheckBox excludeCyclesCheckBox;
	private JTextField minimumPointerAddressField;
	private JComboBox<Integer> addressSizeSelection;
	private JSpinner resultsPageSpinner;
	private JTextField pointerResultsPageSizeField;
	private JLabel pageResultsLabel;
	private JCheckBox useNativePointerSearcherCheckBox;
	private JButton nativePointerSearcherOutputButton;
	private JFormattedTextField maximumPointersCountField;
	private JCheckBox readPointerMapsCheckBox;
	private JFormattedTextField pointerAddressAlignmentField;
	private JLabel readableMaximumMemoryChunkSizeLabel;
	private JTextField lastPointerOffsetsField;
	private PersistentSettingsManager persistentSettingsManager;
	private MemoryDumpTableManager memoryDumpTableManager;
	private Path lastAddedFilePath;
	private Long lastAddedStartingAddress;
	private boolean parseEntireFolder;

	private MemoryPointerSearcher memoryPointerSearcher;
	private boolean isSearching;

	private static String searchButtonText = "Search";
	private WindowsTaskBarProgress windowsTaskBarProgress;
	private List<List<MemoryPointer>> singleMemoryDumpPointers;

	private static UniversalPointerSearcherGUI universalPointerSearcherGUI;
	private int pagesCount;
	private NativePointerSearcherOutput nativePointerSearcherOutput;
	private NativePointerSearcherManager nativePointerSearcher;

	public static UniversalPointerSearcherGUI getInstance()
	{
		if (universalPointerSearcherGUI == null)
		{
			universalPointerSearcherGUI = new UniversalPointerSearcherGUI();
		}

		return universalPointerSearcherGUI;
	}

	private UniversalPointerSearcherGUI()
	{
		searchPointersButton.setText(searchButtonText);
		add(rootPanel);
		setFrameProperties();

		addTextAreaLimits();
		addPointerResultsPageSizeEditedListener();
		configureMaximumPointerOffsetField();
		populateOffsetPrintingSettings();
		setGraphicalInterfaceDefaultValues();

		configureAboutTab();
		addLastPointerOffsetFieldDocumentListener();
		addMaximumMemoryChunkSizeModifiedListener();
		addSignedModificationListener();
		addPointerSearchButtonActionListener();
		addNativePointerSearcherOutputButtonListener();
		addPointerSearchCancelButtonListener();
		addAddMemoryDumpButtonActionListener();
		addEditMemoryDumpButtonListener();
		addRemoveMemoryDumpButtonListener();
		addResetMemoryDumpsButtonListener();
		addSingleMemoryDumpMethodButtonListener();
		addNativePointerSearcherEngineListener();
		configureSortingSelection();
		singleMemoryDumpMethodCheckBox.setVisible(false);
		singleMemoryDumpMethodInformationButton.setVisible(false);
		innerPointerSearchProgressBar.setVisible(false);
		addedMemoryDumpsTable.getSelectionModel()
				.addListSelectionListener(listSelectionEvent -> setButtonAvailability());
		baseOffsetRangeSelection.addItemListener(itemEvent -> setButtonAvailability());
		startingBaseAddressField.setDocument(new JTextAreaLimit(8));
		endBaseAddressField.setDocument(new JTextAreaLimit(8));
		initializeIgnoredMemoryRangesTableManager();
		addDefaultContextMenu(foundPointersOutputArea);
		setButtonAvailability();
		handlePersistentSettings();
		configurePointerResultsPage();
	}

	private void addLastPointerOffsetFieldDocumentListener()
	{
		val document = lastPointerOffsetsField.getDocument();
		document.addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent documentEvent)
			{
				validateParsingLastOffset();
			}

			@Override
			public void removeUpdate(DocumentEvent documentEvent)
			{
				validateParsingLastOffset();
			}

			@Override
			public void changedUpdate(DocumentEvent documentEvent)
			{
				validateParsingLastOffset();
			}
		});

		validateParsingLastOffset();
	}

	private void validateParsingLastOffset()
	{
		try
		{
			parseLastOffsets();
			lastPointerOffsetsField.setBackground(GREEN);
		} catch (Exception exception)
		{
			lastPointerOffsetsField.setBackground(RED);
		}

		setButtonAvailability();
	}

	private void addMaximumMemoryChunkSizeModifiedListener()
	{
		val document = maximumMemoryChunkSizeField.getDocument();
		document.addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent documentEvent)
			{
				updateMaximumMemoryChunkSizeLabel();
			}

			@Override
			public void removeUpdate(DocumentEvent documentEvent)
			{
				updateMaximumMemoryChunkSizeLabel();
			}

			@Override
			public void changedUpdate(DocumentEvent documentEvent)
			{
				updateMaximumMemoryChunkSizeLabel();
			}
		});

		updateMaximumMemoryChunkSizeLabel();
	}

	private void updateMaximumMemoryChunkSizeLabel()
	{
		val memoryChunkSizeFieldText = maximumMemoryChunkSizeField.getText();

		val maximumMemoryChunkSize = memoryChunkSizeFieldText.isEmpty()
				? 0 : parseLong(memoryChunkSizeFieldText, 10);
		String readableFileSize;

		try
		{
			readableFileSize = "= " + readableFileSize(maximumMemoryChunkSize);
		} catch (IllegalArgumentException exception)
		{
			readableFileSize = exception.getMessage();
		}

		readableMaximumMemoryChunkSizeLabel.setText(readableFileSize);
	}

	private void addNativePointerSearcherOutputButtonListener()
	{
		nativePointerSearcherOutputButton.addActionListener(actionEvent ->
		{
			if (nativePointerSearcherOutput == null)
			{
				showMessageDialog(this,
						"No native pointer search has been performed yet.",
						"Warning", WARNING_MESSAGE);
			} else
			{
				val nativePointerSearcherOutputDialog = new NativePointerSearcherOutputDialog();
				val dialogTitle = nativePointerSearcherOutputButton.getText();
				nativePointerSearcherOutputDialog.setTitle(dialogTitle);
				val processOutput = nativePointerSearcherOutput.getProcessOutput();
				nativePointerSearcherOutputDialog.setText(processOutput);
				nativePointerSearcherOutputDialog.setLocationRelativeTo(this);
				nativePointerSearcherOutputDialog.setVisible(true);
			}
		});
	}

	private void addNativePointerSearcherEngineListener()
	{
		useNativePointerSearcherCheckBox.addItemListener(itemEvent -> setButtonAvailability());
	}

	private void addPointerResultsPageSizeEditedListener()
	{
		val document = pointerResultsPageSizeField.getDocument();
		document.addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent documentEvent)
			{
				onPointerResultsPageSizeChanged();
			}

			@Override
			public void removeUpdate(DocumentEvent documentEvent)
			{
				onPointerResultsPageSizeChanged();
			}

			@Override
			public void changedUpdate(DocumentEvent documentEvent)
			{
				onPointerResultsPageSizeChanged();
			}
		});
	}

	private void onPointerResultsPageSizeChanged()
	{
		computeMaximumPagesCount();
		updatePointerResultsPage();
	}

	private void computeMaximumPagesCount()
	{
		if (memoryPointerSearcher != null)
		{
			val memoryPointers = memoryPointerSearcher.getMemoryPointerList();
			if (memoryPointers != null)
			{
				val currentMemoryPointers = memoryPointers.getMemoryPointers();
				setMemoryPointersPagesCount(currentMemoryPointers);
			}
		}
	}

	private void configurePointerResultsPage()
	{
		disableInvalidPointerResultsSpinnerInput();
		resultsPageSpinner.setValue(DEFAULT_RESULTS_PAGE_SIZE);
		resultsPageSpinner.addChangeListener(changeEvent -> updatePointerResultsPage());
	}

	private void disableInvalidPointerResultsSpinnerInput()
	{
		val pageSpinnerEditor = (JSpinner.NumberEditor) resultsPageSpinner.getEditor();
		val spinnerTextField = pageSpinnerEditor.getTextField();
		val numberFormatter = (NumberFormatter) spinnerTextField.getFormatter();
		numberFormatter.setAllowsInvalid(false);
	}

	private void updatePointerResultsPage()
	{
		val currentPageString = resultsPageSpinner.getValue().toString();
		var currentPageNumber = Integer.parseInt(currentPageString);
		currentPageNumber = min(currentPageNumber, pagesCount);
		val memoryPointerList = memoryPointerSearcher.getMemoryPointerList();

		if (memoryPointerList != null)
		{
			val memoryPointers = memoryPointerList.getMemoryPointers();

			val memoryPointersPageSize = getPointerResultsPageSize();
			var fromIndex = currentPageNumber * memoryPointersPageSize - memoryPointersPageSize;
			fromIndex = max(0, fromIndex);
			var toIndex = currentPageNumber * memoryPointersPageSize - 1;
			toIndex = min(toIndex, memoryPointers.size());
			val actualToIndex = toIndex == memoryPointers.size() ? toIndex : toIndex + 1;
			val memoryPointersPage = memoryPointers.subList(fromIndex, actualToIndex);
			val offsetPrintingSetting = getSelectedItem(offsetPrintingSettingSelection);
			val addressSize = getSelectedItem(addressSizeSelection);
			val pointersText = memoryPointersToString(offsetPrintingSetting, addressSize, memoryPointersPage);

			val finalFromIndex = fromIndex;
			val finalCurrentPageNumber = currentPageNumber;
			invokeLater(() ->
			{
				setFoundPointersText(pointersText);
				pageResultsLabel.setText("You are currently viewing pointer results " + (finalFromIndex + 1) +
						" to " + actualToIndex + " from page "
						+ finalCurrentPageNumber + " out of " + pagesCount + " total pages");
			});
		}
	}

	private int getPointerResultsPageSize()
	{
		val resultsPageSizeFieldText = pointerResultsPageSizeField.getText();

		if (resultsPageSizeFieldText.isEmpty())
		{
			return 0;
		}

		return Integer.parseInt(resultsPageSizeFieldText);
	}

	private void addSingleMemoryDumpMethodButtonListener()
	{
		singleMemoryDumpMethodInformationButton.addActionListener(actionEvent ->
		{
			val message = "Explanation by skoolzout1:\n\n" +
					"If you decide you want to attempt searching beyond a depth of 2, the way I do it is to start with 2 unique memory dumps.\n" +
					"Then, run a pointer scan using each of the dumps alone, in order to generate 2 huge lists of pointers.\n" +
					"After that I effectively look through each list of pointers to try and find offset patterns that remained the same between the two dumps.\n" +
					"\n" +
					"Example: \n" +
					"[[0x12345678] + 0x458] + 0xABC\n" +
					"compared to: \n" +
					"[[0x11111111] + 0x458] + 0xABC\n" +
					"\n" +
					"So I look for pointers that have the same offset pattern, but different base addresses between the dumps.\n" +
					"\n" +
					"So after that, I would try my luck again running a pointer scan using both of the dumps,\n" +
					"but searching for pointers based on 0x12345678 for the first dump and 0x11111111 for the second one.\n" +
					"And see if I have any results after that.\n" +
					"\n" +
					"If you get results you can paste one of resulting pointers into your framework of [[--------] + 0x458] + 0xABC and then test it out.\n\n" +
					"Additional notes:\n" +
					"* When a line contains more than 2 entries, it means that the leftmost one matches multiple pointers on the rightmost memory dump(s).\n" +
					"* The " + singleMemoryDumpMethodCheckBox.getText().toLowerCase() + " is only useful for making deeper pointer searches since it doesn't deliver finalized pointers (only intermediate results).\n" +
					"You therefore need to perform a regular pointer search at the end to finish the \"chain\". This way, you can go as deep as you want.";

			showMessageDialog(UniversalPointerSearcherGUI.this,
					message,
					singleMemoryDumpMethodCheckBox.getText(),
					INFORMATION_MESSAGE);
		});
	}

	private void addRemoveMemoryDumpButtonListener()
	{
		removeMemoryDumpButton.addActionListener(actionEvent ->
		{
			val buttons = new String[]{"Yes", "No"};
			val selectedAnswer = showOptionDialog(rootPane,
					"Would you really like to delete the selected memory dump?",
					removeMemoryDumpButton.getText(),
					YES_NO_CANCEL_OPTION,
					QUESTION_MESSAGE,
					null,
					buttons,
					null);

			if (selectedAnswer == YES_OPTION)
			{
				memoryDumpTableManager.removeSelectedMemoryDumps();
				setMemoryPointerSearcherMemoryDumps();
			}
		});
	}

	private void setMemoryPointerSearcherMemoryDumps()
	{
		val memoryDumps = memoryDumpTableManager.getMemoryDumps();
		memoryPointerSearcher.setMemoryDumps(memoryDumps);

		val pointerMaps = memoryDumpTableManager.getPointerMaps();
		memoryPointerSearcher.setImportedPointerMaps(pointerMaps);
	}

	private void handlePersistentSettings()
	{
		persistentSettingsManager = new PersistentSettingsManager();
		restorePersistentSettings();
		addPersistentSettingsBackupWindowClosingListener();
	}

	private void restorePersistentSettings()
	{
		var lastAddedFilePath = persistentSettingsManager.get(LAST_ADDED_FILE_PATH.toString());
		if (lastAddedFilePath != null)
		{
			try
			{
				lastAddedFilePath = separatorsToSystem(lastAddedFilePath);
				this.lastAddedFilePath = Paths.get(lastAddedFilePath);
			} catch (InvalidPathException exception)
			{
				handleException(exception);
			}
		}

		val lastAddedStartingAddress = persistentSettingsManager.get(LAST_ADDED_STARTING_ADDRESS.toString());
		if (lastAddedStartingAddress != null)
		{
			try
			{
				this.lastAddedStartingAddress = parseUnsignedLong(lastAddedStartingAddress, 16);
			} catch (NumberFormatException exception)
			{
				handleException(exception);
			}
		}

		val parseEntireFolderValue = persistentSettingsManager.get(PARSE_ENTIRE_FOLDER.toString());
		if (parseEntireFolderValue != null)
		{
			this.parseEntireFolder = parseBoolean(parseEntireFolderValue);
		}

		restoreString(POINTER_SEARCH_DEPTH, pointerSearchDepthField);
		restoreString(POINTER_VALUE_ALIGNMENT, pointerValueAlignmentField);
		restoreString(POINTER_ADDRESS_ALIGNMENT, pointerAddressAlignmentField);
		restoreString(MAXIMUM_MEMORY_CHUNK_SIZE, maximumMemoryChunkSizeField);
		restoreString(MAXIMUM_POINTERS_COUNT, maximumPointersCountField);
		restoreString(MAXIMUM_OFFSET, maximumPointerOffsetField);
		restoreString(MINIMUM_POINTER_ADDRESS, minimumPointerAddressField);
		restoreString(POINTER_RESULTS_PAGE_SIZE, pointerResultsPageSizeField);
		restoreString(LAST_POINTER_OFFSETS, lastPointerOffsetsField);
		restoreBoolean(ALLOW_NEGATIVE_OFFSETS, allowNegativeOffsetsCheckBox);
		restoreBoolean(SINGLE_MEMORY_DUMP_METHOD, singleMemoryDumpMethodCheckBox);
		restoreBoolean(GENERATE_POINTER_MAPS, generatePointerMapsCheckBox);
		restoreBoolean(WRITE_POINTERS_TO_FILE_SYSTEM, writePointersToFileSystemCheckBox);
		restoreBoolean(BASE_OFFSET_RANGE, baseOffsetRangeSelection);
		restoreBoolean(EXCLUDE_POINTER_CYCLES, excludeCyclesCheckBox);
		restoreBoolean(USE_NATIVE_POINTER_ENGINE, useNativePointerSearcherCheckBox);
		restoreString(STARTING_BASE_ADDRESS, startingBaseAddressField);
		restoreString(END_BASE_ADDRESS, endBaseAddressField);

		val addressSizeString = persistentSettingsManager.get(ADDRESS_SIZE.toString());
		if (addressSizeString != null)
		{
			try
			{
				val addressSize = Integer.parseInt(addressSizeString);
				addressSizeSelection.setSelectedItem(addressSize);
			} catch (Exception exception)
			{
				exception.printStackTrace();
			}
		}
	}

	private void restoreString(PersistentSetting persistentSetting, JTextField textField)
	{
		val startingBaseAddress = persistentSettingsManager.get(persistentSetting.toString());
		if (startingBaseAddress != null)
		{
			textField.setText(startingBaseAddress);
		}
	}

	private void restoreBoolean(PersistentSetting persistentSetting, JCheckBox checkBox)
	{
		val restoredValue = persistentSettingsManager.get(persistentSetting.toString());
		if (restoredValue != null)
		{
			val selected = parseBoolean(restoredValue);
			checkBox.setSelected(selected);
		}
	}

	private void addPersistentSettingsBackupWindowClosingListener()
	{
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent windowEvent)
			{
				super.windowClosing(windowEvent);

				val thread = new Thread(() ->
				{
					if (lastAddedFilePath != null)
					{
						persistentSettingsManager.put(LAST_ADDED_FILE_PATH.toString(), lastAddedFilePath.toString());
					}
					persistentSettingsManager.put(PARSE_ENTIRE_FOLDER.toString(), parseEntireFolder + "");

					if (lastAddedStartingAddress != null)
					{
						persistentSettingsManager.put(LAST_ADDED_STARTING_ADDRESS.toString(), toHexString(lastAddedStartingAddress).toUpperCase());
					}

					persistentSettingsManager.put(POINTER_SEARCH_DEPTH.toString(), pointerSearchDepthField.getText());
					persistentSettingsManager.put(POINTER_VALUE_ALIGNMENT.toString(), pointerValueAlignmentField.getText());
					persistentSettingsManager.put(POINTER_ADDRESS_ALIGNMENT.toString(), pointerAddressAlignmentField.getText());
					persistentSettingsManager.put(MAXIMUM_MEMORY_CHUNK_SIZE.toString(), maximumMemoryChunkSizeField.getText());
					persistentSettingsManager.put(MAXIMUM_POINTERS_COUNT.toString(), maximumPointersCountField.getText());
					persistentSettingsManager.put(MAXIMUM_OFFSET.toString(), maximumPointerOffsetField.getText());
					persistentSettingsManager.put(ADDRESS_SIZE.toString(), getSelectedItem(addressSizeSelection) + "");
					persistentSettingsManager.put(MINIMUM_POINTER_ADDRESS.toString(), minimumPointerAddressField.getText());
					persistentSettingsManager.put(POINTER_RESULTS_PAGE_SIZE.toString(), pointerResultsPageSizeField.getText());
					persistentSettingsManager.put(LAST_POINTER_OFFSETS.toString(), lastPointerOffsetsField.getText());
					persistentSettingsManager.put(ALLOW_NEGATIVE_OFFSETS.toString(), allowNegativeOffsetsCheckBox.isSelected() + "");
					persistentSettingsManager.put(SINGLE_MEMORY_DUMP_METHOD.toString(), singleMemoryDumpMethodCheckBox.isSelected() + "");
					persistentSettingsManager.put(GENERATE_POINTER_MAPS.toString(), generatePointerMapsCheckBox.isSelected() + "");
					persistentSettingsManager.put(WRITE_POINTERS_TO_FILE_SYSTEM.toString(), writePointersToFileSystemCheckBox.isSelected() + "");
					persistentSettingsManager.put(BASE_OFFSET_RANGE.toString(), baseOffsetRangeSelection.isSelected() + "");
					persistentSettingsManager.put(EXCLUDE_POINTER_CYCLES.toString(), excludeCyclesCheckBox.isSelected() + "");
					persistentSettingsManager.put(USE_NATIVE_POINTER_ENGINE.toString(), useNativePointerSearcherCheckBox.isSelected() + "");
					persistentSettingsManager.put(STARTING_BASE_ADDRESS.toString(), startingBaseAddressField.getText());
					persistentSettingsManager.put(END_BASE_ADDRESS.toString(), endBaseAddressField.getText());
					persistentSettingsManager.writeToFile();
				});

				thread.setName("Persistent Settings Writer");
				thread.start();
			}
		});
	}

	private void configureSortingSelection()
	{
		sortingSelection.setModel(new DefaultComboBoxModel<>(MemoryPointerSorting.values()));
		sortingSelection.addItemListener(itemEvent ->
		{
			val state = itemEvent.getStateChange();
			if (state == SELECTED)
			{
				considerSettingFoundPointersTextArea(false, null);
			}
		});
	}

	private void initializeIgnoredMemoryRangesTableManager()
	{
		val ignoredMemoryRangesTableManager = new IgnoredMemoryRangesTableManager(ignoredMemoryRangesTable);
		ignoredMemoryRangesTableManager.configure();

		// TODO Not implemented yet
		ignoredMemoryRangesTableScrollPane.setVisible(false);
		ignoredMemoryRangesLabel.setVisible(false);
		ignoredMemoryRangesTable.setVisible(false);
	}

	private void addEditMemoryDumpButtonListener()
	{
		editMemoryDumpButton.addActionListener(actionEvent ->
		{
			val memoryDump = memoryDumpTableManager.getSelectedMemoryDump();
			val memoryDumpDialog = showMemoryDumpDialog(memoryDump, editMemoryDumpButton,
					null, null, false, false);

			if (memoryDumpDialog.isMemoryDumpAdded())
			{
				val updatedMemoryDump = memoryDumpDialog.getMemoryDump();
				memoryDumpTableManager.replaceSelectedMemoryDumpWith(updatedMemoryDump);
			}
		});
	}

	private void addPointerSearchCancelButtonListener()
	{
		cancelSearchButton.addActionListener(actionEvent ->
		{
			val buttons = new String[]{"Yes", "No"};
			val selectedAnswer = showOptionDialog(UniversalPointerSearcherGUI.this,
					"Would you really like to cancel the current search?",
					cancelSearchButton.getText(),
					YES_NO_CANCEL_OPTION,
					QUESTION_MESSAGE,
					null,
					buttons,
					null);

			if (selectedAnswer == YES_OPTION)
			{
				if (useNativePointerSearcherCheckBox.isSelected())
				{
					nativePointerSearcher.cancel();
				} else
				{
					memoryPointerSearcher.setSearchCanceled(true);
				}
			}
		});

		tutorialButton.addActionListener(actionEvent -> tabbedPane.setSelectedComponent(aboutTab));
	}

	private void addTextAreaLimits()
	{
		configurePointerSearchDepthField();
		val addressSize = Integer.BYTES * 2;
		pointerValueAlignmentField.setDocument(new JTextAreaLimit(addressSize));
		pointerAddressAlignmentField.setDocument(new JTextAreaLimit(addressSize));
		maximumPointersCountField.setDocument(new JTextAreaLimit(addressSize));
		maximumMemoryChunkSizeField.setDocument(new JTextAreaLimit((Long.MAX_VALUE + "").length()));
		maximumPointerOffsetField.setDocument(new JTextAreaLimit(6));
		minimumPointerAddressField.setDocument(new JTextAreaLimit(addressSize));
		pointerResultsPageSizeField.setDocument(new JTextAreaLimit(addressSize));
	}

	private void configurePointerSearchDepthField()
	{
		pointerSearchDepthField.setDocument(new JTextAreaLimit(2));
		addButtonAvailabilityDocumentListener(pointerSearchDepthField);
	}

	private void addButtonAvailabilityDocumentListener(JTextField pointerSearchDepthField)
	{
		val document = pointerSearchDepthField.getDocument();
		document.addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent documentEvent)
			{
				setButtonAvailability();
			}

			@Override
			public void removeUpdate(DocumentEvent documentEvent)
			{
				setButtonAvailability();
			}

			@Override
			public void changedUpdate(DocumentEvent documentEvent)
			{
				setButtonAvailability();
			}
		});
	}

	private void configureAboutTab()
	{
		try
		{
			addHyperLinkListener(aboutTextPane);
			val aboutText = resourceToString("About.html");
			aboutTextPane.setText(aboutText);
		} catch (Exception exception)
		{
			handleException(exception);
		}
	}

	private void removeMemoryDumps()
	{
		memoryDumpTableManager.removeMemoryDumps();
		memoryPointerSearcher.removeMemoryDumps();
	}

	private void addSignedModificationListener()
	{
		allowNegativeOffsetsCheckBox.addItemListener(itemEvent ->
		{
			val state = itemEvent.getStateChange();
			if (state == SELECTED)
			{
				setOffsetPrintingSetting();
			}

			validateParsingLastOffset();
		});
	}

	private void setOffsetPrintingSetting()
	{
		val selected = allowNegativeOffsetsCheckBox.isSelected();

		if (!selected)
		{
			offsetPrintingSettingSelection.setSelectedItem(SIGNED);
		}

		offsetPrintingSettingSelection.setEnabled(selected);
	}

	private void populateOffsetPrintingSettings()
	{
		offsetPrintingSettingSelection.setModel(new DefaultComboBoxModel<>(OffsetPrintingSetting.values()));
		offsetPrintingSettingSelection.setSelectedItem(SIGNED);
		offsetPrintingSettingSelection.addItemListener(itemEvent ->
		{
			val state = itemEvent.getStateChange();
			if (state == SELECTED)
			{
				considerSettingFoundPointersTextArea(false, null);
			}
		});
	}

	private void addResetMemoryDumpsButtonListener()
	{
		resetMemoryDumpsButton.addActionListener(actionEvent ->
		{
			val selectedAnswer = showConfirmDialog(
					rootPane,
					"Would you really like to remove all added entries?",
					"Reset?",
					YES_NO_OPTION);

			if (selectedAnswer == YES_OPTION)
			{
				pointerSearchStatisticsLabel.setText("");
				foundPointersOutputArea.setText("");
				removeMemoryDumps();
				setButtonAvailability();
			}
		});
	}

	private void configureMaximumPointerOffsetField()
	{
		addButtonAvailabilityDocumentListener(maximumPointerOffsetField);
	}

	private void setButtonAvailability()
	{
		val usingNativePointerSearcher = useNativePointerSearcherCheckBox.isSelected();
		val memoryDumps = memoryPointerSearcher.getMemoryDumps();
		val memoryDumpsAdded = memoryDumps.size() > 0;

		val maximumPointerOffset = parseInt(maximumPointerOffsetField.getText());
		// TODO Wrong check?
		val maximumPointerOffsetValid = maximumPointerOffset % 4 == 0;

		val pointerSearchDepth = getPointerSearchDepth();
		val lastPointerOffsetBackgroundColor = lastPointerOffsetsField.getBackground();
		val isSearchButtonAvailable = pointerSearchDepth >= MINIMUM_POINTER_SEARCH_DEPTH
				&& memoryDumpsAdded && maximumPointerOffsetValid
				&& lastPointerOffsetBackgroundColor.equals(GREEN);
		searchPointersButton.setEnabled(isSearchButtonAvailable && !isSearching);
		cancelSearchButton.setVisible(isSearching);
		nativePointerSearcherOutputButton.setVisible(nativePointerSearcherOutput != null);
		excludeCyclesCheckBox.setEnabled(!isSearching);
		useNativePointerSearcherCheckBox.setEnabled(!isSearching);

		pointerSearchDepthField.setEnabled(!isSearching);
		pointerValueAlignmentField.setEnabled(!isSearching);
		pointerAddressAlignmentField.setEnabled(!isSearching && usingNativePointerSearcher);
		maximumMemoryChunkSizeField.setEnabled(!isSearching);
		maximumPointersCountField.setEnabled(!isSearching && usingNativePointerSearcher);
		maximumPointerOffsetField.setEnabled(!isSearching);
		addressSizeSelection.setEnabled(!isSearching);
		minimumPointerAddressField.setEnabled(!isSearching);
		allowNegativeOffsetsCheckBox.setEnabled(!isSearching);
		singleMemoryDumpMethodCheckBox.setEnabled(!isSearching);
		generatePointerMapsCheckBox.setEnabled(!isSearching);
		readPointerMapsCheckBox.setEnabled(!isSearching && usingNativePointerSearcher);
		writePointersToFileSystemCheckBox.setEnabled(!isSearching);
		lastPointerOffsetsField.setEnabled(!isSearching && usingNativePointerSearcher);
		pointerResultsPageSizeField.setEnabled(!isSearching);
		sortingSelection.setEnabled(!isSearching &&
				(singleMemoryDumpPointers == null || singleMemoryDumpPointers.isEmpty()));
		offsetPrintingSettingSelection.setEnabled(!isSearching);
		addMemoryDumpButton.setEnabled(!isSearching);
		resetMemoryDumpsButton.setEnabled(memoryDumpsAdded && !isSearching);
		editMemoryDumpButton.setEnabled(memoryDumpTableManager.isMemoryDumpSelected() && !isSearching);
		removeMemoryDumpButton.setEnabled(memoryDumpTableManager.isMemoryDumpSelected() && !isSearching);
		baseOffsetRangeSelection.setEnabled(!isSearching && !usingNativePointerSearcher);
		val isBaseOffsetRangeEnabled = baseOffsetRangeSelection.isSelected()
				&& baseOffsetRangeSelection.isEnabled();
		startingBaseAddressField.setEnabled(isBaseOffsetRangeEnabled);
		endBaseAddressField.setEnabled(isBaseOffsetRangeEnabled);
		val foundPointersOutputAreaText = foundPointersOutputArea.getText();
		resultsPageSpinner.setEnabled(!foundPointersOutputAreaText.isEmpty());

		setOffsetPrintingSetting();
	}

	private int getPointerSearchDepth()
	{
		val pointerSearchDepthFieldText = pointerSearchDepthField.getText();
		var pointerSearchDepth = 0;
		try
		{
			pointerSearchDepth = (int) parseInt(pointerSearchDepthFieldText);
		} catch (NumberFormatException ignored)
		{

		}
		return pointerSearchDepth;
	}

	private void setGraphicalInterfaceDefaultValues()
	{
		memoryPointerSearcher = new MemoryPointerSearcher();
		memoryDumpTableManager = new MemoryDumpTableManager(addedMemoryDumpsTable);

		val pointerSearchDepth = memoryPointerSearcher.getPointerSearchDepth();
		pointerSearchDepthField.setText(pointerSearchDepth + "");

		val maximumMemoryChunkSize = memoryPointerSearcher.getMaximumMemoryChunkSize();
		maximumMemoryChunkSizeField.setText(maximumMemoryChunkSize + "");

		val cppPointerSearcherManager = new NativePointerSearcherManager();
		val maximumPointersCount = cppPointerSearcherManager.getMaximumPointersCount();
		maximumPointersCountField.setText(maximumPointersCount + "");

		val maximumPointerOffset = memoryPointerSearcher.getMaximumPointerOffset();
		maximumPointerOffsetField.setText(toHexString(maximumPointerOffset).toUpperCase() + "");

		val minimumPointerAddress = memoryPointerSearcher.getMinimumPointerAddress();
		minimumPointerAddressField.setText(toHexString(minimumPointerAddress).toUpperCase() + "");

		pointerResultsPageSizeField.setText(DEFAULT_POINTER_RESULTS_PAGE_SIZE + "");

		initializeAddressSizeSelection();

		val addressSize = memoryPointerSearcher.getAddressSize();
		addressSizeSelection.setSelectedItem(addressSize);

		val pointerValueAlignment = memoryPointerSearcher.getPointerValueAlignment();
		pointerValueAlignmentField.setText(pointerValueAlignment + "");

		val pointerAddressAlignment = memoryPointerSearcher.getPointerAddressAlignment();
		pointerAddressAlignmentField.setText(pointerAddressAlignment + "");

		val allowNegativeOffsets = memoryPointerSearcher.isAllowNegativeOffsets();
		allowNegativeOffsetsCheckBox.setSelected(allowNegativeOffsets);

		val generatePointerMaps = memoryPointerSearcher.isGeneratePointerMaps();
		generatePointerMapsCheckBox.setSelected(generatePointerMaps);

		memoryPointerSearcher.setSearchPointersButton(searchPointersButton);
	}

	private void initializeAddressSizeSelection()
	{
		addressSizeSelection.addItem(Byte.BYTES);
		addressSizeSelection.addItem(Short.BYTES);
		addressSizeSelection.addItem(Integer.BYTES);
		addressSizeSelection.addItem(Long.BYTES);
	}

	private void addAddMemoryDumpButtonActionListener()
	{
		addMemoryDumpButton.addActionListener(actionEvent ->
		{
			try
			{
				val memoryDumpDialog = showMemoryDumpDialog(null,
						addMemoryDumpButton, lastAddedFilePath, lastAddedStartingAddress,
						parseEntireFolder, true);
				if (memoryDumpDialog.isMemoryDumpAdded())
				{
					val addedMemoryDump = memoryDumpDialog.getMemoryDump();
					lastAddedFilePath = addedMemoryDump.getFilePath();
					lastAddedStartingAddress = addedMemoryDump.getStartingAddress();
					parseEntireFolder = memoryDumpDialog.isParseEntireFolderSelected();

					if (memoryDumpDialog.shouldParseEntireFolder())
					{
						val memoryDumps = memoryDumpDialog.getMemoryDumps();
						if (memoryDumps != null)
						{
							for (val memoryDump : memoryDumps)
							{
								addMemoryDump(memoryDump);
							}
						}

						val pointerMaps = memoryDumpDialog.getPointerMaps();
						if (pointerMaps != null)
						{
							for (val pointerMap : pointerMaps)
							{
								addMemoryDump(pointerMap);
							}
						}
					} else
					{
						val successfullyAdded = addMemoryDump(addedMemoryDump);

						if (!successfullyAdded)
						{
							showMessageDialog(rootPane,
									"The entry has already been added.",
									"Already added",
									WARNING_MESSAGE);
						}
					}

					setButtonAvailability();
				}
			} catch (Exception exception)
			{
				handleException(exception);
			}
		});
	}

	private MemoryDumpDialog showMemoryDumpDialog(MemoryDump memoryDump,
	                                              JButton button, Path filePath,
	                                              Long lastAddedStartingAddress,
	                                              boolean parseEntireFolder,
	                                              boolean mayParseFolder)
	{
		val memoryDumpDialog = new MemoryDumpDialog(memoryDump, mayParseFolder);
		memoryDumpDialog.setFilePath(filePath);
		memoryDumpDialog.setLastAddedStartingAddress(lastAddedStartingAddress);
		memoryDumpDialog.setParseEntireFolder(parseEntireFolder);
		memoryDumpDialog.setLocationRelativeTo(this);
		val title = button.getText();
		memoryDumpDialog.setTitle(title);
		memoryDumpDialog.setVisible(true);

		return memoryDumpDialog;
	}

	private void handleException(Exception exception)
	{
		StackTraceUtilities.handleException(rootPane, exception);
	}

	private boolean addMemoryDump(MemoryDump memoryDump)
	{
		val memoryDumps = memoryPointerSearcher.getMemoryDumps();

		if (memoryDumps.contains(memoryDump))
		{
			return false;
		}

		memoryPointerSearcher.addMemoryDump(memoryDump);
		memoryDumpTableManager.addMemoryDump(memoryDump);

		return true;
	}

	private void addPointerSearchButtonActionListener()
	{
		searchPointersButton.addActionListener(actionEvent ->
		{
			try
			{
				nativePointerSearcherOutput = null;
				setPointerSearchOptions();

				val minimumPointerAddress = parseUnsignedLong(minimumPointerAddressField.getText(), 16);

				if (minimumPointerAddress == 0)
				{
					val confirmed = showConfirmDialog(rootPane,
							"You configured the minimum pointer address to be " + minimumPointerAddress + ".\n" +
									"This makes no sense since " + minimumPointerAddress + " is usually defined as the invalid pointer.\n" +
									"Furthermore, this pointer search may take far longer than usual.\n" +
									"Do you really want to continue?",
							"Continue?", YES_NO_OPTION, WARNING_MESSAGE);

					if (confirmed != YES_OPTION)
					{
						return;
					}
				}

				val memoryDumps = memoryDumpTableManager.getMemoryDumps();
				memoryPointerSearcher.setMemoryDumps(memoryDumps);

				val pointerMaps = memoryDumpTableManager.getPointerMaps();
				memoryPointerSearcher.setImportedPointerMaps(pointerMaps);

				val addedGenitive = getSGenitive(memoryDumps, pointerMaps);
				var singleMemoryDumpMethodSelectedAnswer = SINGLE_MEMORY_DUMP_METHOD_DEFAULT_SELECTED_ANSWER;

				if (memoryDumps.size() == 1 && singleMemoryDumpMethodCheckBox.isSelected())
				{
					singleMemoryDumpMethodSelectedAnswer = showConfirmDialog(
							rootPane,
							"You have chosen to use the \""
									+ singleMemoryDumpMethodCheckBox.getText()
									+ "\" but you only added 1 memory dump.\n" +
									"You have to add more than 1 memory dump to make sense out of this.\n" +
									"Do you want to perform a regular pointer search?",
							"Regular Pointer Search?",
							YES_NO_OPTION);

					if (singleMemoryDumpMethodSelectedAnswer != YES_OPTION)
					{
						return;
					}

					singleMemoryDumpMethodCheckBox.setSelected(false);
				}

				if (singleMemoryDumpMethodSelectedAnswer != SINGLE_MEMORY_DUMP_METHOD_DEFAULT_SELECTED_ANSWER)
				{
					searchPointers(false, null);
				} else
				{
					val selectedAnswer = singleMemoryDumpMethodCheckBox.isSelected()
							? showConfirmDialog(rootPane,
							"Do you want to perform a "
									+ singleMemoryDumpMethodCheckBox.getText().toLowerCase()
									+ " pointer search using the added memory dump"
									+ addedGenitive + "?",
							singleMemoryDumpMethodCheckBox.getText() + " Pointer Search?",
							YES_NO_OPTION)
							: showConfirmDialog(rootPane, "Do you want to perform a " +
							(useNativePointerSearcherCheckBox.isSelected() ? "native " : "")
							+ "pointer search using the added memory dump"
							+ addedGenitive + "?", "Pointer Search?", YES_NO_OPTION);

					if (selectedAnswer == YES_OPTION)
					{
						singleMemoryDumpPointers = new ArrayList<>();

						if (singleMemoryDumpMethodCheckBox.isSelected())
						{
							val thread = new Thread(() ->
							{
								val singleMemoryDumpBenchmark = new Benchmark();
								singleMemoryDumpBenchmark.start();

								for (val memoryDump : memoryDumps)
								{
									val singletonMemoryDump = singletonList(memoryDump);
									memoryPointerSearcher.setMemoryDumps(singletonMemoryDump);
									searchPointers(true, singleMemoryDumpBenchmark);
								}
							});

							thread.start();
						} else
						{
							searchPointers(false, null);
						}
					}
				}
			} catch (Exception exception)
			{
				handleException(exception);
			}
		});
	}

	private void setPointerSearchOptions()
	{
		val pointerSearchDepth = parseUnsignedInt(pointerSearchDepthField.getText(), 10);
		memoryPointerSearcher.setPointerSearchDepth(pointerSearchDepth);

		val pointerValueAlignment = parseUnsignedInt(pointerValueAlignmentField.getText(), 16);
		memoryPointerSearcher.setPointerValueAlignment(pointerValueAlignment);

		val maximumMemoryChunkSize = parseLong(maximumMemoryChunkSizeField.getText(), 10);
		memoryPointerSearcher.setMaximumMemoryChunkSize(maximumMemoryChunkSize);

		val maximumPointerOffset = parseUnsignedLong(maximumPointerOffsetField.getText(), 16);
		memoryPointerSearcher.setMaximumPointerOffset(maximumPointerOffset);

		val minimumPointerAddress = parseUnsignedLong(minimumPointerAddressField.getText(), 16);
		memoryPointerSearcher.setMinimumPointerAddress(minimumPointerAddress);

		val addressSize = getSelectedItem(addressSizeSelection);
		memoryPointerSearcher.setAddressSize(addressSize.byteValue());

		val allowNegativeOffsets = allowNegativeOffsetsCheckBox.isSelected();
		memoryPointerSearcher.setAllowNegativeOffsets(allowNegativeOffsets);

		val usePointerMaps = generatePointerMapsCheckBox.isSelected();
		memoryPointerSearcher.setGeneratePointerMaps(usePointerMaps);

		val excludeCycles = excludeCyclesCheckBox.isSelected();
		memoryPointerSearcher.setExcludeCycles(excludeCycles);

		memoryPointerSearcher.setGeneralProgressBar(pointerSearchProgressBar);
		memoryPointerSearcher.setPointerDepthProgressBar(innerPointerSearchProgressBar);

		if (baseOffsetRangeSelection.isSelected())
		{
			val startingAddress = parseUnsignedLong(startingBaseAddressField.getText(), 16);
			val endAddress = parseUnsignedLong(endBaseAddressField.getText(), 16);
			val memoryRange = new MemoryRange(startingAddress, endAddress);
			val memoryRanges = new ArrayList<MemoryRange>();
			memoryRanges.add(memoryRange);
			memoryPointerSearcher.setBaseAddressRanges(memoryRanges);
		}
	}

	private void searchPointers(boolean executeSynchronously, Benchmark singleMemoryDumpBenchmark)
	{
		val benchmark = new Benchmark();
		benchmark.start();

		isSearching = true;

		val useNativePointerSearcher = useNativePointerSearcherCheckBox.isSelected();

		invokeLater(() ->
		{
			pointerSearchStatisticsLabel.setText("");
			foundPointersOutputArea.setText("");
			pageResultsLabel.setText("");
			resultsPageSpinner.setValue(DEFAULT_RESULTS_PAGE_SIZE);
			if (!useNativePointerSearcher)
			{
				searchPointersButton.setText("Reading first memory dump...");
			}
			setButtonAvailability();
			pointerSearchProgressBar.setValue(0);
			pointerSearchProgressBar.setVisible(!useNativePointerSearcher);
		});

		val pointerSearcherThread = new SwingWorker[]{null};
		val backgroundException = new Exception[1];

		val memoryDumpsParserThread = new SwingWorker<String, String>()
		{
			@Override
			protected String doInBackground()
			{
				try
				{
					if (!useNativePointerSearcher)
					{
						memoryPointerSearcher.parseMemoryDumps();
					}
				} catch (Exception exception)
				{
					backgroundException[0] = exception;
					handleException(exception);
				}

				return null;
			}

			@Override
			protected void done()
			{
				if (backgroundException[0] != null)
				{
					disableSearching();
					return;
				}

				invokeLater(() ->
				{
					val labelText = useNativePointerSearcher ? "Running native pointer searcher..." : "Searching pointers...";
					searchPointersButton.setText(labelText);
				});

				pointerSearcherThread[0] = new SwingWorker<String, String>()
				{
					@Override
					protected String doInBackground()
					{
						try
						{
							if (useNativePointerSearcher)
							{
								performNativePointerSearch();
							} else
							{
								memoryPointerSearcher.searchPointers();
							}
						} catch (Exception exception)
						{
							handleException(exception);
						} finally
						{
							considerSettingFoundPointersTextArea(true, singleMemoryDumpBenchmark);
						}

						return null;
					}

					@Override
					protected void done()
					{
						try
						{
							invokeLater(() ->
							{
								val elapsedTime = benchmark.getElapsedTime();
								setPointerSearchStatisticsLabel(elapsedTime);
							});
						} catch (Exception exception)
						{
							handleException(exception);
						} finally
						{
							if (!singleMemoryDumpMethodCheckBox.isSelected()
									|| singleMemoryDumpPointers.size()
									== memoryDumpTableManager.getMemoryDumps().size() - 1)
							{
								disableSearching();
							}
						}
					}
				};

				if (!executeSynchronously)
				{
					pointerSearcherThread[0].execute();
				}
			}
		};

		memoryDumpsParserThread.execute();

		if (executeSynchronously)
		{
			waitForSwingWorkerToComplete(memoryDumpsParserThread);

			while (pointerSearcherThread[0] == null)
			{
				try
				{
					Thread.sleep(10);
				} catch (InterruptedException exception)
				{
					exception.printStackTrace();
				}
			}

			pointerSearcherThread[0].execute();
			waitForSwingWorkerToComplete(pointerSearcherThread[0]);
		}
	}

	private void performNativePointerSearch() throws Exception
	{
		nativePointerSearcher = new NativePointerSearcherManager();
		val memoryDumps = memoryPointerSearcher.getMemoryDumps();
		for (val memoryDump : memoryDumps)
		{
			val startingAddress = memoryDump.getStartingAddress();
			memoryDump.setMinimumPointerAddress(startingAddress);
			val addressSize = getSelectedItem(addressSizeSelection);
			memoryDump.setAddressSize(addressSize);
			val maximumPointerAddress = startingAddress + memoryDump.getSize() - addressSize;
			memoryDump.setMaximumPointerAddress(maximumPointerAddress);
			val pointerAddressAlignment = parseUnsignedInt(pointerAddressAlignmentField.getText(), 16);
			memoryDump.setAddressAlignment(pointerAddressAlignment);
			val pointerValueAlignment = parseUnsignedInt(pointerValueAlignmentField.getText(), 16);
			memoryDump.setValueAlignment(pointerValueAlignment);
			val generatePointerMap = generatePointerMapsCheckBox.isSelected();
			memoryDump.setGeneratePointerMap(generatePointerMap);
			val readPointerMaps = readPointerMapsCheckBox.isSelected();
			memoryDump.setReadPointerMap(readPointerMaps);
			nativePointerSearcher.addMemoryDump(memoryDump);
		}

		val allowNegativeOffsets = allowNegativeOffsetsCheckBox.isSelected();
		nativePointerSearcher.setAllowNegativeOffsets(allowNegativeOffsets);

		val excludeCycles = excludeCyclesCheckBox.isSelected();
		nativePointerSearcher.setExcludeCycles(excludeCycles);

		val maximumMemoryChunkSize = parseLong(maximumMemoryChunkSizeField.getText(), 10);
		nativePointerSearcher.setMaximumMemoryDumpChunkSize(maximumMemoryChunkSize);

		val maximumPointerDepth = parseLong(pointerSearchDepthField.getText());
		nativePointerSearcher.setMaximumPointerDepth(maximumPointerDepth);

		val maximumPointersCount = parseLong(maximumPointersCountField.getText(), 10);
		nativePointerSearcher.setMaximumPointersCount(maximumPointersCount);

		nativePointerSearcher.setPotentialPointerOffsetsCountPerAddressPrediction(40);

		nativePointerSearcher.setSaveAdditionalMemoryDumpRAM(false);

		val maximumPointerOffset = parseLong(maximumPointerOffsetField.getText(), 16);
		nativePointerSearcher.setMaximumPointerOffset(maximumPointerOffset);

		val lastOffsets = parseLastOffsets();
		nativePointerSearcher.setLastPointerOffsets(lastOffsets);

		try
		{
			nativePointerSearcherOutput = nativePointerSearcher.call();
			val exceptionMessage = nativePointerSearcherOutput.getExceptionMessage();
			val isCanceled = nativePointerSearcher.isCanceled();
			if (!isCanceled && exceptionMessage != null)
			{
				throw new IllegalStateException(exceptionMessage);
			}
		} finally
		{
			val lineSeparator = lineSeparator();
			val processOutput = nativePointerSearcherOutput.getProcessOutput();
			val lines = processOutput.split(lineSeparator);
			val memoryPointers = new ArrayList<MemoryPointer>();
			for (val line : lines)
			{
				if (line.startsWith("[")) // TODO Maybe proper parsing?
				{
					val memoryPointer = parseMemoryPointer(line);
					memoryPointers.add(memoryPointer);
				}
			}

			val memoryPointerList = new MemoryPointerList();
			memoryPointerList.setMemoryPointers(memoryPointers);
			memoryPointerSearcher.setMemoryPointerList(memoryPointerList);
		}
	}

	private List<Long> parseLastOffsets()
	{
		val lastOffsetsText = lastPointerOffsetsField.getText();
		val splitOffsets = lastOffsetsText.isEmpty()
				? new String[0] : lastOffsetsText.split(",");
		val lastOffsets = new ArrayList<Long>();
		for (var splitOffset : splitOffsets)
		{
			splitOffset = splitOffset.trim();
			var isNegative = false;
			val negativeSign = "-";
			if (splitOffset.startsWith(negativeSign))
			{
				splitOffset = splitOffset.substring(negativeSign.length());
				isNegative = true;
			}
			var lastOffset = parseUnsignedLong(splitOffset, 16);
			if (isNegative)
			{
				if (!allowNegativeOffsetsCheckBox.isSelected())
				{
					throw new IllegalStateException("Negative offsets not allowed");
				}

				lastOffset = -lastOffset;
			}
			lastOffsets.add(lastOffset);
		}
		return lastOffsets;
	}

	private void disableSearching()
	{
		invokeLater(() ->
		{
			isSearching = false;
			searchPointersButton.setText(searchButtonText);
			setButtonAvailability();
		});
	}

	private void waitForSwingWorkerToComplete(SwingWorker swingWorker)
	{
		while (!swingWorker.isDone())
		{
			try
			{
				Thread.sleep(10);
			} catch (InterruptedException exception)
			{
				exception.printStackTrace();
			}
		}
	}

	private void setPointerSearchStatisticsLabel(double elapsedTime)
	{
		val formattedElapsedTime = formatElapsedTime(elapsedTime);
		val memoryPointers = memoryPointerSearcher.getMemoryPointerList();
		val memoryPointersCount = memoryPointers == null ? 0 : memoryPointers.size();
		val pointerSearchStatisticsText = memoryPointersCount
				+ " pointer(s) found in " + formattedElapsedTime + " second(s).";
		pointerSearchStatisticsLabel.setText(pointerSearchStatisticsText);
	}

	private String formatElapsedTime(double elapsedTime)
	{
		val decimalFormat = new DecimalFormat("#0.00");
		return decimalFormat.format(elapsedTime);
	}

	private void considerSettingFoundPointersTextArea(boolean pointerSearchFinished, Benchmark singleMemoryDumpBenchmark)
	{
		val memoryPointers = memoryPointerSearcher.getMemoryPointerList();

		if (memoryPointers != null)
		{
			if (pointerSearchFinished)
			{
				invokeLater(() -> searchPointersButton.setText("Building results..."));
			}

			val currentMemoryPointers = memoryPointers.getMemoryPointers();

			val offsetPrintingSetting = getSelectedItem(offsetPrintingSettingSelection);

			if (singleMemoryDumpMethodCheckBox.isSelected() && pointerSearchFinished)
			{
				if (singleMemoryDumpPointers == null)
				{
					singleMemoryDumpPointers = new ArrayList<>();
				}

				singleMemoryDumpPointers.add(currentMemoryPointers);

				val memoryDumps = memoryDumpTableManager.getMemoryDumps();
				if (singleMemoryDumpPointers.size() == memoryDumps.size())
				{
					val potentialPointerPairs = findPotentialPointerLists(singleMemoryDumpPointers);
					val foundPointersText = toOutputString(potentialPointerPairs, memoryDumps,
							offsetPrintingSetting.equals(SIGNED));
					writePointersAsynchronously(foundPointersText);
					invokeLater(() -> setFoundPointersText(foundPointersText));
					val elapsedTime = singleMemoryDumpBenchmark.getElapsedTime();
					val formattedElapsedTime = formatElapsedTime(elapsedTime);
					invokeLater(() -> pointerSearchStatisticsLabel.setText(potentialPointerPairs.size()
							+ " pointer list(s) in " + formattedElapsedTime + " second(s) found."));
				}
			} else
			{
				setMemoryPointersPagesCount(currentMemoryPointers);

				val addressSize = getSelectedItem(addressSizeSelection);
				val foundPointersText = memoryPointersToString(offsetPrintingSetting,
						addressSize, currentMemoryPointers);
				writePointersAsynchronously(foundPointersText);

				updatePointerResultsPage();
			}
		}
	}

	private void updateResultsCountSpinnerModel()
	{
		val spinnerNumberModel = new SpinnerNumberModel(1, 1, pagesCount, 1);
		resultsPageSpinner.setModel(spinnerNumberModel);
		resultsPageSpinner.setValue(DEFAULT_RESULTS_PAGE_SIZE);
	}

	private String memoryPointersToString(OffsetPrintingSetting offsetPrintingSetting,
	                                      int addressSize, List<MemoryPointer> memoryPointers)
	{
		applySorting(memoryPointers);
		return MemoryPointer.toString(memoryPointers, addressSize, offsetPrintingSetting);
	}

	private void applySorting(List<MemoryPointer> memoryPointers)
	{
		val memoryPointerSorting = getSelectedItem(sortingSelection);
		val comparator = memoryPointerSorting.getComparator();
		memoryPointers.sort(comparator);
	}

	private void setMemoryPointersPagesCount(List<MemoryPointer> memoryPointers)
	{
		val totalMemoryPointersCount = memoryPointers.size();
		val memoryPointersPageSize = getPointerResultsPageSize();

		if (memoryPointersPageSize != 0)
		{
			pagesCount = totalMemoryPointersCount / memoryPointersPageSize;
			if (totalMemoryPointersCount % memoryPointersPageSize != 0)
			{
				pagesCount++;
			}
		}

		if (pagesCount == 0)
		{
			pagesCount = DEFAULT_RESULTS_PAGE_SIZE;
		}

		invokeLater(this::updateResultsCountSpinnerModel);
	}

	private void writePointersAsynchronously(String pointers)
	{
		val thread = new Thread(() -> considerWritingPointers(pointers));
		thread.setName("Pointers Writer");
		thread.start();
	}

	private synchronized void considerWritingPointers(String pointers)
	{
		val shouldWritePointers = writePointersToFileSystemCheckBox.isSelected();
		if (shouldWritePointers && !pointers.isEmpty())
		{
			val memoryDumps = memoryDumpTableManager.getMemoryDumps();
			val memoryDump = memoryDumps.get(0);
			val memoryDumpFilePath = memoryDump.getFilePath();
			val parentDirectory = memoryDumpFilePath.getParent();
			val resolve = parentDirectory.resolve(STORED_POINTERS_FILE_NAME);
			val bytes = pointers.getBytes(UTF_8);

			try
			{
				write(resolve, bytes);
			} catch (IOException exception)
			{
				exception.printStackTrace();
			}
		}
	}

	private void setFoundPointersText(String foundPointersText)
	{
		// Disable the cursor position from changing when the text area is updated
		val caret = (DefaultCaret) foundPointersOutputArea.getCaret();
		caret.setUpdatePolicy(NEVER_UPDATE);

		foundPointersOutputArea.setText(foundPointersText);
	}

	private void setFrameProperties()
	{
		setTitle(APPLICATION_NAME + " " + APPLICATION_VERSION);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setWindowIconImage(this);
		pack();
		adjustFrameSize();
	}

	private void adjustFrameSize()
	{
		val size = getSize();
		val updatedSize = new Dimension((int) (size.getWidth() * 1.5),
				(int) (size.getHeight() * 1.4));
		val defaultToolkit = Toolkit.getDefaultToolkit();
		val screenSize = defaultToolkit.getScreenSize();
		val screenWidth = screenSize.getWidth();
		val screenHeight = screenSize.getHeight();
		val finalWidth = min(screenWidth, updatedSize.getWidth());
		val finalHeight = min(screenHeight, updatedSize.getHeight());
		updatedSize.setSize(finalWidth, finalHeight);
		setSize(updatedSize);
		setMinimumSize(updatedSize);
	}

	@Override
	public void setVisible(boolean visible)
	{
		val thread = new Thread(() ->
		{
			try
			{
				windowsTaskBarProgress = new WindowsTaskBarProgress(this);
			} catch (ClassNotFoundException exception)
			{
				handleException(exception);
			}
		});

		thread.setName("Windows Task Bar Initializer");
		thread.start();

		super.setVisible(visible);
	}

	public WindowsTaskBarProgress getWindowsTaskBarProgress()
	{
		return windowsTaskBarProgress;
	}
}
