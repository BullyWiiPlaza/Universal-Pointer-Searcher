package com.wiiudev.gecko.pointer.preprocessed_search.data_structures;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.var;

import java.util.Comparator;

import static java.lang.Math.abs;

@RequiredArgsConstructor
public enum MemoryPointerSorting
{
	ADDRESS("Address"),
	ABSOLUTE_OFFSETS("Absolute Offsets"),
	OFFSETS_COUNT("Offsets Count");

	private static final int EQUALS = 0;
	private static final int LESS_THAN = -1;
	private static final int GREATER_THAN = 1;

	public static Comparator<MemoryPointer> ABSOLUTE_OFFSETS_COMPARATOR = (firstMemoryPointer, secondMemoryPointer) ->
	{
		val firstMemoryPointerOffsets = firstMemoryPointer.getOffsets();
		val secondMemoryPointerOffsets = secondMemoryPointer.getOffsets();
		val comparisonResult = compare(firstMemoryPointerOffsets, secondMemoryPointerOffsets);

		if (comparisonResult != null)
		{
			return comparisonResult;
		}

		return EQUALS;
	};

	public static Comparator<MemoryPointer> ADDRESS_COMPARATOR = (firstMemoryPointer, secondMemoryPointer) ->
	{
		val baseAddressOrModuleOffset = firstMemoryPointer.getAddressOrModuleOffset();
		val offsets = firstMemoryPointer.getOffsets();

		if (secondMemoryPointer.getAddressOrModuleOffset() < baseAddressOrModuleOffset)
		{
			return GREATER_THAN;
		}

		if (secondMemoryPointer.getAddressOrModuleOffset() == baseAddressOrModuleOffset)
		{
			val result = compare(secondMemoryPointer.getOffsets(), offsets);
			if (result != null)
			{
				return result;
			}
		} else
		{
			return LESS_THAN;
		}

		return EQUALS;
	};

	public static Comparator<MemoryPointer> OFFSETS_COUNT_COMPARATOR = (firstMemoryPointer, secondMemoryPointer) ->
	{
		val firstMemoryPointerOffsets = firstMemoryPointer.getOffsets();
		val secondMemoryPointerOffsets = secondMemoryPointer.getOffsets();
		return Integer.compare(firstMemoryPointerOffsets.length, secondMemoryPointerOffsets.length);
	};

	private final String text;

	private static Integer compare(long[] firstOffsets, long[] secondOffsets)
	{
		val firstOffsetSum = getSum(firstOffsets);
		val secondOffsetSum = getSum(secondOffsets);

		if (firstOffsetSum < secondOffsetSum)
		{
			return LESS_THAN;
		} else if (firstOffsetSum > secondOffsetSum)
		{
			return GREATER_THAN;
		}

		return null;
	}

	private static int getSum(long[] offsets)
	{
		var sum = 0;

		for (val offset : offsets)
		{
			sum += abs(offset);
		}

		return sum;
	}

	public Comparator<MemoryPointer> getComparator()
	{
		switch (this)
		{
			case ADDRESS:
				return ADDRESS_COMPARATOR;

			case ABSOLUTE_OFFSETS:
				return ABSOLUTE_OFFSETS_COMPARATOR;

			case OFFSETS_COUNT:
				return OFFSETS_COUNT_COMPARATOR;

			default:
				throw new IllegalStateException("Unhandled comparator case");
		}
	}

	@Override
	public String toString()
	{
		return text;
	}
}
