package com.github.pister.tson;

import com.github.pister.tson.objects.FooEnum;
import junit.framework.TestCase;

/**
 * 数组类型保真的回归测试，对应三个存量缺陷：
 *
 * <p>1. boolean[]：Types 的组件类型表只注册了 Boolean.class 没注册 Boolean.TYPE，
 * 编码时组件类型当作用户类型写出 "#types{0:boolean}"，解码 forName("boolean")
 * 直接 ClassNotFoundException。boolean[] 从来没有被任何版本成功往返过。</p>
 *
 * <p>2. char[]：ItemType.CHAR 携带的 Class 是 Character.class（包装类型），
 * 解码 Array.newInstance 出来的是 Character[] 不是 char[]，往返后类型漂移、
 * 强转 ClassCastException。</p>
 *
 * <p>3. 嵌套类数组（枚举数组、bean 数组）：arrayToItem 写组件类型名用的
 * getCanonicalName()，嵌套类的 "." 形式 Class.forName 加载不了
 * （45fb07d 只修了集合那处，漏了数组组件这处）。</p>
 *
 * Created by songlihuang on 2026/9/10.
 */
public class ArrayFidelityTest extends TestCase {

    /**
     * boolean[] 必须原样往返
     */
    public void testBooleanArray() {
        boolean[] a = new boolean[]{true, false, true};
        boolean[] b = (boolean[]) Tsons.decode(Tsons.encode(a));
        assertEquals(boolean.class, b.getClass().getComponentType());
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i]);
        }
    }

    /**
     * char[] 必须解出 char[]（修复前是 Character[]，类型漂移）
     */
    public void testCharArray() {
        char[] a = new char[]{'a', '"', '\\', '中'};
        char[] b = (char[]) Tsons.decode(Tsons.encode(a));
        assertEquals(char.class, b.getClass().getComponentType());
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i]);
        }
    }

    /**
     * 嵌套类枚举数组必须原样往返（修复前组件类型名是 canonical name，加载不了）
     */
    public void testNestedEnumArray() {
        Color[] a = new Color[]{Color.RED, Color.GREEN, null};
        Object decoded = Tsons.decode(Tsons.encode(a, TsonConfig.create().setWriteNullValue(true)));
        Color[] b = (Color[]) decoded;
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i]);
        }
    }

    /**
     * 嵌套类 bean 数组同一条组件类型名路径，一并覆盖
     */
    public void testNestedBeanArray() {
        Point[] a = new Point[]{new Point(1, 2), new Point(3, 4)};
        Point[] b = (Point[]) Tsons.decode(Tsons.encode(a));
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i]);
        }
    }

    /**
     * 空的多维数组必须解出空数组，不能是 null。
     *
     * <p>回归：createArray 对多维 + 空数据的分支返回 null，
     * int[0][] 编码成 "+2i32@[]" 后解码成 null —— null 和空数组
     * 在业务语义上是两回事。</p>
     */
    public void testEmptyMultiDimArrayRoundTrip() {
        int[][] empty = new int[0][];
        Object decoded = Tsons.decode(Tsons.encode(empty));
        assertTrue("应解出数组而不是 " + decoded, decoded instanceof int[][]);
        assertEquals(0, ((int[][]) decoded).length);

        // 手写的等价文本同样解出空数组
        Object decoded2 = Tsons.decode("+2i32@[]");
        assertTrue(decoded2 instanceof int[][]);
        assertEquals(0, ((int[][]) decoded2).length);
    }

    /**
     * 非空多维数组里的空行不受影响（守护性断言，修复前也正确）
     */
    public void testMultiDimArrayWithEmptyRow() {
        int[][] a = new int[2][0];
        Object decoded = Tsons.decode(Tsons.encode(a));
        assertTrue(decoded instanceof int[][]);
        assertEquals(2, ((int[][]) decoded).length);
        assertEquals(0, ((int[][]) decoded)[0].length);
    }

    /**
     * 顺手救活存量死数据：老版本 boolean[] 编码出的 "#types{0:boolean}" 组件名，
     * 那份数据从来没有被任何版本读出来过，新解码器应当认得基本类型名
     */
    public void testOldBooleanArrayDataIsRescued() {
        Object decoded = Tsons.decode("#types{0:boolean}\n+1#0@[bool@true,bool@false]");
        assertTrue(decoded instanceof boolean[]);
        boolean[] b = (boolean[]) decoded;
        assertEquals(2, b.length);
        assertTrue(b[0]);
        assertFalse(b[1]);
    }

    /**
     * 已有的顶层枚举/普通数组不受影响（守护性断言）
     */
    public void testTopLevelArrayStillWorks() {
        int[] a = new int[]{1, 2, 3};
        int[] b = (int[]) Tsons.decode(Tsons.encode(a));
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i]);
        }

        FooEnum[] e = new FooEnum[]{FooEnum.ValueOne};
        FooEnum[] e2 = (FooEnum[]) Tsons.decode(Tsons.encode(e));
        assertEquals(FooEnum.ValueOne, e2[0]);
    }

    public enum Color {
        RED, GREEN
    }

    public static class Point {
        private int x;
        private int y;

        public Point() {
        }

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return 31 * x + y;
        }
    }

}
