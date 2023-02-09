package org.eipgrid.jql.util;

import java.util.LinkedHashMap;

public class KVEntity extends LinkedHashMap<String, Object> {

    public static KVEntity of(String key, Object value) {
        KVEntity entity = new KVEntity();
        entity.put(key, value);
        return entity;
    }

}
