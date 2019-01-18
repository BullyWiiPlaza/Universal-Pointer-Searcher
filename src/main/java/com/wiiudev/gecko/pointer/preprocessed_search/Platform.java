package com.wiiudev.gecko.pointer.preprocessed_search;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.ByteOrder;

import static java.nio.ByteOrder.*;

@AllArgsConstructor
public enum Platform
{
	WII_U(BIG_ENDIAN, 0x10000000),
	THREE_DS(LITTLE_ENDIAN, 0x0),
	WII(BIG_ENDIAN, 0x80000000);

	@Getter
	private final ByteOrder byteOrder;

	@Getter
	private final int defaultStartingOffset;
}
