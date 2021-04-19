package com.github.pister.tson.parse;

import com.github.pister.tson.common.Constants;
import com.github.pister.tson.common.ItemType;
import com.github.pister.tson.models.Item;
import com.github.pister.tson.utils.Base629;
import com.github.pister.tson.utils.StringUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by songlihuang on 2020/1/7.
 */
public class Parser {

    private static ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(Constants.DATE_FORMAT);
        }
    };


    private Lexer lexer;

    private Map<Integer, String> types;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
    }

    public Item parse() {
        return parseRoot();
    }

    private Item parseRoot() {
        // <root> ::= <header> <item>
        this.types = header();
        return item();
    }

    private Map<Integer, String> header() {
        //  <header> ::= <types-define>?
        return typesDefine();
    }

    private Item item() {
        // <item> ::= TOKEN_ARRAY_PREFIX? <define-detail>
        boolean array = false;
        int dimensions = 0;
        ParseResult<Integer> arrayResult = arrayPrefix();
        if (arrayResult.isMatches()) {
            array = true;
            dimensions = arrayResult.getValue();
        }
        ParseResult<ItemWithType> itemResult = defineDetail();
        if (!itemResult.isMatches()) {
            throw new SyntaxException("need value define");
        }
        ItemWithType itemWithType = itemResult.getValue();
        Item item = itemWithType.item;
        DefinedType definedType = itemWithType.definedType;
        if (array) {
            if (item.getType() == ItemType.LIST) {
                item.setArray(true);
                item.setArrayDimensions(dimensions);
                if (definedType == null) {
                    throw new SyntaxException("array must need a type");
                }
                item.setArrayComponentType(definedType.itemType);
                item.setArrayComponentUserTypeName(definedType.userType);
            } else {
                throw new SyntaxException("prefix '+' only support array size, but " + item.getValue());
            }
        } else {
            if (definedType != null) {
                item.setUserTypeName(definedType.userType);
            }
        }
        return item;
    }

    private ParseResult<Integer> arrayPrefix() {
        // <array-prefix> ::= TOKEN_ARRAY_PREFIX TOKEN_VALUE_INT?
        if (!lexer.popIfMatchesType(TokenType.ARRAY_PREFIX)) {
            return ParseResult.createNotMatch();
        }

        Token token = lexer.peek();
        if (token.getTokenType() != TokenType.VALUE_INT) {
            return ParseResult.createMatched(1);
        }
        int dimensions = ((Number)token.getValue()).intValue();
        if (dimensions <= 0) {
            throw new SyntaxException("dimensions must greater than 0");
        }
        lexer.nextToken();
        return ParseResult.createMatched(dimensions);
    }


    public static class ItemWithType {
        DefinedType definedType;
        Item item;

        public ItemWithType(DefinedType definedType, Item item) {
            this.definedType = definedType;
            this.item = item;
        }

    }

    private ParseResult<ItemWithType> defineDetail() {
        // <define-detail> ::= <define-detail-content> | (<define-with-type> TOKEN_AT (<define-detail-content> | <value>))
        ParseResult<Item> itemResult = defineDetailContent();
        if (itemResult.isMatches()) {
            return ParseResult.createMatched(new ItemWithType(null, itemResult.getValue()));
        }
        ParseResult<DefinedType> definedTypeParseResult = defineWithType();
        if (!definedTypeParseResult.isMatches()) {
            throw new SyntaxException("need a type before value");
        }
        DefinedType definedType = definedTypeParseResult.getValue();
        if (!lexer.popIfMatchesType(TokenType.AT)) {
            throw new SyntaxException("need @ after type");
        }
        itemResult = defineDetailContent();
        if (itemResult.isMatches()) {
            return ParseResult.createMatched(new ItemWithType(definedType, itemResult.getValue()));
        }
        ParseResult<Object> objectParseResult = value();
        if (objectParseResult.isMatches()) {
            if (definedType.itemType == null) {
                throw new SyntaxException("need an type (not user type) with value");
            }
            Item item = new Item(definedType.itemType, objectParseResult.getValue());
            castDataForType(item);
            return ParseResult.createMatched(new ItemWithType(definedType, item));
        }
        throw new SyntaxException("need an map, list, or typed value define");
    }

    private byte[] decodeBytes(String s) {
        return Base629.decode(s.getBytes(Constants.DEFAULT_CHARSET));
    }


    private void handleBinary(Item item) {
        String s = (String)item.getValue();
        if (StringUtil.isEmpty(s)) {
            item.setValue(null);
            return;
        }
        char version = s.charAt(0);
        switch (version) {
            case Constants.BINARY_VERSION_BASE33:
                byte[] data = decodeBytes(s.substring(1));
                item.setValue(data);
                break;
            default:
                throw new RuntimeException("unknown ");
        }
    }

    private void castDataForType(Item item) {
        switch (item.getType()) {
            case DATE:
                try {
                    Date date = dateFormatThreadLocal.get().parse((String) item.getValue());
                    item.setValue(date);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                break;
            case BINARY:
                handleBinary(item);
                break;
        }
    }

    private ParseResult<DefinedType> defineWithType() {
        // <define-with-type> ::= <type> | (TOKEN_MARK TOKEN_VALUE_INT)
        ParseResult<ItemType> itemTypeResult = type();
        if (itemTypeResult.isMatches()) {
            return ParseResult.createMatched(new DefinedType(itemTypeResult.getValue(), null));
        }
        if (lexer.popIfMatchesType(TokenType.MARK)) {
            Token indexToken = lexer.nextToken();
            if (indexToken.getTokenType() != TokenType.VALUE_INT) {
                throw new SyntaxException("need an index after #");
            }
            int index = ((Number)indexToken.getValue()).intValue();
            String userName = types.get(index);
            return ParseResult.createMatched(new DefinedType(null, userName));
        }
        return ParseResult.createNotMatch();
    }

    private static class DefinedType {
        ItemType itemType;
        String userType;

        public DefinedType(ItemType itemType, String userType) {
            this.itemType = itemType;
            this.userType = userType;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;

            DefinedType that = (DefinedType) object;

            if (itemType != that.itemType) return false;
            return userType != null ? userType.equals(that.userType) : that.userType == null;
        }

        @Override
        public int hashCode() {
            int result = itemType != null ? itemType.hashCode() : 0;
            result = 31 * result + (userType != null ? userType.hashCode() : 0);
            return result;
        }
    }


    private ParseResult<Item> defineDetailContent() {
        // <define-detail-content> ::= <property-define-detail-content> | <list-define-detail-content>
        ParseResult<Map<Item, Item>> propertyDefineDetail = propertyDefineDetailContent();
        if (propertyDefineDetail.isMatches()) {
            return ParseResult.createMatched(new Item(ItemType.MAP, propertyDefineDetail.getValue()));
        }
        ParseResult<List<Item>> listDefineDetail = listDefineDetailContent();
        if (listDefineDetail.isMatches()) {
            return ParseResult.createMatched(new Item(ItemType.LIST, listDefineDetail.getValue()));
        }
        return ParseResult.createNotMatch();
    }

    private ParseResult<Map<Item, Item>> propertyDefineDetailContent() {
        // <property-define-detail-content> ::= TOKEN_PROPERTY_BEGIN <property-content> TOKEN_PROPERTY_END
        if (!lexer.popIfMatchesType(TokenType.PROPERTY_BEGIN)) {
            return ParseResult.createNotMatch();
        }
        Map<Item, Item> propertyContent = propertyContent();
        if (!lexer.popIfMatchesType(TokenType.PROPERTY_END)) {
            throw new SyntaxException("miss } after property define");
        }
        return ParseResult.createMatched(propertyContent);
    }

    private Map<Item, Item> propertyContent() {
        // <property-content> ::= (<name-item-pair> (TOKEN_COMMA <name-item-pair> )*
        Map<Item, Item> ret = new HashMap<Item, Item>();
        ParseResult<KeyAndItem> nameItem = keyItemPair();
        if (!nameItem.isMatches()) {
            return ret;
        }
        if (nameItem.getValue() == null) {
            return ret;
        }
        ret.put(nameItem.getValue().getKey(), nameItem.getValue().getItem());
        for (;;) {
            if (!lexer.popIfMatchesType(TokenType.COMMA)) {
                break;
            }
            nameItem = keyItemPair();
            if (!nameItem.isMatches()) {
                throw new SyntaxException("need an name after ,");
            }
            ret.put(nameItem.getValue().getKey(), nameItem.getValue().getItem());
        }
        return ret;
    }

    static class NameAndItem {
        String name;
        Item item;

        public NameAndItem(String name, Item item) {
            this.name = name;
            this.item = item;
        }

        public String getName() {
            return name;
        }

        public Item getItem() {
            return item;
        }
    }

    static class KeyAndItem {
        Item key;
        Item item;

        public KeyAndItem(Item key, Item item) {
            this.key = key;
            this.item = item;
        }

        public Item getKey() {
            return key;
        }

        public Item getItem() {
            return item;
        }
    }

    private ParseResult<NameAndItem> nameItemPair2() {
        // <name-item-pair> ::= TOKEN_ID TOKEN_COLON <item>
        Token idToken = lexer.nextToken();
        if (idToken.getTokenType() != TokenType.ID) {
            lexer.pushBack(idToken);
            return ParseResult.createNotMatch();
        }
        if (!lexer.popIfMatchesType(TokenType.COLON)) {
            throw new SyntaxException("miss : after name");
        }
        Item item = item();
        return ParseResult.createMatched(new NameAndItem((String)idToken.getValue(), item));
    }

    private ParseResult<KeyAndItem> keyItemPair() {
        // <key-item-pair> ::= <key-item-key> TOKEN_COLON <item>
        ParseResult<Item> keyResult = keyItemKey();
        if (!keyResult.isMatches()) {
            return ParseResult.createNotMatch();
        }
        if (keyResult.getValue() == null) {
            return ParseResult.createMatched(null);
        }
        if (!lexer.popIfMatchesType(TokenType.COLON)) {
            throw new SyntaxException("miss : after name");
        }
        Item item = item();
        return ParseResult.createMatched(new KeyAndItem(keyResult.getValue(), item));

    }

    private ParseResult<Item> keyItemKey() {
        // <key-item-key> ::= TOKEN_ID | <define-detail>
        Token idToken = lexer.nextToken();
        if (idToken.getTokenType() == TokenType.ID) {
            return ParseResult.createMatched(new Item(ItemType.STRING, idToken.getValue()));
        }
        lexer.pushBack(idToken);
        if (idToken.getTokenType() == TokenType.PROPERTY_END) {
            // End of keyItemKey
            return ParseResult.createMatched(null);
        }

        ParseResult<ItemWithType> itemWithTypeParseResult = defineDetail();
        if (!itemWithTypeParseResult.isMatches()) {
            return ParseResult.createNotMatch();
        }
        ItemWithType itemWithType = itemWithTypeParseResult.getValue();
        Item item = itemWithType.item;
        if (itemWithType.definedType != null) {
            item.setUserTypeName(itemWithType.definedType.userType);
        }
        return ParseResult.createMatched(item);
    }


    private ParseResult<List<Item>> listDefineDetailContent() {
        // <list-define-detail-content> ::= TOKEN_LIST_BEGIN <list-content> TOKEN_LIST_END
        if (!lexer.popIfMatchesType(TokenType.LIST_BEGIN)) {
            return ParseResult.createNotMatch();
        }
        ParseResult<List<Item>> result = listContent();
        if (!lexer.popIfMatchesType(TokenType.LIST_END)) {
            throw new SyntaxException("miss ] after list");
        }
        return result;
    }

    private ParseResult<List<Item>> listContent() {
        // <list-content> ::= (<item> (TOKEN_COMMA <item> )*)?
        List<Item> items = new ArrayList<Item>();
        if (lexer.peek().getTokenType() == TokenType.LIST_END) {
            return ParseResult.createMatched(items);
        }
        Item item = item();
        items.add(item);
        for (;;) {
            if (!lexer.popIfMatchesType(TokenType.COMMA)) {
                break;
            }
            item = item();
            items.add(item);
        }
        return ParseResult.createMatched(items);
    }

    private ParseResult<Object> value() {
        // <value> ::= TOKEN_VALUE_INT | TOKEN_VALUE_FLOAT | TOKEN_VALUE_TRUE | TOKEN_VALUE_FALSE | TOKEN_VALUE_STRING
        Token token = lexer.nextToken();
        switch (token.getTokenType()) {
            case VALUE_INT:
            case VALUE_FLOAT:
            case VALUE_STRING:
                return ParseResult.createMatched(token.getValue());
            case VALUE_TRUE:
                return ParseResult.createMatched((Object) Boolean.TRUE);
            case VALUE_FALSE:
                return ParseResult.createMatched((Object) Boolean.FALSE);
            default:
                lexer.pushBack(token);
                return ParseResult.createNotMatch();
        }
    }

    private ParseResult<ItemType> type() {
        // <type> ::= TOKEN_TYPE_BOOL | TOKEN_TYPE_INT8 | TOKEN_TYPE_INT16 | TOKEN_TYPE_INT32 | TOKEN_TYPE_INT64 | TOKEN_TYPE_FLOAT32 | TOKEN_TYPE_FLOAT64 | TOKEN_TYPE_STRING | TOKEN_TYPE_DATE | TOKEN_TYPE_BINARY
        Token token = lexer.nextToken();
        switch (token.getTokenType()) {
            case KW_TYPE_BOOL:
                return ParseResult.createMatched(ItemType.BOOL);
            case KW_TYPE_INT8:
                return ParseResult.createMatched(ItemType.INT8);
            case KW_TYPE_INT16:
                return ParseResult.createMatched(ItemType.INT16);
            case KW_TYPE_INT32:
                return ParseResult.createMatched(ItemType.INT32);
            case KW_TYPE_INT64:
                return ParseResult.createMatched(ItemType.INT64);
            case KW_TYPE_FLOAT32:
                return ParseResult.createMatched(ItemType.FLOAT32);
            case KW_TYPE_FLOAT64:
                return ParseResult.createMatched(ItemType.FLOAT64);
            case KW_TYPE_CHAR:
                return ParseResult.createMatched(ItemType.CHAR);
            case KW_TYPE_STRING:
                return ParseResult.createMatched(ItemType.STRING);
            case KW_TYPE_DATE:
                return ParseResult.createMatched(ItemType.DATE);
            case KW_TYPE_BINARY:
                return ParseResult.createMatched(ItemType.BINARY);
            default:
                lexer.pushBack(token);
                return ParseResult.createNotMatch();
        }
    }

    private Map<Integer, String> typesDefine() {
        // <types-define> ::= TOKEN_MARK_TYPES TOKEN_PROPERTY_BEGIN <types-define-content> TOKEN_PROPERTY_END
        if (!lexer.popIfMatchesType(TokenType.KW_MARK_TYPES)) {
            return null;
        }
        if (!lexer.popIfMatchesType(TokenType.PROPERTY_BEGIN)) {
            throw new SyntaxException("miss { after #types ");
        }
        Map<Integer, String> ret = typesDefineContent();
        if (!lexer.popIfMatchesType(TokenType.PROPERTY_END)) {
            throw new SyntaxException("miss }");
        }
        return ret;
    }


    private void handleIndexAndName(Map<Integer, String> types) {
        Token token = lexer.peek();
        if (token.getTokenType() != TokenType.VALUE_INT) {
            throw new SyntaxException("need an int value, but:" + token.getValue());
        }
        lexer.nextToken(); // pop int value
        int index = ((Number)token.getValue()).intValue();
        if (!lexer.popIfMatchesType(TokenType.COLON)) {
            throw new SyntaxException("need an ':', but: " + token.getValue());
        }
        String userTypeName = userTypeName();
        types.put(index, userTypeName);
    }

    private Map<Integer, String> typesDefineContent() {
        // <types-define-content> ::= (TOKEN_VALUE_INT TOKEN_COLON <user-type-name>) (TOKEN_COMMA TOKEN_VALUE_INT TOKEN_COLON <user-type-name>)*
        Map<Integer, String> types = new HashMap<Integer, String>();
        handleIndexAndName(types);
        for (;;) {
            if (!lexer.popIfMatchesType(TokenType.COMMA)) {
                break;
            }
            handleIndexAndName(types);
        }
        return types;
    }

    private String userTypeName() {
        // <user-type-name> ::= TOKEN_ID (TOKEN_DOT TOKEN_ID) *
        StringBuilder stringBuilder = new StringBuilder();
        Token token = lexer.peek();
        if (token.getTokenType() != TokenType.ID) {
            throw new SyntaxException("need an identifier, but:" + token.getValue());
        }
        stringBuilder.append(lexer.nextToken().getValue());
        for (;;) {
            if (!lexer.popIfMatchesType(TokenType.DOT)) {
                break;
            }
            stringBuilder.append('.');
            token = lexer.nextToken();
            if (token.getTokenType() != TokenType.ID) {
                throw new SyntaxException("need an identifier, but:" + token.getValue());
            }
            stringBuilder.append(token.getValue());
        }
        return stringBuilder.toString();
    }

}
