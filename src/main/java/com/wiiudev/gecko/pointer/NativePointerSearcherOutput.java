package com.wiiudev.gecko.pointer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class NativePointerSearcherOutput
{
	@Getter
	private String exceptionMessage;

	@Getter
	private String processOutput;
}
