package com.wiiudev.gecko.pointer.swing.utilities;

import lombok.val;

import java.text.DecimalFormat;

import static java.lang.Math.log10;

public class FileSizePrinting
{
	private static final int BASE_DATA_TYPE_SIZE = 1024;
	private static final String[] UNITS = new String[]{"B", "kB", "MB", "GB", "TB"};
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.#");

	public static String readableFileSize(long fileSize)
	{
		if (fileSize == 0)
		{
			val unit = UNITS[0];
			return formatNumber(fileSize, unit);
		}
		val digitGroups = (int) (log10(fileSize) / log10(BASE_DATA_TYPE_SIZE));

		if (digitGroups >= UNITS.length)
		{
			throw new IllegalArgumentException("Unit too large");
		}

		val number = fileSize / Math.pow(BASE_DATA_TYPE_SIZE, digitGroups);
		val unit = UNITS[digitGroups];
		return formatNumber((long) number, unit);
	}

	private static String formatNumber(long fileSize, String unit)
	{
		return DECIMAL_FORMAT.format(fileSize) + " " + unit;
	}
}
