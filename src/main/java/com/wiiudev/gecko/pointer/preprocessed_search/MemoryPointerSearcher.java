package com.wiiudev.gecko.pointer.preprocessed_search;

import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryDump;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryPointer;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryRange;
import com.wiiudev.gecko.pointer.swing.UniversalPointerSearcherGUI;
import com.wiiudev.gecko.pointer.utilities.Benchmark;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.var;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

import static com.wiiudev.gecko.pointer.preprocessed_search.OSMemory.runUsedMemorySetterThread;
import static com.wiiudev.gecko.pointer.preprocessed_search.OSMemory.stopUsedMemorySetter;
import static com.wiiudev.gecko.pointer.preprocessed_search.ProgressBarHelper.setProgress;
import static com.wiiudev.gecko.pointer.preprocessed_search.data_structures.serialization.PointerMapSerializer.deserializePointerMap;
import static com.wiiudev.gecko.pointer.preprocessed_search.data_structures.serialization.PointerMapSerializer.serializePointerMap;
import static com.wiiudev.gecko.pointer.preprocessed_search.utilities.MapUtilities.sortByValue;
import static java.lang.Long.parseUnsignedLong;
import static java.lang.Math.*;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.arraycopy;
import static java.nio.file.Files.exists;
import static java.util.Arrays.copyOf;
import static java.util.logging.Level.INFO;
import static java.util.logging.Logger.getLogger;
import static javax.swing.SwingUtilities.invokeLater;

@Getter
public class MemoryPointerSearcher
{
	public static final int MINIMUM_POINTER_SEARCH_DEPTH_VALUE = 1;
	private static final boolean USE_DEEP_MODE = false;

	private static final Logger LOGGER = getLogger(MemoryPointerSearcher.class.getName());

	static
	{
		LOGGER.setLevel(INFO);
	}

	@Setter
	private List<MemoryDump> memoryDumps;

	@Getter
	@Setter
	private List<MemoryDump> importedPointerMaps = new ArrayList<>();

	@Getter
	@Setter
	private MemoryPointerList memoryPointerList;

	@Getter
	@Setter
	private long maximumMemoryChunkSize = 1_000_000_000;

	@Getter
	@Setter
	private boolean allowNegativeOffsets = false;

	@Getter
	@Setter
	private int pointerSearchDepth = 2;

	@Getter
	@Setter
	private int pointerValueAlignment = 4;

	@Getter
	@Setter
	private int threadCount;

	@Getter
	@Setter
	private int pointerAddressAlignment = 4;

	@Getter
	@Setter
	private long minimumPointerOffset = 0x0;

	@Getter
	@Setter
	private long maximumPointerOffset = 0x400;

	@Getter
	@Setter
	private long minimumPointerAddress = 1;

	@Getter
	@Setter
	private int addressSize = 4;

	@Getter
	@Setter
	private boolean printSignedOffsets = true;

	@Getter
	@Setter
	private List<MemoryRange> ignoredMemoryRanges;

	@Getter
	@Setter
	private List<MemoryRange> baseAddressRanges;

	@Getter
	@Setter
	private boolean generatePointerMaps = false;

	@Setter
	private JProgressBar generalProgressBar;

	@Setter
	private JProgressBar pointerDepthProgressBar;

	@Setter
	private boolean isSearchCanceled;

	@Getter
	private List<Map<Long, Long>> pointerMaps;

	@Getter
	private Map<Long, Long> firstPointerMapSortedValues;

	private Object[] firstPointerMapSortedValuesKeys;

	@Getter
	private List<MemoryDump> pointerMapsAndMemoryDumps;

	@Setter
	private JButton searchPointersButton;

	@Getter
	@Setter
	private boolean excludeCycles;

	public MemoryPointerSearcher()
	{
		memoryDumps = new ArrayList<>();

		val runtime = getRuntime();
		threadCount = runtime.availableProcessors();
	}

	public static String getSGenitive(List<MemoryDump> memoryDumps, List<MemoryDump> pointerMaps)
	{
		val size = pointerMaps.size() + memoryDumps.size();
		return size == 1 ? "" : "s";
	}

