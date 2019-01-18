package com.wiiudev.gecko.pointer.preprocessed_search.utilities;

import lombok.val;

public class DataConversions
{
	private static final String HEXADECIMAL_HEADER = "0x";

	public static String toHexadecimal(int number)
	{
		return HEXADECIMAL_HEADER + Integer.toHexString(number).toUpperCase();
	}

	public static String toHexadecimal(long number, int addressSize, boolean applyPadding)
	{
		switch (addressSize)
		{
			case Integer.BYTES:
			default:
				val hexadecimalIntegerNumber = Integer.toHexString((int) number).toUpperCase();

				if (applyPadding)
				{
					val paddedHexadecimalIntegerNumber = applyPadding(addressSize, hexadecimalIntegerNumber);
					return HEXADECIMAL_HEADER + paddedHexadecimalIntegerNumber;
				}
				return HEXADECIMAL_HEADER + hexadecimalIntegerNumber;

			case Long.BYTES:
				val hexadecimalLongNumber = Long.toHexString(number).toUpperCase();

				if (applyPadding)
				{
					val paddedHexadecimalLongNumber = applyPadding(addressSize, hexadecimalLongNumber);
					return HEXADECIMAL_HEADER + paddedHexadecimalLongNumber;
				}
				return HEXADECIMAL_HEADER + hexadecimalLongNumber;
		}
	}

	private static String applyPadding(int addressSize, String hexadecimalNumber)
	{
		val stringBuilder = new StringBuilder(hexadecimalNumber);
		while (stringBuilder.length() / 2 < addressSize)
		{
			stringBuilder.insert(0, "0");
		}
		return stringBuilder.toString();
	}
}
