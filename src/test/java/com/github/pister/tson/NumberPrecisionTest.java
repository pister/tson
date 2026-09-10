package com.github.pister.tson;

import junit.framework.TestCase;

import java.util.Random;

/**
 * 数字词法解析的回归测试。
 *
 * <p>回归的是这两个 bug：</p>
 * <p>1. 指数前没有小数点时（如 1e3），E 分支没有关掉整数部分状态，
 * 指数数字被拼进尾数 —— i32@1e3 解出 13，f64@5e10 解出 510.0，静默错值。</p>
 * <p>2. 小数和指数用浮点累加/累乘实现，误差逐步放大，往返后 bit 级不一致
 * （修复前随机 double 约 86% 往返失真）。一个"类型不丢失"的持久化格式，
 * 数值本身更不能丢。</p>
 *
 * <p>修复方式：词法层只负责收集数字的原始字符，转换交给
 * Long.parseLong / Double.parseDouble，它们是精确定义的标准语义。</p>
 *
 * Created by songlihuang on 2026/9/10.
 */
public class NumberPrecisionTest extends TestCase {

    /**
     * 指数前没有小数点的科学计数法，指数必须真的当指数用
     */
    public void testExponentWithoutDot() {
        assertEquals(1000, Tsons.decode("i32@1e3"));
        assertEquals(100, Tsons.decode("i32@1e2"));
        assertEquals(100000.0, Tsons.decode("f64@1E5"));
        assertEquals(5.0E10, Tsons.decode("f64@5e10"));
        assertEquals(1.0E10, Tsons.decode("f64@1e10"));
    }

    /**
     * 指数可以是负数，也可以带正号。
     * 修复前 "1e+5" 会把 '+' 当数字位累进指数，解出 1e45。
     */
    public void testExponentSign() {
        assertEquals(0.001, Tsons.decode("f64@1e-3"));
        assertEquals(1.0E5, Tsons.decode("f64@1e+5"));
        assertEquals(-1.24E-12, Tsons.decode("f64@-1.24e-12"));
    }

    /**
     * 编码器自己写出的科学计数法必须精确还原（Double.toString 的完整 17 位输出）
     */
    public void testExhaustiveDoubleRoundTrip() {
        double[] values = {
                123.456, 0.1, 3.14, 0.3, -1.24e-12, 3.20e13,
                -9.773730481407064E-83,
                5.353072455204317E72,
                4.967612493611081E33,
                6.132869304281319E146,
                1.1878217972734457E32,
        };
        for (double d : values) {
            assertBitEquals(d, Tsons.decode(Tsons.encode(d)));
        }
    }

    /**
     * 固定种子的随机 double 往返必须 bit 级一致。
     * 修复前这一段会大面积失真，所以用首个失败即报告的断言。
     */
    public void testRandomDoubleRoundTrip() {
        Random r = new Random(20260910);
        for (int i = 0; i < 5000; i++) {
            double d = Double.longBitsToDouble(r.nextLong());
            if (Double.isNaN(d) || Double.isInfinite(d) || d == 0) {
                continue;
            }
            assertBitEquals(d, Tsons.decode(Tsons.encode(d)));
        }
    }

    /**
     * long 边界值必须精确。Long.MIN_VALUE 修复前靠两次溢出负负得正碰巧正确，
     * 现在改为标准 parseLong，行为从巧合变成保证。
     */
    public void testLongBoundaries() {
        assertEquals(Long.MIN_VALUE, Tsons.decode(Tsons.encode(Long.MIN_VALUE)));
        assertEquals(Long.MAX_VALUE, Tsons.decode(Tsons.encode(Long.MAX_VALUE)));
        assertEquals(Integer.MAX_VALUE, Tsons.decode(Tsons.encode(Integer.MAX_VALUE)));
        assertEquals(Integer.MIN_VALUE, Tsons.decode(Tsons.encode(Integer.MIN_VALUE)));
    }

    /**
     * 超出 long 范围的整数文本必须报错，不能静默回绕成别的数。
     * 修复前 i64@99999999999999999999 会溢出成一个随机值。
     */
    public void testOverlongIntegerIsRejected() {
        try {
            Object decoded = Tsons.decode("i64@99999999999999999999");
            fail("本该报错却解出了: " + decoded);
        } catch (Exception e) {
            // 语法错误即可，具体类型不约束
        }
    }