	public void addMemoryDump(MemoryDump memoryDump)
	{
		memoryDumps.add(memoryDump);
	}

	public void removeMemoryDumps()
	{
		memoryDumps.clear();
	}

	public void parseMemoryDumps() throws Exception
	{
		System.gc();

		pointerMaps = new ArrayList<>();
		pointerMapsAndMemoryDumps = new ArrayList<>();

		readPointerMapsFromMemoryDumps();
		parsePointerMapsDirectly();

		System.gc();
	}

	public List<MemoryPointer> searchPointers() throws Exception
	{
		runUsedMemorySetterThread();

		invokeLater(() ->
		{
			if (pointerDepthProgressBar != null)
			{
				pointerDepthProgressBar.setVisible(true);
			}
		});

		memoryPointerList = new MemoryPointerList();
		isSearchCanceled = false;

		considerStartingProgressBar();

		val memoryDumpsSize = memoryDumps.size();
		val firstMemoryDump = memoryDumpsSize > 0 ? memoryDumps.get(0) : importedPointerMaps.get(0);
		val firstPointerMap = pointerMaps.get(0);
		var currentPointerEntryIndex = 0;

		if (pointerSearchDepth <= 0)
		{
			throw new IllegalStateException("Unsupported pointer search depth " + pointerSearchDepth);
		}

		val targetAddress = firstMemoryDump.getTargetAddress();

		// Iterate over all possible pointers
		val pointerEntries = firstPointerMap.entrySet();
		if (generalProgressBar != null)
		{
			invokeLater(() -> generalProgressBar.setMaximum(100));
		}
		for (val pointerEntry : pointerEntries)
		{
			var previousMemoryPointersCount = memoryPointerList.size();
			val offset = pointerEntry.getKey();
			val value = pointerEntry.getValue();
			addMemoryPointer(firstMemoryDump, targetAddress, null, offset, value);
			var currentMemoryPointersCount = memoryPointerList.size();

			/* if (pointerSearchDepth >= 2)
			{
				addMemoryPointersDepth2(pointerMaps, firstMemoryDump, targetAddress, firstPointerMap, pointerEntry);
			} */

			if (!USE_DEEP_MODE)
			{
				considerGoingDeeper(firstMemoryDump, firstPointerMapSortedValuesKeys, previousMemoryPointersCount, currentMemoryPointersCount);
			}

			if (isSearchCanceled)
			{
				break;
			}

			/*if (outerIndex % 1000 == 0)
			{
				System.out.println("Outer: " + outerIndex + "/" + firstPointerMap.size() + " " + (outerIndex / (double) firstPointerMap.size() * 100) + " %");
			}*/

			considerUpdatingProgressBar(firstPointerMap, currentPointerEntryIndex);
			currentPointerEntryIndex++;
		}

		stopUsedMemorySetter();
		considerFinishingProgressBar();

		invokeLater(() ->
		{
			if (pointerDepthProgressBar != null)
			{
				pointerDepthProgressBar.setVisible(false);
			}
		});

		System.gc();
		removeBadMemoryPointers();

		return memoryPointerList.getMemoryPointers();
	}

	private void removeBadMemoryPointers() throws Exception
	{
		var memoryDumpsIndex = 1;
		val pointerMapsSize = pointerMaps.size();
		for (; memoryDumpsIndex < pointerMapsSize; memoryDumpsIndex++)
		{
			if (searchPointersButton != null)
			{
				val memoryDumpProgress = (memoryDumpsIndex + 1) + "/" + pointerMapsSize;
				invokeLater(() -> searchPointersButton.setText("Validating memory dump " + memoryDumpProgress + "..."));
			}

			// Read the next memory dump
			val currentMemoryDump = pointerMapsAndMemoryDumps.get(memoryDumpsIndex);
			val pointerMap = getPointerMap(memoryDumpsIndex, currentMemoryDump);
			val validatedMemoryPointers = new ArrayList<MemoryPointer>();

			val memoryPointers = memoryPointerList.getMemoryPointers();
			for (val memoryPointer : memoryPointers)
			{
				val isPointerSupported = doesMemoryDumpSupportPointer(memoryPointer,
						currentMemoryDump, pointerMap);
				if (isPointerSupported)
				{
					validatedMemoryPointers.add(memoryPointer);
				}
			}

			memoryPointerList.setMemoryPointers(validatedMemoryPointers);
			System.gc();
		}
	}

