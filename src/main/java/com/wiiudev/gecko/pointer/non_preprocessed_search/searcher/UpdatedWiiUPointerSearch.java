package com.wiiudev.gecko.pointer.non_preprocessed_search.searcher;

import com.wiiudev.gecko.pointer.non_preprocessed_search.MemoryDump;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;

import static java.nio.ByteOrder.*;

public class UpdatedWiiUPointerSearch extends UpdatedPointerSearcher
{
	public UpdatedWiiUPointerSearch(List<MemoryDump> memoryDumps) throws IOException
	{
		super(memoryDumps, 0x10000000, BIG_ENDIAN);
	}
}
