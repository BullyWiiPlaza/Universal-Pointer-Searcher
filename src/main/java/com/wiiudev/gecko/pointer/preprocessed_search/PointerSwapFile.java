package com.wiiudev.gecko.pointer.preprocessed_search;

import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryPointer;
import lombok.val;
import lombok.var;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.gc;
import static java.lang.System.getProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.*;
import static java.time.format.DateTimeFormatter.ofPattern;

public class PointerSwapFile
{
	private static final String LINE_SEPARATOR = "\n";
	private static final Charset ENCODING = UTF_8;
	public static final int STARTING_SWAP_FILE_NUMBER = 1;

	private final Path targetDirectory;
	private String formattedDate;
	private int currentSwapFileNumber = STARTING_SWAP_FILE_NUMBER;

	public PointerSwapFile()
	{
		this(getProperty("java.io.tmpdir"));
	}

	private PointerSwapFile(String targetDirectory)
	{
		this.targetDirectory = Paths.get(targetDirectory);
		formattedDate = getFormattedDate();
	}

	private static String getFormattedDate()
	{
		val now = LocalDate.now();
		val formatter = ofPattern("dd-M-yyyy");
		val localTime = LocalTime.now();
		val timeFormatter = ofPattern("hh-mm-ss");
		val formattedLocalTime = localTime.format(timeFormatter);
		return now.format(formatter) + "_" + formattedLocalTime;
	}

	public void storeToDisk(List<MemoryPointer> memoryPointers, int elementsCount) throws IOException
	{
		val bytes = getBackupPointersBytes(memoryPointers, elementsCount);
		val targetFilePath = getTargetFilePath(currentSwapFileNumber);
		val targetFile = Paths.get(targetFilePath);
		write(targetFile, bytes);
		currentSwapFileNumber++;
		removeBackupPointers(memoryPointers, elementsCount);
		gc();
	}

	private String getTargetFilePath(int currentSwapFileNumber)
	{
		return targetDirectory.resolve(formattedDate + " (" + currentSwapFileNumber + ").pswap").toString();
	}

	public ArrayList<MemoryPointer> getBackupPointers(int swapFileNumber) throws IOException
	{
		val targetFilePathString = getTargetFilePath(swapFileNumber);
		val targetFilePath = Paths.get(targetFilePathString);

		if (isRegularFile(targetFilePath))
		{
			return readMemoryPointers(targetFilePath);
		} else
		{
			val targetFile = targetFilePath.toFile();
			val fileName = targetFile.getName();
			throw new IllegalStateException("Back up file " + fileName + " is not a regular file");
		}
	}

	private ArrayList<MemoryPointer> readMemoryPointers(Path targetFilePath) throws IOException
	{
		val memoryPointers = new ArrayList<MemoryPointer>();
		val memoryPointersString = new String(readAllBytes(targetFilePath), ENCODING);
		val memoryPointerLines = memoryPointersString.split(LINE_SEPARATOR);
		for (val memoryPointerLine : memoryPointerLines)
		{
			val memoryPointer = new MemoryPointer(memoryPointerLine);
			memoryPointers.add(memoryPointer);
		}

		gc();

		return memoryPointers;
	}

	private void removeBackupPointers(List<MemoryPointer> memoryPointers, int elementsCount)
	{
		var memoryPointerIndex = 0;
		while (memoryPointerIndex < elementsCount)
		{
			memoryPointers.remove(0);
			memoryPointerIndex++;
		}
	}

	private byte[] getBackupPointersBytes(List<MemoryPointer> memoryPointers, int elementsCount)
	{
		val stringBuilder = new StringBuilder();
		for (var memoryPointerIndex = 0;
		     memoryPointerIndex < elementsCount;
		     memoryPointers.size())
		{
			val memoryPointer = memoryPointers.get(memoryPointerIndex);
			stringBuilder.append(memoryPointer.toString());

			if (memoryPointerIndex != memoryPointers.size() - 1)
			{
				stringBuilder.append(LINE_SEPARATOR);
			}

			memoryPointerIndex++;
		}

		val backupPointersString = stringBuilder.toString();
		return backupPointersString.getBytes(ENCODING);
	}
}
