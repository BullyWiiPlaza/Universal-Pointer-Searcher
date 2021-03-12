import com.wiiudev.gecko.pointer.NativePointerSearcherManager;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryDump;
import lombok.AllArgsConstructor;
import lombok.val;
import lombok.var;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static com.google.common.base.Stopwatch.createStarted;
import static com.wiiudev.gecko.pointer.NativePointerSearcherManager.findPointers;
import static java.lang.Math.pow;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.*;

public class NativePointerSearcherTest
{
	/* @Test
	public void runTests()
	{
		Class[] classes = {NativePointerSearcherTest.class};
		val parallelComputer = new ParallelComputer(false, true);
		runClasses(parallelComputer, classes);
	} */

	@Ignore
	public void testOptimalThreadCount() throws Exception
	{
		val availableProcessorsCount = getAvailableProcessorsCount();
		val maximumThreadCount = availableProcessorsCount * 2;
		for (var threadCount = 1; threadCount <= maximumThreadCount; threadCount++)
		{
			val stopWatch = createStarted();

			val nativePointerSearcherManager = new NativePointerSearcherManager();
			nativePointerSearcherManager.setThreadCount(threadCount);
			nativePointerSearcherManager.setExcludeCycles(true);
			nativePointerSearcherManager.setMinimumPointerDepth(1);
			nativePointerSearcherManager.setMaximumPointerDepth(3);
			nativePointerSearcherManager.setMaximumMemoryDumpChunkSize(100_000_000);
			nativePointerSearcherManager.setSaveAdditionalMemoryDumpRAM(false);
			nativePointerSearcherManager.setPotentialPointerOffsetsCountPerAddressPrediction(40);
			val maximumPointersCount = 100_000;
			nativePointerSearcherManager.setMaximumPointersCount(maximumPointersCount);
			nativePointerSearcherManager.setPointerOffsetRange(0, 10_000);
			nativePointerSearcherManager.setLastPointerOffsets(emptyList());
			val firstMemoryDump = new MemoryDump("D:\\Cpp\\PointerSearcher\\card_ids",
					0x0L, 0x2D574C28020L, LITTLE_ENDIAN);
			val addressSize = 8;
			firstMemoryDump.setAddressSize(addressSize);
			firstMemoryDump.setAddressAlignment(8);
			val valueAlignment = 8;
			firstMemoryDump.setValueAlignment(valueAlignment);
			firstMemoryDump.setMinimumPointerAddress(0x0);
			firstMemoryDump.setMaximumPointerAddress(0x7FFFFFFFFFFL);
			firstMemoryDump.setFileExtensions(asList("bin", "dmp"));
			firstMemoryDump.setGeneratePointerMap(true);
			firstMemoryDump.setReadPointerMap(false);
			nativePointerSearcherManager.addMemoryDump(firstMemoryDump);
			nativePointerSearcherManager.addMemoryDump(firstMemoryDump);
			findPointers(nativePointerSearcherManager, addressSize, false);

			System.out.print("Pointer search with ");

			if (threadCount < 10)
			{
				System.out.print(" ");
			}

			System.out.println(threadCount + " thread(s) took " + stopWatch);
			System.gc();
		}
	}

