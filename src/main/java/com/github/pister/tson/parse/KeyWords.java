package com.github.pister.tson.parse;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by songlihuang on 2020/1/7.
 */
public class KeyWords {

    private static Map<String, TokenType> name2keywords = new HashMap<String, TokenType>();

    static {
        name2keywords.put("bool", TokenType.KW_TYPE_BOOL);
        name2keywords.put("i8", TokenType.KW_TYPE_INT8);
        name2keywords.put("i16", TokenType.KW_TYPE_INT16);
        name2keywords.put("i32", TokenType.KW_TYPE_INT32);
        name2keywords.put("i64", TokenType.KW_TYPE_INT64);
        name2keywords.put("f32", TokenType.KW_TYPE_FLOAT32);
        name2keywords.put("f64", TokenType.KW_TYPE_FLOAT64);
        name2keywords.put("c",  TokenType.KW_TYPE_CHAR);
        name2keywords.put("str", TokenType.KW_TYPE_STRING);
        name2keywords.put("date", TokenType.KW_TYPE_DATE);
        name2keywords.put("ldt", TokenType.KW_TYPE_LOCAL_DATE_TIME);
        name2keywords.put("ld", TokenType.KW_TYPE_LOCAL_DATE);
        name2keywords.put("lt", TokenType.KW_TYPE_LOCAL_TIME);
        name2keywords.put("bin", TokenType.KW_TYPE_BINARY);
        name2keywords.put("true", TokenType.VALUE_TRUE);
        name2keywords.put("false", TokenType.VALUE_FALSE);

    }

    public static TokenType getTokenTypeByName(String name) {
        return name2keywords.get(name);
    }
}
