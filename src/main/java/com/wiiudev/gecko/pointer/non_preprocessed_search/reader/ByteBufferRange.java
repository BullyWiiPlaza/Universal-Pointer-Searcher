package com.wiiudev.gecko.pointer.non_preprocessed_search.reader;

import java.nio.ByteBuffer;

import static java.lang.Long.toHexString;

public class ByteBufferRange
{
	private long startOffset;
	private long endOffset;
	private ByteBuffer byteBuffer;

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
