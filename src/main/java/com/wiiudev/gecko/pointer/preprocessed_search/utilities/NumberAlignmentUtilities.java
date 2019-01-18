package com.wiiudev.gecko.pointer.preprocessed_search.utilities;

public class NumberAlignmentUtilities
{
	public static long alignLong(long address)
	{
		return address & -4;
	}
}
