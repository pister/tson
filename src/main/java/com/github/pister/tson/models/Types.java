package com.github.pister.tson.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by songlihuang on 2020/1/7.
 */
public class Types {

    private Map<Integer, String> index2types = new HashMap<Integer, String>();

    public void putType(int index, String typeName) {
        index2types.put(index, typeName);
    }

    public String getTypeName(int index) {
        return index2types.get(index);
    }

}
