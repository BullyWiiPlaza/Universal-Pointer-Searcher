package com.wiiudev.gecko.pointer.non_preprocessed_search.searcher;

import com.wiiudev.gecko.pointer.non_preprocessed_search.MemoryDump;
import com.wiiudev.gecko.pointer.non_preprocessed_search.pointer.PointerOffsetChecker;
import com.wiiudev.gecko.pointer.utilities.Benchmark;
import lombok.val;

public class PointerSearcherCommandLine
{

	public static void main(String[] arguments) throws Exception
	{
		/*Benchmark benchmark = new Benchmark();
		benchmark.start();

		List<MemoryDump> memoryDumps = MemoryDump.getMemoryDumps("dumps\\Anti Gravity");
		UpdatedPointerSearcher updatedPointerSearcher = new UpdatedWiiUPointerSearch(memoryDumps);
		updatedPointerSearcher.setMemoryDumpStartingOffset(0x44000000);
		updatedPointerSearcher.setAllowPointerInPointers(true);
		PointerOffsetChecker pointerOffsetChecker = new PointerOffsetChecker();
		pointerOffsetChecker.setAllowNegative(true);
		pointerOffsetChecker.setMaximumOffset(0x500);
		updatedPointerSearcher.setPointerOffsetChecker(pointerOffsetChecker);
		updatedPointerSearcher.performPointerSearch();

		double elapsedTime = benchmark.getElapsedTime();
		System.out.println(elapsedTime + " seconds");

		Set<PossiblePointer> possiblePointers = new HashSet<>();
		possiblePointers.add(new PossiblePointer(1, 0));
		possiblePointers.add(new PossiblePointer(2, 1));
		System.out.println(possiblePointers.contains(new PossiblePointer(1, -1)));
		System.out.println(possiblePointers.);

		System.exit(0);*/

		for (var i = 1; i < 30; i++)
		{
			val benchmark = new Benchmark();
			benchmark.start();
			val universalPointerSearcher = new WiiUPointerSearch();
			universalPointerSearcher.setMemoryDumpStartingOffset(0x10000000);
			// universalPointerSearcher.setMemoryDumpLength(0x10000000);
			val memoryDumps = MemoryDump.getMemoryDumps("dumps//Ink");
			universalPointerSearcher.setMemoryDumps(memoryDumps);
			val pointerOffsetChecker = new PointerOffsetChecker();
			pointerOffsetChecker.setAllowNegative(true);
			pointerOffsetChecker.setMaximumOffset(0x10000);
			universalPointerSearcher.setPointerOffsetChecker(pointerOffsetChecker);
			universalPointerSearcher.setAllowPointerInPointers(false);
			universalPointerSearcher.searchPointers();
			val memoryPointers = universalPointerSearcher.getMemoryPointers();
			// System.out.println(MemoryPointer.toString(memoryPointers, true));
			val elapsedTime = benchmark.getElapsedTime();
			System.out.println(i + ": " + memoryPointers.size() + " pointers found in " + elapsedTime + " seconds");
		}
	}
}
