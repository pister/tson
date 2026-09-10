package com.github.pister.tson;

import junit.framework.TestCase;

/**
 * NaN / Infinity 的回归测试。
 *
 * <p>回归的 bug：编码端直接写 Double.toString 的输出（f64@NaN、f64@-Infinity），
 * 编码"成功"，但词法层把 NaN 当普通标识符、把 -Infinity 当非法负号，
 * 解码端抛 SyntaxException —— 生产者埋雷，消费者引爆。</p>
 *
 * <p>修复方式：'-Infinity' 在词法层识别；NaN / Infinity 作为标识符仅在
 * 类型值位置（type@ 之后）被解释为浮点值，不碰枚举名、map key 的标识符路径，
 * 所以枚举常量叫 Infinity 之类的名字不受影响。</p>
 *
 * Created by songlihuang on 2026/9/10.
 */
public class SpecialDoubleTest extends TestCase {

    public void testNanRoundTrip() {
        double back = (Double) Tsons.decode(Tsons.encode(Double.NaN));
        assertTrue("NaN 往返后变成: " + back, Double.isNaN(back));
    }

    public void testPositiveInfinityRoundTrip() {
        assertEquals(Double.POSITIVE_INFINITY, Tsons.decode(Tsons.encode(Double.POSITIVE_INFINITY)));
    }

    public void testNegativeInfinityRoundTrip() {
        assertEquals(Double.NEGATIVE_INFINITY, Tsons.decode(Tsons.encode(Double.NEGATIVE_INFINITY)));
    }

    public void testFloatSpecialValuesRoundTrip() {
        assertTrue(Float.isNaN((Float) Tsons.decode(Tsons.encode(Float.NaN))));
        assertEquals(Float.POSITIVE_INFINITY, Tsons.decode(Tsons.encode(Float.POSITIVE_INFINITY)));
        assertEquals(Float.NEGATIVE_INFINITY, Tsons.decode(Tsons.encode(Float.NEGATIVE_INFINITY)));
    }

    /**
     * 手写的特殊值文本必须能直接解
     */
    public void testDecodeLiteral() {
        assertTrue(Double.isNaN((Double) Tsons.decode("f64@NaN")));
        assertEquals(Double.POSITIVE_INFINITY, Tsons.decode("f64@Infinity"));
        assertEquals(Double.NEGATIVE_INFINITY, Tsons.decode("f64@-Infinity"));
    }

    /**
     * 特殊值藏在容器里也要能整体往返
     */
    public void testSpecialValuesInContainer() {
        java.util.List<Object> list = new java.util.ArrayList<Object>();
        list.add(Double.NaN);
        list.add(Double.NEGATIVE_INFINITY);
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<String, Object>();
        map.put("nan", Double.NaN);
        map.put("negInf", Double.NEGATIVE_INFINITY);
        list.add(map);

        java.util.List<Object> back = (java.util.List<Object>) Tsons.decode(Tsons.encode(list));
        assertTrue(Double.isNaN((Double) back.get(0)));
        assertEquals(Double.NEGATIVE_INFINITY, back.get(1));
        java.util.Map<String, Object> mapBack = (java.util.Map<String, Object>) back.get(2);
        assertTrue(Double.isNaN((Double) mapBack.get("nan")));
        assertEquals(Double.NEGATIVE_INFINITY, mapBack.get("negInf"));
    }

    /**
     * 名字恰好叫 NaN / Infinity 的枚举常量走的是标识符路径，不能被浮点解释劫持
     */
    public void testEnumNamedInfinityIsNotHijacked() {
        Object decoded = Tsons.decode("#types{0:com.github.pister.tson.SpecialDoubleTest$TrickyEnum}\n!0@Infinity");
        assertEquals(TrickyEnum.Infinity, decoded);
    }

    public enum TrickyEnum {
        Infinity, NaN
    }

}
