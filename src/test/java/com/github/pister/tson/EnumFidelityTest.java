package com.github.pister.tson;

import com.github.pister.tson.objects.FooEnum;
import junit.framework.TestCase;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 枚举保真的回归测试，对应两个存量缺陷：
 *
 * <p>1. 带常量体的枚举常量（enum X { A { ... } }）编码丢失：
 * wrapItemImpl 用 {@code o.getClass().isEnum()} 判断枚举，而带常量体的
 * 常量 getClass() 是匿名子类（X$1），isEnum() 返回 false，
 * 掉进 plain-object 分支编成 "#0@{}"，枚举值无声丢失 —— 这直接打在
 * "类型不丢失"的立项卖点上。解码侧的老 EnumUtil 按字段声明类型过滤，
 * 声明类型就是枚举本身，常量体取得到，所以问题只在编码这一侧。</p>
 *
 * <p>2. 未知枚举常量名静默解成 null：EnumUtil.getEnumInstance 找不到
 * 时不报错返回 null。持久化格式对损坏数据宁可报错（未闭合字符串、
 * 超范围整数都是这么改的），这里不该例外。</p>
 *
 * Created by songlihuang on 2026/9/10.
 */
public class EnumFidelityTest extends TestCase {

    /**
     * 带常量体的枚举常量必须原样往返（修复前编成空对象 #0@{}，
     * 解码抛 InstantiationException）
     */
    public void testEnumWithBodyRoundTrip() {
        Op[] ops = Op.values();
        for (Op op : ops) {
            Op back = (Op) Tsons.decode(Tsons.encode(op));
            assertSame("常量体枚举 " + op + " 往返失真", op, back);
        }
    }

    /**
     * 带常量体的枚举藏在容器里也要能往返：类型名必须取
     * getDeclaringClass()，否则写进 #types 的是匿名子类名
     */
    public void testEnumWithBodyInContainer() {
        java.util.List<Object> list = new java.util.ArrayList<Object>();
        list.add(Op.ADD);
        list.add(Op.SUB);
        java.util.List<Object> back = (java.util.List<Object>) Tsons.decode(Tsons.encode(list));
        assertEquals(Op.ADD, back.get(0));
        assertEquals(Op.SUB, back.get(1));
    }

    /**
     * 普通枚举不受影响（守护性断言）
     */
    public void testPlainEnumStillWorks() {
        assertEquals(FooEnum.ValueOne, Tsons.decode(Tsons.encode(FooEnum.ValueOne)));
        assertEquals(FooEnum.ValueTwo, Tsons.decode(Tsons.encode(FooEnum.ValueTwo)));
    }

    /**
     * 未知枚举常量名必须报错，不能静默解成 null。
     * 修复前 getEnumInstance 找不到返回 null，损坏的数据看起来和正常数据一样。
     */
    public void testUnknownEnumConstantIsRejected() {
        String encoded = "#types{0:com.github.pister.tson.objects.FooEnum}\n!0@NoSuchConstant";
        try {
            Object decoded = Tsons.decode(encoded);
            fail("本该报错却解出了: " + decoded);
        } catch (Exception e) {
            // 期望带上下文的报错
            assertTrue("报错信息应包含常量名: " + e.getMessage(),
                    e.getMessage() != null && e.getMessage().contains("NoSuchConstant"));
        }
    }

    /**
     * 枚举做 map 的 key 必须能往返。
     *
     * <p>回归存量缺陷：编码端对枚举 key 写的是 "!idx@NAME"（writeEnum），
     * 但解码端 keyItemKey 只认 TOKEN_ID / null / define-detail，"!" 语法
     * 只在 item() 位置有产生式 —— 生产者写得出、任何版本的消费者都读不了
     * （SyntaxException）。README 宣称"支持非String类型作为map的key"，
     * 枚举 key 实际从未工作过。</p>
     */
    public void testEnumAsMapKeyRoundTrip() {
        Map<Signal, String> m = new LinkedHashMap<Signal, String>();
        m.put(Signal.RED, "danger");
        m.put(Signal.GREEN, "safe");
        Map<Signal, String> back = (Map<Signal, String>) Tsons.decode(Tsons.encode(m));
        assertEquals(2, back.size());
        assertEquals("danger", back.get(Signal.RED));
        assertEquals("safe", back.get(Signal.GREEN));
    }

    /**
     * 手写的枚举 key 文本也要能直接解（防止只修了往返、没修语法）
     */
    public void testEnumMapKeyLiteralDecode() {
        Object decoded = Tsons.decode(
                "#types{0:com.github.pister.tson.EnumFidelityTest$Signal}\n"
                        + "{!0@RED:str@\"stop\",!0@GREEN:str@\"go\"}");
        Map<Signal, String> back = (Map<Signal, String>) decoded;
        assertEquals("stop", back.get(Signal.RED));
        assertEquals("go", back.get(Signal.GREEN));
    }

    /** 简单枚举，用于 map key 测试 */
    public enum Signal {
        RED, GREEN
    }

    /** 常量体枚举：每个常量带自己的类主体 */
    public enum Op {
        ADD {
            public int apply(int a, int b) { return a + b; }
        },
        SUB {
            public int apply(int a, int b) { return a - b; }
        };

        public abstract int apply(int a, int b);
    }

}
