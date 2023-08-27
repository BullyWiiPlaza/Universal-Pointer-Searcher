package com.wiiudev.gecko.pointer.preprocessed_search;

import lombok.val;
import oshi.SystemInfo;

class OSMemory
{
	private static double usedMemoryPercentage;
	private static boolean runUsedMemoryGetterThread;

	private static double getUsedMemory()
	{
		val systemInfo = new SystemInfo();
		val hardware = systemInfo.getHardware();
		val memory = hardware.getMemory();
		val availableMemory = memory.getAvailable();
		val totalMemory = memory.getTotal();
		return (totalMemory - availableMemory) / (double) totalMemory;
	}

	static void stopUsedMemorySetter()
	{
		runUsedMemoryGetterThread = false;
	}

	static double getUsedMemoryPercentage()
	{
		return usedMemoryPercentage;
	}

	static void runUsedMemorySetterThread()
	{
		runUsedMemoryGetterThread = true;

		val thread = new Thread(() ->
		{
			while (runUsedMemoryGetterThread)
			{
				usedMemoryPercentage = getUsedMemory();

				try
				{
					//noinspection BusyWait
					Thread.sleep(100);
				} catch (InterruptedException exception)
				{
					exception.printStackTrace();
				}
			}
		}, "Used Memory Setter Thread");

		thread.start();
	}
}