	private static final int POINTER_SEARCH_STARTING_DEPTH = 1;

	private void considerGoingDeeper(MemoryDump memoryDump,
	                                 Object[] firstPointerMapSortedValuesKeys,
	                                 int previousMemoryPointersCount,
	                                 int currentMemoryPointersCount)
	{
		var pointerSearchDepthIndex = POINTER_SEARCH_STARTING_DEPTH;

		var progressCounter = 0;
		for (; pointerSearchDepthIndex < pointerSearchDepth; pointerSearchDepthIndex++)
		{
			for (var memoryPointersIndex = previousMemoryPointersCount;
			     memoryPointersIndex < currentMemoryPointersCount; memoryPointersIndex++)
			{
				val memoryPointer = memoryPointerList.get(memoryPointersIndex);
				val innerTargetAddress = memoryPointer.getBaseAddress();

				val startingAddress = getStartingAddress(innerTargetAddress);
				val endingAddress = getEndingAddress(memoryDump, innerTargetAddress);
				val startingIndex = binarySearchForStartingPointerMapIndex(firstPointerMapSortedValues, firstPointerMapSortedValuesKeys, endingAddress);
				val endingIndex = binarySearchForStartingPointerMapIndex(firstPointerMapSortedValues, firstPointerMapSortedValuesKeys, startingAddress) + 1;
				val indicesToProcess = endingIndex - startingIndex;
				val progressMaximum = getTotalProgress(indicesToProcess, previousMemoryPointersCount,
						currentMemoryPointersCount, pointerSearchDepthIndex);

				for (var currentIndex = startingIndex; currentIndex < endingIndex; currentIndex++)
				{
					if (isSearchCanceled)
					{
						return;
					}

					val key = firstPointerMapSortedValuesKeys[currentIndex].toString();
					val pointerOffset = parseUnsignedLong(key);
					val pointerValue = firstPointerMapSortedValues.get(pointerOffset);
					addMemoryPointer(memoryDump, innerTargetAddress, memoryPointer, pointerOffset, pointerValue);
					progressCounter++;
					setProgress(progressCounter, progressMaximum, pointerDepthProgressBar);
					currentIndex++;
				}
			}

			previousMemoryPointersCount = currentMemoryPointersCount;
			currentMemoryPointersCount = memoryPointerList.size();
		}
	}

	private long getEndingAddress(MemoryDump memoryDump, long innerTargetAddress)
	{
		var endingAddress = innerTargetAddress - maximumPointerOffset;

		try
		{
			val lastAddress = memoryDump.getLastAddress();
			endingAddress = min(endingAddress, lastAddress);
		} catch (IOException lastAddress)
		{
			lastAddress.printStackTrace();
		}

		return endingAddress;
	}

	private long getStartingAddress(long innerTargetAddress)
	{
		var startingAddress = innerTargetAddress;
		if (allowNegativeOffsets)
		{
			startingAddress += maximumPointerOffset;
			startingAddress = max(0, startingAddress);
		}
		return startingAddress;
	}

	private int binarySearchForStartingPointerMapIndex(Map<Long, Long> map, Object[] keys, long targetAddress)
	{
		var lowIndex = 0;
		var highIndex = keys.length - 1;
		var middleIndex = 0;

		while (lowIndex <= highIndex)
		{
			middleIndex = (lowIndex + highIndex) / 2;
			val key = keys[middleIndex];
			val value = map.get(parseUnsignedLong(key.toString()));
			if (targetAddress > value)
			{
				lowIndex = middleIndex + 1;
			} else if (targetAddress < value)
			{
				highIndex = middleIndex - 1;
			} else
			{
				return middleIndex;
			}
		}

		return middleIndex;
	}

