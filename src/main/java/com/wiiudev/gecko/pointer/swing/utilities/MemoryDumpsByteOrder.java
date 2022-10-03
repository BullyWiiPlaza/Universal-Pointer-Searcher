package com.wiiudev.gecko.pointer.swing.utilities;

import lombok.Getter;
import lombok.val;

import java.nio.ByteOrder;

public enum MemoryDumpsByteOrder
{
	BIG_ENDIAN("Big Endian", ByteOrder.BIG_ENDIAN),
	LITTLE_ENDIAN("Little Endian", ByteOrder.LITTLE_ENDIAN);

	private final String text;

	@Getter
	private final ByteOrder byteOrder;

	MemoryDumpsByteOrder(String text, ByteOrder byteOrder)
	{
		this.text = text;
		this.byteOrder = byteOrder;
	}

	public String toString()
	{
		return text;
	}

	public static MemoryDumpsByteOrder getMemoryDumpsByteOrder(ByteOrder byteOrder)
	{
		val memoryDumpsByteOrders = MemoryDumpsByteOrder.values();

		for (val memoryDumpsByteOrder : memoryDumpsByteOrders)
		{
			if (memoryDumpsByteOrder.getByteOrder() == byteOrder)
			{
				return memoryDumpsByteOrder;
			}
		}

		return null;
	}
}
