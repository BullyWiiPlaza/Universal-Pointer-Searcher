import com.wiiudev.gecko.pointer.preprocessed_search.MemoryPointerSearcher;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryAddress;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryDump;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryPointer;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryRange;
import com.wiiudev.gecko.pointer.utilities.Benchmark;
import lombok.val;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.wiiudev.gecko.pointer.preprocessed_search.EndianConverter.DUMPS_NO_TRACK_MUSIC_39_CEB148_BIN;
import static com.wiiudev.gecko.pointer.preprocessed_search.Platform.WII_U;
import static java.lang.Math.abs;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static org.junit.Assert.fail;

public class PointerSearchTest
{
	private static MemoryPointerSearcher memoryPointerSearcher = new MemoryPointerSearcher();
	private static MemoryRange baseAddressRange = new MemoryRange(0x39520BA0, 0x39CDDB0C);
	private static List<MemoryRange> baseAddressMemoryRanges = new ArrayList<>();
	private static MemoryRange ignoredAddressRange = new MemoryRange(0x39520BA0, 0x39530000);
	private static List<MemoryRange> ignoredMemoryRanges = new ArrayList<>();
	private static final int MAXIMUM_POINTER_OFFSET = 0x400;
	private static final int POINTER_SEARCH_DEPTH = 3;
	private static final int MEMORY_POINTER_ALIGNMENT = 4;
	private static List<Map<Long, Long>> pointerMaps;

	private static List<MemoryPointer> positiveMemoryPointers;
	private static List<MemoryPointer> positiveMemoryPointersWithMisalignment;
	private static List<MemoryPointer> negativeMemoryPointers;

	@BeforeClass
	public static void initialize() throws Exception
	{
		addMemoryDumps();
		configurePointerSearch();
		parseMemoryDumps();

		positiveMemoryPointers = getMemoryPointers();
		memoryPointerSearcher.setAllowNegativeOffsets(true);
		negativeMemoryPointers = getMemoryPointers();
		memoryPointerSearcher.setPointerValueAlignment(2);
		memoryPointerSearcher.setAllowNegativeOffsets(false);
		val memoryPointerList = memoryPointerSearcher.getMemoryPointerList();
		positiveMemoryPointersWithMisalignment = memoryPointerList.getMemoryPointers();
	}

	@Test
	public void assertIgnoredAddressRange()
	{
		for (val pointerMap : pointerMaps)
		{
			for (val entry : pointerMap.entrySet())
			{
				val offset = entry.getKey();

				for (val memoryRange : ignoredMemoryRanges)
				{
					if (memoryRange.contains(offset))
					{
						fail("Offset " + new MemoryAddress(offset) + " was supposed to be ignored");
					}
				}
			}
		}
	}

	@Test
	public void maximumPointerOffsetRespected()
	{
		assertMaximumPointerOffsetRespected(positiveMemoryPointers);
		assertMaximumPointerOffsetRespected(negativeMemoryPointers);
	}

	private void assertMaximumPointerOffsetRespected(List<MemoryPointer> memoryPointers)
	{
		for (val memoryPointer : memoryPointers)
		{
			val offsets = memoryPointer.getOffsets();
			for (val offset : offsets)
			{
				if (abs(offset) > MAXIMUM_POINTER_OFFSET)
				{
					fail("Maximum pointer offset exceeded: " + new MemoryAddress(offset));
				}
			}
		}
	}

	@Test
	public void assertNegativeContainsPositiveOffsets()
	{
		assertContains(positiveMemoryPointers, negativeMemoryPointers);
	}

	@Test
	public void assertPositiveOffsets()
	{
		assertPositiveOffsets(positiveMemoryPointers);
	}

	@Test
	public void assertCorrectAlignment()
	{
		assertCorrectAlignment(positiveMemoryPointers, MEMORY_POINTER_ALIGNMENT);
		assertCorrectAlignment(positiveMemoryPointersWithMisalignment, 2);
		assertCorrectAlignment(negativeMemoryPointers, MEMORY_POINTER_ALIGNMENT);
	}

	private void assertCorrectAlignment(List<MemoryPointer> memoryPointers, int memoryPointerAlignment)
	{
		for (val memoryPointer : memoryPointers)
		{
			val offsets = memoryPointer.getOffsets();
			for (val offset : offsets)
			{
				if (offset % memoryPointerAlignment != 0)
				{
					val message = "Memory pointer has incorrect alignment: " + memoryPointer;
					throw new IllegalStateException(message);
				}
			}
		}
	}

	@Test
	public void assertCorrectPointerDepth()
	{
		assertCorrectPointerDepth(negativeMemoryPointers);
		assertCorrectPointerDepth(positiveMemoryPointers);
	}

