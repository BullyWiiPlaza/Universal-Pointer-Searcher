package com.wiiudev.gecko.pointer.preprocessed_search.data_structures;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class MemoryRange
{
	@Getter
	private final long startingOffset;

	@Getter
	private final long endOffset;

	public boolean contains(long offset)
	{
		return (offset >= startingOffset) && (offset <= endOffset);
	}

	static List<MemoryRange> copy(List<MemoryRange> memoryRanges)
	{
		if (memoryRanges == null)
		{
			return null;
		}

		val copiedMemoryRanges = new ArrayList<MemoryRange>();

		for (val memoryRange : memoryRanges)
		{
			val startingOffset = memoryRange.getStartingOffset();
			val endOffset = memoryRange.getEndOffset();
			val newMemoryRange = new MemoryRange(startingOffset, endOffset);
			copiedMemoryRanges.add(newMemoryRange);
		}

		return copiedMemoryRanges;
	}
}
