package com.github.pister.tson.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by songlihuang on 2020/1/6.
 */
public final class Types {

    private Types() {}

    public static final Class<?> BYTE_ARRAY_CLASS = (new byte[0]).getClass();

    private static final Map<Class<?>, ItemType> numberType2ItemTypes = new HashMap<Class<?>, ItemType>();

    static {
        numberType2ItemTypes.put(Byte.class, ItemType.INT8);
        numberType2ItemTypes.put(Byte.TYPE, ItemType.INT8);

        numberType2ItemTypes.put(Short.class, ItemType.INT16);
        numberType2ItemTypes.put(Short.TYPE, ItemType.INT16);

        numberType2ItemTypes.put(Integer.class, ItemType.INT32);
        numberType2ItemTypes.put(Integer.TYPE, ItemType.INT32);

        numberType2ItemTypes.put(Long.class, ItemType.INT64);
        numberType2ItemTypes.put(Long.TYPE, ItemType.INT64);

        numberType2ItemTypes.put(Float.class, ItemType.FLOAT32);
        numberType2ItemTypes.put(Float.TYPE, ItemType.FLOAT32);

        numberType2ItemTypes.put(Double.class, ItemType.FLOAT64);
        numberType2ItemTypes.put(Double.TYPE, ItemType.FLOAT64);
    }


    public static ItemType numberTypeToItemType(Class<?> clazz) {
        return numberType2ItemTypes.get(clazz);
    }
}
