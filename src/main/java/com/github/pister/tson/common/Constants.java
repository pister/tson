package com.github.pister.tson.common;

import java.nio.charset.Charset;

/**
 * Created by songlihuang on 2020/1/6.
 */
public interface Constants {

    String TYPE_VALUE_SEP = "@";

    String LIST_BEGIN = "[";

    String LIST_END = "]";

    String MAP_BEGIN = "{";

    String MAP_END = "}";

    String COMMA = ",";

    String COLON = ":";

    String TOKEN_USER_TYPE_PREFIX = "#";

    String TOKEN_ARRAY_PREFIX = "+";

    String TOKEN_ENUM_PREFIX = "!";

    String TYPES_NAME = "types";

    char BINARY_VERSION_BASE33 = '1';

    Charset DEFAULT_CHARSET = Charset.forName("utf-8");


}
