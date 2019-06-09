package com.wiiudev.gecko.pointer.non_preprocessed_search.searcher;

import lombok.val;
import lombok.var;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.sort;

class CollectionsUtils
{
	static <T extends Comparable<? super T>> List<T> toSortedList(Collection<T> collection)
	{
		val list = new LinkedList<T>(collection);
		sort(list);

		return list;
	}

	static String longListToString(List<Long> list)
	{
		val stringBuilder = new StringBuilder();
		stringBuilder.append("[");

		for (var valuesIndex = 0; valuesIndex < list.size(); valuesIndex++)
		{
			val value = list.get(valuesIndex);
			stringBuilder.append("0x");
			stringBuilder.append(Long.toHexString(value).toUpperCase());

			if (valuesIndex != list.size() - 1)
			{
				stringBuilder.append(", ");
			}
		}

		stringBuilder.append("]");

		return stringBuilder.toString();
	}
}
