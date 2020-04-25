package org.regadou.jhux.sys;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.io.File;
import org.regadou.jhux.JHUX;

public class GsonStringableAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {

      @Override
      public JsonElement serialize(T value, java.lang.reflect.Type type, JsonSerializationContext jsc) {
         String txt;
         if (value instanceof File) {
            try { txt = ((File)value).toURI().toString(); }
            catch (Exception e) { txt = "file:"+value; }
         }
         else if (value == null)
            txt = "";
         else
            txt = value.toString();
         return new JsonPrimitive(txt);
      }

      @Override
      public T deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
         if (typeOfT instanceof Class)
            return (T)JHUX.get(Context.class).convert(json.getAsString(), (Class)typeOfT);
         throw new RuntimeException(typeOfT+" is not a class, so don't know what to do with it");
      }
}