	private int getTotalProgress(int indicesToProcess,
	                             int previousMemoryPointersCount,
	                             int currentMemoryPointersCount,
	                             int pointerSearchDepthIndex)
	{
		val memoryPointersCount = currentMemoryPointersCount - previousMemoryPointersCount;
		val pointerSearchIterations = pointerSearchDepth - pointerSearchDepthIndex;
		return pointerSearchIterations * memoryPointersCount * indicesToProcess;
	}

	private boolean isBaseAddressIgnored(long offset)
	{
		if (baseAddressRanges == null)
		{
			// Everything allowed
			return false;
		}

		for (val memoryRange : baseAddressRanges)
		{
			if (memoryRange.contains(offset))
			{
				// Not ignored since it's specified as allowed
				return false;
			}
		}

		// Ignored
		return true;
	}

	private void considerStartingProgressBar()
	{
		val pointerMap = getOneElementPointerMap();
		considerUpdatingProgressBar(pointerMap, 0);
	}

	private void considerFinishingProgressBar()
	{
		val pointerMap = getOneElementPointerMap();
		considerUpdatingProgressBar(pointerMap, 1);
	}

	private TreeMap<Long, Long> getOneElementPointerMap()
	{
		val pointerMap = new TreeMap<Long, Long>();
		pointerMap.put(0L, 0L);
		return pointerMap;
	}

	private void considerUpdatingProgressBar(Map<Long, Long> pointerMap, int currentIndex)
	{
		val maximum = pointerMap.size();
		val progressChangeAccepted = setProgress(currentIndex, maximum, generalProgressBar);

		if (progressChangeAccepted)
		{
			updateTaskBarProgress(currentIndex, maximum);
		}
	}

	private void updateTaskBarProgress(int value, int maximum)
	{
		val thread = new Thread(() ->
		{
			val pointerSearcherGUI = UniversalPointerSearcherGUI.getInstance();
			val windowsTaskBarProgress = pointerSearcherGUI.getWindowsTaskBarProgress();
			windowsTaskBarProgress.setMaximum(maximum);

			if (maximum == value)
			{
				windowsTaskBarProgress.setProgressValue(0);
			} else
			{
				windowsTaskBarProgress.setProgressValue(value);
			}
		});

		thread.setName("Windows Task Bar Progress Updater");
		thread.start();
	}

	/* private void addMemoryPointersDepth2(List<List<OffsetValuePair>> pointerMaps,
	                                     MemoryDump memoryDump,
	                                     Integer targetAddress,
	                                     List<OffsetValuePair> firstPointerMap,
	                                     OffsetValuePair offsetValuePair)
	{
		val startingAddress = getScanIntervalStartingAddress(memoryDump, offsetValuePair);
		val firstBaseOffset = getFirstBaseAddress(startingAddress);
		val lastBaseOffset = getLastAddress(firstPointerMap, firstBaseOffset);
		var innerIndex = getIndexOfBaseOffset(firstPointerMap, firstBaseOffset);

		// Iterate over all pairs in range
		val pairsCount = firstPointerMap.size();
		while (innerIndex < pairsCount)
		{
			val currentOffsetValuePair = firstPointerMap.get(innerIndex);
			val currentOffset = currentOffsetValuePair.getOffset();

			if (currentOffset > lastBaseOffset)
			{
				// The range's upper bound has been exceeded
				break;
			}

			// From here check if a pointer with depth 1 exists
			val innerOffset = currentOffset - startingAddress;
			if (isPointerOffsetAllowed(innerOffset))
			{
				val pointerMap = getPointerMap(offsetValuePair, currentOffsetValuePair);
				addMemoryPointer(pointerMaps, memoryDump, targetAddress, pointerMap, new int[]{innerOffset}, true);
			}

			innerIndex++;

			if (isSearchCanceled)
			{
				break;
			}
		}
	} */

	/* private int getScanIntervalStartingAddress(MemoryDump memoryDump, OffsetValuePair offsetValuePair)
	{
		val pointerValue = offsetValuePair.getValue();
		val memoryStartingAddress = memoryDump.getStartingAddress();

		return pointerValue - memoryStartingAddress;
	}

	private int getIndexOfBaseOffset(List<OffsetValuePair> pointerMap, int baseOffset)
	{
		// Find the index of the base address
		var innerIndex = findBaseOffsetIndex(pointerMap, baseOffset);

		// If it hasn't been found, use the index after it would be inserted
		if (innerIndex < 0)
		{
			innerIndex = (-1 * innerIndex) - 1;
		}

		return innerIndex;
	} */

