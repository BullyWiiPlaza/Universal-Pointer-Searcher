import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryPointer;
import lombok.val;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestPointerExpressionParsing
{
	@Test
	public void testPointerExpressionParsingNoHexadecimalHeader()
	{
		val pointerExpression = "[0x814B8554] + 0";
		val memoryPointer = new MemoryPointer(pointerExpression);
		assertEquals(0x814B8554L, memoryPointer.getBaseAddress());
		val offsets = memoryPointer.getOffsets();
		assertFalse(offsets.length != 1 || offsets[0] != 0);
	}

	@Test
	public void testPointerExpressionParsing2Offsets()
	{
		val pointerExpression = "[[0x914B8554]+0x124] + 0x12A";
		val memoryPointer = new MemoryPointer(pointerExpression);
		assertEquals(0x914B8554L, memoryPointer.getBaseAddress());
		val offsets = memoryPointer.getOffsets();
		assertEquals(2, offsets.length);
		assertEquals(0x124, offsets[0]);
		assertEquals(0x12A, offsets[1]);
	}

	@Test
	public void testPointerExpressionParsing3Offsets()
	{
		val pointerExpression = "[[[0x914B8554]+0x124] + 0x12A] - 0x4";
		val memoryPointer = new MemoryPointer(pointerExpression);
		assertEquals(0x914B8554L, memoryPointer.getBaseAddress());
		val offsets = memoryPointer.getOffsets();
		assertEquals(3, offsets.length);
		assertEquals(0x124, offsets[0]);
		assertEquals(0x12A, offsets[1]);
		assertEquals(-0x4, offsets[2]);
	}

	@Test
	public void testPointerExpressionParsingNegativeOffset()
	{
		val pointerExpression = "[0x914B85540000]-4";
		val memoryPointer = new MemoryPointer(pointerExpression);
		assertEquals(0x914B85540000L, memoryPointer.getBaseAddress());
		val offsets = memoryPointer.getOffsets();
		assertFalse(offsets.length != 1 || offsets[0] != -4);
	}

	@Test
	public void testPointerExpressionParsingWithModuleNames()
	{
		val pointerExpression = "[[SB4E01_DUMP80_1.bin + 0x1457AD8] + 0x74] + 0x5C";
		val memoryPointer = new MemoryPointer(pointerExpression);
		assertEquals("SB4E01_DUMP80_1.bin", memoryPointer.getModuleName());
		assertEquals(0x1457AD8, memoryPointer.getModuleOffset());
		val offsets = memoryPointer.getOffsets();
		assertEquals(2, offsets.length);
		assertEquals(0x74, offsets[0]);
		assertEquals(0x5C, offsets[1]);
	}

	@Test
	public void testPointerExpressionParsingHexadecimalHeader()
	{
		val pointerExpression = "[0x814B8554]+0x0";
		val memoryPointer = new MemoryPointer(pointerExpression);
		assertEquals(0x814B8554L, memoryPointer.getBaseAddress());
		val offsets = memoryPointer.getOffsets();
		assertFalse(offsets.length != 1 || offsets[0] != 0);
	}
}
