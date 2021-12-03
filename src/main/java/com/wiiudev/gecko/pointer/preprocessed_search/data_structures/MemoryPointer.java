package com.wiiudev.gecko.pointer.preprocessed_search.data_structures;

import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.wiiudev.gecko.pointer.preprocessed_search.utilities.DataConversions.toHexadecimal;
import static java.lang.Long.parseUnsignedLong;
import static java.lang.System.lineSeparator;
import static org.apache.commons.lang3.StringUtils.countMatches;

@RequiredArgsConstructor
@EqualsAndHashCode
public class MemoryPointer
{
	private static final String HEXADECIMAL_HEADER = "0x";
	private static final String CLOSING_BRACKET = "]";
	private static final String OPENING_BRACKET = "[";
	private static final String CLOSING_ROUNDED_BRACKET = ")";
	private static final String EQUALS_SIGN = "=";
	public static final String PLUS_SIGN = "+";

	@Getter
	@Setter
	private String baseModuleNameWithOffset;

	@Getter
	@Setter
	private long baseAddress;

	@Getter
	@Setter
	private long[] offsets;

	public MemoryPointer(final String baseModuleNameWithOffset, final long[] offsets)
	{
		this.baseModuleNameWithOffset = baseModuleNameWithOffset;
		this.offsets = offsets;
	}

	public MemoryPointer(final long baseAddress, final long[] offsets)
	{
		this.baseAddress = baseAddress;
		this.offsets = offsets;
	}

	public MemoryPointer(String text)
	{
		text = text.replace(" ", "");

		val firstOpeningBracketIndex = text.indexOf(OPENING_BRACKET);
		val firstClosingBracketIndex = text.indexOf(CLOSING_BRACKET);
		val baseAddressExpression = text.substring(firstOpeningBracketIndex + 1,
				firstClosingBracketIndex);
		if (baseAddressExpression.contains(" "))
		{
			val baseAddressStartIndex = baseAddressExpression.indexOf(EQUALS_SIGN) + 2 + HEXADECIMAL_HEADER.length();
			val baseAddressEndIndex = baseAddressExpression.indexOf(CLOSING_ROUNDED_BRACKET);
			val baseAddressString = baseAddressExpression.substring(baseAddressStartIndex, baseAddressEndIndex);
			baseAddress = parseUnsignedLong(baseAddressString, 16);
		} else
		{
			parseBaseAddress(text);
		}

		val pointerDepth = countMatches(text, OPENING_BRACKET);
		var previousClosingBracketIndex = text.indexOf(CLOSING_BRACKET);
		var pointerDepthIndex = 0;
		offsets = new long[pointerDepth];
		while (pointerDepthIndex < pointerDepth)
		{
			var innerClosingIndex = text.indexOf(CLOSING_BRACKET, previousClosingBracketIndex + 1);
			if (innerClosingIndex == -1)
			{
				innerClosingIndex = text.length();
			}
			val pointerOffsetString = text.substring(previousClosingBracketIndex + 1, innerClosingIndex);
			val isHexadecimalOffset = pointerOffsetString.contains(HEXADECIMAL_HEADER);
			val beginIndex = 1 + (isHexadecimalOffset ? HEXADECIMAL_HEADER.length() : 0);
			var pointerOffset = parseUnsignedLong(pointerOffsetString.substring(beginIndex), isHexadecimalOffset ? 16 : 10);
			if (pointerOffsetString.startsWith("-"))
			{
				pointerOffset *= -1;
			}
			offsets[pointerDepthIndex] = pointerOffset;
			previousClosingBracketIndex = innerClosingIndex;
			pointerDepthIndex++;
		}
	}

	private void parseBaseAddress(final String memoryPointerLine)
	{
		var addressExpressionStartIndex = 0;
		while (addressExpressionStartIndex < memoryPointerLine.length())
		{
			if (!(memoryPointerLine.charAt(addressExpressionStartIndex) + "").equals(OPENING_BRACKET))
			{
				break;
			}
			addressExpressionStartIndex++;
		}

		var addressExpressionEndIndex = addressExpressionStartIndex + 1;
		while (addressExpressionEndIndex < memoryPointerLine.length())
		{
			if ((memoryPointerLine.charAt(addressExpressionEndIndex) + "").equals(CLOSING_BRACKET))
			{
				break;
			}
			addressExpressionEndIndex++;
		}

		val addressExpression = memoryPointerLine.substring(addressExpressionStartIndex, addressExpressionEndIndex);
		if (addressExpression.contains(PLUS_SIGN))
		{
			val splitComponents = addressExpression.split("\\" + PLUS_SIGN);
			if (splitComponents.length != 2)
			{
				throw new IllegalStateException("Unexpected split component count: " + splitComponents.length);
			}
			baseModuleNameWithOffset = splitComponents[0] + " " + PLUS_SIGN + " " + splitComponents[1];
		} else
		{
			if (addressExpression.startsWith(HEXADECIMAL_HEADER))
			{
				val hexadecimalAddressWithoutPrefix = addressExpression.substring(HEXADECIMAL_HEADER.length());
				baseAddress = parseUnsignedLong(hexadecimalAddressWithoutPrefix, 16);
			} else
			{
				baseAddress = parseUnsignedLong(addressExpression, 16);
			}
		}
	}

