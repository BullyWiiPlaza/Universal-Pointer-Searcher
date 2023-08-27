package com.wiiudev.gecko.pointer.swing.preprocessed_search;

import lombok.Getter;
import lombok.val;

@Getter
public enum FileTypeImport
{
	MEMORY_DUMP("Memory Dump", "bin"),
	POINTER_MAP("Pointer Map", "pointermap");

	public static final String MEMORY_DUMP_EXTENSION_DMP = "dmp";
	public static final String MEMORY_DUMP_EXTENSION_RAW = "raw";

	private final String extension;

	private final String value;

	FileTypeImport(String value, String extension)
	{
		this.value = value;
		this.extension = extension;
	}

	public static FileTypeImport parseFileTypeImport(final String name)
	{
		val values = values();
		for (val value : values)
		{
			if (value.toString().equals(name))
			{
				return value;
			}
		}

		return null;
	}

	public static FileTypeImport parseFileTypeImportByFilePath(String filePath)
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
