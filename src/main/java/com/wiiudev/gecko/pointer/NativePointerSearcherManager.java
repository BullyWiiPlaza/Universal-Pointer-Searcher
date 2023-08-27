package com.wiiudev.gecko.pointer;

import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryDump;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryPointer;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryRange;
import com.wiiudev.gecko.pointer.swing.TargetSystem;
import com.wiiudev.gecko.pointer.swing.preprocessed_search.InputType;
import com.wiiudev.gecko.pointer.swing.utilities.MemoryDumpsByteOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.var;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import static com.wiiudev.gecko.pointer.preprocessed_search.data_structures.OffsetPrintingSetting.SIGNED;
import static java.io.File.separator;
import static java.lang.Integer.toHexString;
import static java.lang.Long.toHexString;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
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
	public static final String POINTER_MAP_EXTENSION = "pointermap";

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

	private final List<MemoryDump> pointerMaps = new ArrayList<>();

	@Setter
	private boolean excludeCycles;

	@Getter
	@Setter
	private String[] writePointerMapInputTypes = new String[]{};

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
	private Path loadMemoryPointersFilePath;

	@Getter
	@Setter
	private boolean verboseLogging;

	@Getter
	@Setter
	private boolean printModuleFileNames;

	@Getter
	@Setter
	private TargetSystem targetSystem;

	@Getter
	@Setter
	private List<MemoryRange> pointerAddressRanges = new ArrayList<>();

	@Getter
	@Setter
	private String[] fileExtensions;

	@Getter
	@Setter
	private Integer scanDeeperBy;

	@Getter
	@Setter
	private boolean writePointerMaps = false;

	@Getter
	@Setter
	private boolean readPointerMaps = false;

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
		WINDOWS_SYSTEM32_DIRECTORY = System.getenv("WINDIR") + "\\system32";
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
			Thread.sleep(10);
		}

		val commandList = buildCommandList(executableFilePath);
		val processBuilder = new ProcessBuilder(commandList);
		commandList.remove(executableFilePath.toString());
		var executedCommand = toCommandString(commandList);
		processBuilder.redirectErrorStream(true);
		Path pointerSearcherOutput = USE_FILE_OUTPUT ? Files.createTempFile("prefix", "suffix") : null;

		try
		{
			if (USE_FILE_OUTPUT)
			{
				processBuilder.redirectOutput(pointerSearcherOutput.toFile());
			}

			process = processBuilder.start();
			runningNativePointerSearcher = process;
			val exitCode = process.waitFor();

			executedCommand = considerReplacingTemporaryDirectoryFilePath(executedCommand);
			val actualProcessOutput = USE_FILE_OUTPUT ?
					new String(Files.readAllBytes(pointerSearcherOutput)) : readFromProcess(process);
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
				Files.delete(pointerSearcherOutput);
			}
		}
	}

	public static String compressProcessOutput(String processOutput)
	{
		val stringBuilder = new StringBuilder();
		val lines = processOutput.split(System.lineSeparator());
		var reachedMemoryPointers = false;
		for (val line : lines)
		{
			try
			{
				new MemoryPointer(line);

				// Print out truncation
				if (!reachedMemoryPointers)
				{
					stringBuilder.append("<< Memory pointers truncated >>");
					stringBuilder.append(System.lineSeparator());
					reachedMemoryPointers = true;
				}
			} catch (Exception ignored)
			{
				// Print out the unfiltered line
				stringBuilder.append(line);
				stringBuilder.append(System.lineSeparator());
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
		var temporaryDirectory = System.getProperty("java.io.tmpdir");
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

		command.add("--pointer-offset-range");
		command.add(toHexadecimalString(fromPointerOffset)
		            + "," + toHexadecimalString(toPointerOffset));

		command.add("--pointer-depth-range");
		command.add(minimumPointerDepth + "," + maximumPointerDepth);

		val targetAddresses = new ArrayList<Long>();

		// Combine multiple arguments into a single command line switch
		val initialFilePaths = new ArrayList<Path>();
		val initialStartingAddresses = new ArrayList<Long>();
		for (val memoryDump : memoryDumps)
		{
			val fileType = memoryDump.getInputType();
			if (fileType.equals(InputType.INITIAL))
			{
				val filePath = memoryDump.getFilePath();
				initialFilePaths.add(filePath);

				val startingAddress = memoryDump.getStartingAddress();
				initialStartingAddresses.add(startingAddress);
			}
		}

		if (!memoryDumps.isEmpty())
		{
			val memoryDump = memoryDumps.get(0);
			val targetAddress = memoryDump.getTargetAddress();
			targetAddresses.add(targetAddress);
		}

		if (!initialFilePaths.isEmpty())
		{
			command.add("--initial-file-path");
			for (val filePath : initialFilePaths)
			{
				command.add(filePath.toString());
			}
		}

		if (!initialStartingAddresses.isEmpty())
		{
			command.add("--initial-starting-address");
			for (val startingAddress : initialStartingAddresses)
			{
				val hexadecimalStartingAddress = "0x" + toHexadecimalString(startingAddress);
				command.add(hexadecimalStartingAddress);
			}
		}

		val comparisonFilePathEntries = new TreeMap<Integer, ArrayList<Path>>();
		val comparisonStartingAddressesEntries = new TreeMap<Integer, ArrayList<Long>>();
		val comparisonGroupNumberTargetAddresses = new TreeMap<Integer, Long>();
		for (val memoryDump : memoryDumps)
		{
			val inputType = memoryDump.getInputType();
			if (inputType.equals(InputType.COMPARISON))
			{
				val comparisonGroupNumber = memoryDump.getComparisonGroupNumber();

				val filePath = memoryDump.getFilePath();
				var comparisonFilePathsList = comparisonFilePathEntries.computeIfAbsent(comparisonGroupNumber,
						k -> new ArrayList<>());
				comparisonFilePathsList.add(filePath);

				val startingAddress = memoryDump.getStartingAddress();
				var comparisonStartingAddressesList = comparisonStartingAddressesEntries.computeIfAbsent(comparisonGroupNumber, k -> new ArrayList<>());
				comparisonStartingAddressesList.add(startingAddress);

				val targetAddress = memoryDump.getTargetAddress();
				comparisonGroupNumberTargetAddresses.put(comparisonGroupNumber, targetAddress);
			}
		}

		if (!comparisonFilePathEntries.isEmpty())
		{
			command.add("--comparison-file-path");
			var currentComparisonGroupNumber = 1;
			for (val comparisonFilePathEntry : comparisonFilePathEntries.entrySet())
			{
				val comparisonGroupNumber = comparisonFilePathEntry.getKey();
				if (comparisonGroupNumber != currentComparisonGroupNumber)
				{
					command.add("%%");
					currentComparisonGroupNumber = comparisonGroupNumber;
				}

				val comparisonFilePaths = comparisonFilePathEntry.getValue();
				for (val comparisonFilePath : comparisonFilePaths)
				{
					command.add(comparisonFilePath.toString());
				}
			}
		}

		if (!comparisonStartingAddressesEntries.isEmpty())
		{
			command.add("--comparison-starting-address");
			var currentComparisonGroupNumber = 1;
			for (val comparisonStartingAddressEntry : comparisonStartingAddressesEntries.entrySet())
			{
				val comparisonGroupNumber = comparisonStartingAddressEntry.getKey();
				if (comparisonGroupNumber != currentComparisonGroupNumber)
				{
					command.add("%%");
					currentComparisonGroupNumber = comparisonGroupNumber;
				}

				val startingAddresses = comparisonStartingAddressEntry.getValue();
				for (val startingAddress : startingAddresses)
				{
					val hexadecimalStartingAddress = "0x" + toHexadecimalString(startingAddress);
					command.add(hexadecimalStartingAddress);
				}
			}
		}

		targetAddresses.addAll(comparisonGroupNumberTargetAddresses.values());

		// Add commands for each memory dump
		for (val memoryDump : memoryDumps)
		{
			/* val fileType = memoryDump.getInputType();
			if (fileType.equals(InputType.COMPARISON))
			{
				command.add("--comparison-file-path");
				val memoryDumpFilePath = memoryDump.getFilePath().toAbsolutePath();
				command.add(memoryDumpFilePath.toString());
				command.add("--comparison-starting-address");
				val startingAddress = memoryDump.getStartingAddress();
				command.add("0x" + toHexadecimalString(startingAddress));
				passTargetAddress(command, memoryDump);
			} */

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

		val targetAddressFlagName = "--target-address";
		var isTargetAddressFlagPassed = false;
		if (!targetAddresses.isEmpty())
		{
			// Pass the target addresses (one per memory snapshot)
			command.add(targetAddressFlagName);
			isTargetAddressFlagPassed = true;
			for (val targetAddress : targetAddresses)
			{
				val hexadecimalTargetAddress = "0x" + toHexadecimalString(targetAddress);
				command.add(hexadecimalTargetAddress);
			}
		}

		if (!pointerMaps.isEmpty())
		{
			command.add("--read-pointer-maps-file-paths");
		}

		for (val pointerMap : pointerMaps)
		{
			command.add(pointerMap.getFilePath().toString());
		}

		val targetPointerMaps = "--target-pointer-maps";
		if (!pointerMaps.isEmpty())
		{
			command.add(targetPointerMaps);
		}

		for (val pointerMap : pointerMaps)
		{
			val inputType = pointerMap.getInputType();
			if (inputType.equals(InputType.INITIAL))
			{
				command.add(inputType.toString());
			} else
			{
				val comparisonInputType = pointerMap.getComparisonInputType();
				command.add(comparisonInputType);
			}
		}

		if (!pointerMaps.isEmpty() && !isTargetAddressFlagPassed)
		{
			command.add(targetAddressFlagName);
		}

		for (val pointerMap : pointerMaps)
		{
			val targetAddress = pointerMap.getTargetAddress();
			command.add("0x" + toHexadecimalString(targetAddress));
		}

		if (writePointerMapInputTypes.length > 0)
		{
			command.add(targetPointerMaps);
			command.addAll(asList(writePointerMapInputTypes));
		}

		if (targetSystem == null)
		{
			command.add("--endian");
			val isLittleEndian = byteOrder.getByteOrder().equals(LITTLE_ENDIAN);
			command.add(isLittleEndian ? "little" : "big");

			command.add("--address-size");
			command.add(String.valueOf(addressSize));
		} else
		{
			command.add("--target-system");
			command.add(targetSystem.toString());
		}

		if (!pointerAddressRanges.isEmpty())
		{
			command.add("--pointer-address-range");
			for (val pointerAddressRange : pointerAddressRanges)
			{
				command.add(pointerAddressRange.toString());
			}
		}

		if (storeMemoryPointersFilePath != null)
		{
			command.add("--store-memory-pointers-file-path");
			command.add(storeMemoryPointersFilePath.toString());
		}

		if (loadMemoryPointersFilePath != null)
		{
			command.add("--input-memory-pointers-file-path");
			command.add(loadMemoryPointersFilePath.toString());
		}

		if (!excludeCycles)
		{
			command.add("--exclude-cycles");
			command.add(false + "");
		}

		if (printVisitedAddresses)
		{
			command.add("--print-visited-addresses");
		}

		if (verboseLogging)
		{
			command.add("--verbose");
		}

		if (printModuleFileNames)
		{
			command.add("--print-module-file-names");
		}

		command.add("--maximum-memory-utilization-fraction");
		command.add(String.valueOf(maximumMemoryUtilizationFraction / 100));

		if (fileExtensions != null)
		{
			command.add("--file-extensions");
			command.addAll(asList(fileExtensions));
		}

		if (scanDeeperBy != null)
		{
			command.add("--scan-deeper-by");
			command.add(String.valueOf(scanDeeperBy));
		}

		if (writePointerMaps)
		{
			command.add("--write-pointer-maps-file-paths");

			for (val writePointerMapInputType : writePointerMapInputTypes)
			{
				if (writePointerMapInputType.contains("Initial") && !initialFilePaths.isEmpty())
				{
					addPointerMapCommand(command, initialFilePaths.get(0));
				}
			}

			for (val comparisonFilePathEntry : comparisonFilePathEntries.entrySet())
			{
				val comparisonFilePaths = comparisonFilePathEntry.getValue();
				if (!comparisonFilePaths.isEmpty())
				{
					addPointerMapCommand(command, comparisonFilePaths.get(0));
				}
			}
		}

		command.add("--maximum-pointer-count");
		command.add(String.valueOf(maximumPointerCount));
		command.add("--maximum-pointers-printed-count");
		command.add(String.valueOf(maximumPointerCount));

		return command;
	}

	private static void addPointerMapCommand(final List<String> command, final Path initialFilePath)
	{
		if (Files.isDirectory(initialFilePath))
		{
			// Write the pointer map into the folder
			val pointerMapFilePath = initialFilePath.resolve(initialFilePath.getFileName() + "." + POINTER_MAP_EXTENSION);
			command.add(pointerMapFilePath.toString());
		} else
		{
			// Write the pointer map with the same file name
			val pointerMapFileName = FilenameUtils.removeExtension(initialFilePath.toString())
			                         + "." + POINTER_MAP_EXTENSION;
			val pointerMapFilePath = initialFilePath.getParent().resolve(pointerMapFileName);
			command.add(pointerMapFilePath.toString());
		}
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

	public static Process runningNativePointerSearcher = null;

	private static Path getExecutableFilePath() throws IOException
	{
		val temporaryDirectory = Files.createTempDirectory("prefix");

		val runtime = Runtime.getRuntime();
		runtime.addShutdownHook(new Thread(() ->
		{
			if (runningNativePointerSearcher != null)
			{
				// Checking for isAlive() isn't necessary
				System.out.println("Killing potentially running pointer searcher...");
				runningNativePointerSearcher.destroy();
			}

			var repeatIndex = 0;
			val maximumRepetitionCount = 10;
			while (repeatIndex < maximumRepetitionCount)
			{
				try
				{
					System.out.println("Cleanup attempt: " + (repeatIndex + 1) + "/" + maximumRepetitionCount + "...");
					deleteDirectory(temporaryDirectory.toFile());
					break;
				} catch (final Exception ignored)
				{

				}

				repeatIndex++;

				try
				{
					Thread.sleep(1_000);
				} catch (final InterruptedException ignored)
				{

				}
			}
		}));

		val executableFileBytes = readExecutableFileBytes();
		val fileName = BINARY_NAME + DOT_EXTENSION;
		val executableFilePath = temporaryDirectory.resolve(fileName);
		Files.write(executableFilePath, executableFileBytes);

		if (IS_OS_UNIX)
		{
			giveAllPosixFilePermissions(executableFilePath);
		}

		return executableFilePath;
	}

	public static void giveAllPosixFilePermissions(Path filePath) throws IOException
	{
		val allPosixFilePermissions = asList(PosixFilePermission.values());
		val posixFilePermissionsSet = new HashSet<>(allPosixFilePermissions);
		Files.setPosixFilePermissions(filePath, posixFilePermissionsSet);
	}

	private static byte[] readExecutableFileBytes() throws IOException
	{
		val clazz = NativePointerSearcherManager.class;
		val classLoader = clazz.getClassLoader();
		try (val resourceAsStream = classLoader.getResourceAsStream(POINTER_SEARCHER_BINARY))
		{
			if (resourceAsStream == null)
			{
				throw new IllegalStateException("Cannot find pointer searcher native binary");
			}

			return toByteArray(resourceAsStream);
		}
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
		val lines = processOutput.split(System.lineSeparator());
		val memoryPointers = new ArrayList<MemoryPointer>();
		for (val line : lines)
		{
			try
			{
				val memoryPointer = new MemoryPointer(line);
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

	public void cancel() throws Exception
	{
		isCanceled = true;
		process.destroyForcibly();

		killProcessByName(BINARY_NAME);
	}

	private static void killProcessByName(@SuppressWarnings("SameParameterValue") final String binaryName)
			throws Exception
	{
		ProcessBuilder processBuilder;
		Process process;
		val binaryExtension = getExtension();
		if (IS_OS_WINDOWS)
		{
			processBuilder = new ProcessBuilder("taskkill", "/F", "/IM", binaryName + "." + binaryExtension);
		} else
		{
			// Valid for Linux and macOS
			processBuilder = new ProcessBuilder("pkill", "-f", binaryName + "." + binaryExtension);
		}
		process = processBuilder.start();

		val exitCode = process.waitFor();
		if (exitCode != 0)
		{
			throw new IOException("Failed to terminate process, exit code: " + exitCode);
		}
	}

	public void setPointerOffsetRange(int fromOffset, int toOffset)
	{
		this.fromPointerOffset = fromOffset;
		this.toPointerOffset = toOffset;
	}

	public void addPointerMap(final MemoryDump pointerMap)
	{
		pointerMaps.add(pointerMap);
	}
}
