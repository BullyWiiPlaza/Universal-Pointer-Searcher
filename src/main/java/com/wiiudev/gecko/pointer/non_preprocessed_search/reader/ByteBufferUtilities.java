package com.wiiudev.gecko.pointer.non_preprocessed_search.reader;

import lombok.val;
import lombok.var;

import java.nio.ByteBuffer;

import static com.wiiudev.gecko.pointer.non_preprocessed_search.searcher.UniversalPointerSearcher.INTEGER_SIZE;

public class ByteBufferUtilities
{
	public static void setDecreasedBuffer(ByteBuffer memoryDumpReader, long baseAddressValue,
	                                      int innerPointerOffset, long memoryDumpStartingOffset)
	{
		var decreasedBufferPosition = (int) (baseAddressValue - memoryDumpStartingOffset + innerPointerOffset);

		if (decreasedBufferPosition < 0)
		{
			decreasedBufferPosition = 0;
		}

		memoryDumpReader.position(decreasedBufferPosition);
	}

	public static boolean isIntegerRemaining(ByteBuffer byteBuffer)
	{
		return isIntegerRemaining(byteBuffer, byteBuffer.position());
	}

	public static boolean isIntegerRemaining(ByteBufferRange byteBufferRange)
	{
		val byteBuffer = byteBufferRange.getByteBuffer();
		val currentPosition = byteBuffer.position();
		val endOffset = byteBufferRange.getEndOffset();

		return currentPosition + INTEGER_SIZE - 1 < endOffset;
	}

	public static boolean isIntegerRemaining(ByteBuffer byteBuffer, int position)
	{
		return position + INTEGER_SIZE - 1 < byteBuffer.limit() && position >= 0;
	}
}
