package com.wiiudev.gecko.pointer;

import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryDump;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static java.lang.Integer.toHexString;
import static java.lang.Long.toHexString;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.lineSeparator;
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

	private static Path executableFilePath;

	private List<MemoryDump> memoryDumps;

	@Setter
	private boolean allowNegativeOffsets;

	@Setter
	private boolean excludeCycles;

	@Setter
	private long maximumPointerOffset;

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
		val command = buildCommandList(executableFilePath);
		val processBuilder = new ProcessBuilder(command);
		command.remove(0);
		val executedCommand = toCommandString(command);
		processBuilder.redirectErrorStream(true);
		process = processBuilder.start();
		val processOutput = COMMAND_LINE_STARTING_SYMBOL
				+ executedCommand + "\n\n" + readFromProcess(process);
		val exitCode = process.waitFor();
		if (exitCode != 0)
		{
			val exceptionMessage = getExceptionMessage(exitCode);
			return new NativePointerSearcherOutput(exceptionMessage, processOutput);
		}

		return new NativePointerSearcherOutput(null, processOutput);
	}

	private String toCommandString(List<String> commands)
	{
		val stringBuilder = new StringBuilder();
		var commandItemIndex = 0;
		for (val commandItem : commands)
		{
			stringBuilder.append("\"");
			stringBuilder.append(commandItem);
			stringBuilder.append("\"");

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
		command.add(temporaryExecutableFile.toString());
		command.add(temporaryExecutableFile.toString()); // The file path is the first argument

		// Add commands for each memory dump
		for (val memoryDump : memoryDumps)
		{
			val memoryDumpFilePath = memoryDump.getFilePath().toAbsolutePath();
			command.add("-m");
			command.add(memoryDumpFilePath.toString());
			command.add("-s");
			val startingAddress = memoryDump.getStartingAddress();
			command.add(toHexString(startingAddress).toUpperCase());
			command.add("-t");
			val targetAddress = memoryDump.getTargetAddress();
			command.add(toHexString(targetAddress).toUpperCase());
			command.add("-e");
			val byteOrder = memoryDump.getByteOrder();
			command.add(byteOrderToString(byteOrder));
			command.add("-z");
			val addressSize = memoryDump.getAddressSize();
			command.add(toHexString(addressSize).toUpperCase());
			command.add("-a");
			val addressAlignment = memoryDump.getAddressAlignment();
			command.add(toHexString(addressAlignment).toUpperCase());
			command.add("-u");
			val valueAlignment = memoryDump.getValueAlignment();
			command.add(toHexString(valueAlignment).toUpperCase());
			command.add("-i");
			val minimumPointerAddress = memoryDump.getMinimumPointerAddress();
			command.add(toHexString(minimumPointerAddress).toUpperCase());
			val maximumPointerAddress = memoryDump.getMaximumPointerAddress();
			command.add(toHexString(maximumPointerAddress).toUpperCase());
			command.add("-g");
			command.add(booleanToIntegerString(memoryDump.isGeneratePointerMap()));
			command.add("-r");
			command.add(booleanToIntegerString(memoryDump.isReadPointerMap()));
		}

		command.add("-o");
		command.add(toHexString(maximumPointerOffset).toUpperCase());
		command.add("-n");
		command.add(booleanToIntegerString(allowNegativeOffsets));
		command.add("-k");
		command.add(booleanToIntegerString(excludeCycles));
		command.add("-d");
		command.add(maximumPointerDepth + "");
		command.add("-c");
		command.add(maximumMemoryDumpChunkSize + "");
		command.add("-v");
		command.add(booleanToIntegerString(saveAdditionalMemoryDumpRAM));
		command.add("-p");
		command.add(potentialPointerOffsetsCountPerAddressPrediction + "");
		command.add("-x");
		command.add(maximumPointersCount + "");
		command.add("-q");
		command.add(toCommaSeparated(lastPointerOffsets));

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
			stringBuilder.append(toHexString(list.get(listIndex)));
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

	private String byteOrderToString(ByteOrder byteOrder)
	{
		if (byteOrder.equals(LITTLE_ENDIAN))
		{
			return "l";
		} else if (byteOrder.equals(BIG_ENDIAN))
		{
			return "b";
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
