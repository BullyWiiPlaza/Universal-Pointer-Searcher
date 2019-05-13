package com.wiiudev.gecko.pointer.swing;

import lombok.val;

import javax.swing.*;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeLater;

public class StackTraceUtilities
{
	private static String toString(Throwable throwable)
	{
		val stringBuilder = new StringBuilder(throwable.toString() + "\n");
		for (val stackTraceElement : throwable.getStackTrace())
		{
			stringBuilder.append("\n\tat ");
			stringBuilder.append(stackTraceElement);
		}

		return stringBuilder.toString();
	}

	private static final int MAXIMUM_CHARACTERS_COUNT = 1500;

	private static String truncateStackTrace(Throwable throwable)
	{
		var stackTrace = toString(throwable);
		if (stackTrace.length() > MAXIMUM_CHARACTERS_COUNT)
		{
			val lastIndex = stackTrace.indexOf("\n", MAXIMUM_CHARACTERS_COUNT);
			stackTrace = stackTrace.substring(0, lastIndex) + "\n[...]";
		}

		return stackTrace;
	}

	public static void handleException(JRootPane rootPane, Throwable throwable)
	{
		throwable.printStackTrace();
		val stackTrace = truncateStackTrace(throwable);
		invokeLater(() -> showMessageDialog(rootPane, stackTrace, "Error", ERROR_MESSAGE));
	}
}
