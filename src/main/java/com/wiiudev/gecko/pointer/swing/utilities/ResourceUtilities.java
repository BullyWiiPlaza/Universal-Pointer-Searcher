package com.wiiudev.gecko.pointer.swing.utilities;

import lombok.val;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class ResourceUtilities
{
	public static String resourceToString(String filePath) throws IOException
	{
		try (val inputStream = ResourceUtilities.class.getClassLoader().getResourceAsStream(filePath))
		{
			return inputStreamToString(inputStream);
		}
	}

	private static String inputStreamToString(InputStream inputStream)
	{
		try (val scanner = new Scanner(inputStream).useDelimiter("\\A"))
		{
			return scanner.hasNext() ? scanner.next() : "";
		}
	}
}
