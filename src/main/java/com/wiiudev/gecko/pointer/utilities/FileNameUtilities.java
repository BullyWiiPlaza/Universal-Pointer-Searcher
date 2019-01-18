package com.wiiudev.gecko.pointer.utilities;

import lombok.val;

import static java.lang.Long.parseUnsignedLong;
import static org.apache.commons.io.FilenameUtils.getBaseName;

public class FileNameUtilities
{
	private static final int HEXADECIMAL_ADDRESS_LENGTH = 8;

	public static long getTargetAddressFromFile(String filePath)
	{
		val baseFileName = getBaseFileName(filePath);
		val length = baseFileName.length();

		if (length > HEXADECIMAL_ADDRESS_LENGTH)
		{
			val potentialBaseFileNameAddress = baseFileName.substring(length - HEXADECIMAL_ADDRESS_LENGTH);
			return parseAddress(potentialBaseFileNameAddress);
		}

		return parseAddress(baseFileName);
	}

	private static long parseAddress(String hexadecimalAddress)
	{
		return parseUnsignedLong(hexadecimalAddress, 16);
	}

	public static String getBaseFileName(String filePath)
	{
		return getBaseName(filePath);
	}
}
