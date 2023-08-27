package com.wiiudev.gecko.pointer.non_preprocessed_search.searcher;

import com.wiiudev.gecko.pointer.non_preprocessed_search.MemoryDump;
import com.wiiudev.gecko.pointer.non_preprocessed_search.pointer.MemoryPointer;
import com.wiiudev.gecko.pointer.non_preprocessed_search.pointer.PointerAddressRange;
import com.wiiudev.gecko.pointer.non_preprocessed_search.pointer.PointerOffsetChecker;
import com.wiiudev.gecko.pointer.non_preprocessed_search.reader.ByteBufferRange;
import com.wiiudev.gecko.pointer.non_preprocessed_search.reader.ByteBufferUtilities;
import lombok.val;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class UniversalPointerSearcher
{
	private boolean allowPointerInPointers;
	private long memoryDumpStartingOffset;
	private List<MemoryDump> memoryDumps;
	private PointerAddressRange pointerAddressRange;
	private final ByteOrder byteOrder;
	private PointerOffsetChecker pointerOffsetChecker;
	private final Set<MemoryPointer> memoryPointers;
	public static final int INTEGER_SIZE = 4;

	UniversalPointerSearcher()
	{
		pointerOffsetChecker = new PointerOffsetChecker();
		memoryDumps = new ArrayList<>();
		memoryPointers = new HashSet<>();
		allowPointerInPointers = false;

		memoryDumpStartingOffset = returnMemoryDumpStartingOffset();
		byteOrder = returnByteOrder();
	}

	public abstract ByteOrder returnByteOrder();

	public abstract long returnMemoryDumpStartingOffset();

	public void setPointerOffsetChecker(PointerOffsetChecker pointerOffsetChecker)
	{
		this.pointerOffsetChecker = pointerOffsetChecker;
	}

	public void setMemoryDumpStartingOffset(int memoryDumpStartingOffset)
	{
		this.memoryDumpStartingOffset = memoryDumpStartingOffset;
	}

	public List<MemoryPointer> getMemoryPointers()
	{
		return getMemoryPointers(memoryPointers);
	}

	private List<MemoryPointer> getMemoryPointers(Set<MemoryPointer> memoryPointers)
	{
		List<MemoryPointer> memoryPointersList = new ArrayList<>(memoryPointers);
		Collections.sort(memoryPointersList);

		return memoryPointersList;
	}

	public void setAllowPointerInPointers(boolean allowPointerInPointers)
	{
		this.allowPointerInPointers = allowPointerInPointers;
	}

	public void searchPointers() throws Exception
	{
		memoryPointers.clear();
		MemoryDump memoryDump = memoryDumps.get(0);
		setPointerAddressRange(memoryDump);

		// Use a modified amount of threads because it works best
		Runtime runtime = Runtime.getRuntime();
		int threadsCount = runtime.availableProcessors() * 2;

		ByteBufferRange[] byteBufferRanges = getByteBufferRanges(memoryDump, threadsCount);

		ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);
		ExecutorCompletionService<String> completionService = new ExecutorCompletionService<>(executorService);

		long targetAddress = memoryDump.getTargetAddress();

		// Process each range concurrently
		for (ByteBufferRange byteBufferRange : byteBufferRanges)
		{
			Callable<String> searchTask = () ->
			{
				searchPointers(targetAddress, byteBufferRange);

				return null;
			};

			// Start the search
			completionService.submit(searchTask);
		}

		// Wait for all tasks to finish
		for (@SuppressWarnings("UnusedAssignment") val ignored : byteBufferRanges)
		{
			completionService.take().get();
		}

		// Shutdown the thread pool
		executorService.shutdown();
	}

	private void setPointerAddressRange(MemoryDump memoryDump)
	{
		long fileLength = memoryDump.getBinaryFilePath().toFile().length();
		pointerAddressRange = new PointerAddressRange(memoryDumpStartingOffset, memoryDumpStartingOffset + fileLength);
	}

	private ByteBufferRange[] getByteBufferRanges(MemoryDump memoryDump, int threadsCount) throws IOException
	{
		// Utilize multi-threading smarter since some memory blocks might be sparse so the thread would be done quickly
		int chunksCount = threadsCount * 10;

		ByteBufferRange[] byteBufferRanges = new ByteBufferRange[chunksCount];
		int totalBytes = (int) memoryDump.getBytesCount();

		// Force an aligned chunk size
		int chunkSize = totalBytes / chunksCount & (-1) * INTEGER_SIZE;
		int startingOffset = 0;

		for (int byteBuffersIndex = 0;
		     byteBuffersIndex < byteBufferRanges.length;
		     byteBuffersIndex++)
		{
			int offsetLimit = chunkSize * (byteBuffersIndex + 1);

			if(byteBuffersIndex == byteBufferRanges.length - 1)
			{
				offsetLimit = totalBytes;
			}

			ByteBuffer byteBuffer = memoryDump.getFreshByteBuffer();
			byteBuffer.order(byteOrder);
			ByteBufferRange byteBufferRange = new ByteBufferRange(startingOffset, offsetLimit, byteBuffer);
			byteBufferRanges[byteBuffersIndex] = byteBufferRange;
			startingOffset += chunkSize;

			if(byteBuffersIndex == 0)
			{
				startingOffset += 4;
			}
		}

		return byteBufferRanges;
	}

	private void searchPointers(long targetAddress,
	                            ByteBufferRange byteBufferRange) throws IOException
	{
		ByteBuffer byteBuffer = byteBufferRange.getByteBuffer();

		while (ByteBufferUtilities.isIntegerRemaining(byteBufferRange))
		{
			long relativeBaseAddress = byteBuffer.position();
			long absoluteBaseAddress = relativeBaseAddress + memoryDumpStartingOffset;

			long baseAddressValue = byteBuffer.getInt();
			boolean isPointerValueValid = isValidPointerValue(baseAddressValue);

			if (isPointerValueValid)
			{
				checkForPointerDepth1(absoluteBaseAddress, baseAddressValue, targetAddress);

				if (allowPointerInPointers)
				{
					checkForPointerDepth2(byteBuffer, targetAddress, absoluteBaseAddress, baseAddressValue);
				}
			}
		}
	}

	private void checkForPointerDepth1(long absoluteBaseAddress, long baseAddressValue, long targetAddress)
	{
		int[] pointerOffsets = getPointerOffsets(baseAddressValue, targetAddress);
		validatePointer(absoluteBaseAddress, pointerOffsets);
	}

	private int[] getPointerOffsets(long addressValue, long targetAddress)
	{
		int pointerOffset = (int) (targetAddress - addressValue);
		return new int[]{pointerOffset};
	}

	private int[] getPointerOffsets(long addressValue, long targetAddress, int innerPointerOffset)
	{
		return new int[]{innerPointerOffset, getPointerOffsets(addressValue, targetAddress)[0]};
	}

	private void checkForPointerDepth2(ByteBuffer memoryDumpReader, long targetAddress, long absoluteBaseAddress, long baseAddressValue)
	{
		// Backup the buffer position since we're possibly changing it
		int positionBackup = memoryDumpReader.position();

		int innerPointerOffset = getInnerPointerStartingOffset();
		ByteBufferUtilities.setDecreasedBuffer(memoryDumpReader, baseAddressValue, innerPointerOffset, memoryDumpStartingOffset);

		for (; innerPointerOffset < pointerOffsetChecker.getMaximumOffset(); innerPointerOffset += INTEGER_SIZE)
		{
			if (!ByteBufferUtilities.isIntegerRemaining(memoryDumpReader))
			{
				break;
			}

			long nextPointerAddressValue = memoryDumpReader.getInt();

			if (isValidPointerValue(nextPointerAddressValue))
			{
				int[] pointerOffsets = getPointerOffsets(nextPointerAddressValue, targetAddress, innerPointerOffset);
				validatePointer(absoluteBaseAddress, pointerOffsets);
			}
		}

		int newPosition = positionBackup + INTEGER_SIZE;

		if (ByteBufferUtilities.isIntegerRemaining(memoryDumpReader, newPosition))
		{
			// Restore the buffer position
			memoryDumpReader.position(newPosition);
		}
	}

	private void validatePointer(long baseAddress, int[] pointerOffsets)
	{
		int lastPointerOffset = pointerOffsets[pointerOffsets.length - 1];

		if (pointerOffsetChecker.fulfillsSettings(lastPointerOffset))
		{
			MemoryPointer memoryPointer = new MemoryPointer(baseAddress, pointerOffsets);

			if (memoryPointer.supportsAllMemoryDumps(memoryDumps, memoryDumpStartingOffset))
			{
				memoryPointers.add(memoryPointer);
			}
		}
	}

	private int getInnerPointerStartingOffset()
	{
		int innerPointerStartingOffset = -1 * pointerOffsetChecker.getMaximumOffset();

		if (innerPointerStartingOffset < 0 && pointerOffsetChecker.allowsPositive())
		{
			innerPointerStartingOffset = 0;
		}

		return innerPointerStartingOffset;
	}

	private boolean isValidPointerValue(long baseAddressValue)
	{
		return pointerAddressRange.contains(baseAddressValue);
	}

	void setMemoryDumps(List<MemoryDump> memoryDumps)
	{
		this.memoryDumps = memoryDumps;
	}
}
