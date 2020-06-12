package com.github.pister.tson.common;

import com.github.pister.tson.models.Item;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by songlihuang on 2020/1/6.
 */
public final class Types {

    private Types() {}

    public static final Class<?> BYTE_ARRAY_CLASS = (new byte[0]).getClass();

    private static final Map<Class<?>, ItemType> numberType2ItemTypes = new HashMap<Class<?>, ItemType>();
    private static final Map<Class<?>, ItemType> class2ItemTypes = new HashMap<Class<?>, ItemType>();
    private static final Map<Class<?>, ItemType> arrayComponentType2ItemTypes = new HashMap<Class<?>, ItemType>();

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

        numberType2ItemTypes.put(Character.class, ItemType.CHAR);
        numberType2ItemTypes.put(Character.TYPE, ItemType.CHAR);

        class2ItemTypes.putAll(numberType2ItemTypes);
        class2ItemTypes.put(String.class, ItemType.STRING);
        class2ItemTypes.put(Boolean.class, ItemType.BOOL);
        class2ItemTypes.put(Date.class, ItemType.DATE);
        class2ItemTypes.put(String.class, ItemType.STRING);

        arrayComponentType2ItemTypes.put(Byte.TYPE, ItemType.INT8);
        arrayComponentType2ItemTypes.put(Short.TYPE, ItemType.INT16);
        arrayComponentType2ItemTypes.put(Integer.TYPE, ItemType.INT32);
        arrayComponentType2ItemTypes.put(Long.TYPE, ItemType.INT64);
        arrayComponentType2ItemTypes.put(Float.TYPE, ItemType.FLOAT32);
        arrayComponentType2ItemTypes.put(Double.TYPE, ItemType.FLOAT64);
        arrayComponentType2ItemTypes.put(Character.TYPE, ItemType.CHAR);
        arrayComponentType2ItemTypes.put(String.class, ItemType.STRING);
        arrayComponentType2ItemTypes.put(Boolean.class, ItemType.BOOL);
        arrayComponentType2ItemTypes.put(Date.class, ItemType.DATE);
        arrayComponentType2ItemTypes.put(String.class, ItemType.STRING);
    }


    public static ItemType numberTypeToItemType(Class<?> clazz) {
        return numberType2ItemTypes.get(clazz);
    }

    public static ItemType getArrayComponentType(Class<?> clazz) {
        return arrayComponentType2ItemTypes.get(clazz);
    }

    public static ItemType classToItemType(Class<?> clazz) {
        ItemType itemType = class2ItemTypes.get(clazz);
        if (itemType != null) {
            return itemType;
        }
        if (Date.class.isAssignableFrom(clazz)) {
            return ItemType.DATE;
        }
        if (List.class.isAssignableFrom(clazz)) {
            return ItemType.LIST;
        }
        if (Map.class.isAssignableFrom(clazz)) {
            return ItemType.MAP;
        }
        return null;
    }

    public static String getUserItemTypeName(Class<?> clazz) {
        ItemType itemType = class2ItemTypes.get(clazz);
        if (itemType != null) {
            return itemType.getTypeName();
        }
        return clazz.getCanonicalName();
    }
}
