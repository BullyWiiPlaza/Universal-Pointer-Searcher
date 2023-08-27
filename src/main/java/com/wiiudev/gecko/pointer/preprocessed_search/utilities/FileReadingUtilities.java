package com.wiiudev.gecko.pointer.preprocessed_search.utilities;

// import sun.misc.Unsafe;

import lombok.val;
import lombok.var;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;

public class FileReadingUtilities
{
	public static List<ByteBuffer> getByteBuffers(String filePath, long startingOffset, long totalSize) throws IOException
	{
		val byteBuffers = new ArrayList<ByteBuffer>();

		val binaryFile = new File(filePath);
		try (val binaryFileChannel = new RandomAccessFile(binaryFile, "r").getChannel())
		{
			var remainingSize = totalSize;
			while (remainingSize > 0)
			{
				var byteBufferSize = remainingSize;

				if (remainingSize > Integer.MAX_VALUE)
				{
					byteBufferSize = Integer.MAX_VALUE;
				}

				val mappedByteBuffer = binaryFileChannel.map(READ_ONLY, startingOffset, byteBufferSize);
				byteBuffers.add(mappedByteBuffer);

				remainingSize -= byteBufferSize;
			}
		}

		return byteBuffers;
	}

	/*public static Unsafe getUnsafe() throws Exception
	{
		Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
		unsafe.setAccessible(true);

		return (Unsafe) unsafe.get(null);
	}*/
}
