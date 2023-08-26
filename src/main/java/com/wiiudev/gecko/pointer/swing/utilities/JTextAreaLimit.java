package com.wiiudev.gecko.pointer.swing.utilities;

import lombok.val;
import lombok.var;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import static com.wiiudev.gecko.pointer.swing.utilities.TextAreaLimitType.HEXADECIMAL;

public class JTextAreaLimit extends PlainDocument
{
	private final int lengthLimit;
	private final TextAreaLimitType textAreaLimitType;
	private final boolean allowNegative;

	public JTextAreaLimit(final int lengthLimit, final TextAreaLimitType textAreaLimitType,
	                      final boolean allowNegative)
	{
		super();
		this.lengthLimit = lengthLimit;
		this.textAreaLimitType = textAreaLimitType;
		this.allowNegative = allowNegative;
	}

	public JTextAreaLimit()
	{
		this(Long.BYTES * 2, HEXADECIMAL, false);
	}

	public void insertString(final int offset, String input,
	                         final AttributeSet attributeSet) throws BadLocationException
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
		val minusPrefix = "-";
		if (allowNegative && input.startsWith(minusPrefix))
		{
			input = input.substring(minusPrefix.length());
		}

		switch (textAreaLimitType)
		{
			case HEXADECIMAL:
				return isHexadecimal(input);

			case HEXADECIMAL_WITH_COMMAS_AND_SPACES:
				return isHexadecimalSpaceOrComma(input);

			case HEXADECIMAL_WITH_COMMAS_DASHES_AND_SPACES:
				return isHexadecimalSpaceDashOrComma(input);

			case NUMERIC:
				return isNumeric(input);

			default:
				throw new IllegalStateException("Illegal text area limit type: " + textAreaLimitType);
		}
	}

	private boolean isHexadecimalSpaceDashOrComma(final String input)
	{
		for (var inputCharacter : input.toCharArray())
		{
			if (!(isHexadecimal(inputCharacter + "") || inputCharacter == ' '
			      || inputCharacter == ',' || inputCharacter == '-'))
			{
				return false;
			}
		}

		return true;
	}

	private boolean isHexadecimalSpaceOrComma(final String input)
	{
		for (var inputCharacter : input.toCharArray())
		{
			if (!(isHexadecimal(inputCharacter + "") || inputCharacter == ' ' || inputCharacter == ','))
			{
				return false;
			}
		}

		return true;
	}

	private static boolean isNumeric(String input)
	{
		return input.isEmpty() || input.matches("[0-9]+");
	}

	public static boolean isHexadecimal(String input)
	{
		return input.isEmpty() || input.matches("[0-9A-F]+");
	}
}
