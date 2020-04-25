package org.regadou.jhux.sys;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GsonClassAdapter implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

   @Override
   public JsonElement serialize(Class<?> klazz, java.lang.reflect.Type type, JsonSerializationContext jsc) {
      return new JsonPrimitive(klazz.getName());
   }

   @Override
   public Class<?> deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      try { return Class.forName(json.getAsString()); }
      catch (ClassNotFoundException e) { throw new RuntimeException(e); }
   }
}
