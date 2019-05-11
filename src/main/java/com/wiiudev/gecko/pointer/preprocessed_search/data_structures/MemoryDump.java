package com.wiiudev.gecko.pointer.preprocessed_search.data_structures;

import com.wiiudev.gecko.pointer.swing.preprocessed_search.FileTypeImport;
import com.wiiudev.gecko.pointer.utilities.Benchmark;
import lombok.*;

import javax.swing.*;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;

import static com.wiiudev.gecko.pointer.preprocessed_search.utilities.DataConversions.toHexadecimal;
import static com.wiiudev.gecko.pointer.preprocessed_search.utilities.FileReadingUtilities.getByteBuffers;
import static com.wiiudev.gecko.pointer.preprocessed_search.utilities.NumberAlignmentUtilities.alignLong;
import static com.wiiudev.gecko.pointer.swing.preprocessed_search.FileTypeImport.MEMORY_DUMP;
import static com.wiiudev.gecko.pointer.swing.preprocessed_search.FileTypeImport.POINTER_MAP;
import static com.wiiudev.gecko.pointer.utilities.FileNameUtilities.getBaseFileName;
import static java.io.File.separator;
import static java.lang.Integer.toUnsignedLong;
import static java.lang.Math.min;
import static java.util.logging.Level.INFO;
import static java.util.logging.Logger.getLogger;
import static javax.swing.SwingUtilities.invokeLater;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MemoryDump
{
	private static final int MEMORY_DUMP_READING_UPDATE_PERCENTAGE_SIZE = 1000;

	private static final Logger LOGGER = getLogger(MemoryDump.class.getName());

	static
	{
		LOGGER.setLevel(INFO);
	}

	@Getter
	@EqualsAndHashCode.Include
	private final Path filePath;

	@Getter
	@Setter
	private FileTypeImport fileType;

	@Getter
	@Setter
	private Long startingAddress;

	@Getter
	private final Long targetAddress;

	@Getter
	private final ByteOrder byteOrder;

	@Getter
	@Setter
	private long addressSize;

	@Getter
	@Setter
	private long addressAlignment;

	@Getter
	@Setter
	private long valueAlignment;

	@Getter
	@Setter
	private long minimumPointerAddress;

	@Getter
	@Setter
	private long maximumPointerAddress;

	@Getter
	@Setter
	private boolean generatePointerMap;

	@Getter
	@Setter
	private boolean readPointerMap;

	@Getter
	@Setter
	private boolean isAddedAsFolder;

	public MemoryDump(String memoryDumpFilePath,
	                  Long startingAddress,
	                  Long targetAddress,
	                  ByteOrder byteOrder)
	{
		filePath = Paths.get(memoryDumpFilePath);

		/* if (!isRegularFile(filePath))
		{
			throw new IllegalArgumentException(filePath + " is not a regular file!");
		} */

		this.startingAddress = startingAddress;
		this.targetAddress = targetAddress;

		/* val memoryDumpSize = getSize();
		val targetOffset = getTargetOffset();

		 if (targetOffset >= memoryDumpSize)
		{
			throw new IllegalStateException("The target offset was "
					+ toHexadecimal(targetAddress)
					+ " but cannot be bigger than the memory dump file size "
					+ toHexadecimal(memoryDumpSize) + "!");
		} */

		this.byteOrder = byteOrder;
		this.fileType = MEMORY_DUMP;
	}

	public long getSize() throws IOException
	{
		return Files.size(filePath);
	}

	private long getTargetOffset()
	{
		if (startingAddress == null || targetAddress == null)
		{
			return 0;
		}

		return targetAddress - startingAddress;
	}

	public long getLastAddress() throws IOException
	{
		val address = startingAddress + getSize() - 1;
		return alignLong(address);
	}

	public TreeMap<Long, Long> readOffsetValuePairs(long maximumMemoryChunkSize,
	                                                int alignment,
	                                                List<MemoryRange> ignoredMemoryRanges,
	                                                int memoryDumpIndex,
	                                                int memoryDumpsCount,
	                                                long minimumPointerAddress,
	                                                int addressSize,
	                                                JProgressBar progressBar,
	                                                JButton searchPointersButton) throws Exception
	{
		LOGGER.log(INFO, "Reading offset/value pairs...");
		val benchmark = new Benchmark();
		benchmark.start();
		val copiedIgnoredMemoryRanges = MemoryRange.copy(ignoredMemoryRanges);
		val pointerMap = new TreeMap<Long, Long>();
		val filePath = getFilePath();
		val fileSize = getSize();
		val startingAddress = getStartingAddress();
		val lastAddress = getLastAddress();
		// Unsafe unsafe = FileReadingUtilities.getUnsafe();

		var startingOffset = 0L;
		while (startingOffset < fileSize)
		{
			val ceiledFileSize = min(fileSize - startingOffset, maximumMemoryChunkSize);
			val memoryDumpReaders = getByteBuffers(filePath.toString(), startingOffset, ceiledFileSize);

			var absoluteMemoryDumpOffset = 0;
			var memoryDumpReaderIndex = 0;
			val memoryDumpReadersCount = memoryDumpReaders.size();
			for (val memoryDumpReader : memoryDumpReaders)
			{
				if (searchPointersButton != null && memoryDumpReadersCount > 1)
				{
					val finalMemoryDumpReaderIndex = memoryDumpReaderIndex;
					invokeLater(() -> searchPointersButton.setText("Parsing"
							+ (memoryDumpIndex == 0 ? " first" : "") + " memory dump... (part "
							+ (finalMemoryDumpReaderIndex + 1) + "/" + memoryDumpReadersCount + ")"));
				}

				memoryDumpReader.order(byteOrder);
				// long address = ((DirectBuffer) memoryDumpReader).address();

				var currentPosition = 0;
				val memoryDumpSize = memoryDumpReader.limit();
				val statusUpdateStepSize = memoryDumpSize / MEMORY_DUMP_READING_UPDATE_PERCENTAGE_SIZE;
				invokeLater(() ->
				{
					if (progressBar != null)
					{
						progressBar.setValue(0);
						progressBar.setMaximum(memoryDumpSize);
					}
				});
				while (currentPosition + addressSize - 1 < memoryDumpSize)
				{
					if (currentPosition % statusUpdateStepSize == 0 && progressBar != null)
					{
						// val percentageCompleted = (currentPosition / (double) memoryDumpSize) * 100;
						val finalCurrentPosition = currentPosition;
						invokeLater(() -> progressBar.setValue(finalCurrentPosition));
						// System.out.println(percentageCompleted + "% completed");
					}

					val value = readValue(memoryDumpReader, addressSize);

					// int value = memoryDumpReader.getInt();
					// int value = unsafe.getInt(address + currentPosition);

					var isOffsetIgnored = false;

					if (value >= minimumPointerAddress
							&& value >= startingAddress
							&& value < lastAddress
							&& value % alignment == 0)
					{
						val address = new MemoryAddress(startingOffset + currentPosition, startingAddress);

						val relativeOffset = address.getRelativeOffset();
						if (copiedIgnoredMemoryRanges != null
								&& !copiedIgnoredMemoryRanges.isEmpty())
						{
							for (val memoryRange : copiedIgnoredMemoryRanges)
							{
								val ignoredStartingOffset = memoryRange.getStartingOffset();
								val ignoredEndOffset = memoryRange.getEndOffset();
								val absoluteAddress = address.getAbsoluteAddress();

								if (absoluteAddress >= ignoredStartingOffset && absoluteAddress <= ignoredEndOffset)
								{
									isOffsetIgnored = true;

									// Skip ahead the whole section
									currentPosition = (int) (ignoredEndOffset - startingAddress + alignment);

									break;
								}
							}

							// Remove dead entries for performance
							val memoryRange = copiedIgnoredMemoryRanges.get(0);
							if (relativeOffset >= memoryRange.getEndOffset())
							{
								copiedIgnoredMemoryRanges.remove(0);
							}
						}

						if (!isOffsetIgnored)
						{
							pointerMap.put(absoluteMemoryDumpOffset + relativeOffset, value);
						}
					}

				/*if (currentPosition % 10000 == 0)
				{
					System.out.println(currentPosition / (double) memoryDumpReader.limit() * 100 + " %");
				}*/

					if (!isOffsetIgnored)
					{
						considerAdjustingByteBufferPosition(memoryDumpReader, alignment, addressSize);
						currentPosition += alignment;
					}
				}

				if (progressBar != null)
				{
					invokeLater(() -> progressBar.setValue((int) maximumMemoryChunkSize));
				}

				startingOffset += ceiledFileSize;

			/*int finalStartingOffset = startingOffset;

			SwingUtilities.invokeLater(() ->
			{
				if(generalProgressBar != null)
				{
					int progress = finalStartingOffset * 100 / memoryDumpsCount;
					generalProgressBar.setValue(progress);
				}
			});*/

				absoluteMemoryDumpOffset += memoryDumpReader.limit();
				memoryDumpReaderIndex++;

				System.gc();
			}
		}

		val elapsedTime = benchmark.getElapsedTime();
		LOGGER.log(INFO, "Reading offset/value pairs took " + elapsedTime + " seconds");
		return pointerMap;
	}

	private long readValue(ByteBuffer memoryDumpReader, int addressSize)
	{
		switch (addressSize)
		{
			case Byte.BYTES:
				return toUnsignedLong(memoryDumpReader.get());

			case Short.BYTES:
				return toUnsignedLong(memoryDumpReader.getShort());

			case Integer.BYTES:
				return toUnsignedLong(memoryDumpReader.getInt());

			case Long.BYTES:
				return memoryDumpReader.getLong();

			default:
				throw new IllegalStateException("Unhandled address size: " + addressSize);
		}
	}

	private void considerAdjustingByteBufferPosition(Buffer buffer, int alignment, int addressSize)
	{
		if (alignment != addressSize)
		{
			// Set the byte buffer backwards depending on the alignment
			val currentPosition = buffer.position();
			val updatedPosition = currentPosition - addressSize + alignment;
			buffer.position(updatedPosition);
		}
	}

	@Override
	public String toString()
	{
		return filePath.toAbsolutePath()
				+ ": "
				+ toHexadecimal(targetAddress, Long.BYTES, true);
	}

	public Path getMemoryDumpFilePath()
	{
		return getCorrespondingFileWithExtension("." + MEMORY_DUMP.getExtension());
	}

	public Path getPointerMapFilePath()
	{
		return getCorrespondingFileWithExtension("." + POINTER_MAP.getExtension());
	}

	private Path getCorrespondingFileWithExtension(String extension)
	{
		val binaryFilePath = getFilePath();
		val filePathString = binaryFilePath.toString();
		val baseFileName = getBaseFileName(filePathString);
		val parentDirectory = binaryFilePath.getParent().toString();
		val pointerMapFilePath = parentDirectory + separator + baseFileName + extension;
		return Paths.get(pointerMapFilePath);
	}
}
