package com.github.pister.tson;

import com.github.pister.tson.parse.SyntaxException;
import junit.framework.TestCase;

/**
 * 解码端健壮性的回归测试：坏输入要报带上下文的错，不能 NPE / 静默丢类型 /
 * StackOverflowError。
 *
 * <p>对应四个存量缺陷：</p>
 * <p>1. 文本引用了类型索引但没有 #types 头（如 "!0@RED"）：types 为 null，
 * types.get(index) 裸 NPE，没有任何信息。</p>
 * <p>2. #types 头里查不到的索引（如 "#9@"）：types.get 返回 null 被当成
 * "没有类型" 继续走，解码结果静默丢掉用户类型 —— 坏数据看起来像好数据。</p>
 * <p>3. 深层嵌套：Parser 全程递归下降，恶意/损坏的几千层 "[[[[..."
 * 直接 StackOverflowError（Error 不是 Exception，常规兜底接不住）。</p>
 * <p>4. bean setter 收到类型不匹配的值：反射抛裸
 * IllegalArgumentException("argument type mismatch")，不知道是哪个属性、
 * 期望什么类型。</p>
 *
 * Created by songlihuang on 2026/9/10.
 */
public class ParserRobustnessTest extends TestCase {

    /**
     * 枚举引用没有 #types 头必须报语法错误（修复前是 NPE）
     */
    public void testEnumRefWithoutTypesHeaderIsSyntaxError() {
        try {
            Tsons.decode("!0@RED");
            fail("本该报错");
        } catch (SyntaxException e) {
            // 期望
        }
    }

    /**
     * 用户类型引用没有 #types 头必须报语法错误（修复前是 NPE）
     */
    public void testUserTypeRefWithoutTypesHeaderIsSyntaxError() {
        try {
            Tsons.decode("#0@{a:i32@1}");
            fail("本该报错");
        } catch (SyntaxException e) {
            // 期望
        }
    }

    /**
     * #types 里不存在的索引必须报语法错误（修复前静默解成无类型的裸 map，
     * 用户类型凭空消失）
     */
    public void testUnknownTypeIndexIsSyntaxError() {
        try {
            Tsons.decode("#types{0:com.github.pister.tson.objects.FooEnum}\n#9@[i32@1]");
            fail("本该报错");
        } catch (SyntaxException e) {
            // 期望
        }
    }

    /**
     * 枚举引用的索引不在 #types 里必须报语法错误（修复前已有该检查，守护）
     */
    public void testEnumRefUnknownIndexIsSyntaxError() {
        try {
            Tsons.decode("#types{0:com.github.pister.tson.objects.FooEnum}\n!9@ValueOne");
            fail("本该报错");
        } catch (SyntaxException e) {
            // 期望
        }
    }

    /**
     * 恶意/损坏的超深嵌套必须报语法错误，不能 StackOverflowError。
     * Error 逃出去可能拖垮线程甚至进程，递归下降必须有深度上限。
     */
    public void testTooDeepNestingIsRejected() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            sb.append('[');
        }
        sb.append("i32@0");
        for (int i = 0; i < 2000; i++) {
            sb.append(']');
        }
        try {
            Tsons.decode(sb.toString());
            fail("本该报错");
        } catch (SyntaxException e) {
            // 期望
        }
    }

    /**
     * 合理深度的嵌套不受深度上限影响（守护性断言）
     */
    public void testReasonableNestingStillWorks() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append('[');
        }
        sb.append("i32@7");
        for (int i = 0; i < 100; i++) {
            sb.append(']');
        }
        Object decoded = Tsons.decode(sb.toString());
        Object inner = decoded;
        for (int i = 0; i < 100; i++) {
            inner = ((java.util.List) inner).get(0);
        }
        assertEquals(7, inner);
    }

    /**
     * 空白输入和空串行为要一致：都返回 null。
     * 修复前 "" 返回 null 而 "   " 抛异常。
     */
    public void testWhitespaceOnlyDecodesToNull() {
        assertNull(Tsons.decode("   "));
        assertNull(Tsons.decode("\t\n"));
    }

    /**
     * bean setter 收到类型不匹配的值时，报错必须带上属性名和类型信息，
     * 不能是裸的 "argument type mismatch"。
     */
    public void testSetterTypeMismatchErrorHasContext() {
        Holder holder = new Holder();
        holder.setV(7);
        String encoded = Tsons.encode(holder);
        // 把 i32@ 篡改成 i64@，模拟手写/版本漂移的数据：Long 喂给 int setter
        String tampered = encoded.replace("i32@", "i64@");
        try {
            Tsons.decode(tampered);
            fail("本该报错");
        } catch (RuntimeException e) {
            assertTrue("报错应包含属性名 v: " + e.getMessage(),
                    e.getMessage() != null && e.getMessage().contains("'v'"));
        }
    }

    public static class Holder {
        private int v;

        public int getV() {
            return v;
        }

        public void setV(int v) {
            this.v = v;
        }
    }

}
