package com.wiiudev.gecko.pointer.non_preprocessed_search.pointer;

public class PointerOffsetChecker
{
	private boolean allowNegative;
	private int maximumOffset;

	public PointerOffsetChecker()
	{
		this.allowNegative = false;
		this.maximumOffset = 0x400;
	}

	public void setAllowNegative(boolean allowNegative)
	{
		this.allowNegative = allowNegative;
	}

	public void setMaximumOffset(int maximumOffset)
	{
		this.maximumOffset = maximumOffset;
	}

	public int getMaximumOffset()
	{
		return maximumOffset;
	}

	public boolean allowsPositive()
	{
		return !allowNegative;
	}

	private boolean isNegative(int offset)
	{
		return !allowNegative && offset < 0;
	}

	public boolean fulfillsSettings(int offset)
	{
		return !isNegative(offset) && !exceedsMaximumOffset(offset);
	}

	private boolean exceedsMaximumOffset(int offset)
	{
		return Math.abs(offset) > maximumOffset;
	}
}
