package com.wiiudev.gecko.pointer.swing.preprocessed_search;

import lombok.Getter;
import lombok.val;

public enum FileTypeImport
{
	MEMORY_DUMP("Memory Dump", "bin"),
	POINTER_MAP("Pointer Map", "pointermap");

	@Getter
	private final String extension;

	private final String value;

	FileTypeImport(String value, String extension)
	{
		this.value = value;
		this.extension = extension;
	}

	public static FileTypeImport parseFileTypeImport(String filePath)
	{
		val values = values();
		for (val value : values)
		{
			val selectedExtension = value.getExtension();
			if (filePath.endsWith(selectedExtension))
			{
				return value;
			}
		}

		return null;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
