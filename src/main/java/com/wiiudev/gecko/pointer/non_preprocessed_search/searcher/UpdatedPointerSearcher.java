package com.wiiudev.gecko.pointer.non_preprocessed_search.searcher;

import com.wiiudev.gecko.pointer.non_preprocessed_search.MemoryDump;
import com.wiiudev.gecko.pointer.non_preprocessed_search.pointer.MemoryPointer;
import com.wiiudev.gecko.pointer.non_preprocessed_search.pointer.PointerAddressRange;
import com.wiiudev.gecko.pointer.non_preprocessed_search.pointer.PointerOffsetChecker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public abstract class UpdatedPointerSearcher
{
	private boolean allowPointerInPointers;
	private PointerAddressRange pointerAddressRange;
	private long memoryDumpStartingOffset;
	private List<MemoryDump> memoryDumps;
	private PossiblePointers possiblePointers;
	private List<Long> possiblePointerAddresses;
	private MemoryDump memoryDump;
	private ByteOrder byteOrder;
	private PointerOffsetChecker pointerOffsetChecker;
	private List<ByteBuffer> memoryDumpBuffers;

	public UpdatedPointerSearcher(List<MemoryDump> memoryDumps, int memoryDumpStartingOffset, ByteOrder byteOrder) throws IOException
	{
		this.memoryDumpStartingOffset = memoryDumpStartingOffset;
		this.byteOrder = byteOrder;
		pointerOffsetChecker = new PointerOffsetChecker();
		memoryDump = memoryDumps.get(0);
		this.memoryDumps = memoryDumps;
		memoryDumpBuffers = new ArrayList<>();
		allowPointerInPointers = false;

		for (MemoryDump memoryDump : memoryDumps)
		{
			ByteBuffer byteBuffer = memoryDump.getByteBuffer();
			byteBuffer.order(byteOrder);
			memoryDumpBuffers.add(byteBuffer);
		}
	}

	public void setAllowPointerInPointers(boolean allowPointerInPointers)
	{
		this.allowPointerInPointers = allowPointerInPointers;
	}

	public ByteOrder getByteOrder()
	{
		return byteOrder;
	}

	public void setByteOrder(ByteOrder byteOrder)
	{
		this.byteOrder = byteOrder;
	}

	public void setMemoryDumpStartingOffset(int memoryDumpStartingOffset)
	{
		this.memoryDumpStartingOffset = memoryDumpStartingOffset;
	}

	public PointerOffsetChecker getPointerOffsetChecker()
	{
		return pointerOffsetChecker;
	}

	public void setPointerOffsetChecker(PointerOffsetChecker pointerOffsetChecker)
	{
		this.pointerOffsetChecker = pointerOffsetChecker;
	}

	@SuppressWarnings("unchecked")
	public void performPointerSearch() throws IOException
	{
		pointerAddressRange = new PointerAddressRange(memoryDumpStartingOffset, memoryDump);
		possiblePointers = new PossiblePointers(memoryDumpBuffers, pointerAddressRange);
		possiblePointers.populatePossiblePointers();
		System.out.println(possiblePointers.size());

		if (allowPointerInPointers)
		{
			// Sorted base addresses
			Set<Long> offsets = possiblePointers.keySet();
			possiblePointerAddresses = CollectionsUtils.toSortedList(offsets);

			Collection<List<Long>> memoryDumpsHorizontalValues = possiblePointers.values();
			List<List<Long>> memoryDumpsVerticalValues = new LinkedList<>();

			for (int memoryDumpsIndex = 0; memoryDumpsIndex < memoryDumps.size(); memoryDumpsIndex++)
			{
				memoryDumpsVerticalValues.add(new LinkedList<>());
			}

			for (List<Long> verticalValues : memoryDumpsHorizontalValues)
			{
				for (int valuesListIndex = 0; valuesListIndex < verticalValues.size(); valuesListIndex++)
				{
					List<Long> verticalValuesList = memoryDumpsVerticalValues.get(valuesListIndex);
					verticalValuesList.add(verticalValues.get(valuesListIndex));
				}
			}

			memoryDumpsVerticalValues.forEach(Collections::sort);

			for (int baseAddressesIndex = 0; baseAddressesIndex < possiblePointerAddresses.size(); baseAddressesIndex++)
			{
				long baseAddress = possiblePointerAddresses.get(baseAddressesIndex);

				if (baseAddress == 0x4293D0)
				{
					// System.out.println(CollectionsUtils.longListToString(possiblePointerAddresses));
					// System.out.println("0x" + Long.toHexString(baseAddress).toUpperCase());
					List<Long> values = possiblePointers.get(baseAddress);
					// System.out.println(CollectionsUtils.longListToString(values));
					long maximumOffset = pointerOffsetChecker.getMaximumOffset();

					long theValue = values.get(0) - memoryDumpStartingOffset;
					long decreasedBaseAddress = theValue - maximumOffset;

					if (decreasedBaseAddress < 0 || pointerOffsetChecker.allowsPositive())
					{
						decreasedBaseAddress = 0;
					}

					System.out.println("Decreased: " +Long.toHexString(decreasedBaseAddress).toUpperCase());

					long increasedBaseAddress = theValue + maximumOffset;

					long bytesCount = memoryDumps.get(0).getBytesCount();
					if (increasedBaseAddress >= memoryDumpStartingOffset + bytesCount)
					{
						increasedBaseAddress = bytesCount - 4;
					}

					System.out.println("Increased: " +Long.toHexString(increasedBaseAddress).toUpperCase());

					List<Long> containedPointerAddresses = new LinkedList<>();

					for (int i = baseAddressesIndex; i < possiblePointerAddresses.size(); i++)
					{
						long possiblePointerAddress = possiblePointerAddresses.get(i);

						if (possiblePointerAddress >= decreasedBaseAddress && possiblePointerAddress < increasedBaseAddress)
						{
							containedPointerAddresses.add(possiblePointerAddress);
						}
						else
						{
							break;
						}
					}

					System.out.println(CollectionsUtils.longListToString(containedPointerAddresses));

					for (long possiblePointerAddress : containedPointerAddresses)
					{
						Set<Long> uniqueInnerOffset = new HashSet<>();
						values = possiblePointers.get(possiblePointerAddress);

						for (int i = 0; i < values.size(); i++)
						{
							long value = values.get(0) - memoryDumpStartingOffset;
							long innerOffset = possiblePointerAddress - value;
							uniqueInnerOffset.add(innerOffset);
						}

						if (uniqueInnerOffset.size() == 1)
						{
							System.out.println(Long.toHexString(uniqueInnerOffset.iterator().next()).toUpperCase());
						}
					}

					/*for (int memoryDumpsIndex = 0; memoryDumpsIndex < memoryDumpsVerticalValues.size(); memoryDumpsIndex++)
					{
						List<Long> verticalValues = memoryDumpsVerticalValues.get(memoryDumpsIndex);
						System.out.println(CollectionsUtils.longListToString(verticalValues));
						long value = values.get(memoryDumpsIndex);
						long addr = value - memoryDumpStartingOffset;
						int index = possiblePointerAddresses.indexOf(addr);
						System.out.println("Index: " + index);
						List<Long> containedOffsets = new LinkedList<>();

						for (int i = 0; i < verticalValues.size(); i++)
						{
							long retrievedValue = verticalValues.get(i);
							if (retrievedValue >= decreasedBaseAddress && retrievedValue < increasedBaseAddress)
							{
								containedOffsets.add(retrievedValue);
							} else
							{
								break;
							}
						}

						System.out.println(CollectionsUtils.longListToString(containedOffsets));
					}*/
				}
			}

			// List<List<Long>> memoryDumpsVerticalValues = new LinkedList<>();

			/*for(int i = 0; i < memoryDumpsHorizontalValues.get(0).size(); i++)
			{
				List<Long> l = new LinkedList<>();
				memoryDumpsVerticalValues.add(l);
			}

			for(List<Long> memoryDumpValues : memoryDumpsHorizontalValues)
			{

			}

			for(long possiblePointerAddress : possiblePointerAddresses)
			{
				long value = possiblePointers.get(possiblePointerAddress).get(0);
				// Sorted values
			}*/
		}

		for (Object possiblePointer : possiblePointers.entrySet())
		{
			Map.Entry<Long, List<Long>> offsetValuesPair = (Map.Entry) possiblePointer;
			long offset = offsetValuesPair.getKey();
			List<Long> values = offsetValuesPair.getValue();
			List<Long> targetAddresses = MemoryDump.getTargetAddresses(memoryDumps);
			Set<Long> pointerOffsets = new HashSet<>();

			for (int targetAddressesIndex = 0; targetAddressesIndex < targetAddresses.size(); targetAddressesIndex++)
			{
				long targetAddress = targetAddresses.get(targetAddressesIndex);
				long value = values.get(targetAddressesIndex);
				long pointerOffset = (int) (targetAddress - value);
				pointerOffsets.add(pointerOffset);
			}

			// Were all pointer offsets unique?
			if (pointerOffsets.size() == 1)
			{
				int pointerOffset = Math.toIntExact(pointerOffsets.iterator().next());

				if (pointerOffsetChecker.fulfillsSettings(pointerOffset))
				{
					int[] offsets = new int[]{pointerOffset};
					long baseAddress = offset + memoryDumpStartingOffset;
					MemoryPointer memoryPointer = new MemoryPointer(baseAddress, offsets);
					System.out.println(memoryPointer);
				}
			}
		}
	}
}
