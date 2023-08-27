package com.wiiudev.gecko.pointer.preprocessed_search;

import com.wiiudev.gecko.pointer.utilities.Benchmark;
import lombok.val;
import lombok.var;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;

import static java.nio.file.Files.readAllBytes;

public class EndianConverter
{
	public static final String DUMPS_NO_TRACK_MUSIC_39_CEB148_BIN = "dumps/No Track Music/39CEB148.bin";

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
