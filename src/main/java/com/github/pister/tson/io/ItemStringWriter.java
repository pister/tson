package com.github.pister.tson.io;

import com.github.pister.tson.common.Constants;
import com.github.pister.tson.common.ItemType;
import com.github.pister.tson.models.Item;
import com.github.pister.tson.utils.Base629;
import com.github.pister.tson.utils.DateTimeUtil;
import com.github.pister.tson.utils.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;


/**
 * Item 树到 tson 文本的写出器。
 *
 * <p>内部实现类，外部配置请走 {@code Tsons} / {@code TsonConfig}，
 * 不要直接使用这个类。</p>
 *
 * Created by songlihuang on 2020/1/6.
 */
public class ItemStringWriter {

    /**
     * 是否忽略 null（不写出 null 字面量）。true 与老版本行为一致：
     * map 的 null value / null key 条目整个不写，list / array 的 null 元素跳过。
     *
     * <p>跳过意味着数据不再完整：map 丢条目，list/array 长度变化。老版本对
     * list 里 null 元素写出的 "[a,,b]" 本来就是解不开的坏文本，这里选择跳过
     * 至少保证输出可读。要完整保留 null，用
     * {@code Tsons.encode(o, TsonConfig.create().setWriteNullValue(true))}。</p>
     */
    private final boolean ignoreNullValue;

    /**
     * @param ignoreNullValue true 时不写 null（老版本兼容行为）
     */
    public ItemStringWriter(boolean ignoreNullValue) {
        this.ignoreNullValue = ignoreNullValue;
    }

    /**
     * 老版本兼容默认：不写 null
     */
    public ItemStringWriter() {
        this(true);
    }

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
            case CHAR:
                writeString(item.getType().getTypeName(), String.valueOf(item.getValue()));
                break;
            case STRING:
                writeString(item.getType().getTypeName(), (String)item.getValue());
                break;
            case DATE:
                String dateValue = DateTimeUtil.format((Date)item.getValue());
                writeString(item.getType().getTypeName(), dateValue);
                break;
            case LOCAL_DATE_TIME:
                writeString(item.getType().getTypeName(), DateTimeUtil.format((LocalDateTime)item.getValue(), DateTimeUtil.LOCAL_DATE_TIME_FORMAT));
                break;
            case LOCAL_DATE:
                writeString(item.getType().getTypeName(), DateTimeUtil.format((LocalDate)item.getValue(), DateTimeUtil.LOCAL_DATE_FORMAT));
                break;
            case LOCAL_TIME:
                writeString(item.getType().getTypeName(), DateTimeUtil.format((LocalTime)item.getValue(), DateTimeUtil.LOCAL_TIME_FORMAT));
                break;
            case ENUM:
                Enum enumValue = (Enum)item.getValue();
                writeEnum(item.getUserTypeName(), enumValue);
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
            case NULL:
                stringBuilder.append(item.getType().getTypeName());
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
        return new String(Base629.encode(data), Constants.DEFAULT_CHARSET);
    }

    private void writeUserType(String userTypeName) {
        int index = findTypeIndex(userTypeName);
        stringBuilder.append(Constants.TOKEN_USER_TYPE_PREFIX);
        stringBuilder.append(index);
        stringBuilder.append(Constants.TYPE_VALUE_SEP);
    }

    private void writeEnum(String userTypeName, Enum enumValue) {
        int index = findTypeIndex(userTypeName);
        stringBuilder.append(Constants.TOKEN_ENUM_PREFIX);
        stringBuilder.append(index);
        stringBuilder.append(Constants.TYPE_VALUE_SEP);
        stringBuilder.append(enumValue.name());
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
            if (ignoreNullValue && (subItem == null || subItem.getType() == ItemType.NULL)) {
                continue;
            }
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
        Map<Object, Item> map = (Map<Object, Item>)item.getValue();
        if (!StringUtil.isEmpty(item.getUserTypeName())) {
            writeUserType(item.getUserTypeName());
        }
        stringBuilder.append(Constants.MAP_BEGIN);
        boolean first = true;
        for (Map.Entry<Object, Item> entry : map.entrySet()) {
            Item valueItem = entry.getValue();
            Object key = entry.getKey();
            Item keyItem = key instanceof Item ? (Item) key : null;
            if (ignoreNullValue && (valueItem == null || valueItem.getType() == ItemType.NULL
                    || (keyItem != null && keyItem.getType() == ItemType.NULL))) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                stringBuilder.append(Constants.COMMA);
            }
            if (keyItem != null) {
                write(keyItem);
            } else {
                stringBuilder.append(key);
            }

            stringBuilder.append(Constants.COLON);
            write(valueItem);
        }
        stringBuilder.append(Constants.MAP_END);
    }

    private void writeString(String name, String value) {
        stringBuilder.append(name);
        stringBuilder.append(Constants.TYPE_VALUE_SEP);
        stringBuilder.append("\"");
        escapeTo(stringBuilder, value);
        stringBuilder.append("\"");
    }

    /**
     * 把字符串值写进引号里，反斜杠和双引号都要转义。
     *
     * <p>只转义双引号是不够的：Lexer 读到反斜杠就会把它当转义引导符，
     * \n \r \t \b \\ \" \' 按转义序列还原，其余 \X 直接丢掉反斜杠。
     * 所以值里任何一个裸反斜杠都会让解码结果和原值不一致；
     * 若它正好落在末尾，还会把闭合引号一并吃掉，导致整份文档解析失败。</p>
     *
     * <p>必须单次遍历，不能写成两次 replace 串联：先转义出来的反斜杠会被第二次再转义一遍。</p>
     */
    private static void escapeTo(StringBuilder out, String value) {
        for (int i = 0, len = value.length(); i < len; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    out.append('\\').append('\\');
                    break;
                case '"':
                    out.append('\\').append('"');
                    break;
                default:
                    out.append(c);
            }
        }
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
}
