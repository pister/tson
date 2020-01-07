package com.github.pister.tson.parse;

import com.github.pister.tson.common.ItemType;
import com.github.pister.tson.models.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by songlihuang on 2020/1/7.
 */
public class Parser {

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
        // <item> ::= <define> | <element>
        ParseResult<Item> itemResult = define();
        if (itemResult.isMatches()) {
            return itemResult.getValue();
        }
        return element();
    }

    private ParseResult<Item> define() {
        // <define> ::= TOKEN_ARRAY_PREFIX? <define-detail>
        boolean array = false;
        if (lexer.popIfMatchesType(TokenType.ARRAY_PREFIX)) {
            array = true;
        }
        ParseResult<Item> itemResult = defineDetail();
        if (!itemResult.isMatches()) {
            return ParseResult.createNotMatch();
        }
        itemResult.getValue().setArray(array);
        if (array && itemResult.getValue().getType() == ItemType.MAP) {
            throw new SyntaxException("property type not support array mark '+'");
        }
        return itemResult;
    }


    private ParseResult<Item> defineDetail() {
        // <define-detail> ::= (TOKEN_MARK TOKEN_VALUE_INT)? <define-detail-content>
        String userType = null;
        if (lexer.popIfMatchesType(TokenType.MARK)) {
            Token indexToken = lexer.nextToken();
            if (indexToken.getTokenType() != TokenType.VALUE_INT) {
                throw new SyntaxException("need a int index value after #");
            }
            int index = (Integer) indexToken.getValue();
            userType = this.types.get(index);
        }
        ParseResult<Item> itemResult = defineDetailContent();
        if (!itemResult.isMatches()) {
            return ParseResult.createNotMatch();
        }
        itemResult.getValue().setUserTypeName(userType);
        return itemResult;
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
            return new ParseResult<Map<String, Item>>(false, null);
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


    private Item element() {
        // <element> ::= <type> TOKEN_AT <value>
        ItemType type = type();
        if (!lexer.popIfMatchesType(TokenType.AT)) {
            throw new SyntaxException("miss @ after type");
        }
        Object value = value();
        return new Item(type, value);
    }

    private Object value() {
        // <value> ::= TOKEN_VALUE_INT | TOKEN_VALUE_FLOAT | TOKEN_VALUE_TRUE | TOKEN_VALUE_FALSE | TOKEN_VALUE_STRING
        Token token = lexer.nextToken();
        switch (token.getTokenType()) {
            case VALUE_INT:
            case VALUE_FLOAT:
            case VALUE_TRUE:
            case VALUE_FALSE:
            case VALUE_STRING:
                return token.getValue();
            default:
                throw new SyntaxException("need a value");
        }
    }

    private ItemType type() {
        // <type> ::= TOKEN_TYPE_BOOL | TOKEN_TYPE_INT8 | TOKEN_TYPE_INT16 | TOKEN_TYPE_INT32 | TOKEN_TYPE_INT64 | TOKEN_TYPE_FLOAT32 | TOKEN_TYPE_FLOAT64 | TOKEN_TYPE_STRING | TOKEN_TYPE_DATE | TOKEN_TYPE_BINARY
        Token token = lexer.nextToken();
        switch (token.getTokenType()) {
            case KW_TYPE_BOOL:
                return ItemType.BOOL;
            case KW_TYPE_INT8:
                return ItemType.INT8;
            case KW_TYPE_INT16:
                return ItemType.INT16;
            case KW_TYPE_INT32:
                return ItemType.INT32;
            case KW_TYPE_INT64:
                return ItemType.INT64;
            case KW_TYPE_FLOAT32:
                return ItemType.FLOAT32;
            case KW_TYPE_FLOAT64:
                return ItemType.FLOAT64;
            case KW_TYPE_STRING:
                return ItemType.STRING;
            case KW_TYPE_DATE:
                return ItemType.DATE;
            case KW_TYPE_BINARY:
                return ItemType.BINARY;
            default:
                throw new SyntaxException("need a type");
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
        int index = (Integer)token.getValue();
        if (!lexer.popIfMatchesType(TokenType.COLON)) {
            throw new SyntaxException("need an ':', but:" + token.getValue());
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
