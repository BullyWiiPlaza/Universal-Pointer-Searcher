package com.wiiudev.gecko.pointer.swing.utilities;

import lombok.val;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import static com.wiiudev.gecko.pointer.swing.utilities.TextAreaLimitType.HEXADECIMAL;

public class JTextAreaLimit extends PlainDocument
{
	private int lengthLimit;
	private final TextAreaLimitType textAreaLimitType;

	public JTextAreaLimit(int lengthLimit, TextAreaLimitType textAreaLimitType)
	{
		super();
		this.lengthLimit = lengthLimit;
		this.textAreaLimitType = textAreaLimitType;
	}

	public JTextAreaLimit()
	{
		this(8, HEXADECIMAL);
	}

	public void insertString(int offset, String input, AttributeSet attributeSet) throws BadLocationException
	{
		input = input.toUpperCase();
		val limitHeld = getLength() + input.length() <= lengthLimit;

		if (limitHeld && isAccepted(input))
		{
			super.insertString(offset, input, attributeSet);
		}
	}

	private boolean isAccepted(String input)
	{
		switch (textAreaLimitType)
		{
			case HEXADECIMAL:
				return isHexadecimal(input);

			case NUMERIC:
				return isNumeric(input);

			default:
				throw new IllegalStateException("Illegal text area limit type: " + textAreaLimitType);
		}
	}

	private static boolean isNumeric(String input)
	{
		return input.matches("[0-9]+");
	}

	public static boolean isHexadecimal(String input)
	{
		return input.matches("[0-9A-F]+");
	}
}