	@Test
	public void testPSVitaPointerSearch() throws Exception
	{
		val nativePointerSearcherManager = new NativePointerSearcherManager();
		val availableProcessorsCount = getAvailableProcessorsCount();
		nativePointerSearcherManager.setThreadCount(availableProcessorsCount * 2L);
		nativePointerSearcherManager.setExcludeCycles(true);
		nativePointerSearcherManager.setMinimumPointerDepth(1);
		nativePointerSearcherManager.setMaximumPointerDepth(3);
		nativePointerSearcherManager.setMaximumMemoryDumpChunkSize(100_000_000);
		nativePointerSearcherManager.setSaveAdditionalMemoryDumpRAM(false);
		nativePointerSearcherManager.setPotentialPointerOffsetsCountPerAddressPrediction(40);
		val maximumPointersCount = 100_000;
		nativePointerSearcherManager.setMaximumPointersCount(maximumPointersCount);
		nativePointerSearcherManager.setPointerOffsetRange(-400, 400);
		nativePointerSearcherManager.setLastPointerOffsets(emptyList());
		val memoryDumpsBaseDirectory = "D:\\Programs\\Source Codes\\Java\\IntelliJ\\Universal-Pointer-Searcher\\dumps\\Epic Mickey (PS Vita)";
		val firstMemoryDump = new MemoryDump(memoryDumpsBaseDirectory + "\\PCSF00309_0x81000000_0x87000000_0.bin",
				0x81000000L, 0x86BA4A9CL, LITTLE_ENDIAN);
		val addressSize = 4;
		firstMemoryDump.setAddressSize(addressSize);
		firstMemoryDump.setAddressAlignment(4);
		val valueAlignment = 4;
		firstMemoryDump.setValueAlignment(valueAlignment);
		firstMemoryDump.setMinimumPointerAddress(0x81000000L);
		firstMemoryDump.setMaximumPointerAddress(0x86FFFFFCL);
		firstMemoryDump.setFileExtensions(asList("bin", "dmp"));
		firstMemoryDump.setGeneratePointerMap(true);
		firstMemoryDump.setReadPointerMap(false);
		nativePointerSearcherManager.addMemoryDump(firstMemoryDump);

		val secondMemoryDump = new MemoryDump(memoryDumpsBaseDirectory + "\\PCSF00309_0x81000000_0x87000000_1.bin",
				0x81000000L, 0x86BA479CL, LITTLE_ENDIAN);
		secondMemoryDump.setAddressSize(addressSize);
		secondMemoryDump.setAddressAlignment(4);
		secondMemoryDump.setValueAlignment(valueAlignment);
		secondMemoryDump.setMinimumPointerAddress(0x81000000L);
		secondMemoryDump.setMaximumPointerAddress(0x86FFFFFCL);
		secondMemoryDump.setFileExtensions(asList("bin", "dmp"));
		secondMemoryDump.setGeneratePointerMap(true);
		secondMemoryDump.setReadPointerMap(false);
		nativePointerSearcherManager.addMemoryDump(secondMemoryDump);

		val firstMemoryDumpPointers = findPointers(nativePointerSearcherManager, addressSize, true);
		assertFalse(firstMemoryDumpPointers.isEmpty());

		/* nativePointerSearcherManager.setPointerOffsetRange(0, 0x400);
		val secondMemoryDumpPointers = findPointers(nativePointerSearcherManager, addressSize, true);
		assertTrue(firstMemoryDumpPointers.size() >= secondMemoryDumpPointers.size());

		nativePointerSearcherManager.setPointerOffsetRange(0, 0x2000);
		val thirdMemoryDumpPointers = findPointers(nativePointerSearcherManager, addressSize, true);
		assertTrue(thirdMemoryDumpPointers.size() >= secondMemoryDumpPointers.size());

		nativePointerSearcherManager.setPointerOffsetRange(0, 0x4000);
		val forthMemoryDumpPointers = findPointers(nativePointerSearcherManager, addressSize, true);
		assertTrue(forthMemoryDumpPointers.size() >= thirdMemoryDumpPointers.size()); */
	}

