package com.github.pister.tson.parse;

/**
 * Created by songlihuang on 2020/1/7.
 */
public class Token {

    private TokenType tokenType;

    private Object value;

    public Token(TokenType tokenType, Object value) {
        this.tokenType = tokenType;
        this.value = value;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public Object getValue() {
        return value;
    }

}
