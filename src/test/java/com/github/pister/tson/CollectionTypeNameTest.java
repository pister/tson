package com.github.pister.tson;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 集合类型名的写入与还原。
 *
 * <p>原来用 getCanonicalName() 写类型名，内部类的 $ 会被写成 .，
 * 得到的名字 Class.forName 加载不了 —— java.util.Arrays.ArrayList 就是这么来的，
 * 当时只能在解码端硬编码一个字符串把它挡掉。</p>
 *
 * Created by songlihuang on 2026/9/8.
 */
public class CollectionTypeNameTest extends TestCase {

    /**
     * 公开且有公开无参构造函数的集合，类型名要写对（内部类用 $），并且能原样还原
     */
    public void testInstantiableTypeIsPreserved() {
        MyMap map = new MyMap();
        map.put("k", "v");
        String encoded = Tsons.encode(map);
        assertTrue("类型名应当用 $ 分隔内部类: " + encoded,
                encoded.contains(MyMap.class.getName()));
        assertFalse("不应出现 getCanonicalName() 那种点号形式: " + encoded,
                encoded.contains("CollectionTypeNameTest.MyMap"));

        Object decoded = Tsons.decode(encoded);
        assertEquals(MyMap.class, decoded.getClass());
        assertEquals("v", ((Map) decoded).get("k"));
    }

    /**
     * 还原不出来的类型不写名字，解码端退回默认集合。
     * Arrays.asList 和 Collections.unmodifiableXxx 返回的都是私有内部类，
     * 名字写对了 newInstance 也失败，写了反而更糟。
     */
    public void testNonInstantiableTypeFallsBackToDefault() {
        String encoded = Tsons.encode(Arrays.asList("a", "b"));
        assertFalse("不可还原的类型不该写进类型表: " + encoded, encoded.contains("#types"));
        assertEquals(Arrays.asList("a", "b"), Tsons.decode(encoded));

        List<String> source = new ArrayList<String>();
        source.add("x");
        encoded = Tsons.encode(Collections.unmodifiableList(source));
        assertFalse("不可还原的类型不该写进类型表: " + encoded, encoded.contains("#types"));
        assertEquals(source, Tsons.decode(encoded));
    }

    /**
     * 存量数据里还有老版本写下的 java.util.Arrays.ArrayList，必须继续认
     */
    public void testLegacyArraysArrayListNameStillDecodes() {
        String legacy = "#types{0:java.util.Arrays.ArrayList}\n#0@[str@\"a\",str@\"b\"]";
        assertEquals(Arrays.asList("a", "b"), Tsons.decode(legacy));
    }

    /**
     * 顶层类的名字 getName() 和 getCanonicalName() 相同，存量数据不受影响
     */
    public void testTopLevelTypeNameUnchanged() {
        TreeMap<String, Object> map = new TreeMap<String, Object>();
        map.put("k", "v");
        String encoded = Tsons.encode(map);
        assertTrue(encoded.contains("java.util.TreeMap"));
        assertEquals(map, Tsons.decode(encoded));

        String legacy = "#types{0:java.util.TreeMap}\n#0@{str@\"k\":str@\"v\"}";
        assertEquals(map, Tsons.decode(legacy));
    }

    public static class MyMap extends TreeMap<String, Object> {
        private static final long serialVersionUID = 1L;
    }
}
