package com.wiiudev.gecko.pointer.preprocessed_search.data_structures.serialization;

import lombok.val;
import lombok.var;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;

import static java.util.Arrays.copyOf;

class LZ4Compression
{
	private static final LZ4Factory lz4Factory = LZ4Factory.safeInstance();
	private static final LZ4SafeDecompressor deCompressor = lz4Factory.safeDecompressor();
	private static final LZ4Compressor fastCompressor = lz4Factory.highCompressor();

	static byte[] compress(byte[] raw)
	{
		val maxCompressedLength = fastCompressor.maxCompressedLength(raw.length);
		val comp = new byte[maxCompressedLength];
		val compressedLength = fastCompressor.compress(raw, 0, raw.length, comp, 0, maxCompressedLength);
		return copyOf(comp, compressedLength);
	}

	static byte[] decompress(byte[] compressed)
	{
		var decompressed = new byte[compressed.length * 4];
		val source = copyOf(compressed, compressed.length);
		decompressed = deCompressor.decompress(source, decompressed.length);
		return decompressed;
	}
}
