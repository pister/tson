package com.github.pister.tson.common;

import java.util.Date;

/**
 * Created by songlihuang on 2020/1/5.
 */
public enum ItemType {

    BOOL("bool", boolean.class),
    INT8("i8", byte.class),
    INT16("i16", short.class),
    INT32("i32", int.class),
    INT64("i64", long.class),
    FLOAT32("f32", float.class),
    FLOAT64("f64", double.class),
    STRING("str", String.class),
    DATE("date", Date.class),
    BINARY("bin", null),
    LIST("", null),
    MAP("", null),
    ;

    private final String typeName;

    private Class<?> type;

    ItemType(String typeName, Class<?> type) {
        this.typeName = typeName;
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }
}
