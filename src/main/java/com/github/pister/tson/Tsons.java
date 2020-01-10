package com.github.pister.tson;

import com.github.pister.tson.io.FastStringReader;
import com.github.pister.tson.io.ItemStringWriter;
import com.github.pister.tson.models.Item;
import com.github.pister.tson.parse.Lexer;
import com.github.pister.tson.parse.LexerReader;
import com.github.pister.tson.parse.Parser;
import com.github.pister.tson.utils.StringUtil;

/**
 * Created by songlihuang on 2020/1/5.
 */
public final class Tsons {

    private Tsons() {}

    public static Object decode(String text) {
        if (StringUtil.isEmpty(text)) {
            return null;
        }
        Lexer lexer = new Lexer(new LexerReader(new FastStringReader(text)));
        Parser parser = new Parser(lexer);
        Item item = parser.parse();
        return item.extract();
    }

    public static String encode(Object o) {
        if (o == null) {
            return null;
        }
        ItemStringWriter itemStringWriter = new ItemStringWriter();
        Item item = Item.wrap(o);
        itemStringWriter.write(item);
        return itemStringWriter.toString();
    }


}
