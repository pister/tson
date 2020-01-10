package com.github.pister.tson.io;

import com.github.pister.tson.common.Constants;
import com.github.pister.tson.models.Item;
import com.github.pister.tson.utils.Base33;
import com.github.pister.tson.utils.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by songlihuang on 2020/1/6.
 */
public class ItemStringWriter {


    private boolean ignoreNullValue = true;

    private static ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(Constants.DATE_FORMAT);
        }
    };

    private int indexSeq = 0;

    private Map<String, Integer> typeIndexMap = new LinkedHashMap<String, Integer>();

    private StringBuilder stringBuilder = new StringBuilder(1024 * 4);

    private int findTypeIndex(String typeName) {
        Integer index = typeIndexMap.get(typeName);
        if (index == null) {
            int newIndex = indexSeq++;
            typeIndexMap.put(typeName, newIndex);
            return newIndex;
        }
        return index;
    }

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
        stringBuilder.append(Constants.TYPE_VALUE_SEP);
        stringBuilder.append("\"");
        stringBuilder.append(Constants.BINARY_VERSION_BASE33);
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
        return new String(Base33.encode(data), Constants.DEFAULT_CHARSET);
    }

    private void writeUserType(String userTypeName) {
        int index = findTypeIndex(userTypeName);
        stringBuilder.append(Constants.TOKEN_USER_TYPE_PREFIX);
        stringBuilder.append(index);
        stringBuilder.append(Constants.TYPE_VALUE_SEP);
    }


    private void writeList(Item item) {
        List<Item> list = (List<Item>)item.getValue();
        if (!StringUtil.isEmpty(item.getUserTypeName())) {
            writeUserType(item.getUserTypeName());
        } else if (item.isArray()) {
            stringBuilder.append(Constants.TOKEN_ARRAY_PREFIX);
            stringBuilder.append(item.getArrayDimensions());
            if (!StringUtil.isEmpty(item.getArrayComponentUserTypeName())) {
                writeUserType(item.getArrayComponentUserTypeName());
            } else {
                stringBuilder.append(item.getArrayComponentType().getTypeName());
                stringBuilder.append(Constants.TYPE_VALUE_SEP);
            }
        }
        stringBuilder.append(Constants.LIST_BEGIN);
        boolean first = true;
        for (Item subItem : list) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(Constants.COMMA);
            }
            write(subItem);
        }
        stringBuilder.append(Constants.LIST_END);
    }

    private void writeMap(Item item) {
        Map<String, Item> map = (Map<String, Item>)item.getValue();
        if (!StringUtil.isEmpty(item.getUserTypeName())) {
            writeUserType(item.getUserTypeName());
        }
        stringBuilder.append(Constants.MAP_BEGIN);
        boolean first = true;
        for (Map.Entry<String, Item> entry : map.entrySet()) {
            if (ignoreNullValue && entry.getValue() == null) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                stringBuilder.append(Constants.COMMA);
            }
            stringBuilder.append(entry.getKey());
            stringBuilder.append(Constants.COLON);
            write(entry.getValue());
        }
        stringBuilder.append(Constants.MAP_END);
    }

    private void writeString(String name, String value) {
        stringBuilder.append(name);
        stringBuilder.append(Constants.TYPE_VALUE_SEP);
        stringBuilder.append("\"");
        stringBuilder.append(value.replace("\"", "\\\""));
        stringBuilder.append("\"");
    }

    private void writeDirect(Item item) {
        stringBuilder.append(item.getType().getTypeName());
        stringBuilder.append(Constants.TYPE_VALUE_SEP);
        stringBuilder.append(item.getValue());
    }

    private String headerToString() {
        StringBuilder header = new StringBuilder();
        header.append(Constants.TOKEN_USER_TYPE_PREFIX);
        header.append(Constants.TYPES_NAME);
        header.append(Constants.MAP_BEGIN);
        boolean first = true;
        for (Map.Entry<String, Integer> entry : typeIndexMap.entrySet()) {
            String name = entry.getKey();
            Integer index =entry.getValue();
            if (first) {
                first = false;
            } else {
                header.append(Constants.COMMA);
            }
            header.append(index);
            header.append(Constants.COLON);
            header.append(name);
        }
        header.append(Constants.MAP_END);
        return header.toString();
    }

    public String toString() {
        // body
        String body = stringBuilder.toString();
        // header
        if (typeIndexMap.isEmpty()) {
            return body;
        }

        String header = headerToString();

        StringBuilder data = new StringBuilder();
        data.append(header);
        data.append("\n");
        data.append(body);
        return data.toString();
    }

    public boolean isIgnoreNullValue() {
        return ignoreNullValue;
    }

    public void setIgnoreNullValue(boolean ignoreNullValue) {
        this.ignoreNullValue = ignoreNullValue;
    }
}
