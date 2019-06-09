package com.wiiudev.gecko.pointer;

import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryDump;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryPointer;
import lombok.val;
import lombok.var;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class SingleMemoryDumpPointersFinder
{
	public static String toOutputString(ArrayList<ArrayList<MemoryPointer>> memoryPointerLists, List<MemoryDump> memoryDumps, boolean signedOffsets)
	{
		val stringBuilder = new StringBuilder();

		// Add memory dumps header information
		for (var memoryDumpsIndex = 0;
		     memoryDumpsIndex < memoryDumps.size();
		     memoryDumpsIndex++)
		{
			val memoryDump = memoryDumps.get(memoryDumpsIndex);
			val fileName = memoryDump.getFilePath().toFile().getName();
			stringBuilder.append(fileName);

			if (memoryDumpsIndex != memoryDumps.size() - 1)
			{
				stringBuilder.append(", ");
			}
		}

		stringBuilder.append(lineSeparator());

		// Add pointer lists
		for (var memoryPointerListsIndex = 0;
		     memoryPointerListsIndex < memoryPointerLists.size();
		     memoryPointerListsIndex++)
		{
			val memoryPointerList = memoryPointerLists.get(memoryPointerListsIndex);
			for (var memoryPointerIndex = 0;
			     memoryPointerIndex < memoryPointerList.size();
			     memoryPointerIndex++)
			{
				val memoryPointer = memoryPointerList.get(memoryPointerIndex);
				stringBuilder.append(memoryPointer.toString(signedOffsets, Long.BYTES));

				if (memoryPointerIndex != memoryPointerList.size() - 1)
				{
					stringBuilder.append(", ");
				}
			}

			if (memoryPointerListsIndex != memoryPointerLists.size() - 1)
			{
				stringBuilder.append(lineSeparator());
			}
		}

		return stringBuilder.toString();
	}

	private static final int BASE_MEMORY_DUMP_INDEX = 0;

	public static ArrayList<ArrayList<MemoryPointer>> findPotentialPointerLists(List<List<MemoryPointer>> memoryDumpPointerLists)
	{
		val results = new ArrayList<ArrayList<MemoryPointer>>();

		val memoryPointerList = memoryDumpPointerLists.get(BASE_MEMORY_DUMP_INDEX);
		val memoryPointerMap = setupMemoryPointerMap(memoryPointerList);

		for (var innerMemoryDumpListsIndex = 0;
		     innerMemoryDumpListsIndex < memoryDumpPointerLists.size();
		     innerMemoryDumpListsIndex++)
		{
			if (innerMemoryDumpListsIndex == BASE_MEMORY_DUMP_INDEX)
			{
				continue;
			}

			val memoryPointerPairs = new ArrayList<MemoryPointer>();

			val innerMemoryPointersList = memoryDumpPointerLists.get(innerMemoryDumpListsIndex);
			for (val innerMemoryPointer : innerMemoryPointersList)
			{
				val innerOffsets = getOffsetsList(innerMemoryPointer);
				val matchedIndex = memoryPointerMap.get(innerOffsets);

				if (matchedIndex == null)
				{
					continue;
				}

				val memoryPointer = memoryPointerList.get(matchedIndex.intValue());
				val baseAddress = memoryPointer.getBaseAddress();
				val innerBaseAddress = innerMemoryPointer.getBaseAddress();
				if (baseAddress == innerBaseAddress)
				{
					continue;
				}

				if (memoryPointerPairs.isEmpty())
				{
					memoryPointerPairs.add(memoryPointer);
				}

				memoryPointerPairs.add(innerMemoryPointer);
			}

			// Only add this if it matched at least once
			if (memoryPointerPairs.size() > 1)
			{
				results.add(memoryPointerPairs);
			}
		}

		return results;
	}

	private static HashMap<List<Long>, Long> setupMemoryPointerMap(List<MemoryPointer> memoryPointerList)
	{
		val memoryPointerMap = new HashMap<List<Long>, Long>();

		var memoryPointerIndex = 0L;
		for (val memoryPointer : memoryPointerList)
		{
			val offsets = getOffsetsList(memoryPointer);
			memoryPointerMap.put(offsets, memoryPointerIndex);
			memoryPointerIndex++;
		}

		return memoryPointerMap;
	}

	private static List<Long> getOffsetsList(MemoryPointer memoryPointer)
	{
		return stream(memoryPointer.getOffsets()).boxed().collect(toList());
	}
}
