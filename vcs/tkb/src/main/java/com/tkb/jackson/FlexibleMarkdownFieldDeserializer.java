package com.tkb.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.tkb.utils.FlexibleFieldConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FlexibleMarkdownFieldDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token == JsonToken.VALUE_STRING) {
            return FlexibleFieldConverter.parseJsonArrayStringIfNeeded(p.getValueAsString());
        }
        if (token == JsonToken.START_ARRAY) {
            JsonNode node = p.getCodec().readTree(p);
            List<String> items = new ArrayList<>();
            node.forEach(n -> {
                if (!n.isNull()) {
                    items.add(n.asText());
                }
            });
            return FlexibleFieldConverter.toMarkdownText(items);
        }
        return p.getValueAsString();
    }
}
