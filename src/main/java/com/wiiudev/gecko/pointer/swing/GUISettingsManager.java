package com.wiiudev.gecko.pointer.swing;

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

import static com.cedarsoftware.util.io.JsonWriter.formatJson;
import static com.wiiudev.gecko.pointer.preprocessed_search.utilities.DataConversions.toHexadecimal;
import static com.wiiudev.gecko.pointer.swing.utilities.FrameUtilities.getSelectedItem;
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
		val inputFilesJSONObject = new JSONObject();
		for (val memoryDump : memoryDumps)
		{
			val inputFileJSONObject = new JSONObject();
			inputFileJSONObject.put("file-path", memoryDump.getFilePath().toString());
			inputFileJSONObject.put("input-type", memoryDump.getInputType());
			inputFileJSONObject.put("starting-address", toHexadecimal(memoryDump.getStartingAddress()));

			inputFilesJSONObject.put("input-file", inputFileJSONObject);
		}
		rootJSONObject.put("input-files", inputFilesJSONObject);

		if (memoryDumps.size() != 0)
		{
			val firstMemoryDump = memoryDumps.get(0);
			rootJSONObject.put("target-address", toHexadecimal(firstMemoryDump.getTargetAddress()));
		}

		val pointerDepthRangeJSONObject = buildRangeJSON(pointerSearcherGUI.getMinimumPointerSearchDepthField(),
				pointerSearcherGUI.getMaximumPointerSearchDepthField(), false);
		rootJSONObject.put("pointer-depth-range", pointerDepthRangeJSONObject);
		val pointerOffsetRangeJSONObject = buildRangeJSON(pointerSearcherGUI.getMinimumPointerOffsetField(),
				pointerSearcherGUI.getMaximumPointerOffsetField(), true);
		rootJSONObject.put("pointer-offset-range", pointerOffsetRangeJSONObject);
		rootJSONObject.put("maximum-result-count", Long.parseLong(pointerSearcherGUI.getMaximumPointersCountField().getText()));
		rootJSONObject.put("maximum-memory-utilization-percentage", Double.parseDouble(pointerSearcherGUI.getMaximumMemoryUtilizationPercentageField().getText()));
		val fileExtensionsJSON = new JSONArray();
		val fileExtensions = pointerSearcherGUI.parseFileExtensions();
		for (val fileExtension : fileExtensions)
		{
			fileExtensionsJSON.put(fileExtension);
		}
		rootJSONObject.put("file-extensions", fileExtensionsJSON);
		rootJSONObject.put("address-size", getSelectedItem(pointerSearcherGUI.getAddressSizeSelection()));
		rootJSONObject.put("byte-order", getSelectedItem(pointerSearcherGUI.getByteOrderSelection()).toString());

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
