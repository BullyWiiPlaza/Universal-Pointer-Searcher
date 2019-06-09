import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.serialization.BackupMethod;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.serialization.CompressionMethod;
import lombok.val;
import lombok.var;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.DataFormatException;

import static com.wiiudev.gecko.pointer.preprocessed_search.data_structures.serialization.BackupMethod.GSON;
import static com.wiiudev.gecko.pointer.preprocessed_search.data_structures.serialization.BackupMethod.LINES;
import static com.wiiudev.gecko.pointer.preprocessed_search.data_structures.serialization.CompressionMethod.*;
import static com.wiiudev.gecko.pointer.preprocessed_search.data_structures.serialization.PointerMapSerializer.*;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.delete;
import static org.junit.Assert.assertEquals;

public class TestPointerMapSerialization
{
	private static Path tempFile;
	private static TreeMap<Long, Long> firstOffsetValuePairs;

	@BeforeClass
	public static void setup() throws IOException
	{
		val entriesCount = 300_000;
		firstOffsetValuePairs = new TreeMap<>();
		for (var currentIndex = 0; currentIndex < entriesCount; currentIndex++)
		{
			val offset = getRandomNumber();
			val value = getRandomNumber();
			firstOffsetValuePairs.put(offset, value);
		}

		tempFile = createTempFile("prefix", "suffix");
	}

	private static long getRandomNumber()
	{
		return getRandomNumber(0, 0x50000000);
	}

	@SuppressWarnings("SameParameterValue")
	static long getRandomNumber(long minimum, long maximum)
	{
		val current = ThreadLocalRandom.current();
		return current.nextLong(minimum, maximum + 1);
	}

	@Test
	public void testLinesSerializationWithLZ4Compression() throws IOException, DataFormatException
	{
		runSerializationTest(LINES, LZ4);
	}

	@Test
	public void testLinesSerializationWithDeflaterCompression() throws IOException, DataFormatException
	{
		runSerializationTest(LINES, DEFLATER);
	}

	@Test
	public void testLinesSerializationWithoutCompression() throws IOException, DataFormatException
	{
		runSerializationTest(LINES, NONE);
	}

	@Test
	public void testGSONSerializationWithLZ4Compression() throws IOException, DataFormatException
	{
		runSerializationTest(GSON, LZ4);
	}

	@Test
	public void testGSONSerializationWithDeflaterCompression() throws IOException, DataFormatException
	{
		runSerializationTest(GSON, DEFLATER);
	}

	@Test
	public void testGSONSerializationWithoutCompression() throws IOException, DataFormatException
	{
		runSerializationTest(GSON, NONE);
	}

	private void runSerializationTest(BackupMethod backupMethod, CompressionMethod compressionMethod) throws IOException, DataFormatException
	{
		BACKUP_METHOD = backupMethod;
		COMPRESSION_METHOD = compressionMethod;
		serializePointerMap(tempFile, firstOffsetValuePairs);
		val length = tempFile.toFile().length();
		System.out.println("File Size: " + length);
		val secondOffsetValuePairs = deserializePointerMap(tempFile);
		val expected = firstOffsetValuePairs;
		assertEquals(expected, secondOffsetValuePairs);
	}

	@AfterClass
	public static void cleanup() throws IOException
	{
		delete(tempFile);
	}
}
