package com.github.pister.tson.common;

/**
 * Created by songlihuang on 2022/8/27.
 */
public final class EnumUtil {

    private EnumUtil() {}

    /**
     * 按常量名取枚举实例。
     *
     * <p>必须走 {@link Enum#valueOf}：老实现自己反射遍历字段，找不到常量名时
     * 静默返回 null，损坏的数据（常量被删掉后的存量文本）看起来和正常数据
     * 一样。valueOf 找不到会抛 IllegalArgumentException，由调用方转成语义
     * 清晰的语法错误；常量名存在时两者行为一致。</p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object getEnumInstance(Class enumClass, String name) {
        return Enum.valueOf(enumClass, name);
    }

}