	@Test
	public void testNintendoSwitchMonsterJamPointerSearch() throws Exception
	{
		val nativePointerSearcherManager = new NativePointerSearcherManager();
		val availableProcessorsCount = getAvailableProcessorsCount();
		nativePointerSearcherManager.setThreadCount(availableProcessorsCount * 2L);
		nativePointerSearcherManager.setExcludeCycles(true);
		nativePointerSearcherManager.setMinimumPointerDepth(1);
		nativePointerSearcherManager.setMaximumPointerDepth(2);
		nativePointerSearcherManager.setMaximumMemoryDumpChunkSize(100_000_000);
		nativePointerSearcherManager.setSaveAdditionalMemoryDumpRAM(false);
		nativePointerSearcherManager.setPotentialPointerOffsetsCountPerAddressPrediction(40);
		val maximumPointersCount = 100_000;
		nativePointerSearcherManager.setMaximumPointersCount(maximumPointersCount);
		nativePointerSearcherManager.setPointerOffsetRange(0, 500);
		nativePointerSearcherManager.setLastPointerOffsets(emptyList());
		val memoryDumpsBaseDirectory = "D:\\Programs\\Source Codes\\Java\\IntelliJ\\Universal-Pointer-Searcher\\dumps\\Monster Jam [Switch]";
		val firstMemoryDump = new MemoryDump(memoryDumpsBaseDirectory + "\\modules1",
				0x0L, 0x41614B46F8L, LITTLE_ENDIAN);
		val addressSize = 8;
		firstMemoryDump.setAddressSize(addressSize);
		firstMemoryDump.setAddressAlignment(8);
		val valueAlignment = 8;
		firstMemoryDump.setValueAlignment(valueAlignment);
		firstMemoryDump.setMinimumPointerAddress(0x0);
		firstMemoryDump.setMaximumPointerAddress(0x7FFFFFFFFFFL);
		firstMemoryDump.setFileExtensions(asList("bin", "dmp"));
		firstMemoryDump.setGeneratePointerMap(true);
		firstMemoryDump.setReadPointerMap(false);
		nativePointerSearcherManager.addMemoryDump(firstMemoryDump);

		val firstMemoryDumpPointers = findPointers(nativePointerSearcherManager, addressSize, true);
		assertFalse(firstMemoryDumpPointers.isEmpty());
		assertTrue(firstMemoryDumpPointers.size() <= maximumPointersCount);

		for (val memoryPointer : firstMemoryDumpPointers)
		{
			val offsets = memoryPointer.getOffsets();
			for (val offset : offsets)
			{
				// Assert the value alignment to be respected
				assertEquals(0, offset % valueAlignment);
			}
		}

		nativePointerSearcherManager.addMemoryDump(firstMemoryDump);
		val secondMemoryDumpPointers = findPointers(nativePointerSearcherManager, addressSize, true);
		assertEquals(firstMemoryDumpPointers.size(), secondMemoryDumpPointers.size());
	}

	@Test
	public void testWindowsPointerSearch() throws Exception
	{
		val nativePointerSearcherManager = new NativePointerSearcherManager();
		val availableProcessorsCount = getAvailableProcessorsCount();
		nativePointerSearcherManager.setThreadCount(availableProcessorsCount * 2L);
		nativePointerSearcherManager.setExcludeCycles(true);
		nativePointerSearcherManager.setMinimumPointerDepth(1);
		nativePointerSearcherManager.setMaximumPointerDepth(3);
		nativePointerSearcherManager.setMaximumMemoryDumpChunkSize(100_000_000);
		nativePointerSearcherManager.setSaveAdditionalMemoryDumpRAM(false);
		nativePointerSearcherManager.setPotentialPointerOffsetsCountPerAddressPrediction(40);
		val maximumPointersCount = 100_000;
		nativePointerSearcherManager.setMaximumPointersCount(maximumPointersCount);
		nativePointerSearcherManager.setPointerOffsetRange(0, 10_000);
		nativePointerSearcherManager.setLastPointerOffsets(emptyList());
		val firstMemoryDump = new MemoryDump("D:\\Cpp\\PointerSearcher\\card_ids_1",
				0x0L, 0x2D574C28020L, LITTLE_ENDIAN);
		val addressSize = 8;
		firstMemoryDump.setAddressSize(addressSize);
		firstMemoryDump.setAddressAlignment(8);
		val valueAlignment = 8;
		firstMemoryDump.setValueAlignment(valueAlignment);
		firstMemoryDump.setMinimumPointerAddress(0x0);
		firstMemoryDump.setMaximumPointerAddress(0x7FFFFFFFFFFL);
		firstMemoryDump.setFileExtensions(asList("bin", "dmp"));
		firstMemoryDump.setGeneratePointerMap(true);
		firstMemoryDump.setReadPointerMap(false);
		nativePointerSearcherManager.addMemoryDump(firstMemoryDump);

		val firstMemoryDumpPointers = findPointers(nativePointerSearcherManager, addressSize, true);
		assertFalse(firstMemoryDumpPointers.isEmpty());
		assertTrue(firstMemoryDumpPointers.size() < maximumPointersCount);

		for (val memoryPointer : firstMemoryDumpPointers)
		{
			val offsets = memoryPointer.getOffsets();
			for (val offset : offsets)
			{
				// Assert the value alignment to be respected
				assertEquals(0, offset % valueAlignment);
			}
		}

		nativePointerSearcherManager.addMemoryDump(firstMemoryDump);
		val secondMemoryDumpPointers = findPointers(nativePointerSearcherManager, addressSize, true);
		assertEquals(firstMemoryDumpPointers.size(), secondMemoryDumpPointers.size());
	}

