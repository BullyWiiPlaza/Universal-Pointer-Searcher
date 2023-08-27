package com.wiiudev.gecko.pointer.swing;

import lombok.Getter;
import lombok.val;

@Getter
public enum TargetSystem
{
	TRIFORCE("Triforce"),
	NES("NES"),
	GAME_BOY("Game Boy"),
	GAME_BOY_COLOR("Game Boy Color"),
	GAME_BOY_ADVANCE("Game Boy Advance"),
	SNES("SNES"),
	NINTENDO_GAMECUBE("Nintendo GameCube"),
	NINTENDO_WII("Nintendo Wii"),
	NINTENDO_WIIU("Nintendo Wii U"),
	NINTENDO_DS("Nintendo DS"),
	NINTENDO_3DS("Nintendo 3DS"),
	NINTENDO_64("Nintendo 64"),
	NINTENDO_SWITCH("Nintendo Switch"),
	PLAYSTATION_1("Playstation 1"),
	PLAYSTATION_2("Playstation 2"),
	PLAYSTATION_3("Playstation 3"),
	PLAYSTATION_4("Playstation 4"),
	PLAYSTATION_5("Playstation 5"),
	PLAYSTATION_PORTABLE("Playstation Portable"),
	PLAYSTATION_VITA("Playstation Vita"),
	XBOX("Xbox"),
	XBOX_ONE("Xbox One"),
	XBOX_SERIES("Xbox Series"),
	SEGA_CD("Sega CD"),
	SEGA_SATURN("Sega Saturn"),
	SEGA_DREAMCAST("Sega Dreamcast"),
	MICROSOFT_WINDOWS_X86("Microsoft Windows x86"),
	MICROSOFT_WINDOWS_X64("Microsoft Windows x64");

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
