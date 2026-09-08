package com.github.pister.tson;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * map 的顺序保持。
 *
 * <p>写入端 mapToItem 一直用的 LinkedHashMap，文本里的顺序是对的；
 * 但解析出来的中间结构和还原出来的对象都用了 HashMap，顺序在读的这一侧被打散。
 * LinkedHashMap 存进去、读出来变成乱序的 HashMap，而 Map.equals 不比较顺序，
 * 所以普通的 assertEquals 往返测试完全发现不了。</p>
 *
 * Created by songlihuang on 2026/9/8.
 */
public class MapOrderTest extends TestCase {

    /**
     * key 要够多，少量 key 在 HashMap 里可能碰巧不重排，测不出问题
     */
    private static final String[] KEYS =
            {"zebra", "apple", "mango", "banana", "cherry", "date", "elder", "fig"};

    public void testLinkedHashMapKeepsInsertionOrder() {
        LinkedHashMap<String, Object> source = new LinkedHashMap<String, Object>();
        for (int i = 0; i < KEYS.length; i++) {
            source.put(KEYS[i], i);
        }
        Object decoded = Tsons.decode(Tsons.encode(source));
        assertOrder(source, decoded);
    }

    /**
     * 顺序必须在整条链路上都保住，不能只在文本里对
     */
    public void testOrderSurvivesNestedStructures() {
        LinkedHashMap<String, Object> inner = new LinkedHashMap<String, Object>();
        for (int i = 0; i < KEYS.length; i++) {
            inner.put(KEYS[i], i);
        }
        LinkedHashMap<String, Object> outer = new LinkedHashMap<String, Object>();
        outer.put("first", inner);
        outer.put("second", Arrays.asList(inner, inner));

        Map decoded = (Map) Tsons.decode(Tsons.encode(outer));
        assertEquals("外层顺序", new ArrayList<Object>(outer.keySet()),
                new ArrayList<Object>(decoded.keySet()));
        assertOrder(inner, decoded.get("first"));
        List nested = (List) decoded.get("second");
        assertOrder(inner, nested.get(0));
        assertOrder(inner, nested.get(1));
    }

    /**
     * 存量数据：老版本写下的文本本来就是插入顺序，只是读的时候被打散了，
     * 修好解码侧之后不用重写数据就能还原
     */
    public void testLegacyTextOrderIsRecovered() {
        StringBuilder text = new StringBuilder("{");
        for (int i = 0; i < KEYS.length; i++) {
            if (i > 0) {
                text.append(',');
            }
            text.append("str@\"").append(KEYS[i]).append("\":i32@").append(i);
        }
        text.append('}');

        Map decoded = (Map) Tsons.decode(text.toString());
        assertEquals(Arrays.asList(KEYS), new ArrayList<Object>(decoded.keySet()));
    }

    /**
     * 带类型名的 map 也要保序，还原出来的实例类型要对
     */
    public void testTypedMapKeepsOrder() {
        String text = "#types{0:java.util.LinkedHashMap}\n"
                + "#0@{str@\"zebra\":i32@0,str@\"apple\":i32@1,str@\"mango\":i32@2,"
                + "str@\"banana\":i32@3,str@\"cherry\":i32@4,str@\"date\":i32@5,"
                + "str@\"elder\":i32@6,str@\"fig\":i32@7}";
        Object decoded = Tsons.decode(text);
        assertEquals(LinkedHashMap.class, decoded.getClass());
        assertEquals(Arrays.asList(KEYS), new ArrayList<Object>(((Map) decoded).keySet()));
    }

    /**
     * 普通 HashMap 现在解出来是 LinkedHashMap。它仍然是 HashMap，
     * instanceof、强转、equals 都不受影响，调用方不会被打断
     */
    public void testPlainHashMapStillUsableAsHashMap() {
        Map<String, Object> source = new HashMap<String, Object>();
        source.put("k1", "v1");
        source.put("k2", 2);

        Object decoded = Tsons.decode(Tsons.encode(source));
        assertTrue("必须仍然是 HashMap", decoded instanceof HashMap);
        assertEquals(source, decoded);
        assertEquals(2, ((HashMap) decoded).size());
    }

    /**
     * 自带顺序语义的 map 不受影响
     */
    public void testTreeMapUnaffected() {
        TreeMap<String, Object> source = new TreeMap<String, Object>();
        for (String key : KEYS) {
            source.put(key, 1);
        }
        Object decoded = Tsons.decode(Tsons.encode(source));
        assertEquals(TreeMap.class, decoded.getClass());
        assertEquals(new ArrayList<Object>(source.keySet()),
                new ArrayList<Object>(((Map) decoded).keySet()));
    }

    private static void assertOrder(Map<String, Object> expected, Object actual) {
        assertTrue("应当是 Map: " + actual, actual instanceof Map);
        assertEquals("key 的顺序必须和存入时一致",
                new ArrayList<Object>(expected.keySet()),
                new ArrayList<Object>(((Map) actual).keySet()));
        assertEquals(expected, actual);
    }
}
