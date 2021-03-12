package com.wiiudev.gecko.pointer.swing.preprocessed_search;

public enum InputType
{
	INITIAL("Initial"),
	COMPARISON("Comparison");

	private final String renderedText;

	InputType(String renderedText)
	{
		this.renderedText = renderedText;
	}

	@Override
	public String toString()
	{
		return renderedText;
	}
}