	/* private int getLastAddress(List<OffsetValuePair> pointerMap, int firstBaseOffset)
	{
		var lastBaseOffset = firstBaseOffset + maximumPointerOffset;
		val lastGlobalOffset = pointerMap.get(pointerMap.size() - 1).getOffset();

		if (lastBaseOffset > lastGlobalOffset)
		{
			lastBaseOffset = lastGlobalOffset;
		}

		return lastBaseOffset;
	} */

	private void addMemoryPointer(MemoryDump memoryDump,
	                              Long targetAddress,
	                              MemoryPointer memoryPointer,
	                              long pointerOffset,
	                              long pointerValue)
	{
		val existingOffsets = memoryPointer == null ? new long[]{} : memoryPointer.getOffsets();
		addMemoryPointer(memoryDump, targetAddress, pointerOffset, pointerValue, existingOffsets);
	}

	private void addMemoryPointer(MemoryDump memoryDump,
	                              Long targetAddress,
	                              long pointerOffset,
	                              long pointerValue,
	                              long[] innerOffsets)
	{
		if (isSearchCanceled)
		{
			return;
		}

		val targetAddressOffset = targetAddress - pointerValue;

		var isPointerOffsetAllowed = true;
		val doesPointerNeedToWorkNow = !USE_DEEP_MODE || pointerSearchDepth == innerOffsets.length + 1;
		if (doesPointerNeedToWorkNow)
		{
			isPointerOffsetAllowed = isPointerOffsetAllowed(targetAddressOffset);
		}

		if (isPointerOffsetAllowed)
		{
			val pointerOffsets = getPointerOffsets(innerOffsets, targetAddressOffset);
			val startingOffset = memoryDump.getStartingAddress();
			val pointerBaseAddress = startingOffset + pointerOffset;

			if (isBaseAddressIgnored(pointerBaseAddress))
			{
				return;
			}

			val memoryPointer = new MemoryPointer(pointerBaseAddress, pointerOffsets);

			if (!doesPointerNeedToWorkNow)
			{
				val offsets = memoryPointer.getOffsets();
				val lastOffset = offsets[offsets.length - 1];
				if (!isPointerOffsetAllowed(lastOffset))
				{
					val pointerMap = pointerMaps.get(0);
					val innerTargetAddress = memoryPointer.followPointer(pointerMap, startingOffset,
							excludeCycles, true);

					if (innerTargetAddress == null)
					{
						return;
					}

					val startingAddress = getStartingAddress(innerTargetAddress);
					val endingAddress = getEndingAddress(memoryDump, innerTargetAddress);
					val startingIndex = binarySearchForStartingPointerMapIndex(firstPointerMapSortedValues, firstPointerMapSortedValuesKeys, endingAddress);
					val endingIndex = binarySearchForStartingPointerMapIndex(firstPointerMapSortedValues, firstPointerMapSortedValuesKeys, startingAddress) + 1;

					for (var currentIndex = startingIndex; currentIndex < endingIndex; currentIndex++)
					{
						val key = firstPointerMapSortedValuesKeys[currentIndex].toString();
						val currentPointerOffset = parseUnsignedLong(key);
						val innerTargetValue = firstPointerMapSortedValues.get(currentPointerOffset);

						if (innerTargetValue == null)
						{
							continue;
						}

						val outerOffset = targetAddress - innerTargetValue;
						if (pointerSearchDepth == offsets.length + 1
								&& !isPointerOffsetAllowed(outerOffset))
						{
							continue;
						}

						val updatedPointerOffsets = copyOf(offsets, offsets.length);
						updatedPointerOffsets[updatedPointerOffsets.length - 1] = outerOffset;
						val innerMemoryPointer = new MemoryPointer(innerTargetAddress, updatedPointerOffsets);
						addMemoryPointer(memoryDump, innerTargetAddress, innerMemoryPointer,
								pointerOffset, pointerValue);
					}

					return;
				}
			}

			considerAddingMemoryPointer(memoryDump, startingOffset, memoryPointer);
		}
	}