	private int getAvailableProcessorsCount()
	{
		Runtime runtime = Runtime.getRuntime();
		return runtime.availableProcessors();
	}

	@Test
	public void testNintendoSwitchSuperSmashPointerSearch() throws Exception
	{
		val nativePointerSearcherManager = new NativePointerSearcherManager();
		val availableProcessorsCount = getAvailableProcessorsCount();
		nativePointerSearcherManager.setThreadCount(availableProcessorsCount);
		nativePointerSearcherManager.setExcludeCycles(true);
		nativePointerSearcherManager.setMinimumPointerDepth(1);
		nativePointerSearcherManager.setMaximumPointerDepth(2);
		nativePointerSearcherManager.setMaximumMemoryDumpChunkSize(100_000_000);
		nativePointerSearcherManager.setSaveAdditionalMemoryDumpRAM(false);
		nativePointerSearcherManager.setPotentialPointerOffsetsCountPerAddressPrediction(40);
		val maximumPointersCount = 100_000;
		nativePointerSearcherManager.setMaximumPointersCount(maximumPointersCount);
		nativePointerSearcherManager.setPointerOffsetRange(0, 5_000);
		nativePointerSearcherManager.setLastPointerOffsets(emptyList());
		val firstMemoryDump = new MemoryDump("D:\\Cpp\\PointerSearcher\\smash_switch_2\\Dump1",
				0x0L, 0x5FDF030420L, LITTLE_ENDIAN);
		val addressSize = 8;
		firstMemoryDump.setAddressSize(addressSize);
		firstMemoryDump.setAddressAlignment(8);
		firstMemoryDump.setValueAlignment(8);
		firstMemoryDump.setMinimumPointerAddress(0x0);
		firstMemoryDump.setMaximumPointerAddress(0x7FFFFFFFFFFL);
		firstMemoryDump.setFileExtensions(asList("bin", "dmp"));
		firstMemoryDump.setGeneratePointerMap(true);
		firstMemoryDump.setReadPointerMap(true);
		nativePointerSearcherManager.addMemoryDump(firstMemoryDump);
		nativePointerSearcherManager.addMemoryDump(firstMemoryDump);

		val firstMemoryDumpPointers = findPointers(nativePointerSearcherManager, addressSize, true);
		assertFalse(firstMemoryDumpPointers.isEmpty());
	}