	private void assertCorrectPointerDepth(List<MemoryPointer> memoryPointers)
	{
		for (val memoryPointer : memoryPointers)
		{
			val offsets = memoryPointer.getOffsets();
			if (offsets.length > POINTER_SEARCH_DEPTH)
			{
				val message = "Memory pointer had bigger depth than expected: " + memoryPointer;
				throw new IllegalStateException(message);
			}
		}
	}

	@Test
	public void assertBaseAddressRangeRespected()
	{
		assertBaseAddressRangeRespected(positiveMemoryPointers);
		assertBaseAddressRangeRespected(negativeMemoryPointers);
	}

	private void assertBaseAddressRangeRespected(List<MemoryPointer> memoryPointers)
	{
		for (val memoryPointer : memoryPointers)
		{
			val baseAddress = new MemoryAddress(memoryPointer.getBaseAddress());
			var isContained = false;

			for (val memoryRange : baseAddressMemoryRanges)
			{
				if (memoryRange.contains(baseAddress.getAbsoluteAddress()))
				{
					isContained = true;
					break;
				}
			}

			if (!isContained)
			{
				fail("Base address " + baseAddress + " is out of the allowed base address range");
			}
		}
	}

	private void assertContains(List<MemoryPointer> positiveMemoryPointers,
	                            List<MemoryPointer> negativeMemoryPointers)
	{
		for (val memoryPointer : positiveMemoryPointers)
		{
			if (!negativeMemoryPointers.contains(memoryPointer))
			{
				fail("Positive memory pointer " + memoryPointer
						+ " not contained in allowed negative memory pointers");
			}
		}
	}

	private void assertPositiveOffsets(List<MemoryPointer> memoryPointers)
	{
		for (val memoryPointer : memoryPointers)
		{
			val offsets = memoryPointer.getOffsets();
			for (var offset : offsets)
			{
				if (offset < 0)
				{
					val memoryAddress = new MemoryAddress(offset);
					fail("Negative offset: " + memoryAddress);
				}
			}
		}
	}

	private static void parseMemoryDumps() throws Exception
	{
		val parsingBenchmark = new Benchmark();
		parsingBenchmark.start();
		memoryPointerSearcher.parseMemoryDumps();
		pointerMaps = memoryPointerSearcher.getPointerMaps();
		System.out.println("Parsing took " + parsingBenchmark.getElapsedTime() + " seconds...");
	}

	private static List<MemoryPointer> getMemoryPointers() throws Exception
	{
		val searchingBenchmark = new Benchmark();
		searchingBenchmark.start();
		val positiveMemoryPointers = memoryPointerSearcher.searchPointers();
		System.out.println("Searching took " + searchingBenchmark.getElapsedTime() + " seconds...");
		memoryPointerSearcher.printMemoryPointers(positiveMemoryPointers);
		System.out.println(positiveMemoryPointers.size() + " pointer(s) found!");

		return positiveMemoryPointers;
	}

	private static void configurePointerSearch()
	{
		memoryPointerSearcher.setMaximumMemoryChunkSize(1_000_000_000);
		memoryPointerSearcher.setPointerValueAlignment(MEMORY_POINTER_ALIGNMENT);
		memoryPointerSearcher.setMaximumPointerOffset(MAXIMUM_POINTER_OFFSET);
		memoryPointerSearcher.setAllowNegativeOffsets(false);
		memoryPointerSearcher.setPrintSignedOffsets(true);
		memoryPointerSearcher.setPointerSearchDepth(POINTER_SEARCH_DEPTH);
		baseAddressMemoryRanges.add(baseAddressRange);
		memoryPointerSearcher.setBaseAddressRanges(baseAddressMemoryRanges);
		ignoredMemoryRanges.add(ignoredAddressRange);
		memoryPointerSearcher.setIgnoredMemoryRanges(ignoredMemoryRanges);
		memoryPointerSearcher.setMinimumPointerAddress(WII_U.getDefaultStartingOffset());
		memoryPointerSearcher.setAddressSize((byte) Integer.BYTES);
	}

	private static void addMemoryDumps()
	{
		val startingOffset = 0x30000000L;

		val firstMemoryDump = new MemoryDump(DUMPS_NO_TRACK_MUSIC_39_CEB148_BIN,
				startingOffset, 0x39CEB148L, BIG_ENDIAN);
		memoryPointerSearcher.addMemoryDump(firstMemoryDump);

		/*MemoryDump secondMemoryDump = new MemoryDump("dumps/No Track Music/39CF14D8.bin", startingOffset, 0x39CF14D8, ByteOrder.BIG_ENDIAN);
		memoryPointerSearcher.addMemoryDump(secondMemoryDump);*/
	}
}
