package com.wiiudev.gecko.pointer;

import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryDump;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryPointer;
import com.wiiudev.gecko.pointer.swing.TargetSystem;
import com.wiiudev.gecko.pointer.swing.utilities.MemoryDumpsByteOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.var;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryPointer.parseMemoryPointer;
import static com.wiiudev.gecko.pointer.preprocessed_search.data_structures.OffsetPrintingSetting.SIGNED;
import static com.wiiudev.gecko.pointer.swing.preprocessed_search.FileTypeImport.MEMORY_DUMP;
import static com.wiiudev.gecko.pointer.swing.preprocessed_search.FileTypeImport.MEMORY_DUMP_EXTENSION;
import static java.io.File.separator;
import static java.lang.Integer.toHexString;
import static java.lang.Long.toHexString;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.*;
import static java.lang.Thread.sleep;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.*;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.commons.lang3.SystemUtils.*;

public class NativePointerSearcherManager
{
	private static final String BINARY_NAME = "UniversalPointerSearcher";
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

	private static volatile Path executableFilePath;

	private final List<MemoryDump> memoryDumps;

	@Setter
	private boolean excludeCycles;

	@Getter
	@Setter
	private double maximumMemoryUtilizationFraction;

	@Getter
	@Setter
	private boolean printVisitedAddresses;

	@Getter
	@Setter
	private Path storeMemoryPointersFilePath;

	@Getter
	@Setter
	private boolean verboseLogging;

	// TODO Parsing the pointer expressions does not work with this flag turned on
	@Getter
	@Setter
	private boolean printModuleFileNames;

	@Getter
	@Setter
	private TargetSystem targetSystem;

	@Setter
	private long fromPointerOffset;

	@Setter
	private long toPointerOffset;

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
	@Setter
	private MemoryDumpsByteOrder byteOrder;

	@Getter
	@Setter
	private int addressSize;

	@Getter
	private boolean isCanceled;

	@Getter
	@Setter
	private long maximumPointerCount = 100000;

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

	private static final boolean USE_FILE_OUTPUT = false;

	public NativePointerSearcherOutput call() throws Exception
	{
		while (executableFilePath == null)
		{
			//noinspection BusyWait
			sleep(10);
		}

		val commandList = buildCommandList(executableFilePath);
		val processBuilder = new ProcessBuilder(commandList);
		commandList.remove(executableFilePath.toString());
		var executedCommand = toCommandString(commandList);
		processBuilder.redirectErrorStream(true);
		val pointerSearcherOutput = USE_FILE_OUTPUT ? createTempFile("prefix", "suffix") : null;

		try
		{
			if (USE_FILE_OUTPUT)
			{
				processBuilder.redirectOutput(pointerSearcherOutput.toFile());
			}

			process = processBuilder.start();
			val exitCode = process.waitFor();

			executedCommand = considerReplacingTemporaryDirectoryFilePath(executedCommand);
			val actualProcessOutput = USE_FILE_OUTPUT ?
					new String(readAllBytes(pointerSearcherOutput)) : readFromProcess(process);
			val processOutput = COMMAND_LINE_STARTING_SYMBOL
					+ executedCommand + "\n\n" + actualProcessOutput;
			if (exitCode != 0)
			{
				val exceptionMessage = getExceptionMessage(exitCode);
				return new NativePointerSearcherOutput(exceptionMessage, processOutput);
			}

			return new NativePointerSearcherOutput(null, processOutput);
		} finally
		{
			if (pointerSearcherOutput != null)
			{
				delete(pointerSearcherOutput);
			}
		}
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

		/* command.add("--thread-count");
		command.add(threadCount + "");
		command.add("--pointer-offset-range");
		command.add(toHexadecimalString(fromPointerOffset)
				+ "," + toHexadecimalString(toPointerOffset));
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
		command.add(commaSeparatedLastPointerOffsets); */

		if (!lastPointerOffsets.isEmpty())
		{
			command.add("--last-pointer-offsets");
			val commaSeparatedLastPointerOffsets = toCommaSeparated(lastPointerOffsets);
			command.add(commaSeparatedLastPointerOffsets);
		}

		command.add("--maximum-pointer-count");
		command.add(maximumPointerCount + "");

		command.add("--pointer-depth-range");
		command.add(minimumPointerDepth + "," + maximumPointerDepth);

		// Add commands for each memory dump
		for (val memoryDump : memoryDumps)
		{
			val memoryDumpFilePath = memoryDump.getFilePath().toAbsolutePath();
			command.add("--initial-file-path");
			command.add(memoryDumpFilePath.toString());
			command.add("--initial-starting-address");
			val startingAddress = memoryDump.getStartingAddress();
			command.add("0x" + toHexadecimalString(startingAddress));
			command.add("--target-address");
			val targetAddress = memoryDump.getTargetAddress();
			command.add("0x" + toHexadecimalString(targetAddress));
			/* command.add("--file-path");
			command.add(memoryDumpFilePath.toString());
			addFileExtensionsCommand(command, memoryDump);
			command.add("--starting-address");
			val startingAddress = memoryDump.getStartingAddress();
			command.add(toHexadecimalString(startingAddress));
			command.add("--destination-address");
			val targetAddress = memoryDump.getTargetAddress();
			command.add(toHexadecimalString(targetAddress));
			command.add("--endian");
			val byteOrder = memoryDump.getByteOrder();
			command.add(byteOrderToString(byteOrder));
			command.add("--address-size");
			val addressSize = memoryDump.getAddressSize();
			command.add(toHexadecimalString(addressSize));
			command.add("--address-alignment");
			val addressAlignment = memoryDump.getAddressAlignment();
			command.add(toHexadecimalString(addressAlignment));
			command.add("--value-size-alignment");
			val valueAlignment = memoryDump.getValueAlignment();
			command.add(toHexadecimalString(valueAlignment));
			command.add("--pointer-address-range");
			val minimumPointerAddress = memoryDump.getMinimumPointerAddress();
			val maximumPointerAddress = memoryDump.getMaximumPointerAddress();
			command.add(toHexadecimalString(minimumPointerAddress)
					+ "-" + toHexadecimalString(maximumPointerAddress));
			command.add("--write-pointer-map");
			command.add(booleanToIntegerString(memoryDump.isGeneratePointerMap()));
			command.add("--read-pointer-map");
			command.add(booleanToIntegerString(memoryDump.isReadPointerMap())); */
		}

		if (targetSystem != null)
		{
			command.add("--target-system");
			command.add(targetSystem.toString());
		}

		if (storeMemoryPointersFilePath != null)
		{
			command.add("--store-memory-pointers-file-path");
			command.add(storeMemoryPointersFilePath.toString());
		}

		command.add("--endian");
		val isLittleEndian = byteOrder.getByteOrder().equals(LITTLE_ENDIAN);
		command.add(isLittleEndian ? "little" : "big");

		command.add("--address-size");
		command.add(addressSize + "");

		command.add("--exclude-cycles");
		command.add(excludeCycles + "");

		if (printVisitedAddresses)
		{
			command.add("--print-visited-addresses");
		}

		command.add("--verbose");
		command.add(verboseLogging + "");

		if (printModuleFileNames)
		{
			command.add("--print-module-file-names");
		}

		command.add("--maximum-memory-utilization-fraction");
		command.add((maximumMemoryUtilizationFraction / 100) + "");

		/* TODO
		command.add("--scan-deeper-by");
		command.add("1"); */

		return command;
	}

