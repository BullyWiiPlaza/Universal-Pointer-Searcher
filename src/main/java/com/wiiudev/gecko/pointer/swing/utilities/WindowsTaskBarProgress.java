package com.wiiudev.gecko.pointer.swing.utilities;

import lombok.val;
import org.bridj.Pointer;
import org.bridj.cpp.com.COMRuntime;
import org.bridj.cpp.com.shell.ITaskbarList3;

import java.awt.*;

import static org.apache.commons.lang3.SystemUtils.*;
import static org.bridj.Pointer.pointerToAddress;
import static org.bridj.jawt.JAWTUtils.getNativePeerHandle;

public class WindowsTaskBarProgress
{
	private static final int DEFAULT_MAXIMUM_VALUE = 100;

	private ITaskbarList3 taskBarList3;
	private Pointer<Integer> pointer;
	private long maximum;

	public WindowsTaskBarProgress(Component component) throws ClassNotFoundException
	{
		if (isSupportedPlatform())
		{
			taskBarList3 = COMRuntime.newInstance(ITaskbarList3.class);
			val nativePeerHandle = getNativePeerHandle(component);
			Pointer.Releaser release = pointer -> {
			};

			pointer = pointerToAddress(nativePeerHandle, Integer.class, release);
		}

		this.maximum = DEFAULT_MAXIMUM_VALUE;
	}

	public void setProgressValue(long value)
	{
		if (isSupportedPlatform())
		{
			taskBarList3.SetProgressValue(pointer, value, maximum);
		}
	}

	private static boolean isSupportedPlatform()
	{
		return IS_OS_WINDOWS_7
				|| IS_OS_WINDOWS_8
				|| IS_OS_WINDOWS_10;
	}

	public void setMaximum(long maximum)
	{
		this.maximum = maximum;
	}
}
