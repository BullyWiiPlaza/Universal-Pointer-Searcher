package com.wiiudev.gecko.pointer.non_preprocessed_search;

import lombok.Getter;
import lombok.val;
import lombok.var;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.wiiudev.gecko.pointer.swing.utilities.JTextAreaLimit.isHexadecimal;
import static com.wiiudev.gecko.pointer.utilities.FileNameUtilities.getBaseFileName;
import static java.lang.Long.parseLong;
import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.nio.file.Files.newDirectoryStream;

@Getter
public class MemoryDump
{
	private final Path binaryFilePath;

	private final long targetAddress;

	private final RandomAccessFile randomAccessFile;

	private final ByteBuffer byteBuffer;

	private MemoryDump(Path binaryFilePath) throws IOException
	{
		this(binaryFilePath.toString());
	}

	public MemoryDump(String binaryFilePath) throws IOException
	{
		this(binaryFilePath, parseLong(getBaseFileName(binaryFilePath), 16));
	}

	public MemoryDump(String binaryFilePath, long targetAddress) throws IOException
	{
		this.binaryFilePath = Paths.get(binaryFilePath);
		this.targetAddress = targetAddress;

		val binaryFile = this.binaryFilePath.toFile();
		randomAccessFile = new RandomAccessFile(binaryFile, "r");

		byteBuffer = getFreshByteBuffer();
	}

	public ByteBuffer getFreshByteBuffer() throws IOException
	{
		val fileChannel = randomAccessFile.getChannel();
		return fileChannel.map(READ_ONLY, 0, fileChannel.size());
	}

	public long getBytesCount()
	{
		return binaryFilePath.toFile().length();
	}

	public static ArrayList<MemoryDump> getMemoryDumps(String sourceDirectory) throws IOException
	{
		val memoryDumps = new ArrayList<MemoryDump>();

		try (val directoryStream = newDirectoryStream(Paths.get(sourceDirectory)))
		{
			for (val path : directoryStream)
			{
				if (path.toString().endsWith(".bin"))
				{
					memoryDumps.add(new MemoryDump(path));
				}
			}
		}

		return memoryDumps;
	}

	public static List<Long> getTargetAddresses(List<MemoryDump> memoryDumps)
	{
		val targetAddresses = new ArrayList<Long>();

		for (val memoryDump : memoryDumps)
		{
			val targetAddress = memoryDump.getTargetAddress();
			targetAddresses.add(targetAddress);
		}

		return targetAddresses;
	}

	public static long getDumpStartAddress(String sourceDirectory) throws IOException
	{
		try (val directoryStream = newDirectoryStream(Paths.get(sourceDirectory)))
		{
			for (val path : directoryStream)
			{
				var pathString = path.toFile().getName();
				val extension = ".txt";

				if (pathString.endsWith(extension))
				{
					pathString = pathString.replace(extension, "");

					if (isHexadecimal(pathString))
					{
						return parseLong(pathString, 16);
					}
				}
			}
		}

		return -1;
	}

	@Override
	public String toString()
	{
		return binaryFilePath.toFile().getName();
	}
}
