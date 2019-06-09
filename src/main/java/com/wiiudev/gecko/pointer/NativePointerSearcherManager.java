package com.wiiudev.gecko.pointer;

import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryDump;
import com.wiiudev.gecko.pointer.swing.preprocessed_search.FileTypeImport;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.var;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryPointer.parseMemoryPointer;
import static java.io.File.separator;
import static java.lang.Integer.toHexString;
import static java.lang.Long.toHexString;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.*;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.file.Files.*;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.commons.lang3.SystemUtils.*;

public class NativePointerSearcherManager
{
	private static final String BINARY_NAME = "PointerSearcher";
	private static final String EXTENSION = getExtension();
	private static final String DOT_EXTENSION = "." + EXTENSION;
	private static final String WINDOWS_SYSTEM32_DIRECTORY;
	private static final String CMD_FILE_PATH;
	private static final String WINDOWS_TEMPORARY_DIRECTORY_COMMAND = "%TEMP%";

	private static String getExtension()
	{
		if (IS_OS_WINDOWS)
		{
			return "exe";
		} else if (IS_OS_LINUX)
		{
			return "elf";
		} else if (IS_OS_MAC)
		{
			return "macho";
		}

		return "";
	}

	private static final String POINTER_SEARCHER_BINARY = BINARY_NAME + DOT_EXTENSION;
	private static final String COMMAND_LINE_STARTING_SYMBOL = IS_OS_WINDOWS ? ">" : "$ ";
	private static final boolean ELEVATE_PROCESS_PRIORITY = true;

	private static Path executableFilePath;

	private List<MemoryDump> memoryDumps;

	@Setter
	private boolean allowNegativeOffsets;

	@Setter
	private boolean excludeCycles;

	@Setter
	private long maximumPointerOffset;

	@Setter
	private long threadCount;

	@Setter
	private long minimumPointerDepth;

	@Setter
	private long maximumPointerDepth;

	@Setter
	private long maximumMemoryDumpChunkSize;

	@Setter
	private boolean saveAdditionalMemoryDumpRAM;

	@Setter
	private long potentialPointerOffsetsCountPerAddressPrediction;

	@Getter
	private boolean isCanceled;

	@Getter
	@Setter
	private long maximumPointersCount = 100000;

	@Getter
	@Setter
	private List<Long> lastPointerOffsets = new ArrayList<>();

	static
	{
		WINDOWS_SYSTEM32_DIRECTORY = getenv("WINDIR") + "\\system32";
		CMD_FILE_PATH = WINDOWS_SYSTEM32_DIRECTORY + "\\" + "cmd";

		val thread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					executableFilePath = getExecutableFilePath();
				} catch (IOException exception)
				{
					exception.printStackTrace();
				}

