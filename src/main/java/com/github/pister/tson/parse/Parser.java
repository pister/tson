package com.github.pister.tson.parse;

import com.github.pister.tson.common.Constants;
import com.github.pister.tson.common.ItemType;
import com.github.pister.tson.models.Item;

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
        if (lexer.popIfMatchesType(TokenType.ARRAY_PREFIX)) {
            array = true;
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
                if (definedType == null) {
                    throw new SyntaxException("array must need a type");
                }
                item.setArrayComponentType(definedType.itemType);
                item.setArrayComponentUserTypeName(definedType.userType);
            } else {
                throw new SyntaxException("prefix '+' only support array size, but " + item.getValue());
            }
        } else {
            if (item.getType() == ItemType.MAP) {
                if (definedType != null) {
                    item.setUserTypeName(definedType.userType);
                }
            }
        }
        return item;
    }


    private static class ItemWithType {
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
                // TODO
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
    }


    private ParseResult<Item> defineDetailContent() {
        // <define-detail-content> ::= <property-define-detail-content> | <list-define-detail-content>
        ParseResult<Map<String, Item>> propertyDefineDetail = propertyDefineDetailContent();
        if (propertyDefineDetail.isMatches()) {
            return ParseResult.createMatched(new Item(ItemType.MAP, propertyDefineDetail.getValue()));
        }
        ParseResult<List<Item>> listDefineDetail = listDefineDetailContent();
        if (listDefineDetail.isMatches()) {
            return ParseResult.createMatched(new Item(ItemType.LIST, listDefineDetail.getValue()));
        }
        return ParseResult.createNotMatch();
    }

    private ParseResult<Map<String, Item>> propertyDefineDetailContent() {
        // <property-define-detail-content> ::= TOKEN_PROPERTY_BEGIN <property-content> TOKEN_PROPERTY_END
        if (!lexer.popIfMatchesType(TokenType.PROPERTY_BEGIN)) {
            return ParseResult.createNotMatch();
        }
        Map<String, Item> propertyContent = propertyContent();
        if (!lexer.popIfMatchesType(TokenType.PROPERTY_END)) {
            throw new SyntaxException("miss } after property define");
        }
        return ParseResult.createMatched(propertyContent);
    }

    private Map<String, Item> propertyContent() {
        // <property-content> ::= (<name-item-pair> (TOKEN_COMMA <name-item-pair> )*
        Map<String, Item> ret = new HashMap<String, Item>();
        ParseResult<NameAndItem> nameItem = nameItemPair();
        if (!nameItem.isMatches()) {
            return ret;
        }
        ret.put(nameItem.getValue().getName(), nameItem.getValue().getItem());
        for (;;) {
            if (!lexer.popIfMatchesType(TokenType.COMMA)) {
                break;
            }
            nameItem = nameItemPair();
            if (!nameItem.isMatches()) {
                throw new SyntaxException("need an name after ,");
            }
            ret.put(nameItem.getValue().getName(), nameItem.getValue().getItem());
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

    private ParseResult<NameAndItem> nameItemPair() {
        // <name-item-pair> ::= TOKEN_ID TOKEN_COLON <item>
        Token idToken = lexer.nextToken();
        if (idToken.getTokenType() != TokenType.ID) {
            return ParseResult.createNotMatch();
        }
        if (!lexer.popIfMatchesType(TokenType.COLON)) {
            throw new SyntaxException("miss : after name");
        }
        Item item = item();
        return ParseResult.createMatched(new NameAndItem((String)idToken.getValue(), item));
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