	@Test
	public void testNintendoWiiUPointerSearch() throws Exception
	{
		val nativePointerSearcherManager = new NativePointerSearcherManager();
		nativePointerSearcherManager.setThreadCount(1);
		nativePointerSearcherManager.setExcludeCycles(true);
		val minimumPointerDepth = 1;
		nativePointerSearcherManager.setMinimumPointerDepth(minimumPointerDepth);
		val maximumPointerDepth = 3;
		nativePointerSearcherManager.setMaximumPointerDepth(maximumPointerDepth);
		nativePointerSearcherManager.setMaximumMemoryDumpChunkSize(100_000_000);
		nativePointerSearcherManager.setSaveAdditionalMemoryDumpRAM(false);
		nativePointerSearcherManager.setPotentialPointerOffsetsCountPerAddressPrediction(40);
		val maximumPointersCount = 100_000;
		nativePointerSearcherManager.setMaximumPointersCount(maximumPointersCount);
		val fromOffset = 0;
		val toOffset = 0x400;
		nativePointerSearcherManager.setPointerOffsetRange(fromOffset, toOffset);
		val lastPointerOffsets = asList(0x1A4L, 0xC8L);
		nativePointerSearcherManager.setLastPointerOffsets(lastPointerOffsets);
		val firstMemoryDump = new MemoryDump("D:\\Cpp\\PointerSearcher\\39CEB148.bin",
				0x30000000L, 0x39CEB148L, BIG_ENDIAN);
		val addressSize = Integer.BYTES;
		firstMemoryDump.setAddressSize(addressSize);
		val addressAlignment = Long.BYTES;
		firstMemoryDump.setAddressAlignment(addressAlignment);
		val valueAlignment = Integer.BYTES;
		firstMemoryDump.setValueAlignment(valueAlignment);
		val minimumPointerAddress = 0x39000000;
		firstMemoryDump.setMinimumPointerAddress(minimumPointerAddress);
		val maximumPointerAddress = 0x3A000000;
		firstMemoryDump.setMaximumPointerAddress(maximumPointerAddress);
		firstMemoryDump.setGeneratePointerMap(true);
		firstMemoryDump.setReadPointerMap(false);
		nativePointerSearcherManager.addMemoryDump(firstMemoryDump);

		// Pointers should be found
		val firstMemoryDumpPointers = findPointers(nativePointerSearcherManager, addressSize, true);
		assertFalse(firstMemoryDumpPointers.isEmpty());

		// Assert the maximum pointers count to be respected
		assertTrue(firstMemoryDumpPointers.size() <= maximumPointersCount);

		val bounds = getBounds(addressSize);

		for (val memoryPointer : firstMemoryDumpPointers)
		{
			val offsets = memoryPointer.getOffsets();
			val offsetsCount = offsets.length;

			// Pointers should not exceed the specified bounds
			assertTrue(offsetsCount >= minimumPointerDepth && offsetsCount <= maximumPointerDepth);

			// Last pointer offsets should be respected
			isLastPointerOffsetsRespected(lastPointerOffsets, offsets);

			val baseAddress = memoryPointer.getBaseAddress();

			// Assert minimum and maximum pointer address bounds being respected
			assertTrue(baseAddress >= minimumPointerAddress && baseAddress <= maximumPointerAddress);

			// Assert address size respected
			assertTrue(baseAddress >= bounds.minimum && baseAddress <= bounds.maximum);

			// The base address is divisible by the address size
			assertEquals(0, baseAddress % addressAlignment);

			for (val offset : offsets)
			{
				// Assert the offset range being respected
				assertTrue(offset >= fromOffset && offset <= toOffset);

				// Assert the value alignment to be respected
				assertEquals(0, offset % valueAlignment);
			}
		}

		val secondMemoryDump = new MemoryDump("D:\\Cpp\\PointerSearcher\\39CF14D8.bin",
				0x30000000L, 0x39CF14D8L, BIG_ENDIAN);
		secondMemoryDump.setAddressSize(addressSize);
		secondMemoryDump.setAddressAlignment(addressAlignment);
		secondMemoryDump.setValueAlignment(valueAlignment);
		secondMemoryDump.setMinimumPointerAddress(0x39000000);
		secondMemoryDump.setMaximumPointerAddress(0x3A000000);
		secondMemoryDump.setGeneratePointerMap(true);
		secondMemoryDump.setReadPointerMap(false);
		nativePointerSearcherManager.addMemoryDump(secondMemoryDump);

		val secondMemoryDumpPointers = findPointers(nativePointerSearcherManager, addressSize, true);

		// Pointers may not be empty
		assertFalse(secondMemoryDumpPointers.isEmpty());

		// Pointers may be filtered
		assertTrue(secondMemoryDumpPointers.size() <= firstMemoryDumpPointers.size());
	}

	@AllArgsConstructor
	private static class Bounds
	{
		private final long minimum;
		private final long maximum;
	}

	@SuppressWarnings("SameParameterValue")
	private static Bounds getBounds(long addressSize)
	{
		val bitsPerByte = 8;
		val maximum = ((long) pow(2, addressSize * bitsPerByte) - 1) / 2;
		val minimum = -1 * maximum - 1;
		return new Bounds(minimum, maximum);
	}

	private static void isLastPointerOffsetsRespected(List<Long> lastPointerOffsets, long[] offsets)
	{
		for (var lastPointerOffsetIndex = lastPointerOffsets.size() - 1;
		     lastPointerOffsetIndex >= 0; lastPointerOffsetIndex--)
		{
			val offsetsIndex = offsets.length - lastPointerOffsetIndex;
			if (offsetsIndex >= offsets.length)
			{
				break;
			}

			val offset = offsets[offsetsIndex];
			val lastPointerOffset = lastPointerOffsets.get(lastPointerOffsetIndex);
			assertEquals(lastPointerOffset.longValue(), offset);
		}
	}
}
