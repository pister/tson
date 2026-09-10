package com.github.pister.tson;

/**
 * <p>所有开关的默认值都等于老版本行为，不配置就是完全兼容。</p>
 *
 * Created by songlihuang on 2026/9/10.
 */
public class TsonConfig {

    private boolean writeNullValue = false;

    public static TsonConfig create() {
        return new TsonConfig();
    }

    /**
     * 是否写出 null 字面量。
     *
     * <p>false（默认）：map 的 null value / null key 条目不写，
     * list / array 的 null 元素跳过 —— 输出与老版本逐字节一致，
     * 代价是这些 null 不落盘（list 长度会变）。</p>
     *
     * <p>true：null 完整落盘。注意老版本读不了 null 字面量，
     * 混合版本部署时先升级读端。</p>
     *
     * @return this，支持链式配置
     */
    public TsonConfig setWriteNullValue(boolean writeNullValue) {
        this.writeNullValue = writeNullValue;
        return this;
    }

    public boolean isWriteNullValue() {
        return writeNullValue;
    }

}
