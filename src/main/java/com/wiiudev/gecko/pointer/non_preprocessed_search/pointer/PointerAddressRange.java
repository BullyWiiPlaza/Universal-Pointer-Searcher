package com.wiiudev.gecko.pointer.non_preprocessed_search.pointer;

import com.wiiudev.gecko.pointer.non_preprocessed_search.MemoryDump;
import lombok.val;

public class PointerAddressRange
{
	private long startingAddress;
	private long endAddress;

	public PointerAddressRange(long startingAddress, long endAddress)
	{
		this.startingAddress = startingAddress;
		this.endAddress = endAddress;
	}

	public PointerAddressRange(long startingAddress, MemoryDump memoryDump)
	{
		this(startingAddress, getMemoryDumpEndOffset(startingAddress, memoryDump));
	}

	private static long getMemoryDumpEndOffset(long startingAddress, MemoryDump memoryDump)
	{
		val fileLength = memoryDump.getBytesCount();

		return startingAddress + fileLength;
	}

	public boolean contains(long memoryValue)
	{
		return (startingAddress <= memoryValue) && (memoryValue <= endAddress);
	}
}
