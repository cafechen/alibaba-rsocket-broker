package com.alibaba.rsocket.encoding;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.cloudevents.json.ZonedDateTimeDeserializer;
import io.cloudevents.json.ZonedDateTimeSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Json utils
 *
 * @author leijuan
 */
public class JsonUtils {
    public static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // add Jackson datatype for ZonedDateTime
        objectMapper.registerModule(new Jdk8Module());
        final SimpleModule module = new SimpleModule();
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        module.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        objectMapper.registerModule(module);
        objectMapper.setDefaultPrettyPrinter(new MinimalPrettyPrinter());
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static <T> T readJsonValue(ByteBuf byteBuf, Class<T> valueType) throws IOException {
        return objectMapper.readValue((InputStream) new ByteBufInputStream(byteBuf), valueType);
    }

    public static byte[] toJsonBytes(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsBytes(object);
    }

    public static ByteBuf toJsonByteBuf(Object object) throws EncodingException {
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            ByteBufOutputStream bos = new ByteBufOutputStream(byteBuf);
            objectMapper.writeValue((OutputStream) bos, object);
            return byteBuf;
        } catch (Exception e) {
            ReferenceCountUtil.release(byteBuf);
            throw new EncodingException(e.getMessage());
        }
    }

    public static void updateJsonValue(ByteBuf byteBuf, Object object) throws IOException {
        objectMapper.readerForUpdating(object).readValue((InputStream) new ByteBufInputStream(byteBuf));
    }

    public static String toJsonText(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    public static <T> T readJsonValue(String text, Class<T> valueType) throws JsonProcessingException {
        return objectMapper.readValue(text, valueType);
    }

    public static <T> T convertValue(Object source, Class<T> valueType) {
        return objectMapper.convertValue(source, valueType);
    }

    public static void updateJsonValue(String text, Object object) throws JsonProcessingException {
        objectMapper.readerForUpdating(object).readValue(text);
    }

    public static Object[] readJsonArray(ByteBuf byteBuf, Class<?>[] targetClasses) throws IOException {
        Object[] targets = new Object[targetClasses.length];
        List<JsonNode> jsonNodes = objectMapper.readValue(new ByteBufInputStream(byteBuf), new TypeReference<List<JsonNode>>() {
        });
        for (int i = 0; i < targetClasses.length; i++) {
            targets[i] = objectMapper.treeToValue(jsonNodes.get(i), targetClasses[i]);
        }
        return targets;
    }
}
