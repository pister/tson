package com.github.pister.tson.io;

import com.github.pister.tson.common.Tokens;
import com.github.pister.tson.models.Item;
import com.github.pister.tson.utils.Base33;
import com.github.pister.tson.utils.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by songlihuang on 2020/1/6.
 */
public class ItemStringWriter {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private static final Charset DEFAULT_CHARSET = Charset.forName("utf-8");

    private static ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(DATE_FORMAT);
        }
    };

    private StringBuilder stringBuilder = new StringBuilder(1024 * 4);

    public void write(Item item) {
        if (item == null) {
            return;
        }
        switch (item.getType()) {
            case BOOL:
            case INT8:
            case INT16:
            case INT32:
            case INT64:
            case FLOAT32:
            case FLOAT64:
                writeDirect(item);
                break;
            case STRING:
                writeString(item.getType().getTypeName(), (String)item.getValue());
                break;
            case DATE:
                String dateValue = dateFormatThreadLocal.get().format((Date)item.getValue());
                writeString(item.getType().getTypeName(), dateValue);
                break;
            case LIST:
                writeList(item);
                break;
            case MAP:
                writeMap(item);
                break;
            case BINARY:
                writeBinary(item);
                break;
        }
    }

    private void writeBinary(Item item) {
        stringBuilder.append(item.getType().getTypeName());
        stringBuilder.append(Tokens.TYPE_VALUE_SEP);
        stringBuilder.append("\"");
        writeData(item.getValue());
        stringBuilder.append("\"");
    }

    private void writeData(Object value) {
        byte[] data;
        if (value instanceof byte[]) {
            data = (byte[])value;
        } else if (value instanceof InputStream) {
            try {
                data = IoUtil.readAll((InputStream)value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new UnsupportedOperationException("not support binary type for:" + value);
        }
        if (data == null) {
            return;
        }
        stringBuilder.append(encodeBytes(data));
    }

    private String encodeBytes(byte[] data) {
        return new String(Base33.encode(data), DEFAULT_CHARSET);
    }

    private byte[] decodeBytes(String s) {
        return Base33.decode(s.getBytes(DEFAULT_CHARSET));
    }

    private void writeList(Item item) {
        List<Item> list = (List<Item>)item.getValue();
        if (!StringUtil.isEmpty(item.getUserTypeName())) {
            if (item.isArray()) {
                stringBuilder.append(Tokens.TOKEN_ARRAY_PREFIX);
            } else {
                stringBuilder.append(Tokens.TOKEN_USER_CLASS_PREFIX);
            }
            stringBuilder.append(item.getUserTypeName());
        }
        stringBuilder.append(Tokens.LIST_BEGIN);
        boolean first = true;
        for (Item subItem : list) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(Tokens.COMMA);
            }
            write(subItem);
        }
        stringBuilder.append(Tokens.LIST_END);
    }

    private void writeMap(Item item) {
        Map<String, Item> map = (Map<String, Item>)item.getValue();
        if (!StringUtil.isEmpty(item.getUserTypeName())) {
            stringBuilder.append(Tokens.TOKEN_USER_CLASS_PREFIX);
            stringBuilder.append(item.getUserTypeName());
        }
        stringBuilder.append(Tokens.MAP_BEGIN);
        boolean first = true;
        for (Map.Entry<String, Item> entry : map.entrySet()) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(Tokens.COMMA);
            }
            stringBuilder.append(entry.getKey());
            stringBuilder.append(Tokens.COLON);
            write(entry.getValue());
        }
        stringBuilder.append(Tokens.MAP_END);
    }

    private void writeString(String name, String value) {
        stringBuilder.append(name);
        stringBuilder.append(Tokens.TYPE_VALUE_SEP);
        stringBuilder.append("\"");
        stringBuilder.append(value.replace("\"", "\\\""));
        stringBuilder.append("\"");
    }

    private void writeDirect(Item item) {
        stringBuilder.append(item.getType().getTypeName());
        stringBuilder.append(Tokens.TYPE_VALUE_SEP);
        stringBuilder.append(item.getValue());
    }

    public String toString() {
        return stringBuilder.toString();
    }

}
