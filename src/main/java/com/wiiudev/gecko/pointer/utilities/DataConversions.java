package com.wiiudev.gecko.pointer.utilities;

import static java.lang.Long.parseLong;

public class DataConversions
{
	/*public static String toHexadecimal(int number)
	{
		return toHexadecimal((long) number, false);
	}*/

	/*private static String toHexadecimal(long number, boolean padding)
	{
		var hexadecimal = toHexString(number);
		hexadecimal = hexadecimal.toUpperCase();

		if (padding)
		{
			val hexadecimalBuilder = new StringBuilder(hexadecimal);
			while (hexadecimalBuilder.length() < 8)
			{
				hexadecimalBuilder.insert(0, "0");
			}

			hexadecimal = hexadecimalBuilder.toString();
		}

		return hexadecimal;
	}*/

	public static long parseInt(String text)
	{
		if (text.equals(""))
		{
			return 0;
		}

		return parseLong(text, 16);
	}
}
