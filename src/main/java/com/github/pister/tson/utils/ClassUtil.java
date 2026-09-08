package com.github.pister.tson.utils;

/**
 * Created by songlihuang on 2026/9/8.
 */
public final class ClassUtil {

    private ClassUtil() {}

    /**
     * 按类名加载类，优先用线程上下文类加载器。
     *
     * <p>直接调 {@link Class#forName(String)} 用的是 tson 自己的类加载器。
     * 当 tson 被放进容器的公共 lib 目录、而业务类在各自的 webapp 里时，
     * tson 的加载器看不到业务类，只有上下文加载器能看到。</p>
     *
     * <p>上下文加载器找不到时退回本类的加载器，覆盖上下文没有设置、
     * 或者目标类恰好和 tson 同在一个加载器里的情况。</p>
     */
    public static Class<?> forName(String className) throws ClassNotFoundException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            try {
                return Class.forName(className, true, contextClassLoader);
            } catch (ClassNotFoundException e) {
                // 上下文加载器里没有，继续用本类的加载器试
            }
        }
        return Class.forName(className);
    }
}
