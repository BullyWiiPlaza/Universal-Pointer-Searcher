package com.wiiudev.gecko.pointer.preprocessed_search.data_structures;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;

import static com.wiiudev.gecko.pointer.preprocessed_search.utilities.DataConversions.toHexadecimal;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class OffsetValuePair implements Comparable<OffsetValuePair>, Comparator<OffsetValuePair>
{
	private final int offset;

	@Getter
	private final int value;

	public String toString()
	{
		return "*" + toHexadecimal(offset)
				+ " = "
				+ toHexadecimal(value);
	}

	@Override
	public int compareTo(OffsetValuePair offsetValuePair)
	{
		return offset - offsetValuePair.getOffset();
	}

	@Override
	public int compare(OffsetValuePair firstOffsetValuePair, OffsetValuePair secondOffsetValuePair)
	{
		return firstOffsetValuePair.getOffset() - secondOffsetValuePair.getOffset();
	}
}
