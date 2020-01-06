package com.github.pister.tson.common;

/**
 * Created by songlihuang on 2020/1/5.
 */
public enum ItemType {

    BOOL("bool"),
    INT8("i8"),
    INT16("i16"),
    INT32("i32"),
    INT64("i64"),
    FLOAT32("f32"),
    FLOAT64("f64"),
    STRING("str"),
    DATE("date"),
    BINARY("bin"),
    EMPTY(""),
    ;

    private final String value;

    ItemType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