	private Object[] getKeySet()
	{
		val keySet = firstPointerMapSortedValues.keySet();
		return keySet.toArray();
	}

	private void considerAddingMemoryPointer(MemoryDump memoryDump,
	                                         Long startingOffset,
	                                         MemoryPointer memoryPointer)
	{
		val pointerMap = pointerMaps.get(0);
		val memoryDumpTargetAddress = memoryDump.getTargetAddress();
		if (!excludeCycles || memoryPointer.reachesDestination(pointerMap,
				memoryDumpTargetAddress, startingOffset, true))
		{
			memoryPointerList.add(memoryPointer);
		}
	}

	private long[] getPointerOffsets(long[] existingOffsets, long targetAddressOffset)
	{
		val pointerOffsets = new long[existingOffsets.length + 1];

		pointerOffsets[0] = targetAddressOffset;
		arraycopy(existingOffsets, 0, pointerOffsets, 1, existingOffsets.length);

		return pointerOffsets;
	}

	private boolean isPointerOffsetAllowed(long targetAddressOffset)
	{
		val isPointerOffsetPositiveOkay = targetAddressOffset >= 0;
		val isPointerOffsetSmallEnough = abs(targetAddressOffset) < maximumPointerOffset;
		val isSignOkay = isPointerOffsetPositiveOkay || allowNegativeOffsets;
		return isPointerOffsetSmallEnough && isSignOkay;
	}

	private void parsePointerMapsDirectly() throws Exception
	{
		if (importedPointerMaps != null)
		{
			for (val pointerMapMemoryDump : importedPointerMaps)
			{
				val lookupPointerMap = shouldReadPointerMapLater() ? null : readPointerMapFromPointerMap(pointerMapMemoryDump);
				if (lookupPointerMap != null)
				{
					addPointerMap(lookupPointerMap);
					pointerMapsAndMemoryDumps.add(pointerMapMemoryDump);
				}
			}
		}
	}

	private boolean shouldReadPointerMapLater()
	{
		return !pointerMaps.isEmpty();
	}

	private void addPointerMap(TreeMap<Long, Long> pointerMap)
	{
		pointerMaps.add(pointerMap);

		if (firstPointerMapSortedValues == null)
		{
			val benchmark = new Benchmark();
			benchmark.start();
			if (searchPointersButton != null)
			{
				invokeLater(() -> searchPointersButton.setText("Sorting pointer map..."));
			}
			LOGGER.log(INFO, "Sorting pointer map...");
			firstPointerMapSortedValues = sortByValue(pointerMap);
			firstPointerMapSortedValuesKeys = getKeySet();
			val elapsedTime = benchmark.getElapsedTime();
			LOGGER.log(INFO, "Sorting took " + elapsedTime + " seconds");
		}
	}

	private void readPointerMapsFromMemoryDumps() throws Exception
	{
		var memoryDumpIndex = 0;
		val memoryDumpsCount = memoryDumps.size();
		for (val memoryDump : memoryDumps)
		{
			val pointerMap = shouldReadPointerMapLater()
					? null : readPointerMapFromMemoryDump(memoryDumpIndex, memoryDumpsCount, memoryDump);
			addPointerMap(pointerMap);
			pointerMapsAndMemoryDumps.add(memoryDump);
			memoryDumpIndex++;
		}
	}

	private TreeMap<Long, Long> readPointerMapFromPointerMap(MemoryDump pointerMap) throws IOException, DataFormatException
	{
		if (!hasCorrespondingMemoryDump(pointerMap))
		{
			val fileName = pointerMap.getPointerMapFilePath().toFile().getName();
			LOGGER.log(INFO, "Importing pointer map " + fileName + "...");
			val pointerMapFilePath = pointerMap.getFilePath();
			return deserializePointerMap(pointerMapFilePath);
		}

		return null;
	}