				val runtime = getRuntime();
				runtime.addShutdownHook(new Thread(() ->
				{
					try
					{
						delete(executableFilePath);
					} catch (IOException exception)
					{
						exception.printStackTrace();
					}
				}));
			}
		});

		thread.setName("Native Pointer Searcher Extractor");
		thread.start();
	}

	private Process process;

	public NativePointerSearcherManager()
	{
		memoryDumps = new ArrayList<>();
	}

	public void addMemoryDump(MemoryDump memoryDump)
	{
		memoryDumps.add(memoryDump);
	}

	public NativePointerSearcherOutput call() throws Exception
	{
		val commandList = buildCommandList(executableFilePath);
		val processBuilder = new ProcessBuilder(commandList);
		commandList.remove(executableFilePath.toString());
		var executedCommand = toCommandString(commandList);
		processBuilder.redirectErrorStream(true);
		process = processBuilder.start();
		executedCommand = considerReplacingTemporaryDirectoryFilePath(executedCommand);
		val actualProcessOutput = readFromProcess(process);
		val processOutput = COMMAND_LINE_STARTING_SYMBOL
				+ executedCommand + "\n\n" + actualProcessOutput;
		val exitCode = process.waitFor();
		if (exitCode != 0)
		{
			val exceptionMessage = getExceptionMessage(exitCode);
			return new NativePointerSearcherOutput(exceptionMessage, processOutput);
		}

		return new NativePointerSearcherOutput(null, processOutput);
	}

	public static String compressProcessOutput(String processOutput)
	{
		val stringBuilder = new StringBuilder();
		val lines = processOutput.split(lineSeparator());
		var reachedMemoryPointers = false;
		for (val line : lines)
		{
			try
			{
				parseMemoryPointer(line);

				// Print out truncation
				if (!reachedMemoryPointers)
				{
					stringBuilder.append("<< Memory pointers truncated >>");
					stringBuilder.append(lineSeparator());
					reachedMemoryPointers = true;
				}
			} catch (Exception ignored)
			{
				// Print out the unfiltered line
				stringBuilder.append(line);
				stringBuilder.append(lineSeparator());
			}
		}

		return stringBuilder.toString();
	}

	private String considerReplacingTemporaryDirectoryFilePath(String input)
	{
		if (IS_OS_WINDOWS)
		{
			val temporaryDirectory = getTemporaryFilePath();
			if (input.contains(temporaryDirectory))
			{
				input = input.replace(temporaryDirectory, WINDOWS_TEMPORARY_DIRECTORY_COMMAND);
			}
		}
		return input;
	}

	private static String getTemporaryFilePath()
	{
		var temporaryDirectory = getProperty("java.io.tmpdir");
		if (temporaryDirectory.endsWith(separator))
		{
			temporaryDirectory = temporaryDirectory.substring(0,
					temporaryDirectory.length() - separator.length());
		}
		return temporaryDirectory;
	}

	private String toCommandString(List<String> commands)
	{
		val stringBuilder = new StringBuilder();
		var commandItemIndex = 0;
		for (val commandItem : commands)
		{
			if (commandItem.contains(" "))
			{
				stringBuilder.append("\"");
			}
			stringBuilder.append(commandItem);
			if (commandItem.contains(" "))
			{
				stringBuilder.append("\"");
			}

			val commandsCount = commands.size();
			if (commandItemIndex != commandsCount - 1)
			{
				stringBuilder.append(" ");
			}

			commandItemIndex++;
		}

		return stringBuilder.toString();
	}

	private String getExceptionMessage(int exitCode)
	{
		var additionalErrorHint = "";
		if (exitCode == -1073740791)
		{
			additionalErrorHint = "\nPlease try setting your \"maximum memory chunk size\" " +
					"large enough to read the entire file at once.";
		}

		return "Native pointer searcher " +
				"finished abnormally with exit code " + exitCode + " (0x"
				+ toHexString(exitCode).toUpperCase() + ")" + additionalErrorHint;
	}

	private List<String> buildCommandList(Path temporaryExecutableFile)
	{
		val command = new ArrayList<String>();

		if (IS_OS_WINDOWS && ELEVATE_PROCESS_PRIORITY)
		{
			command.add(CMD_FILE_PATH); // Command prompt
			command.add("/c"); // Carry out the command
			command.add("start"); // Start an external process
			command.add("\"Native-Pointer-Searcher\""); // The name of the process
			command.add("/realtime"); // Highest possible priority
			command.add("/b"); // Same command window
		}

		command.add(temporaryExecutableFile.toString());
		command.add(temporaryExecutableFile.toString()); // The file path is the first argument

		command.add("--thread-count");
		command.add(threadCount + "");
		command.add("--maximum-pointer-offset");
		command.add(toHexString(maximumPointerOffset).toUpperCase());
		command.add("--allow-negative-offsets");
		command.add(booleanToIntegerString(allowNegativeOffsets));
		command.add("--exclude-cycles");
		command.add(booleanToIntegerString(excludeCycles));
		command.add("--minimum-pointer-depth");
		command.add(minimumPointerDepth + "");
		command.add("--maximum-pointer-depth");
		command.add(maximumPointerDepth + "");
		command.add("--maximum-chunk-size");
		command.add(maximumMemoryDumpChunkSize + "");
		command.add("--save-additional-memory-dump-ram");
		command.add(booleanToIntegerString(saveAdditionalMemoryDumpRAM));
		command.add("--potential-pointer-offsets-count-per-address-prediction");
		command.add(potentialPointerOffsetsCountPerAddressPrediction + "");
		command.add("--maximum-pointers-count");
		command.add(maximumPointersCount + "");
		command.add("--last-pointer-offsets");
		val commaSeparatedLastPointerOffsets = toCommaSeparated(lastPointerOffsets);
		command.add(commaSeparatedLastPointerOffsets);

		// Add commands for each memory dump
		for (val memoryDump : memoryDumps)
		{
			val memoryDumpFilePath = memoryDump.getFilePath().toAbsolutePath();
			command.add("--file-path");
			command.add(memoryDumpFilePath.toString());
			command.add("--file-extensions");
			command.add("." + FileTypeImport.MEMORY_DUMP.getExtension()
					+ ",." + FileTypeImport.MEMORY_DUMP_EXTENSION);
			command.add("--starting-address");
			val startingAddress = memoryDump.getStartingAddress();
			command.add(toHexString(startingAddress).toUpperCase());
			command.add("--destination-address");
			val targetAddress = memoryDump.getTargetAddress();
			command.add(toHexString(targetAddress).toUpperCase());
			command.add("--endian");
			val byteOrder = memoryDump.getByteOrder();
			command.add(byteOrderToString(byteOrder));
			command.add("--address-size");
			val addressSize = memoryDump.getAddressSize();
			command.add(toHexString(addressSize).toUpperCase());
			command.add("--address-alignment");
			val addressAlignment = memoryDump.getAddressAlignment();
			command.add(toHexString(addressAlignment).toUpperCase());
			command.add("--value-size-alignment");
			val valueAlignment = memoryDump.getValueAlignment();
			command.add(toHexString(valueAlignment).toUpperCase());
			command.add("--pointer-address-range");
			val minimumPointerAddress = memoryDump.getMinimumPointerAddress();
			val maximumPointerAddress = memoryDump.getMaximumPointerAddress();
			command.add(toHexString(minimumPointerAddress).toUpperCase()
					+ "-" + toHexString(maximumPointerAddress).toUpperCase());
			command.add("--write-pointer-map");
			command.add(booleanToIntegerString(memoryDump.isGeneratePointerMap()));
			command.add("--read-pointer-map");
			command.add(booleanToIntegerString(memoryDump.isReadPointerMap()));
		}

		return command;
	}

	private String toCommaSeparated(List<Long> list)
	{
		if (list.isEmpty())
		{
			return " ";
		}

		val stringBuilder = new StringBuilder();
		for (var listIndex = 0; listIndex < list.size(); listIndex++)
		{
			var value = list.get(listIndex);
			if (value < 0)
			{
				stringBuilder.append("-");
				value *= -1;
			}

			val hexadecimalValue = toHexString(value).toUpperCase();
			stringBuilder.append(hexadecimalValue);
			if (listIndex != list.size() - 1)
			{
				stringBuilder.append(",");
			}
		}
		return stringBuilder.toString();
	}

	private String booleanToIntegerString(boolean bool)
	{
		return bool ? "1" : "0";
	}

	private static Path getExecutableFilePath() throws IOException
	{
		val executableFileBytes = readExecutableFileBytes();
		val temporaryExecutableFile = createTempFile(BINARY_NAME, DOT_EXTENSION);
		write(temporaryExecutableFile, executableFileBytes);

		if (IS_OS_UNIX)
		{
			giveAllPosixFilePermissions(temporaryExecutableFile);
		}

		return temporaryExecutableFile;
	}

	public static void giveAllPosixFilePermissions(Path filePath) throws IOException
	{
		val allPosixFilePermissions = asList(PosixFilePermission.values());
		val posixFilePermissionsSet = new HashSet<PosixFilePermission>(allPosixFilePermissions);
		setPosixFilePermissions(filePath, posixFilePermissionsSet);
	}

	private static byte[] readExecutableFileBytes() throws IOException
	{
		val clazz = NativePointerSearcherManager.class;
		val classLoader = clazz.getClassLoader();
		val resourceAsStream = classLoader.getResourceAsStream(POINTER_SEARCHER_BINARY);

		if (resourceAsStream == null)
		{
			throw new IllegalStateException("Cannot find pointer searcher native binary");
		}

		return toByteArray(resourceAsStream);
	}

	public static String readFromProcess(Process process) throws IOException
	{
		StringBuilder stringBuilder;
		val lineSeparator = lineSeparator();
		try (val bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream())))
		{
			stringBuilder = new StringBuilder();
			String line;
			while ((line = bufferedReader.readLine()) != null)
			{
				stringBuilder.append(line);
				stringBuilder.append(lineSeparator);
			}
		}

		return stringBuilder.toString().trim();
	}

	public static String byteOrderToString(ByteOrder byteOrder)
	{
		if (byteOrder.equals(LITTLE_ENDIAN))
		{
			return "little";
		} else if (byteOrder.equals(BIG_ENDIAN))
		{
			return "big";
		}

		// Should never happen
		throw new IllegalStateException("Illegal byte order");
	}

	public void cancel()
	{
		isCanceled = true;
		process.destroyForcibly();
	}
}
