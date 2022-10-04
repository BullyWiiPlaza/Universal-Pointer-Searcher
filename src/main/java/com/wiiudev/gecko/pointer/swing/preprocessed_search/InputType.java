package com.wiiudev.gecko.pointer.swing.preprocessed_search;

import lombok.val;

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

	public static InputType parseInputType(final String text)
	{
		for (val inputType : values())
		{
			if (inputType.toString().equals(text))
			{
				return inputType;
			}
		}

		return null;
	}
}
