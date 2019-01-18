package com.wiiudev.gecko.pointer.non_preprocessed_search.searcher;

import java.nio.ByteOrder;

import static java.nio.ByteOrder.BIG_ENDIAN;

public class WiiUPointerSearch extends UniversalPointerSearcher
{
	public WiiUPointerSearch()
	{
		super();
	}

	@Override
	public long returnMemoryDumpStartingOffset()
	{
		return 0x10000000;
	}

	@Override
	public ByteOrder returnByteOrder()
	{
		return BIG_ENDIAN;
	}
}
