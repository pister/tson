package com.github.pister.tson;

import com.github.pister.tson.io.ItemStringWriter;
import com.github.pister.tson.models.Item;
import com.github.pister.tson.parse.Lexer;
import com.github.pister.tson.parse.LexerReader;
import com.github.pister.tson.parse.Parser;

import java.io.StringReader;

/**
 * Created by songlihuang on 2020/1/5.
 */
public final class Tsons {

    private Tsons() {}

    public static TsonObject parseForTson(String text) {
        Lexer lexer = new Lexer(new LexerReader(new StringReader(text)));
        Parser parser = new Parser(lexer);
        Item item = parser.parse();
        return new TsonObject(item);
    }

    public static Object parseForObject(String text) {
        return parseForTson(text).getRoot().extract();
    }

    public static String toTsonString(Object o) {
        return toTsonString(new TsonObject(o));
    }

    public static String toTsonString(TsonObject tsonObject) {
        ItemStringWriter itemStringWriter = new ItemStringWriter();
        itemStringWriter.write(tsonObject.getRoot());
        return itemStringWriter.toString();
    }

}