	public boolean reachesDestination(Map<Long, Long> pointerMap,
	                                  long targetAddress,
	                                  long startingOffset,
	                                  boolean excludeCycles)
	{
		val destinationAddress = followPointer(pointerMap, startingOffset, excludeCycles, false);
		if (destinationAddress == null)
		{
			return false;
		}

		return destinationAddress == targetAddress;
	}

	@Override
	public String toString()
	{
		return toString(true, Long.BYTES);
	}

	public String toString(boolean signedOffsets, int addressSize)
	{
		val pointerBuilder = new StringBuilder();

		for (val ignored : offsets)
		{
			pointerBuilder.append(OPENING_BRACKET);
		}

		if (baseModuleNameWithOffset != null)
		{
			pointerBuilder.append(baseModuleNameWithOffset);
		} else
		{
			val formattedBaseAddress = toHexadecimal(baseAddress, addressSize, false);
			pointerBuilder.append(formattedBaseAddress);
		}

		pointerBuilder.append(CLOSING_BRACKET + " ");

		for (var offsetsIndex = 0; offsetsIndex < offsets.length; offsetsIndex++)
		{
			var offset = offsets[offsetsIndex];
			val isNegative = offset < 0;

			if (isNegative && signedOffsets)
			{
				val integerMaxValue = Integer.MAX_VALUE + Math.abs(Integer.MIN_VALUE);
				offset = integerMaxValue - offset + 1;
				pointerBuilder.append("-");
			} else
			{
				pointerBuilder.append("+");
			}

			pointerBuilder.append(" ");
			val formattedOffset = toHexadecimal(offset, addressSize, false);
			pointerBuilder.append(formattedOffset);

			if (offsetsIndex != offsets.length - 1)
			{
				pointerBuilder.append(CLOSING_BRACKET + " ");
			}
		}

		return pointerBuilder.toString();
	}

	public static String toString(List<MemoryPointer> memoryPointers,
	                              int addressSize, OffsetPrintingSetting offsetPrintingSetting)
	{
		val stringBuilder = new StringBuilder();

		var index = 0;
		for (val memoryPointer : memoryPointers)
		{
			val signedPointerOffsets = offsetPrintingSetting.equals(OffsetPrintingSetting.SIGNED);
			val string = memoryPointer.toString(signedPointerOffsets, addressSize);
			stringBuilder.append(string);

			if (index != memoryPointers.size() - 1)
			{
				stringBuilder.append(lineSeparator());
			}

			{
				index++;
			}
		}

		return stringBuilder.toString().trim();
	}

	public Long followPointer(Map<Long, Long> pointerMap,
	                          long startingOffset,
	                          boolean excludeCycles,
	                          boolean returnOffset)
	{
		var currentBaseOffset = baseAddress - startingOffset;
		val baseAddressesHashSet = new HashSet<Long>();
		var hasOffset = pointerMap.containsKey(currentBaseOffset);

		// Has the address been found?
		if (hasOffset)
		{
			baseAddressesHashSet.add(currentBaseOffset);

			// Read values and apply offsets
			for (var offsetsIndex = 0; offsetsIndex < offsets.length; offsetsIndex++)
			{
				val pointerOffset = offsets[offsetsIndex];
				val value = pointerMap.get(currentBaseOffset);

				if (returnOffset && offsetsIndex == offsets.length - 1)
				{
					return value;
				}

				currentBaseOffset = value + pointerOffset;

				val successfullyAdded = baseAddressesHashSet.add(currentBaseOffset - startingOffset);
				if (excludeCycles && !successfullyAdded)
				{
					return null;
				}

				if (offsetsIndex == offsets.length - 1)
				{
					break;
				}

				currentBaseOffset -= startingOffset;
				hasOffset = pointerMap.containsKey(currentBaseOffset);

				if (!hasOffset)
				{
					// Bad address, not a possible pointer
					return null;
				}
			}

			// Does the current base address reach the target address now?
			return currentBaseOffset;
		}

		return null;
	}
}
