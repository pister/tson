package com.github.pister.tson.parse;

/**
 * Created by songlihuang on 2020/1/7.
 */
public class ParseResult<T> {

    private boolean matches;

    private T value;

    public static <T> ParseResult<T> createMatched(T t) {
        return new ParseResult<T>(true, t);
    }

    public static <T> ParseResult<T> createNotMatch() {
        return new ParseResult<T>(false, null);
    }


    public ParseResult(boolean matches, T value) {
        this.matches = matches;
        this.value = value;
    }

    public boolean isMatches() {
        return matches;
    }

    public void setMatches(boolean matches) {
        this.matches = matches;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