	private boolean hasCorrespondingMemoryDump(MemoryDump pointerMap)
	{
		val memoryDumpFilePath = pointerMap.getMemoryDumpFilePath();
		val correspondingMemoryDumpExists = exists(memoryDumpFilePath);

		if (correspondingMemoryDumpExists)
		{
			for (val memoryDump : memoryDumps)
			{
				val currentMemoryDumpFilePath = memoryDump.getMemoryDumpFilePath().toString();
				if (currentMemoryDumpFilePath.equals(memoryDumpFilePath.toString()))
				{
					return true;
				}
			}
		}

		return false;
	}

	private TreeMap<Long, Long> readPointerMapFromMemoryDump(int memoryDumpIndex,
	                                                         int memoryDumpsCount,
	                                                         MemoryDump memoryDump) throws Exception
	{
		val pointerMapFilePath = memoryDump.getPointerMapFilePath();
		val pointerMapFileName = pointerMapFilePath.toFile().getName();
		TreeMap<Long, Long> pointerMap;

		if (exists(pointerMapFilePath))
		{
			if (isPointerMapAvailable(pointerMapFilePath))
			{
				LOGGER.log(INFO, "De-serializing pointer map " + pointerMapFileName + "...");
				pointerMap = deserializePointerMap(pointerMapFilePath);
			} else
			{
				pointerMap = getOffsetValuePairs(memoryDumpIndex, memoryDumpsCount, memoryDump);
			}
		} else
		{
			pointerMap = getOffsetValuePairs(memoryDumpIndex, memoryDumpsCount, memoryDump);

			if (generatePointerMaps)
			{
				LOGGER.log(INFO, "Serializing pointer map " + pointerMapFileName + "...");
				serializePointerMap(pointerMapFilePath, pointerMap);
			}
		}

		return pointerMap;
	}

	private TreeMap<Long, Long> getOffsetValuePairs(int memoryDumpIndex, int memoryDumpsCount, MemoryDump memoryDump) throws Exception
	{
		val fileName = memoryDump.getMemoryDumpFilePath().toFile().getName();
		LOGGER.log(INFO, "Building pointer map from memory dump " + fileName + "...");
		return memoryDump.readOffsetValuePairs(maximumMemoryChunkSize,
				pointerValueAlignment, ignoredMemoryRanges, memoryDumpIndex,
				memoryDumpsCount, minimumPointerAddress, addressSize, generalProgressBar,
				searchPointersButton);
	}

	private boolean isPointerMapAvailable(Path pointerMapFilePath)
	{
		if (importedPointerMaps != null)
		{
			for (val pointerMap : importedPointerMaps)
			{
				val filePath = pointerMap.getFilePath();
				if (filePath.toString().equals(pointerMapFilePath.toString()))
				{
					return true;
				}
			}
		}

		return false;
	}

	private boolean doesMemoryDumpSupportPointer(MemoryPointer memoryPointer, MemoryDump currentMemoryDump, Map<Long, Long> pointerMap)
	{
		// Check if this is compatible with the memory dump
		val currentTargetAddress = currentMemoryDump.getTargetAddress();
		val startingOffset = currentMemoryDump.getStartingAddress();
		return memoryPointer.reachesDestination(pointerMap, currentTargetAddress, startingOffset, excludeCycles);
	}

	private Map<Long, Long> getPointerMap(int memoryDumpIndex, MemoryDump currentMemoryDump) throws Exception
	{
		var pointerMap = pointerMaps.get(memoryDumpIndex);

		if (pointerMap == null)
		{
			val fileType = currentMemoryDump.getFileType();
			switch (fileType)
			{
				case MEMORY_DUMP:
					val pointerMapsCount = pointerMaps.size();
					pointerMap = readPointerMapFromMemoryDump(memoryDumpIndex, pointerMapsCount, currentMemoryDump);
					break;

				case POINTER_MAP:
					pointerMap = readPointerMapFromPointerMap(currentMemoryDump);
					break;

				default:
					throw new IllegalStateException("Unhandled file type");
			}
		}

		return pointerMap;
	}

	public void printMemoryPointers(List<MemoryPointer> memoryPointers)
	{
		for (val memoryPointer : memoryPointers)
		{
			val memoryPointerString = memoryPointer.toString(printSignedOffsets, addressSize);
			System.out.println(memoryPointerString);
		}
	}
}
