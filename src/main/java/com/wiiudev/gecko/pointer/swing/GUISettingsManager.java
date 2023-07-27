package com.wiiudev.gecko.pointer.swing;

import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryDump;
import lombok.val;
import lombok.var;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static com.cedarsoftware.util.io.JsonWriter.formatJson;
import static com.wiiudev.gecko.pointer.preprocessed_search.utilities.DataConversions.parseNumeric;
import static com.wiiudev.gecko.pointer.preprocessed_search.utilities.DataConversions.toHexadecimal;
import static com.wiiudev.gecko.pointer.swing.preprocessed_search.InputType.parseInputType;
import static com.wiiudev.gecko.pointer.swing.utilities.FrameUtilities.getSelectedItem;
import static com.wiiudev.gecko.pointer.swing.utilities.MemoryDumpsByteOrder.parseMemoryDumpsByteOrder;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.readAllBytes;
import static javax.swing.JOptionPane.showMessageDialog;

public class GUISettingsManager
{
	private static final String INPUT_FILES_JSON_KEY = "input-files";
	private static final String INPUT_FILE_JSON_KEY = "input-file";
	private static final String STARTING_ADDRESS_JSON_KEY = "starting-address";
	private static final String INPUT_TYPE_JSON_KEY = "input-type";
	private static final String FILE_PATH_JSON_KEY = "file-path";
	private static final String TARGET_ADDRESS_JSON_KEY = "target-address";
	private static final String POINTER_DEPTH_RANGE_JSON_KEY = "pointer-depth-range";
	private static final String POINTER_OFFSET_RANGE_JSON_KEY = "pointer-offset-range";
	private static final String MINIMUM_POINTER_ADDRESS_JSON_KEY = "minimum-pointer-address";
	private static final String LAST_POINTER_OFFSETS_JSON_KEY = "last-pointer-offsets";
	private static final String MAXIMUM_RESULT_COUNT_JSON_KEY = "maximum-result-count";
	private static final String MAXIMUM_MEMORY_UTILIZATION_PERCENTAGE_JSON_KEY = "maximum-memory-utilization-percentage";
	private static final String FILE_EXTENSIONS_JSON_KEY = "file-extensions";
	private static final String USING_TARGET_SYSTEM_JSON_KEY = "using-target-system";
	private static final String TARGET_SYSTEM_JSON_KEY = "target-system";
	private static final String ADDRESS_SIZE_JSON_KEY = "address-size";
	private static final String BYTE_ORDER_JSON_KEY = "byte-order";
	private static final String STORE_MEMORY_POINTER_RESULTS_JSON_KEY = "store-memory-pointer-results";
	private static final String STORE_MEMORY_POINTER_RESULTS_JSON_KEY_FILE_PATH = "store-memory-pointer-results-file-path";
	private static final String LOAD_MEMORY_POINTER_RESULTS_JSON_KEY = "load-memory-pointer-results";
	private static final String LOAD_MEMORY_POINTER_RESULTS_JSON_KEY_FILE_PATH = "load-memory-pointer-results-file-path";
	private static final String PRINT_MODULE_FILE_NAMES_JSON_KEY = "print-module-file-names";
	private static final String VERBOSE_LOGGING_JSON_KEY = "verbose-logging";
	private static final String EXCLUDE_CYCLES_JSON_KEY = "exclude-cycles";
	private static final String READ_POINTER_MAPS_JSON_KEY = "read-pointer-maps";
	private static final String GENERATE_POINTER_MAPS_JSON_KEY = "generate-pointer-maps";
	private static final String TRUNCATE_MEMORY_POINTERS_DEBUGGING_OUTPUT_JSON_KEY = "truncate-memory-pointers-debugging-output";
	private static final String PRINT_VISITED_ADDRESSES_JSON_KEY = "print-visited-addresses";
	private static final String COMPARISON_GROUP_NUMBER_JSON_KEY = "comparison-group-number";

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
	public void loadSettings(final UniversalPointerSearcherGUI pointerSearcherGUI) throws IOException
	{
		val filePath = showFileChooser(pointerSearcherGUI.getRootPane(), FileChooserOpenDialogType.OPEN_DIALOG);
		if (filePath != null)
		{
			val fileContents = new String(readAllBytes(filePath), StandardCharsets.UTF_8);

			val jsonObject = new JSONObject(fileContents);
			val targetAddress = parseNumeric(jsonObject.getString(TARGET_ADDRESS_JSON_KEY));
			val memoryDumpsByteOrder = parseMemoryDumpsByteOrder(jsonObject.getString(BYTE_ORDER_JSON_KEY));
			if (memoryDumpsByteOrder == null)
			{
				throw new IOException("Byte order not recognized");
			}

			if (jsonObject.has(INPUT_FILES_JSON_KEY))
			{
				val memoryDumps = new ArrayList<MemoryDump>();
				val inputFilesJSONArray = jsonObject.getJSONArray(INPUT_FILES_JSON_KEY);
				for (var inputFileIndex = 0; inputFileIndex < inputFilesJSONArray.length(); inputFileIndex++)
				{
					val inputFileJSONObject = inputFilesJSONArray.getJSONObject(inputFileIndex);
					val inputFileFilePath = inputFileJSONObject.getString(FILE_PATH_JSON_KEY);
					val startingAddress = parseNumeric(inputFileJSONObject.getString(STARTING_ADDRESS_JSON_KEY));
					val memoryDump = new MemoryDump(inputFileFilePath, startingAddress, targetAddress, memoryDumpsByteOrder.getByteOrder());
					val inputType = inputFileJSONObject.getString(INPUT_TYPE_JSON_KEY);
					memoryDump.setInputType(parseInputType(inputType));
					memoryDump.setComparisonGroupNumber(inputFileJSONObject.getInt(COMPARISON_GROUP_NUMBER_JSON_KEY));
					memoryDumps.add(memoryDump);
				}

				val memoryDumpTableManager = pointerSearcherGUI.getMemoryDumpTableManager();
				memoryDumpTableManager.removeMemoryDumps();

				for (val memoryDump : memoryDumps)
				{
					memoryDumpTableManager.addMemoryDump(memoryDump);
				}
			}

			val addressSize = jsonObject.getInt(ADDRESS_SIZE_JSON_KEY);
			pointerSearcherGUI.getAddressSizeSelection().setSelectedItem(addressSize);

			val byteOrder = parseMemoryDumpsByteOrder(jsonObject.getString(BYTE_ORDER_JSON_KEY));
			pointerSearcherGUI.getByteOrderSelection().setSelectedItem(byteOrder);

			val usingTargetSystem = jsonObject.getBoolean(USING_TARGET_SYSTEM_JSON_KEY);
			pointerSearcherGUI.getTargetSystemCheckbox().setSelected(usingTargetSystem);
			val targetSystem = jsonObject.getString(TARGET_SYSTEM_JSON_KEY);
			pointerSearcherGUI.getTargetSystemSelection().setSelectedItem(TargetSystem.parseTargetSystem(targetSystem));

			showMessageDialog(pointerSearcherGUI.getRootPane(),
					"The configuration has been restored successfully.",
					"Successfully restored",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	public void saveSettings(final UniversalPointerSearcherGUI pointerSearcherGUI) throws IOException
	{
		val filePath = showFileChooser(pointerSearcherGUI, FileChooserOpenDialogType.SAVE_DIALOG);
		if (filePath != null)
		{
			val formattedJSON = buildPointerSearcherProfileJSON(pointerSearcherGUI);
			Files.write(filePath, formattedJSON.getBytes(StandardCharsets.UTF_8));
			showMessageDialog(pointerSearcherGUI,
					"The configuration has been saved successfully.",
					"Successfully saved",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private static String buildPointerSearcherProfileJSON(final UniversalPointerSearcherGUI pointerSearcherGUI)
	{
		val rootJSONObject = new JSONObject();
		val memoryDumpTableManager = pointerSearcherGUI.getMemoryDumpTableManager();
		val memoryDumps = memoryDumpTableManager.getMemoryDumps();
		val inputFilesJSONObject = new JSONArray();
		for (val memoryDump : memoryDumps)
		{
			val inputFileJSONObject = new JSONObject();
			inputFileJSONObject.put(FILE_PATH_JSON_KEY, memoryDump.getFilePath().toString());
			inputFileJSONObject.put(INPUT_TYPE_JSON_KEY, memoryDump.getInputType().toString());
			inputFileJSONObject.put(STARTING_ADDRESS_JSON_KEY, toHexadecimal(memoryDump.getStartingAddress()));
			inputFileJSONObject.put(COMPARISON_GROUP_NUMBER_JSON_KEY, memoryDump.getComparisonGroupNumber());

			inputFilesJSONObject.put(inputFileJSONObject);
		}
		rootJSONObject.put(INPUT_FILES_JSON_KEY, inputFilesJSONObject);

		if (memoryDumps.size() != 0)
		{
			val firstMemoryDump = memoryDumps.get(0);
			rootJSONObject.put(TARGET_ADDRESS_JSON_KEY, toHexadecimal(firstMemoryDump.getTargetAddress()));
		}

		val pointerDepthRangeJSONObject = buildRangeJSON(pointerSearcherGUI.getMinimumPointerSearchDepthField(),
				pointerSearcherGUI.getMaximumPointerSearchDepthField(), false);
		rootJSONObject.put(POINTER_DEPTH_RANGE_JSON_KEY, pointerDepthRangeJSONObject);
		rootJSONObject.put(MAXIMUM_MEMORY_UTILIZATION_PERCENTAGE_JSON_KEY, Double.parseDouble(pointerSearcherGUI.getMaximumMemoryUtilizationPercentageField().getText()));
		rootJSONObject.put(MAXIMUM_RESULT_COUNT_JSON_KEY, Long.parseLong(pointerSearcherGUI.getMaximumPointersCountField().getText()));
		val pointerOffsetRangeJSONObject = buildRangeJSON(pointerSearcherGUI.getMinimumPointerOffsetField(),
				pointerSearcherGUI.getMaximumPointerOffsetField(), true);
		rootJSONObject.put(POINTER_OFFSET_RANGE_JSON_KEY, pointerOffsetRangeJSONObject);
		rootJSONObject.put(MINIMUM_POINTER_ADDRESS_JSON_KEY, pointerSearcherGUI.getMinimumPointerAddressField().getText());
		rootJSONObject.put(LAST_POINTER_OFFSETS_JSON_KEY, pointerSearcherGUI.getLastPointerOffsetsField().getText());
		val fileExtensionsJSON = new JSONArray();
		val fileExtensions = pointerSearcherGUI.parseFileExtensions();
		for (val fileExtension : fileExtensions)
		{
			fileExtensionsJSON.put(fileExtension);
		}
		rootJSONObject.put(FILE_EXTENSIONS_JSON_KEY, fileExtensionsJSON);
		rootJSONObject.put(USING_TARGET_SYSTEM_JSON_KEY, pointerSearcherGUI.getTargetSystemCheckbox().isSelected());
		rootJSONObject.put(TARGET_SYSTEM_JSON_KEY, getSelectedItem(pointerSearcherGUI.getTargetSystemSelection()).toString());
		rootJSONObject.put(ADDRESS_SIZE_JSON_KEY, getSelectedItem(pointerSearcherGUI.getAddressSizeSelection()));
		rootJSONObject.put(BYTE_ORDER_JSON_KEY, getSelectedItem(pointerSearcherGUI.getByteOrderSelection()).toString());
		rootJSONObject.put(STORE_MEMORY_POINTER_RESULTS_JSON_KEY, pointerSearcherGUI.getStoreMemoryPointerResultsCheckBox().isSelected());
		rootJSONObject.put(STORE_MEMORY_POINTER_RESULTS_JSON_KEY_FILE_PATH, pointerSearcherGUI.getStoreMemoryPointersFilePathField().getText());
		rootJSONObject.put(LOAD_MEMORY_POINTER_RESULTS_JSON_KEY, pointerSearcherGUI.getLoadMemoryPointerResultsCheckBox().isSelected());
		rootJSONObject.put(LOAD_MEMORY_POINTER_RESULTS_JSON_KEY_FILE_PATH, pointerSearcherGUI.getLoadMemoryPointersFilePathField().getText());
		rootJSONObject.put(PRINT_MODULE_FILE_NAMES_JSON_KEY, pointerSearcherGUI.getPrintModuleFileNamesCheckBox().isSelected());
		rootJSONObject.put(VERBOSE_LOGGING_JSON_KEY, pointerSearcherGUI.getVerboseLoggingCheckBox().isSelected());
		rootJSONObject.put(EXCLUDE_CYCLES_JSON_KEY, pointerSearcherGUI.getExcludeCyclesCheckBox().isSelected());
		rootJSONObject.put(PRINT_VISITED_ADDRESSES_JSON_KEY, pointerSearcherGUI.getPrintVisitedAddressesCheckBox().isSelected());
		rootJSONObject.put(READ_POINTER_MAPS_JSON_KEY, pointerSearcherGUI.getReadPointerMapsCheckBox().isSelected());
		rootJSONObject.put(GENERATE_POINTER_MAPS_JSON_KEY, pointerSearcherGUI.getGeneratePointerMapsCheckBox().isSelected());
		rootJSONObject.put(TRUNCATE_MEMORY_POINTERS_DEBUGGING_OUTPUT_JSON_KEY, pointerSearcherGUI.getTruncateMemoryPointersDebuggingOutputCheckBox().isSelected());

		return formatJson(rootJSONObject.toString());
	}

	private static JSONObject buildRangeJSON(final JTextField minimumPointerSearchDepthField,
	                                         final JTextField maximumPointerSearchDepthField,
	                                         final boolean isHexadecimal)
	{
		val pointerDepthRangeJSONObject = new JSONObject();
		pointerDepthRangeJSONObject.put("from", isHexadecimal ? minimumPointerSearchDepthField.getText() : Long.parseLong(minimumPointerSearchDepthField.getText()));
		pointerDepthRangeJSONObject.put("to", isHexadecimal ? maximumPointerSearchDepthField.getText() : Long.parseLong(maximumPointerSearchDepthField.getText()));
		return pointerDepthRangeJSONObject;
	}
}
