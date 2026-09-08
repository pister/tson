package com.github.pister.tson;

import com.github.pister.tson.parse.SyntaxException;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字符串转义的往返测试。
 *
 * <p>回归的是这个 bug：编码只转义了双引号，没转义反斜杠，而 Lexer 会解释反斜杠。
 * 结果是值里只要出现一个反斜杠，往返必然失真 —— \d{16} 解出来变成 d{16}，
 * a\n 变成真实换行符，末尾的反斜杠会吃掉闭合引号让整份文档解析失败。</p>
 *
 * Created by songlihuang on 2026/9/8.
 */
public class StringEscapeTest extends TestCase {

    /**
     * 值里的反斜杠必须原样带回来
     */
    public void testBackslashRoundTrip() {
        assertRoundTrip("C:\\Users\\name\\ktp.jpg");
        assertRoundTrip("\\d{16}");
        assertRoundTrip("{\"nik\":\"32\\/01\"}");
        assertRoundTrip("O\\'Brien");
        assertRoundTrip("\\");
        assertRoundTrip("\\\\");
        assertRoundTrip("\\\\\\");
    }

    /**
     * 反斜杠后面跟着转义序列里的字母时，不能被当成转义还原掉
     */
    public void testBackslashFollowedByEscapeLetter() {
        assertRoundTrip("a\\nb");
        assertRoundTrip("a\\rb");
        assertRoundTrip("a\\tb");
        assertRoundTrip("a\\bb");
        assertRoundTrip("a\\\"b");
        assertRoundTrip("a\\'b");
        // 真正的控制字符本身也要能往返
        assertRoundTrip("a\nb");
        assertRoundTrip("a\tb");
    }

    /**
     * 末尾反斜杠原来会吃掉闭合引号，让整份文档解析失败
     */
    public void testTrailingBackslashDoesNotBreakDocument() {
        assertRoundTrip("尾部反斜杠\\");

        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("remark", "trailing\\");
        map.put("code", 500);
        Object decoded = Tsons.decode(Tsons.encode(map));
        assertEquals(map, decoded);
    }

    /**
     * 三种载体都走同一条编码路径，都要覆盖到
     */
    public void testBackslashInEveryCarrier() {
        String value = "a\\b\\";

        assertRoundTrip(value);

        FooObject bean = new FooObject();
        bean.setName(value);
        assertEquals(value, ((FooObject) Tsons.decode(Tsons.encode(bean))).getName());

        Map<String, Object> asValue = new LinkedHashMap<String, Object>();
        asValue.put("k", value);
        assertEquals(asValue, Tsons.decode(Tsons.encode(asValue)));

        Map<String, Object> asKey = new LinkedHashMap<String, Object>();
        asKey.put(value, "v");
        assertEquals(asKey, Tsons.decode(Tsons.encode(asKey)));
    }

    /**
     * 穷举反斜杠、双引号和几个转义目标字母的所有 1~4 长度组合。
     * 这个 bug 的表现和反斜杠的个数、位置强相关，逐个手写覆盖不全。
     */
    public void testExhaustiveCombinations() {
        char[] alphabet = {'\\', '"', 'n', 'b', 'a'};
        List<String> all = new ArrayList<String>();
        generate(alphabet, "", 4, all);
        assertEquals(780, all.size());
        for (String s : all) {
            assertRoundTrip(s);

            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put(s, s);
            assertEquals("map 往返失真: " + describe(s), map, Tsons.decode(Tsons.encode(map)));
        }
    }

    /**
     * 不含反斜杠的值编码结果必须和修复前逐字节一致，存量数据才读得回来
     */
    public void testEncodingUnchangedWithoutBackslash() {
        assertEquals("str@\"abc\"", Tsons.encode("abc"));
        assertEquals("str@\"a\\\"b\"", Tsons.encode("a\"b"));
        assertEquals("str@\"中文\"", Tsons.encode("中文"));
        assertEquals("str@\"\"", Tsons.encode(""));
    }

    /**
     * 未闭合的字符串必须报错，不能把读到一半的内容当成完整值返回。
     * 数据库字段截断、文本被拦腰砍断都会走到这里，静默返回残缺值比抛异常危险得多。
     */
    public void testUnterminatedStringIsRejected() {
        assertSyntaxError("str@\"unterminated");
        assertSyntaxError("str@\"dangling escape\\");
        assertSyntaxError("#types{0:java.util.HashMap}\n#0@{str@\"k\":str@\"被截断的值");
    }

    private static void assertSyntaxError(String text) {
        try {
            Object decoded = Tsons.decode(text);
            fail("本该报错却静默返回了: " + decoded);
        } catch (SyntaxException e) {
            assertTrue("报错信息要说清楚是未闭合: " + e.getMessage(),
                    e.getMessage() != null && e.getMessage().contains("unterminated"));
        }
    }

    private static void assertRoundTrip(String value) {
        String encoded = Tsons.encode(value);
        Object decoded = Tsons.decode(encoded);
        assertEquals("往返失真: " + describe(value) + "，编码为 " + encoded,
                value, decoded);
    }

    private static void generate(char[] alphabet, String prefix, int maxLength, List<String> out) {
        if (prefix.length() > 0) {
            out.add(prefix);
        }
        if (prefix.length() == maxLength) {
            return;
        }
        for (char c : alphabet) {
            generate(alphabet, prefix + c, maxLength, out);
        }
    }

    /**
     * 断言失败信息里直接打控制字符看不出差异，转成可见形式
     */
    private static String describe(String value) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.append("]").toString();
    }

    public static class FooObject {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