	private String toHexadecimalString(long value)
	{
		if (value >= 0)
		{
			return toHexString(value).toUpperCase();
		} else
		{
			value *= -1;
			return "-" + toHexString(value).toUpperCase();
		}
	}

	private void addFileExtensionsCommand(List<String> command, MemoryDump memoryDump)
	{
		command.add("--file-extensions");
		val fileExtensions = memoryDump.getFileExtensions();
		if (fileExtensions == null)
		{
			val defaultFileExtensions = new ArrayList<String>();
			defaultFileExtensions.add(MEMORY_DUMP.getExtension());
			defaultFileExtensions.add(MEMORY_DUMP_EXTENSION);
			val fileExtensionsCommand = buildFileExtensionsCommand(defaultFileExtensions);
			command.add(fileExtensionsCommand);
		} else
		{
			val fileExtensionsCommand = buildFileExtensionsCommand(fileExtensions);
			command.add(fileExtensionsCommand);
		}
	}

	private String buildFileExtensionsCommand(List<String> fileExtensions)
	{
		val stringBuilder = new StringBuilder();
		var fileExtensionsIndex = 0;
		for (val fileExtension : fileExtensions)
		{
			stringBuilder.append(".");
			stringBuilder.append(fileExtension);

			if (fileExtensionsIndex != fileExtensions.size() - 1)
			{
				stringBuilder.append(",");
			}

			fileExtensionsIndex++;
		}

		return stringBuilder.toString();
	}

	private String toCommaSeparated(final List<Long> list)
	{
		if (list.isEmpty())
		{
			return "";
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

			val hexadecimalValue = toHexadecimalString(value);
			stringBuilder.append("0x");
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
		val posixFilePermissionsSet = new HashSet<>(allPosixFilePermissions);
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
		val inputStream = process.getInputStream();
		return IOUtils.toString(inputStream, UTF_8).trim();
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

	public static List<MemoryPointer> parseMemoryPointersFromOutput(String processOutput)
	{
		val lines = processOutput.split(lineSeparator());
		val memoryPointers = new ArrayList<MemoryPointer>();
		for (val line : lines)
		{
			try
			{
				val memoryPointer = parseMemoryPointer(line);
				memoryPointers.add(memoryPointer);
			} catch (Exception ignored)
			{

			}
		}
		return memoryPointers;
	}

	public static List<MemoryPointer> findPointers(NativePointerSearcherManager nativePointerSearcherManager,
	                                               int addressSize, boolean printResults) throws Exception
	{
		val nativePointerSearcherOutput = nativePointerSearcherManager.call();
		val processOutput = nativePointerSearcherOutput.getProcessOutput();

		if (printResults)
		{
			System.out.println(processOutput);
		}

		val memoryPointers = parseMemoryPointersFromOutput(processOutput);

		if (printResults)
		{
			System.out.println(MemoryPointer.toString(memoryPointers, addressSize, SIGNED));
			System.out.println("A total of " + memoryPointers.size() + " memory pointer(s) found");
		}

		return memoryPointers;
	}

	public void cancel()
	{
		isCanceled = true;
		process.destroyForcibly();
	}

	public void setPointerOffsetRange(int fromOffset, int toOffset)
	{
		this.fromPointerOffset = fromOffset;
		this.toPointerOffset = toOffset;
	}
}
