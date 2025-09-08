package io.sci.citizen.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GsonDeserializer implements JsonDeserializer<Date> {

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Override
    public Date deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        try {
            return sdf.parse(json.getAsJsonPrimitive().getAsString().replaceFirst(":(?=\\d{2}$)", ""));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
