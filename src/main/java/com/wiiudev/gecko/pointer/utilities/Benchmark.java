package com.wiiudev.gecko.pointer.utilities;

import lombok.val;

import static java.lang.Math.*;
import static java.lang.System.*;

public class Benchmark
{
	private long startingTime;
	private boolean started;

	public Benchmark()
	{
		started = false;
	}

	public void start()
	{
		started = true;
		startingTime = getSystemTime();
	}

	private long getSystemTime()
	{
		return nanoTime();
	}

	/**
	 * @return The elapsed time in seconds
	 */
	public double getElapsedTime()
	{
		if (!started)
		{
			throw new IllegalStateException("Not started!");
		}

		val elapsedTime = getSystemTime() - startingTime;
		return (double) elapsedTime / pow(10, 9);
	}
}
