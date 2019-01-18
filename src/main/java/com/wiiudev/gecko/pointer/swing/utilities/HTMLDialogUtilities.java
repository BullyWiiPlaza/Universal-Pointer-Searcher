package com.wiiudev.gecko.pointer.swing.utilities;

import lombok.val;

import javax.swing.*;
import java.awt.*;

import static com.wiiudev.gecko.pointer.swing.StackTraceUtilities.handleException;
import static javax.swing.event.HyperlinkEvent.EventType.ACTIVATED;

public class HTMLDialogUtilities
{
	public static void addHyperLinkListener(JEditorPane editorPane)
	{
		editorPane.addHyperlinkListener(hyperlinkListener ->
		{
			if (ACTIVATED.equals(hyperlinkListener.getEventType()))
			{
				val desktop = Desktop.getDesktop();

				try
				{
					desktop.browse(hyperlinkListener.getURL().toURI());
				} catch (Exception exception)
				{
					handleException(null, exception);
				}
			}
		});
	}
}
