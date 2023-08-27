package com.wiiudev.gecko.pointer.swing.utilities;

import lombok.val;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

import static com.wiiudev.gecko.pointer.swing.StackTraceUtilities.handleException;

public class FrameUtilities
{
	public static void setWindowIconImage(final Window window)
	{
		try
		{
			val imageInputStream = window.getClass().getResourceAsStream("/Icon.png");
			if (imageInputStream == null)
			{
				throw new IOException("Window icon resource not found");
			}

			val bufferedImage = ImageIO.read(imageInputStream);
			window.setIconImage(bufferedImage);
		} catch (IOException exception)
		{
			handleException(null, exception);
		}
	}

	public static <T> T getSelectedItem(JComboBox<T> comboBox)
	{
		val index = comboBox.getSelectedIndex();
		return comboBox.getItemAt(index);
	}
}
