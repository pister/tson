package com.github.pister.tson.models;

import com.github.pister.tson.common.ItemType;
import com.github.pister.tson.common.ObjectUtil;
import com.github.pister.tson.common.Types;

import java.util.*;

/**
 * Created by songlihuang on 2020/1/5.
 */
public class Item {

    private ItemType type;

    private Object value;

    public Item(ItemType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public static Item wrap(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            return new Item(ItemType.STRING, o);
        }
        if (o instanceof Number) {
            return new Item(Types.numnerTypeToItemType(o.getClass()), o);
        }
        if (o instanceof Boolean) {
            return new Item(ItemType.BOOL, o);
        }
        if (o instanceof Character) {
            return new Item(ItemType.INT16, (short)((Character)o).charValue());
        }
        if (o instanceof Date) {
            return new Item(ItemType.DATE, o);
        }
        if (o instanceof Map) {
            return mapToItem((Map)o);
        }
        if (o instanceof Iterable) {
            return iterableToItem((Iterable)o);
        }
        // plain object
        Map<String, Object> properties = ObjectUtil.objectPropertiesToMap(o);
        return mapToItem(properties);
    }

    private static Item iterableToItem(Iterable it) {
        List<Item> items = new ArrayList<Item>();
        for (Object o : it) {
            Item value = wrap(o);
            items.add(value);
        }
        return new Item(ItemType.EMPTY, items);
    }

    private static Item mapToItem(Map<?, ?> m) {
        Map<String, Item> tsonMap = new LinkedHashMap<String, Item>();
        for (Map.Entry<?, ?> entry: m.entrySet()) {
            Object keyObject = entry.getKey();
            if (keyObject == null) {
                continue;
            }
            String key = keyObject.toString();
            Item value = wrap(entry.getValue());
            tsonMap.put(key, value);
        }
        return new Item(ItemType.EMPTY, tsonMap);
    }


    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isIntType() {
        switch (type) {
            case INT8:
            case INT16:
            case INT32:
            case INT64:
                return true;
            default:
                return false;
        }
    }

    public boolean isFloatType() {
        switch (type) {
            case FLOAT32:
            case FLOAT64:
                return true;
            default:
                return false;
        }
    }

    public boolean isNumberType() {
        return isIntType() || isFloatType();
    }

    public boolean getBoolean(boolean defaultValue) {
        if (type != ItemType.BOOL) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean)value;
        }
        return defaultValue;
    }

    public byte getByte(byte defaultValue) {
        if (!isIntType()) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number)value).byteValue();
        }
        return defaultValue;
    }

    public short getShort(short defaultValue) {
        if (!isIntType()) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number)value).shortValue();
        }
        return defaultValue;
    }

    public int getInt(int defaultValue) {
        if (!isIntType()) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number)value).intValue();
        }
        return defaultValue;
    }

    public long getLong(long defaultValue) {
        if (!isIntType()) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number)value).longValue();
        }
        return defaultValue;
    }

    public float getFloat(float defaultValue) {
        if (!isNumberType()) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number)value).floatValue();
        }
        return defaultValue;
    }

    public double getDouble(double defaultValue) {
        if (!isNumberType()) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number)value).doubleValue();
        }
        return defaultValue;
    }

    public String getString(String defaultValue) {
        if (type != ItemType.STRING) {
            return defaultValue;
        }
        if (value instanceof String) {
            return (String)value;
        }
        return defaultValue;
    }

    public Date getDate(Date defaultValue) {
        if (type != ItemType.DATE) {
            return defaultValue;
        }
        if (value instanceof Date) {
            return (Date)value;
        }
        return defaultValue;
    }

    public byte[] getBinary(byte[] defaultValue) {
        if (type != ItemType.BINARY) {
            return defaultValue;
        }
        if (value == null) {
            return null;
        }
        if (Types.BYTE_ARRAY_CLASS.isAssignableFrom(value.getClass())) {
            return (byte[])value;
        }
        return defaultValue;
    }



}