    /**
     * 超大指数不能循环累乘（修复前会空转约十亿次），也不该报错 ——
     * 标准语义是饱和为 Infinity。
     */
    public void testHugeExponentSaturates() {
        Object decoded = Tsons.decode("f64@1.5e999999999");
        assertEquals(Double.POSITIVE_INFINITY, decoded);
    }

    /**
     * 前导零超长数字必须正确解出。
     *
     * <p>回归 1.5 快速路径引入的崩溃：溢出守护只看数值不看位数，
     * acc 被前导零压住不增长，永远不触发溢出守护，第 21 个数字写
     * consumed[20] 直接 ArrayIndexOutOfBoundsException。
     * 定宽补零的数字 ID（Excel 导出、大型机系统）就是这种形态。</p>
     */
    public void testLongLeadingZeros() {
        assertEquals(1, Tsons.decode("i32@0000000000000000000001"));
        assertEquals(Long.valueOf(-1L), Tsons.decode("i64@-00000000000000000000001"));
        assertEquals(512, Tsons.decode("i32@0000000000000000000000000000000000000512"));
    }

    /**
     * 窄类型整数超范围必须报错，不能静默回绕。
     * 修复前 i8@128 byteValue() 回绕成 -128、i8@1000 回绕成 -24，
     * 坏数据看起来像好数据。
     */
    public void testNarrowIntegerOutOfRangeIsRejected() {
        assertOutOfRange("i8@128");
        assertOutOfRange("i8@-129");
        assertOutOfRange("i16@32768");
        assertOutOfRange("i32@2147483648");
        assertOutOfRange("i32@-2147483649");
    }

    /**
     * 整数类型收到浮点/NaN 字面量必须报错，不能截断取整
     * （修复前 i8@1.5 byteValue() 静默变 1）
     */
    public void testIntegerTypeRejectsFloatLiteral() {
        assertRejected("i8@1.5", "integer");
        assertRejected("i64@NaN", "integer");
        assertRejected("i32@Infinity", "integer");
    }

    /**
     * 窄类型的合法边界值（含两端）必须原样解出（守护性断言）
     */
    public void testNarrowIntegerBoundariesStillWork() {
        assertEquals(Byte.valueOf((byte) 127), Tsons.decode("i8@127"));
        assertEquals(Byte.valueOf((byte) -128), Tsons.decode("i8@-128"));
        assertEquals(Short.valueOf((short) 32767), Tsons.decode("i16@32767"));
        assertEquals(Integer.valueOf(2147483647), Tsons.decode("i32@2147483647"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Tsons.decode("i64@" + Long.MIN_VALUE));
    }

    /**
     * f32 收到超出 float 范围的有限值必须报错，不能静默饱和成 Infinity。
     * NaN / Infinity 本身合法，照常通过（见 SpecialDoubleTest）。
     */
    public void testFloatOutOfRangeIsRejected() {
        assertOutOfRange("f32@1e40");
        assertOutOfRange("f32@-1e40");
        // 合理范围不受影响：1e38 < Float.MAX_VALUE(约3.4e38)
        assertEquals(1.0e38f, Tsons.decode("f32@1e38"));
    }

    private static void assertOutOfRange(String text) {
        assertRejected(text, "out of range");
    }

    private static void assertRejected(String text, String expectedFragment) {
        try {
            Object decoded = Tsons.decode(text);
            fail(text + " 本该报错却解出了: " + decoded);
        } catch (Exception e) {
            assertTrue("报错信息应包含 '" + expectedFragment + "': " + e.getMessage(),
                    e.getMessage() != null && e.getMessage().contains(expectedFragment));
        }
    }

    private static void assertBitEquals(double expected, Object actual) {
        assertTrue("往返失真: 期望 " + expected + "，实际 " + actual,
                actual instanceof Double
                        && Double.doubleToRawLongBits(expected) == Double.doubleToRawLongBits((Double) actual));
    }

}
