package com.github.pister.tson;

import com.github.pister.tson.io.FastStringReader;
import com.github.pister.tson.io.ItemStringWriter;
import com.github.pister.tson.models.Item;
import com.github.pister.tson.parse.Lexer;
import com.github.pister.tson.parse.LexerReader;
import com.github.pister.tson.parse.Parser;

/**
 * Created by songlihuang on 2020/1/5.
 */
public final class Tsons {

    private Tsons() {}

    public static Object parseForObject(String text) {
        Lexer lexer = new Lexer(new LexerReader(new FastStringReader(text)));
        Parser parser = new Parser(lexer);
        Item item = parser.parse();
        return item.extract();
    }

    public static String toTsonString(Object o) {
        ItemStringWriter itemStringWriter = new ItemStringWriter();
        Item item = Item.wrap(o);
        itemStringWriter.write(item);
        return itemStringWriter.toString();
    }


}
