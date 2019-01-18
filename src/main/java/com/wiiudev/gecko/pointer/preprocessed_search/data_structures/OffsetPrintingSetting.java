package com.wiiudev.gecko.pointer.preprocessed_search.data_structures;

public enum OffsetPrintingSetting
{
	SIGNED("Signed"),
	UNSIGNED("Unsigned");

	private final String text;

	OffsetPrintingSetting(String text)
	{
		this.text = text;
	}

	public String toString()
	{
		return text;
	}
}