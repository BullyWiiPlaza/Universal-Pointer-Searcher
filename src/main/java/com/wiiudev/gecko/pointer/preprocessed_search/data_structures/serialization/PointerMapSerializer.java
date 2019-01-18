package com.wiiudev.gecko.pointer.preprocessed_search.data_structures.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.OffsetValuePair;
import lombok.val;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.zip.DataFormatException;

import static com.wiiudev.gecko.pointer.preprocessed_search.data_structures.serialization.BackupMethod.GSON;
import static com.wiiudev.gecko.pointer.preprocessed_search.data_structures.serialization.BackupMethod.LINES;
import static com.wiiudev.gecko.pointer.preprocessed_search.data_structures.serialization.CompressionMethod.NONE;
import static java.lang.Long.parseUnsignedLong;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;

public class PointerMapSerializer
{
	private static Gson gson;

	public static BackupMethod BACKUP_METHOD = LINES;
	public static CompressionMethod COMPRESSION_METHOD = NONE;
	private static final String OFFSET_VALUE_DELIMITER = "=";
	private static final String NEW_LINE_DELIMITER = ";";

	private static void considerInitializingGSONBuilder()
	{
		if (gson == null)
		{
			val gsonBuilder = new GsonBuilder();
			val typeToken = getTypeToken();
			val typeAdapter = new OffsetValuePairSerializer();
			gsonBuilder.registerTypeAdapter(typeToken, typeAdapter);
			gsonBuilder.setPrettyPrinting();
			gson = gsonBuilder.create();
		}
	}

	private static Type getTypeToken()
	{
		return new TypeToken<ArrayList<OffsetValuePair>>()
		{
		}.getType();
	}

	private static String writeGSONStorage(TreeMap<Long, Long> offsetValuePairs)
	{
		val typeToken = getTypeToken();
		return gson.toJson(offsetValuePairs, typeToken);
	}

	private static TreeMap<Long, Long> restoreFromGSONStorage(String json)
	{
		return null;
		/* val typeToken = getTypeToken();
		return gson.fromJson(json, typeToken); */
	}

	public static void serializePointerMap(Path path, TreeMap<Long, Long> offsetValuePairs) throws IOException
	{
		if (BACKUP_METHOD.equals(GSON))
		{
			considerInitializingGSONBuilder();
		}

		val json = writeStorage(offsetValuePairs);
		val jsonBytes = json.getBytes(UTF_8);
		val compressedBytes = compressBytes(jsonBytes);
		write(path, compressedBytes);
	}

	private static String writeStorage(TreeMap<Long, Long> offsetValuePairs)
	{
		return BACKUP_METHOD.equals(GSON) ? writeGSONStorage(offsetValuePairs) : writeLinesStorage(offsetValuePairs);
	}

	private static byte[] compressBytes(byte[] bytes) throws IOException
	{
		switch (COMPRESSION_METHOD)
		{
			case DEFLATER:
				return DeflaterCompression.compress(bytes);

			case LZ4:
				return LZ4Compression.compress(bytes);

			case NONE:
				return bytes;
		}

		throw new IllegalStateException("No compression method associated");
	}

	private static String writeLinesStorage(TreeMap<Long, Long> offsetValuePairs)
	{
		val stringBuilder = new StringBuilder();
		for (val offsetValuePair : offsetValuePairs.entrySet())
		{
			stringBuilder.append(offsetValuePair.getKey());
			stringBuilder.append(OFFSET_VALUE_DELIMITER);
			stringBuilder.append(offsetValuePair.getValue());
			stringBuilder.append(NEW_LINE_DELIMITER);
		}

		return stringBuilder.toString().trim();
	}

	public static TreeMap<Long, Long> deserializePointerMap(Path path) throws IOException, DataFormatException
	{
		if (BACKUP_METHOD.equals(GSON))
		{
			considerInitializingGSONBuilder();
		}

		val bytes = readAllBytes(path);
		val decompressedBytes = decompressBytes(bytes);
		val decompressed = new String(decompressedBytes, UTF_8);
		return restoreFromStorage(decompressed);
	}

	private static TreeMap<Long, Long> restoreFromStorage(String decompressed)
	{
		return BACKUP_METHOD.equals(GSON) ? restoreFromGSONStorage(decompressed) : restoreFromLinesStorage(decompressed);
	}

	private static byte[] decompressBytes(byte[] bytes) throws IOException, DataFormatException
	{
		switch (COMPRESSION_METHOD)
		{
			case DEFLATER:
				return DeflaterCompression.decompress(bytes);

			case LZ4:
				return LZ4Compression.decompress(bytes);

			case NONE:
				return bytes;
		}

		throw new IllegalStateException("No compression method associated");
	}

	private static TreeMap<Long, Long> restoreFromLinesStorage(String input)
	{
		val lines = input.split(NEW_LINE_DELIMITER);
		val offsetValuePairs = new TreeMap<Long, Long>();
		for (val line : lines)
		{
			val lineComponents = line.split(OFFSET_VALUE_DELIMITER);
			val offset = parseUnsignedLong(lineComponents[0]);
			val value = parseUnsignedLong(lineComponents[1]);
			offsetValuePairs.put(offset, value);
		}

		return offsetValuePairs;
	}
}
