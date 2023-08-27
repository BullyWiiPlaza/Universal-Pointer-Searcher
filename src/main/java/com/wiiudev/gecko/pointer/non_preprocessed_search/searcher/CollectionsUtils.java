package com.wiiudev.gecko.pointer.non_preprocessed_search.searcher;

import lombok.val;
import lombok.var;

import java.util.List;

class CollectionsUtils
{
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
