package com.wiiudev.gecko.pointer.swing.utilities;

import lombok.val;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static com.wiiudev.gecko.pointer.swing.StackTraceUtilities.handleException;

public class FrameUtilities
{
	public static void setWindowIconImage(Window window)
	{
		try
		{
			InputStream imageInputStream = window.getClass().getResourceAsStream("/Icon.png");
			BufferedImage bufferedImage = ImageIO.read(imageInputStream);
			window.setIconImage(bufferedImage);
		}
		catch(IOException exception)
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
