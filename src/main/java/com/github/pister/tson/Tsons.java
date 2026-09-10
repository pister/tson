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

    /**
     * 全局默认配置，{@link #encode(Object)} 不传配置时使用它。
     *
     * <p>初始实例的每个开关都是老版本行为；需要改默认的应用在启动时调
     * {@link #setDefaultConfig(TsonConfig)}。返回的实例是活引用，
     * 可以就地修改，多线程场景建议只在启动阶段动它。</p>
     */
    private static volatile TsonConfig defaultConfig = new TsonConfig();

    public static TsonConfig getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * 设置 {@link #encode(Object)} 使用的全局默认配置。
     * 建议只在启动阶段调用，避免和并发中的编码互相干扰。
     */
    public static void setDefaultConfig(TsonConfig config) {
        Tsons.defaultConfig = config == null ? new TsonConfig() : config;
    }

    public static Object decode(String text) {
        // 空串和纯空白行为要一致：都返回 null。修复前 "" 返回 null
        // 而 "   " 走到解析器报"need a type before value"
        if (StringUtil.isBlank(text)) {
            return null;
        }
        Lexer lexer = new Lexer(new LexerReader(new FastStringReader(text)));
        Parser parser = new Parser(lexer);
        Item item = parser.parse();
        return item.extract();
    }

    public static String encode(Object o) {
        return encode(o, defaultConfig);
    }

    /**
     * @param config 编码配置；传 null 时跟随全局默认配置
     */
    public static String encode(Object o, TsonConfig config) {
        if (o == null) {
            return null;
        }
        TsonConfig c = config == null ? defaultConfig : config;
        ItemStringWriter itemStringWriter = new ItemStringWriter(!c.isWriteNullValue());
        Item item = Item.wrap(o);
        itemStringWriter.write(item);
        return itemStringWriter.toString();
    }


}
