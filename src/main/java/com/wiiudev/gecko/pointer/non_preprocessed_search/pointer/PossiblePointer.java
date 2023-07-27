package com.wiiudev.gecko.pointer.non_preprocessed_search.pointer;

import lombok.val;

public class PossiblePointer
{
	private final long address;
	private final long value;

	public PossiblePointer(long address, long value)
	{
		this.address = address;
		this.value = value;
	}

	public long getValue()
	{
		return value;
	}

	public long getAddress()
	{
		return address;
	}

	@Override
	public int hashCode()
	{
		return Long.hashCode(address);
	}

	@Override
	public boolean equals(Object object)
	{
		if(!(object instanceof PossiblePointer))
		{
			return false;
		}

		val possiblePointer = (PossiblePointer) object;

		return address == possiblePointer.getAddress();
	}
}
