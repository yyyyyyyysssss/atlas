package com.atlas.notification.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/5 13:50
 */
public abstract class AbstractMessageAdapter implements MessageAdapter{


    protected String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    protected String getAsString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    protected String getAsString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : defaultValue;
    }

    protected Integer getAsInteger(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number num) return num.intValue();
        if (val instanceof String str) return Integer.parseInt(str);
        return null;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getAsMap(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return (v instanceof Map) ? (Map<String, Object>) v : Collections.emptyMap();
    }

    protected String[] getAsArray(Map<String, Object> ext, String key) {
        Object val = ext.get(key);
        if (val == null) return new String[0];
        if (val instanceof String[] ary) return ary;
        if (val instanceof Collection<?> col) {
            return col.stream().filter(Objects::nonNull).map(String::valueOf).toArray(String[]::new);
        }
        return new String[0];
    }


}
