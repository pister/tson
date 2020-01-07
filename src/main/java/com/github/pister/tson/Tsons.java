package com.github.pister.tson;

import com.github.pister.tson.io.ItemStringWriter;

/**
 * Created by songlihuang on 2020/1/5.
 */
public final class Tsons {

    private Tsons() {}

    public static TsonObject parseForTson(String text) {

        return null;
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
