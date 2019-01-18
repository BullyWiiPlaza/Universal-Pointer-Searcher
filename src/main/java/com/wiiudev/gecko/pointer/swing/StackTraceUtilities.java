package com.wiiudev.gecko.pointer.swing;

import lombok.val;

import javax.swing.*;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeLater;

public class StackTraceUtilities
{
	private static String toString(Exception exception)
	{
		val stringBuilder = new StringBuilder(exception.toString() + "\n");
		for (val stackTraceElement : exception.getStackTrace())
		{
			stringBuilder.append("\n\tat ");
			stringBuilder.append(stackTraceElement);
		}

		return stringBuilder.toString();
	}

	private static final int MAXIMUM_CHARACTERS_COUNT = 1500;

	private static String truncateStackTrace(Exception exception)
	{
		var stackTrace = toString(exception);

		if (stackTrace.length() > MAXIMUM_CHARACTERS_COUNT)
		{
			val lastIndex = stackTrace.indexOf("\n", MAXIMUM_CHARACTERS_COUNT);
			stackTrace = stackTrace.substring(0, lastIndex) + "\n[...]";
		}

		return stackTrace;
	}

	public static void handleException(JRootPane rootPane, Exception exception)
	{
		exception.printStackTrace();
		val stackTrace = truncateStackTrace(exception);

		invokeLater(() ->
				showMessageDialog(rootPane,
						stackTrace,
						"Error",
						ERROR_MESSAGE));
	}
}
