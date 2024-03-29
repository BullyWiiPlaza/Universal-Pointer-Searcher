package com.wiiudev.gecko.pointer.preprocessed_search;

import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryPointer;
import lombok.Getter;
import lombok.val;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.wiiudev.gecko.pointer.preprocessed_search.OSMemory.getUsedMemoryPercentage;

@Getter
public class MemoryPointerList
{
	private static final double MEMORY_POINTERS_BACKUP_PERCENTAGE = 0.2;

	private List<MemoryPointer> memoryPointers;

	private int size;

	private final PointerSwapFile pointerSwapFile;

	public MemoryPointerList()
	{
		memoryPointers = new ArrayList<>();
		pointerSwapFile = new PointerSwapFile();
	}

	public void setMemoryPointers(List<MemoryPointer> memoryPointers)
	{
		this.memoryPointers = memoryPointers;
		size = memoryPointers.size();
	}

	public void add(MemoryPointer memoryPointer)
	{
		memoryPointers.add(memoryPointer);
		size++;

		val usedMemoryPercentage = getUsedMemoryPercentage();
		if(false)
		// if (usedMemoryPercentage > MEMORY_USED_PERCENTAGE_THRESHOLD)
		{
			try
			{
				val elementsCount = (int) (MEMORY_POINTERS_BACKUP_PERCENTAGE * memoryPointers.size());
				pointerSwapFile.storeToDisk(memoryPointers, elementsCount);
			} catch (IOException exception)
			{
				exception.printStackTrace();
			}
		}
	}

	public int size()
	{
		return size;
	}

	public MemoryPointer get(int memoryPointersIndex)
	{
		return memoryPointers.get(memoryPointersIndex);
	}
}
