package com.wiiudev.gecko.pointer.preprocessed_search;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.ByteOrder;

import static java.nio.ByteOrder.*;

@Getter
@AllArgsConstructor
public enum Platform
{
	WII_U(BIG_ENDIAN, 0x10000000);

	private final ByteOrder byteOrder;

	@Getter
	private final int defaultStartingOffset;
}
