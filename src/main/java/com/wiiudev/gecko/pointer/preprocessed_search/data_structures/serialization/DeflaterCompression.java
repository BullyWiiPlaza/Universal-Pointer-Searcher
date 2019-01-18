package com.wiiudev.gecko.pointer.preprocessed_search.data_structures.serialization;

import lombok.val;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static java.util.zip.Deflater.BEST_COMPRESSION;

class DeflaterCompression
{
	private static final int BUFFER_SIZE = 1024;

	static byte[] compress(byte[] data) throws IOException
	{
		val deflater = new Deflater();
		deflater.setLevel(BEST_COMPRESSION);
		deflater.setInput(data);

		try (val outputStream = new ByteArrayOutputStream(data.length))
		{
			deflater.finish();
			val buffer = new byte[BUFFER_SIZE];
			while (!deflater.finished())
			{
				val count = deflater.deflate(buffer);
				outputStream.write(buffer, 0, count);
			}

			return outputStream.toByteArray();
		}
	}

	static byte[] decompress(byte[] data) throws IOException, DataFormatException
	{
		val inflater = new Inflater();
		inflater.setInput(data);

		try (val outputStream = new ByteArrayOutputStream(data.length))
		{
			val buffer = new byte[1024];
			while (!inflater.finished())
			{
				val count = inflater.inflate(buffer);
				outputStream.write(buffer, 0, count);
			}

			return outputStream.toByteArray();
		}
	}
}
