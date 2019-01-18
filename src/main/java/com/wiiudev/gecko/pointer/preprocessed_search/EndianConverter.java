package com.wiiudev.gecko.pointer.preprocessed_search;

import com.wiiudev.gecko.pointer.utilities.Benchmark;
import lombok.val;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;

public class EndianConverter
{
	public static final String DUMPS_NO_TRACK_MUSIC_39_CEB148_BIN = "dumps/No Track Music/39CEB148.bin";

	public static void convertEndian(String inputFilePath, String outputFilePath) throws IOException
	{
		val readAllBytes = readAllBytes(Paths.get(inputFilePath));
		swapByteOrder(readAllBytes);
		write(Paths.get(outputFilePath), readAllBytes);
	}

	private static void swapByteOrder(byte[] bytes)
	{
		for (var bytesIndex = 0;
		     bytesIndex < bytes.length;
		     bytesIndex += Integer.BYTES)
		{
			var tmp = bytes[bytesIndex];
			bytes[bytesIndex] = bytes[bytesIndex + 3];
			bytes[bytesIndex + 3] = tmp;
			tmp = bytes[bytesIndex + 1];
			bytes[bytesIndex + 1] = bytes[bytesIndex + 2];
			bytes[bytesIndex + 2] = tmp;
		}
	}

	public static void main(String[] arguments) throws IOException
	{
		// convertEndian(DUMPS_NO_TRACK_MUSIC_39_CEB148_BIN, DUMPS_NO_TRACK_MUSIC_39_CEB148_BIN + "2");
		val benchmark = new Benchmark();
		benchmark.start();
		val readAllBytes = readAllBytes(Paths.get(DUMPS_NO_TRACK_MUSIC_39_CEB148_BIN));
		val byteBuffer = ByteBuffer.wrap(readAllBytes);
		val startingAddress = 0x30000000;
		var potentialPointerFound = 0;

		while (byteBuffer.hasRemaining())
		{
			val value = byteBuffer.getInt();
			if (value >= startingAddress && value <= startingAddress + byteBuffer.capacity())
			{
				potentialPointerFound++;
			}
		}

		System.out.println("Potential pointers found: " + potentialPointerFound);

		val elapsedTime = benchmark.getElapsedTime();
		System.out.println("Elapsed time: " + elapsedTime);
	}
}
