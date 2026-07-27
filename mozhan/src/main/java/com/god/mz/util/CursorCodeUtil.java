package com.god.mz.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class CursorCodeUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String encode(List<Long> cursors) {
        if (cursors == null || cursors.isEmpty()) {
            return null;
        }
        try {
            String json = mapper.writeValueAsString(cursors);
            return Base64.getEncoder().encodeToString(json.getBytes());
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static List<Long> decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        try {
            String json = new String(Base64.getDecoder().decode(encoded));
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

}
