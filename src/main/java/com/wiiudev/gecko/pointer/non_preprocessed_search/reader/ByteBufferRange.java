package com.wiiudev.gecko.pointer.non_preprocessed_search.reader;

import lombok.Getter;

import java.nio.ByteBuffer;

import static java.lang.Long.toHexString;

public class ByteBufferRange
{
	private final long startOffset;
	private final long endOffset;

	@Getter
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

	@Override
	public String toString()
	{
		return toHexString(startOffset).toUpperCase()
				+ " - " + toHexString(endOffset).toUpperCase();
	}
}
