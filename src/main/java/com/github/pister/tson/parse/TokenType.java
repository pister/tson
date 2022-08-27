package com.github.pister.tson.parse;

/**
 * Created by songlihuang on 2020/1/7.
 */
public enum TokenType {
    END,
    ERROR,
    ID,             // name1
    PROPERTY_BEGIN, // {
    PROPERTY_END, // }
    LIST_BEGIN, // [
    LIST_END,   // ]
    COMMA,      // ,
    COLON,      // :
    AT,         // @
    DOT,        // .
    MARK,       // #
    ENUM_PREFIX,  // !
    ARRAY_PREFIX, // +
    KW_TYPE_BOOL, // bool
    KW_TYPE_INT8, // i8
    KW_TYPE_INT16, // i16
    KW_TYPE_INT32, // i32
    KW_TYPE_INT64, // i64
    KW_TYPE_FLOAT32, // f32
    KW_TYPE_FLOAT64, // f64
    KW_TYPE_CHAR,   // c
    KW_TYPE_STRING, // str
    KW_TYPE_DATE,   // date
    KW_TYPE_ENUM,   // enum
    KW_TYPE_BINARY, // bin
    KW_MARK_TYPES,     // #types
    VALUE_INT,      // 1234
    VALUE_FLOAT,     // 3.14
    VALUE_TRUE,      // true
    VALUE_FALSE,     // false
    VALUE_STRING,    // "xx hello"
    ;

}
