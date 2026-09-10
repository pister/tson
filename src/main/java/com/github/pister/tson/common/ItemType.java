package com.github.pister.tson.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    // type 字段是数组重建时 Array.newInstance 的组件类型，char 数组要的是
    // Character.TYPE（char.class），写成 Character.class 会解出 Character[] 装箱数组
    CHAR("c", Character.TYPE),
    STRING("str", String.class),
    DATE("date", Date.class),
    LOCAL_DATE_TIME("ldt", LocalDateTime.class),
    LOCAL_DATE("ld", LocalDate.class),
    LOCAL_TIME("lt", LocalTime.class),
    ENUM("", Enum.class),
    BINARY("bin", null),
    LIST("", null),
    MAP("", null),
    // null 字面量。null 和非 null 一样是"值"，没有它，容器里的 null
    // 只能被丢弃或写出非法文本，往返必然失真
    NULL("null", null),
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
