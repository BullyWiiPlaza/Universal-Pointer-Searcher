package com.wiiudev.gecko.pointer.preprocessed_search.data_structures.serialization;

import com.google.gson.*;
import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.OffsetValuePair;
import lombok.val;

import java.lang.reflect.Type;
import java.util.List;

public class OffsetValuePairSerializer implements JsonSerializer<List<OffsetValuePair>>
{
	@Override
	public JsonElement serialize(List<OffsetValuePair> offsetValuePairs,
	                             Type type,
	                             JsonSerializationContext jsonSerializationContext)
	{
		val jsonAuthorsArray = new JsonArray();
		for (val offsetValuePair : offsetValuePairs)
		{
			val jsonObject = new JsonObject();
			jsonObject.addProperty("offset", offsetValuePair.getOffset());
			jsonObject.addProperty("value", offsetValuePair.getValue());
			jsonAuthorsArray.add(jsonObject);
		}

		return jsonAuthorsArray;
	}
}
