package com.wiiudev.gecko.pointer.swing;

import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryDump;
import com.wiiudev.gecko.pointer.swing.preprocessed_search.FileTypeImport;
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
	private static final String STARTING_ADDRESS_JSON_KEY = "starting-address";
	private static final String INPUT_TYPE_JSON_KEY = "input-type";
	private static final String FILE_TYPE_JSON_KEY = "file-type";
	private static final String FILE_PATH_JSON_KEY = "file-path";
	private static final String TARGET_ADDRESS_JSON_KEY = "target-address";
	private static final String POINTER_DEPTH_RANGE_JSON_KEY = "pointer-depth-range";
	private static final String POINTER_OFFSET_RANGE_JSON_KEY = "pointer-offset-range";
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
	private static final String SCAN_DEEPER_BY_CHECKBOX_JSON_KEY = "scan-deeper-by-checkbox";
	private static final String SCAN_DEEPER_BY_SPINNER_JSON_KEY = "scan-deeper-by-spinner";
	private static final String PRINT_MODULE_FILE_NAMES_JSON_KEY = "print-module-file-names";
	private static final String VERBOSE_LOGGING_JSON_KEY = "verbose-logging";
	private static final String EXCLUDE_CYCLES_JSON_KEY = "exclude-cycles";
	private static final String READ_POINTER_MAPS_JSON_KEY = "read-pointer-maps";
	private static final String GENERATE_POINTER_MAPS_JSON_KEY = "generate-pointer-maps";
	private static final String TARGET_POINTER_MAPS_JSON_KEY = "target-pointer-maps";
	private static final String TRUNCATE_MEMORY_POINTERS_DEBUGGING_OUTPUT_JSON_KEY = "truncate-memory-pointers-debugging-output";
	private static final String PRINT_VISITED_ADDRESSES_JSON_KEY = "print-visited-addresses";
	private static final String COMPARISON_GROUP_NUMBER_JSON_KEY = "comparison-group-number";
	public static final String POINTER_DEPTH_RANGE_FROM_JSON_KEY = "from";
	public static final String POINTER_DEPTH_RANGE_TO_JSON_KEY = "to";

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

	public void loadSettings(final UniversalPointerSearcherGUI pointerSearcherGUI) throws IOException
	{
		val filePath = showFileChooser(pointerSearcherGUI.getRootPane(), FileChooserOpenDialogType.OPEN_DIALOG);
		if (filePath != null)
		{
			val fileContents = new String(readAllBytes(filePath), StandardCharsets.UTF_8);

			val jsonObject = new JSONObject(fileContents);
			val targetAddress = jsonObject.has(TARGET_ADDRESS_JSON_KEY) ? parseNumeric(jsonObject.getString(TARGET_ADDRESS_JSON_KEY)) : 0;
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
					val startingAddress = inputFileJSONObject.has(STARTING_ADDRESS_JSON_KEY)
							? parseNumeric(inputFileJSONObject.getString(STARTING_ADDRESS_JSON_KEY)) : null;
					val memoryDump = new MemoryDump(inputFileFilePath, startingAddress, targetAddress, memoryDumpsByteOrder.getByteOrder());
					val inputType = inputFileJSONObject.getString(INPUT_TYPE_JSON_KEY);
					memoryDump.setInputType(parseInputType(inputType));
					if (inputFileJSONObject.has(FILE_TYPE_JSON_KEY))
					{
						val fileType = inputFileJSONObject.getString(FILE_TYPE_JSON_KEY);
						val fileTypeImport = FileTypeImport.parseFileTypeImport(fileType);
						memoryDump.setFileType(fileTypeImport);
					}
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

			if (jsonObject.has(SCAN_DEEPER_BY_CHECKBOX_JSON_KEY))
			{
				val shouldScanDeeperBy = jsonObject.getBoolean(SCAN_DEEPER_BY_CHECKBOX_JSON_KEY);
				pointerSearcherGUI.getScanDeeperByCheckBox().setSelected(shouldScanDeeperBy);
				val scanDeeperByDepth = jsonObject.getInt(SCAN_DEEPER_BY_SPINNER_JSON_KEY);
				pointerSearcherGUI.getScanDeeperBySpinner().setValue(scanDeeperByDepth);
			}

			val pointerDepthRangeJSONObject = jsonObject.getJSONObject(POINTER_DEPTH_RANGE_JSON_KEY);
			val pointerDepthRangeFrom = pointerDepthRangeJSONObject.getInt(POINTER_DEPTH_RANGE_FROM_JSON_KEY);
			pointerSearcherGUI.getMinimumPointerSearchDepthField().setText(String.valueOf(pointerDepthRangeFrom));
			val pointerDepthRangeTo = pointerDepthRangeJSONObject.getInt(POINTER_DEPTH_RANGE_TO_JSON_KEY);
			pointerSearcherGUI.getMaximumPointerSearchDepthField().setText(String.valueOf(pointerDepthRangeTo));

			val maximumMemoryUtilizationPercentage = jsonObject.getInt(MAXIMUM_MEMORY_UTILIZATION_PERCENTAGE_JSON_KEY);
			pointerSearcherGUI.getMaximumMemoryUtilizationPercentageField().setText(String.valueOf(maximumMemoryUtilizationPercentage));

			val maximumResultCount = jsonObject.getLong(MAXIMUM_RESULT_COUNT_JSON_KEY);
			pointerSearcherGUI.getMaximumPointersCountField().setText(String.valueOf(maximumResultCount));

			val pointerOffsetRangeJSONArray = jsonObject.getJSONObject(POINTER_OFFSET_RANGE_JSON_KEY);
			val pointerOffsetRangeFrom = pointerOffsetRangeJSONArray.getString(POINTER_DEPTH_RANGE_FROM_JSON_KEY);
			pointerSearcherGUI.getMinimumPointerOffsetField().setText(pointerOffsetRangeFrom);
			val pointerOffsetRangeTo = pointerOffsetRangeJSONArray.getString(POINTER_DEPTH_RANGE_TO_JSON_KEY);
			pointerSearcherGUI.getMaximumPointerOffsetField().setText(pointerOffsetRangeTo);

			val lastPointerOffsetsJSONArray = jsonObject.getJSONArray(LAST_POINTER_OFFSETS_JSON_KEY);
			val lastPointerOffsetBuilder = new StringBuilder();
			var lastPointerOffsetsJSONArrayIndex = 0;
			for (val lastPointerOffset : lastPointerOffsetsJSONArray)
			{
				lastPointerOffsetBuilder.append(lastPointerOffset);

				if (lastPointerOffsetsJSONArrayIndex != lastPointerOffsetsJSONArray.toList().size() - 1)
				{
					lastPointerOffsetBuilder.append(",");
				}

				lastPointerOffsetsJSONArrayIndex++;
			}
			pointerSearcherGUI.getLastPointerOffsetsField().setText(lastPointerOffsetBuilder.toString());

			val fileExtensionsJSONArray = jsonObject.getJSONArray(FILE_EXTENSIONS_JSON_KEY);
			val fileExtensions = buildFileExtensionsString(fileExtensionsJSONArray);
			pointerSearcherGUI.getFileExtensionsField().setText(fileExtensions);

			val addressSize = jsonObject.getInt(ADDRESS_SIZE_JSON_KEY);
			pointerSearcherGUI.getAddressSizeSelection().setSelectedItem(addressSize);

			val byteOrder = parseMemoryDumpsByteOrder(jsonObject.getString(BYTE_ORDER_JSON_KEY));
			pointerSearcherGUI.getByteOrderSelection().setSelectedItem(byteOrder);

			val usingTargetSystem = jsonObject.getBoolean(USING_TARGET_SYSTEM_JSON_KEY);
			pointerSearcherGUI.getTargetSystemCheckbox().setSelected(usingTargetSystem);
			val targetSystem = jsonObject.getString(TARGET_SYSTEM_JSON_KEY);
			val parsedTargetSystem = TargetSystem.parseTargetSystem(targetSystem);
			if (parsedTargetSystem != null)
			{
				pointerSearcherGUI.getTargetSystemSelection().setSelectedItem(parsedTargetSystem);
			}

			val storeMemoryPointerResults = jsonObject.getBoolean(STORE_MEMORY_POINTER_RESULTS_JSON_KEY);
			pointerSearcherGUI.getStoreMemoryPointerResultsCheckBox().setSelected(storeMemoryPointerResults);
			val storeMemoryPointerResultsFilePath = jsonObject.getString(STORE_MEMORY_POINTER_RESULTS_JSON_KEY_FILE_PATH);
			pointerSearcherGUI.getStoreMemoryPointersFilePathField().setText(storeMemoryPointerResultsFilePath);

			val loadMemoryPointerResults = jsonObject.getBoolean(LOAD_MEMORY_POINTER_RESULTS_JSON_KEY);
			pointerSearcherGUI.getLoadMemoryPointerResultsCheckBox().setSelected(loadMemoryPointerResults);
			val loadMemoryPointerResultsFilePath = jsonObject.getString(LOAD_MEMORY_POINTER_RESULTS_JSON_KEY_FILE_PATH);
			pointerSearcherGUI.getLoadMemoryPointersFilePathField().setText(loadMemoryPointerResultsFilePath);

			val printModuleFileNames = jsonObject.getBoolean(PRINT_MODULE_FILE_NAMES_JSON_KEY);
			pointerSearcherGUI.getPrintModuleFileNamesCheckBox().setSelected(printModuleFileNames);

			val verboseLogging = jsonObject.getBoolean(VERBOSE_LOGGING_JSON_KEY);
			pointerSearcherGUI.getVerboseLoggingCheckBox().setSelected(verboseLogging);

			val excludeCycles = jsonObject.getBoolean(EXCLUDE_CYCLES_JSON_KEY);
			pointerSearcherGUI.getExcludeCyclesCheckBox().setSelected(excludeCycles);

			val printVisitedAddresses = jsonObject.getBoolean(PRINT_VISITED_ADDRESSES_JSON_KEY);
			pointerSearcherGUI.getPrintVisitedAddressesCheckBox().setSelected(printVisitedAddresses);

			val readPointerMaps = jsonObject.getBoolean(READ_POINTER_MAPS_JSON_KEY);
			pointerSearcherGUI.getReadPointerMapsCheckBox().setSelected(readPointerMaps);

			val generatePointerMaps = jsonObject.getBoolean(GENERATE_POINTER_MAPS_JSON_KEY);
			pointerSearcherGUI.getGeneratePointerMapsCheckBox().setSelected(generatePointerMaps);

			if (jsonObject.has(TARGET_POINTER_MAPS_JSON_KEY))
			{
				val targetPointerMaps = jsonObject.getString(TARGET_POINTER_MAPS_JSON_KEY);
				pointerSearcherGUI.getGeneratePointerMapsInputTypesField().setText(targetPointerMaps);
			}

			val truncateMemoryPointersDebuggingOutput = jsonObject.getBoolean(TRUNCATE_MEMORY_POINTERS_DEBUGGING_OUTPUT_JSON_KEY);
			pointerSearcherGUI.getTruncateMemoryPointersDebuggingOutputCheckBox().setSelected(truncateMemoryPointersDebuggingOutput);

			showMessageDialog(pointerSearcherGUI.getRootPane(),
					"The configuration has been restored successfully.",
					"Successfully restored",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private static String buildFileExtensionsString(final JSONArray fileExtensionsJSONArray)
	{
		val joinedFileExtensionsBuilder = new StringBuilder();
		var fileExtensionsJSONArrayIndex = 0;
		for (val fileExtension : fileExtensionsJSONArray)
		{
			joinedFileExtensionsBuilder.append(fileExtension.toString());
			if (fileExtensionsJSONArrayIndex != fileExtensionsJSONArray.toList().size() - 1)
			{
				joinedFileExtensionsBuilder.append(",");
			}

			fileExtensionsJSONArrayIndex++;
		}
		return joinedFileExtensionsBuilder.toString();
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
		val pointerMaps = memoryDumpTableManager.getPointerMaps();
		memoryDumps.addAll(pointerMaps);
		val inputFilesJSONObject = new JSONArray();
		for (val memoryDump : memoryDumps)
		{
			val inputFileJSONObject = new JSONObject();
			inputFileJSONObject.put(FILE_PATH_JSON_KEY, memoryDump.getFilePath().toString());
			inputFileJSONObject.put(INPUT_TYPE_JSON_KEY, memoryDump.getInputType().toString());
			inputFileJSONObject.put(FILE_TYPE_JSON_KEY, memoryDump.getFileType().toString());
			val startingAddress = memoryDump.getStartingAddress();
			if (startingAddress != null)
			{
				inputFileJSONObject.put(STARTING_ADDRESS_JSON_KEY, toHexadecimal(startingAddress));
			}
			inputFileJSONObject.put(COMPARISON_GROUP_NUMBER_JSON_KEY, memoryDump.getComparisonGroupNumber());

			inputFilesJSONObject.put(inputFileJSONObject);
		}
		rootJSONObject.put(INPUT_FILES_JSON_KEY, inputFilesJSONObject);

		if (!memoryDumps.isEmpty())
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
		val lastPointerOffsets = pointerSearcherGUI.getLastPointerOffsetsField().getText();
		val lastPointerOffsetsArray = lastPointerOffsets.split(",");
		val lastPointerOffsetsJSONArray = new JSONArray();
		for (val lastPointerOffset : lastPointerOffsetsArray)
		{
			lastPointerOffsetsJSONArray.put(lastPointerOffset);
		}
		rootJSONObject.put(LAST_POINTER_OFFSETS_JSON_KEY, lastPointerOffsetsJSONArray);
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
		rootJSONObject.put(SCAN_DEEPER_BY_CHECKBOX_JSON_KEY, pointerSearcherGUI.getScanDeeperByCheckBox().isSelected() + "");
		rootJSONObject.put(SCAN_DEEPER_BY_SPINNER_JSON_KEY, pointerSearcherGUI.getScanDeeperBySpinner().getValue().toString());
		rootJSONObject.put(PRINT_MODULE_FILE_NAMES_JSON_KEY, pointerSearcherGUI.getPrintModuleFileNamesCheckBox().isSelected());
		rootJSONObject.put(VERBOSE_LOGGING_JSON_KEY, pointerSearcherGUI.getVerboseLoggingCheckBox().isSelected());
		rootJSONObject.put(EXCLUDE_CYCLES_JSON_KEY, pointerSearcherGUI.getExcludeCyclesCheckBox().isSelected());
		rootJSONObject.put(PRINT_VISITED_ADDRESSES_JSON_KEY, pointerSearcherGUI.getPrintVisitedAddressesCheckBox().isSelected());
		rootJSONObject.put(READ_POINTER_MAPS_JSON_KEY, pointerSearcherGUI.getReadPointerMapsCheckBox().isSelected());
		rootJSONObject.put(GENERATE_POINTER_MAPS_JSON_KEY, pointerSearcherGUI.getGeneratePointerMapsCheckBox().isSelected());
		rootJSONObject.put(TARGET_POINTER_MAPS_JSON_KEY, pointerSearcherGUI.getGeneratePointerMapsInputTypesField().getText());
		rootJSONObject.put(TRUNCATE_MEMORY_POINTERS_DEBUGGING_OUTPUT_JSON_KEY, pointerSearcherGUI.getTruncateMemoryPointersDebuggingOutputCheckBox().isSelected());

		return formatJson(rootJSONObject.toString());
	}

	private static JSONObject buildRangeJSON(final JTextField minimumPointerSearchDepthField,
	                                         final JTextField maximumPointerSearchDepthField,
	                                         final boolean isHexadecimal)
	{
		val pointerDepthRangeJSONObject = new JSONObject();
		pointerDepthRangeJSONObject.put(POINTER_DEPTH_RANGE_FROM_JSON_KEY, isHexadecimal ? minimumPointerSearchDepthField.getText() : Long.parseLong(minimumPointerSearchDepthField.getText()));
		pointerDepthRangeJSONObject.put(POINTER_DEPTH_RANGE_TO_JSON_KEY, isHexadecimal ? maximumPointerSearchDepthField.getText() : Long.parseLong(maximumPointerSearchDepthField.getText()));
		return pointerDepthRangeJSONObject;
	}
}
