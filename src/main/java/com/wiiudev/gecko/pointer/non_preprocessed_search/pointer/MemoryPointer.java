package com.wiiudev.gecko.pointer.non_preprocessed_search.pointer;

import com.wiiudev.gecko.pointer.non_preprocessed_search.MemoryDump;
import lombok.val;

import java.util.Arrays;
import java.util.List;

public class MemoryPointer implements Comparable<MemoryPointer>
{
	private long baseAddress;
	private int[] offsets;

	public MemoryPointer(long baseAddress, int[] offsets)
	{
		this.baseAddress = baseAddress;
		this.offsets = offsets;
	}

	public boolean supportsAllMemoryDumps(List<MemoryDump> memoryDumps, long memoryDumpStartingOffset)
	{
		for (var memoryDumpIndex = 1; memoryDumpIndex < memoryDumps.size(); memoryDumpIndex++)
		{
			val memoryDump = memoryDumps.get(memoryDumpIndex);
			val isValidPointer = reachesTargetAddress(memoryDump, memoryDumpStartingOffset);

			if (!isValidPointer)
			{
				return false;
			}
		}

		return true;
	}

	private long getBaseAddress()
	{
		return baseAddress;
	}

	private int[] getOffsets()
	{
		return offsets;
	}

	private boolean reachesTargetAddress(MemoryDump memoryDump,
	                                     long memoryDumpStartingOffset)
	{
		val memoryDumpReader = memoryDump.getByteBuffer();

		val relativeBaseAddress = baseAddress - memoryDumpStartingOffset;

		memoryDumpReader.position((int) relativeBaseAddress);
		var baseAddressValue = memoryDumpReader.getInt();
		var pointerDestination = -1;

		for (val pointerOffset : offsets)
		{
			pointerDestination = baseAddressValue + pointerOffset;
			val relativeOffset = pointerDestination - memoryDumpStartingOffset;

			// Bail out for invalid pointers
			if (relativeOffset < 0
					|| relativeOffset > memoryDumpReader.limit() - 4)
			{
				return false;
			}

			memoryDumpReader.position((int) relativeOffset);
			baseAddressValue = memoryDumpReader.getInt();
		}

		return pointerDestination == memoryDump.getTargetAddress();
	}

	@Override
	public String toString()
	{
		return toString(true);
	}

	public String toString(boolean signedOffsets)
	{
		val pointerBuilder = new StringBuilder();

		for (val ignored : offsets)
		{
			pointerBuilder.append("[");
		}

		pointerBuilder.append("0x");
		pointerBuilder.append(Long.toHexString(baseAddress).toUpperCase());
		pointerBuilder.append("] ");

		for (var offsetsIndex = 0; offsetsIndex < offsets.length; offsetsIndex++)
		{
			var offset = offsets[offsetsIndex];
			val isNegative = offset < 0;

			if (isNegative && signedOffsets)
			{
				val integerMaxValue = Integer.MAX_VALUE + Math.abs(Integer.MIN_VALUE);
				offset = integerMaxValue - offset + 1;
				pointerBuilder.append("-");
			} else
			{
				pointerBuilder.append("+");
			}

			pointerBuilder.append(" 0x");
			pointerBuilder.append(Integer.toHexString(offset).toUpperCase());

			if (offsetsIndex != offsets.length - 1)
			{
				pointerBuilder.append("] ");
			}
		}

		return pointerBuilder.toString();
	}

	public static String toString(List<MemoryPointer> memoryPointers, boolean signedOffsets)
	{
		val foundPointers = new StringBuilder();

		for (val memoryPointer : memoryPointers)
		{
			foundPointers.append(memoryPointer.toString(signedOffsets));
			foundPointers.append(System.lineSeparator());
		}

		return foundPointers.toString().trim();
	}

	@Override
	public boolean equals(Object object)
	{
		if (!(object instanceof MemoryPointer))
		{
			return false;
		}

		val memoryPointer = (MemoryPointer) object;

		return memoryPointer.getBaseAddress() == baseAddress && Arrays.equals(memoryPointer.getOffsets(), offsets);
	}

	@Override
	public int hashCode()
	{
		return Long.valueOf(baseAddress).hashCode() + Arrays.hashCode(offsets);
	}

	@Override
	public int compareTo(MemoryPointer memoryPointer)
	{
		val baseAddress = memoryPointer.getBaseAddress();
		val offsets = memoryPointer.getOffsets();

		val EQUALS = 0;
		val LESS_THAN = -1;
		val GREATER_THAN = 1;

		if (this.baseAddress < baseAddress)
		{
			return LESS_THAN;
		} else if (this.baseAddress == baseAddress)
		{
			for (var offsetIndex = 0; offsetIndex < offsets.length; offsetIndex++)
			{
				val offset = offsets[offsetIndex];
				val thisOffset = this.offsets[offsetIndex];

				if (thisOffset < offset)
				{
					return LESS_THAN;
				} else if (thisOffset > offset)
				{
					return GREATER_THAN;
				}
			}
		} else
		{
			return GREATER_THAN;
		}

		return EQUALS;
	}
}
