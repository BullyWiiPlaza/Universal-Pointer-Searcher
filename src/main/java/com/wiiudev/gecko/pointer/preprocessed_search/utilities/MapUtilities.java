package com.wiiudev.gecko.pointer.preprocessed_search.utilities;

import lombok.val;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Map.Entry.comparingByValue;

public class MapUtilities
{
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map)
	{
		val list = new ArrayList<>(map.entrySet());
		list.sort(comparingByValue());

		val sortedMap = new LinkedHashMap<K, V>();
		for (val entry : list)
		{
			K key = entry.getKey();
			V value = entry.getValue();
			sortedMap.put(key, value);
		}

		return sortedMap;
	}
}
