package com.wiiudev.gecko.pointer.non_preprocessed_search.reader;

import java.nio.ByteBuffer;

import static java.lang.Long.toHexString;

public class ByteBufferRange
{
	private final long startOffset;
	private final long endOffset;
	private final ByteBuffer byteBuffer;

	public ByteBufferRange(long startingOffset, long endOffset, ByteBuffer byteBuffer)
	{
		this.startOffset = startingOffset;
		byteBuffer.position((int) startingOffset);
		this.endOffset = endOffset;
		this.byteBuffer = byteBuffer;
	}

	long getEndOffset()
	{
		return endOffset;
	}

	public ByteBuffer getByteBuffer()
	{
		return byteBuffer;
	}

	@Override
	public String toString()
	{
		return toHexString(startOffset).toUpperCase()
				+ " - " + toHexString(endOffset).toUpperCase();
	}
}
