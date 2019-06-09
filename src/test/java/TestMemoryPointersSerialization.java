import com.wiiudev.gecko.pointer.preprocessed_search.PointerSwapFile;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryPointer;
import lombok.val;
import lombok.var;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.wiiudev.gecko.pointer.preprocessed_search.PointerSwapFile.STARTING_SWAP_FILE_NUMBER;
import static org.junit.Assert.assertEquals;

public class TestMemoryPointersSerialization
{
	private static final int TOTAL_MEMORY_POINTERS = 1000;
	private static final int ELEMENTS_COUNT = 100;

	private static PointerSwapFile pointerSwapFile;
	private static List<MemoryPointer> memoryPointers;

	@BeforeClass
	public static void setup()
	{
		pointerSwapFile = new PointerSwapFile();
		setupMemoryPointers();
	}

	private static void setupMemoryPointers()
	{
		memoryPointers = new ArrayList<>();
		for (var memoryPointerIndex = 0;
		     memoryPointerIndex < TOTAL_MEMORY_POINTERS;
		     memoryPointerIndex++)
		{
			val randomAddress = TestPointerMapSerialization.getRandomNumber(0, 0x1000_0000);
			val randomOffsets = getRandomOffsets();
			val memoryPointer = new MemoryPointer(randomAddress, randomOffsets);
			memoryPointers.add(memoryPointer);
		}
	}

	@Test
	public void testBackupPointers() throws IOException
	{
		val backupList = getSubList(0, ELEMENTS_COUNT);
		val secondBackupList = getSubList(ELEMENTS_COUNT, ELEMENTS_COUNT * 2);
		assertEquals(TOTAL_MEMORY_POINTERS, memoryPointers.size());
		pointerSwapFile.storeToDisk(memoryPointers, ELEMENTS_COUNT);
		pointerSwapFile.storeToDisk(memoryPointers, ELEMENTS_COUNT);
		assertEquals(TOTAL_MEMORY_POINTERS - ELEMENTS_COUNT * 2, memoryPointers.size());
		val restoredPointers = pointerSwapFile.getBackupPointers(STARTING_SWAP_FILE_NUMBER);
		assertEquals(backupList, restoredPointers);
		val secondRestoredPointers = pointerSwapFile.getBackupPointers(STARTING_SWAP_FILE_NUMBER + 1);
		assertEquals(secondBackupList, secondRestoredPointers);
	}

	@SuppressWarnings("SameParameterValue")
	private ArrayList<MemoryPointer> getSubList(int startingIndex, int elementsCount)
	{
		val backupList = new ArrayList<MemoryPointer>();
		for (var elementIndex = startingIndex; elementIndex < elementsCount; elementIndex++)
		{
			val memoryPointer = memoryPointers.get(elementIndex);
			backupList.add(memoryPointer);
		}
		return backupList;
	}

	private static long[] getRandomOffsets()
	{
		val offsetsCount = TestPointerMapSerialization.getRandomNumber(1, 5);
		val offsets = new long[(int) offsetsCount];

		for (var offsetIndex = 0; offsetIndex < offsetsCount; offsetIndex++)
		{
			offsets[offsetIndex] = TestPointerMapSerialization.getRandomNumber(-100, 100);
		}

		return offsets;
	}
}
