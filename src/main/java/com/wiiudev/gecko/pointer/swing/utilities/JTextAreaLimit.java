package com.wiiudev.gecko.pointer.swing.utilities;

import lombok.val;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class JTextAreaLimit extends PlainDocument
{
	private int lengthLimit;

	public JTextAreaLimit(int lengthLimit)
	{
		super();
		this.lengthLimit = lengthLimit;
	}

	public JTextAreaLimit()
	{
		this(8);
	}

	public void insertString(int offset, String input, AttributeSet attributeSet) throws BadLocationException
	{
		input = input.toUpperCase();

		val limitHeld = getLength() + input.length() <= lengthLimit;

		if (limitHeld && isHexadecimal(input))
		{
			super.insertString(offset, input, attributeSet);
		}
	}

	public static boolean isHexadecimal(String input)
	{
		return input.matches("[0-9A-F]+");
	}
}
