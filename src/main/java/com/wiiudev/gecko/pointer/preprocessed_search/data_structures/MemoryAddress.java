package com.wiiudev.gecko.pointer.preprocessed_search.data_structures;

import lombok.Getter;

import static java.lang.Long.toHexString;

@Getter
public class MemoryAddress
{
	private final long relativeOffset;

	private final long startingOffset;

	public MemoryAddress(long absoluteAddress)
	{
		this(absoluteAddress, 0);
	}

	public MemoryAddress(long relativeOffset, long startingOffset)
	{
		this.relativeOffset = relativeOffset;
		this.startingOffset = startingOffset;
	}

	public long getAbsoluteAddress()
	{
		return relativeOffset + startingOffset;
	}

	@Override
	public String toString()
	{
		long absoluteAddress = getAbsoluteAddress();
		return "0x" + toHexString(absoluteAddress).toUpperCase();
	}
}
