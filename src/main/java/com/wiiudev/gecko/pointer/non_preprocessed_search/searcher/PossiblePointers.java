package com.wiiudev.gecko.pointer.non_preprocessed_search.searcher;

import com.wiiudev.gecko.pointer.non_preprocessed_search.pointer.PointerAddressRange;
import lombok.val;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.wiiudev.gecko.pointer.non_preprocessed_search.searcher.CollectionsUtils.longListToString;
import static java.lang.Long.toHexString;

public class PossiblePointers extends HashMap<Long, List<Long>>
{
	private final PointerAddressRange pointerAddressRange;
	private final List<ByteBuffer> byteBuffers;

	public PossiblePointers(List<ByteBuffer> byteBuffers, PointerAddressRange pointerAddressRange)
	{
		super();
		this.pointerAddressRange = pointerAddressRange;
		this.byteBuffers = byteBuffers;
	}

	void populatePossiblePointers()
	{
		val byteBuffer = byteBuffers.get(0);

		while (byteBuffer.hasRemaining())
		{
			addPossiblePointer(byteBuffer);
		}
	}

	private void addPossiblePointer(ByteBuffer byteBuffer)
	{
		val offset = (long) byteBuffer.position();
		val value = (long) byteBuffer.getInt();
		val validOffset = isValidPointerOffset(value);

		if (validOffset)
		{
			val values = new ArrayList<Long>();
			values.add(value);

			val additionalValues = getValidPointerValues(offset);

			if (additionalValues != null)
			{
				values.addAll(additionalValues);

			}
			if (additionalValues != null || byteBuffers.size() == 1)
			{
				put(offset, values);
			}
		}
	}

	private List<Long> getValidPointerValues(long offset)
	{
		val values = new ArrayList<Long>();
		val memoryDumpBuffer = byteBuffers.get(0);

		memoryDumpBuffer.position((int) offset);
		val value = (long) memoryDumpBuffer.getInt();

		if (isValidPointerOffset(value))
		{
			values.add(value);
		} else
		{
			// At least one of the memory dumps failed
			return null;
		}

		// All good
		return values;
	}

	private boolean isValidPointerOffset(long value)
	{
		return pointerAddressRange.contains(value);
	}

	@Override
	public String toString()
	{
		val stringBuilder = new StringBuilder();
		val iterator = entrySet().iterator();

		while (iterator.hasNext())
		{
			val entry = iterator.next();
			stringBuilder.append("0x");
			stringBuilder.append(toHexString(entry.getKey()).toUpperCase());
			stringBuilder.append(" = ");

			stringBuilder.append(longListToString(entry.getValue()));

			if (iterator.hasNext())
			{
				stringBuilder.append(',').append(' ');
			}
		}

		return stringBuilder.toString();
	}
}
