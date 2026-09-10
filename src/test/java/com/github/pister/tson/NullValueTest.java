package com.github.pister.tson;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * null 写出策略的测试：默认兼容老版本，显式开启才写 null 字面量。
 *
 * <p>默认（不写 null）和老版本逐字节一致：map 的 null value / null key
 * 条目整个不写。老版本对 list 里 null 元素写出的 "[a,,b]" 是解不开的坏文本，
 * 不存在可保留的兼容行为，兼容模式下选择跳过该元素（list 长度会变），
 * 需要完整保留 null 时用 encode(o, TsonConfig.create().setWriteNullValue(true))。</p>
 *
 * <p>解码侧无条件认 null 字面量：老数据本来就不含 null，不受默认值影响；
 * 新写出的含 null 数据只要读端也是新版本就能读。</p>
 *
 * <p>null 是否写出通过 {@link TsonConfig} 配置，后续新的特性开关
 * 也挂在这个类上，避免调用点出现一串 boolean。</p>
 *
 * Created by songlihuang on 2026/9/10.
 */
public class NullValueTest extends TestCase {

    /**
     * 兼容默认：map 的 null value 条目不写，输出和老版本逐字节一致。
     * 黄金样本取自修复前版本的实测输出。
     */
    public void testDefaultOutputMatchesOldVersionByteForByte() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("k1", "v1");
        map.put("k2", null);
        map.put("k3", "v3");
        assertEquals("{str@\"k1\":str@\"v1\",str@\"k3\":str@\"v3\"}", Tsons.encode(map));
    }

    /**
     * 兼容默认：null key 的条目也不写（老版本行为）
     */
    public void testDefaultDropsNullKey() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(null, "v");
        map.put("k", "v2");
        assertEquals("{str@\"k\":str@\"v2\"}", Tsons.encode(map));
    }

    /**
     * 兼容默认：list 跳过 null 元素。老版本在这里写出的是 "[a,,b]" 坏文本，
     * 兼容模式至少保证输出可读、可解码，代价是长度变化。
     */
    public void testDefaultSkipsListNullElement() {
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add(null);
        list.add("b");
        assertEquals("[str@\"a\",str@\"b\"]", Tsons.encode(list));
    }

    /**
     * 兼容默认下 bean 的 null 字段不写，解码后字段仍是默认 null，往返相等
     */
    public void testDefaultBeanNullFieldRoundTrip() {
        FooObject bean = new FooObject();
        bean.setName(null);
        bean.setAge(7);
        assertEquals(bean, Tsons.decode(Tsons.encode(bean)));
    }

    /**
     * 开启 null 写出，null 和非 null 一样是"值"，完整往返
     */
    public void testWriteNullListRoundTrip() {
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add(null);
        list.add("b");
        assertEquals(list, Tsons.decode(Tsons.encode(list, TsonConfig.create().setWriteNullValue(true))));

        List<String> onlyNull = new ArrayList<String>();
        onlyNull.add(null);
        List<String> back = (List<String>) Tsons.decode(Tsons.encode(onlyNull, TsonConfig.create().setWriteNullValue(true)));
        assertEquals(1, back.size());
        assertNull(back.get(0));
    }

    public void testWriteNullMapRoundTrip() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("k1", "v1");
        map.put("k2", null);
        map.put("k3", "v3");
        assertEquals(map, Tsons.decode(Tsons.encode(map, TsonConfig.create().setWriteNullValue(true))));
    }

    public void testWriteNullKeyRoundTrip() {
        Map<String, String> map = new HashMap<String, String>();
        map.put(null, "v");
        assertEquals(map, Tsons.decode(Tsons.encode(map, TsonConfig.create().setWriteNullValue(true))));
    }

    public void testWriteNullArrayRoundTrip() {
        String[] array = new String[]{"a", null, "b"};
        String[] back = (String[]) Tsons.decode(Tsons.encode(array, TsonConfig.create().setWriteNullValue(true)));
        assertEquals(array.length, back.length);
        assertEquals("a", back[0]);
        assertNull(back[1]);
        assertEquals("b", back[2]);
    }

    public void testWriteNullBeanFieldRoundTrip() {
        FooObject bean = new FooObject();
        bean.setName(null);
        bean.setAge(7);
        assertEquals(bean, Tsons.decode(Tsons.encode(bean, TsonConfig.create().setWriteNullValue(true))));
    }

    /**
     * JVM 级默认开关：设置后 encode(o) 跟随，还原后恢复兼容默认
     */
    public void testWriteNullDefaultToggle() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("k", null);
        try {
            Tsons.setDefaultConfig(TsonConfig.create().setWriteNullValue(true));
            assertEquals(map, Tsons.decode(Tsons.encode(map)));
        } finally {
            Tsons.setDefaultConfig(TsonConfig.create());
        }
        assertEquals("{}", Tsons.encode(map));
    }

    /**
     * 手写的 null 字面量必须能直接解（解码侧无条件支持）
     */
    public void testDecodeNullLiteral() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("k", null);
        assertEquals(map, Tsons.decode("{str@\"k\":null}"));
    }

    /**
     * 顶层 null 字面量解出 null（与 decode("") 的既有语义一致）
     */
    public void testDecodeRootNull() {
        assertNull(Tsons.decode("null"));
    }

    /**
     * 不含 null 的值编码结果必须和老版本逐字节一致，存量数据才读得回来
     */
    public void testEncodingUnchangedWithoutNull() {
        assertEquals("str@\"abc\"", Tsons.encode("abc"));
        assertEquals("[str@\"a\",str@\"b\"]", Tsons.encode(java.util.Arrays.asList("a", "b")));
        assertEquals("{str@\"k\":str@\"v\"}", Tsons.encode(java.util.Collections.singletonMap("k", "v")));
    }

    /**
     * 配置类默认值必须与老版本兼容：不写 null
     */
    public void testConfigDefaultIsCompatible() {
        assertFalse(new TsonConfig().isWriteNullValue());
        assertFalse(TsonConfig.create().isWriteNullValue());
    }

    /**
     * encode(o, null) 跟随全局默认配置，不抛 NPE
     */
    public void testNullConfigFallsBackToDefault() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("k", null);
        assertEquals("{}", Tsons.encode(map, null));
    }

    public static class FooObject {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FooObject that = (FooObject) o;
            return age == that.age && !(name != null ? !name.equals(that.name) : that.name != null);
        }

        @Override
        public int hashCode() {
            return 31 * (name != null ? name.hashCode() : 0) + age;
        }
    }

}
