package com.wiiudev.gecko.pointer.swing;

import lombok.Getter;
import lombok.val;

public enum TargetSystem
{
	MICROSOFT_WINDOWS("Microsoft Windows"),
	NINTENDO_3DS("Nintendo 3DS"),
	NINTENDO_64("Nintendo 64"),
	NINTENDO_DS("Nintendo DS"),
	NINTENDO_SWITCH("Nintendo Switch"),
	NINTENDO_WII("Nintendo Wii"),
	NINTENDO_WIIU("Nintendo Wii U"),
	PLAYSTATION_3("Playstation 3");

	@Getter
	private final String value;

	TargetSystem(final String value)
	{
		this.value = value;
	}

	public static TargetSystem parseTargetSystem(final String targetSystemText)
	{
		for (val currentTargetSystem : values())
		{
			if (currentTargetSystem.getValue().equals(targetSystemText))
			{
				return currentTargetSystem;
			}
		}

		return null;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
