package com.github.pister.tson.models;

import com.github.pister.tson.common.ItemType;
import com.github.pister.tson.common.Types;
import com.github.pister.tson.utils.ItemUtil;

import java.util.Date;

/**
 * Created by songlihuang on 2020/1/5.
 */
public class Item {

    private ItemType type;

    private Object value;

    private String userTypeName;

    private boolean array;

    private ItemType arrayComponentType;

    private String arrayComponentUserTypeName;

    public Item(ItemType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public Item(ItemType type, Object value, String userTypeName) {
        this.type = type;
        this.value = value;
        this.userTypeName = userTypeName;
    }

    public static Item wrap(Object o) {
        return ItemUtil.wrapItem(o);
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

    public String getUserTypeName() {
        return userTypeName;
    }

    public void setUserTypeName(String userTypeName) {
        this.userTypeName = userTypeName;
    }

    public boolean isArray() {
        return array;
    }

    public void setArray(boolean array) {
        this.array = array;
    }

    public ItemType getArrayComponentType() {
        return arrayComponentType;
    }

    public void setArrayComponentType(ItemType arrayComponentType) {
        this.arrayComponentType = arrayComponentType;
    }

    public String getArrayComponentUserTypeName() {
        return arrayComponentUserTypeName;
    }

    public void setArrayComponentUserTypeName(String arrayComponentUserTypeName) {
        this.arrayComponentUserTypeName = arrayComponentUserTypeName;
    }
}
