package com.github.pister.tson.utils;

import com.github.pister.tson.Tsons;
import junit.framework.TestCase;

import java.util.Map;

/**
 * Created by songlihuang on 2026/9/8.
 */
public class ClassUtilTest extends TestCase {

    public void testLoadsNormalClass() throws Exception {
        assertEquals(Map.class, ClassUtil.forName("java.util.Map"));
        assertEquals(String.class, ClassUtil.forName("java.lang.String"));
    }

    public void testMissingClassStillThrows() {
        try {
            ClassUtil.forName("no.such.Class.Anywhere");
            fail("类不存在时应当抛 ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // 期望
        }
    }

    /**
     * 关键行为：优先问线程上下文类加载器。
     * tson 放在容器公共 lib 里时，只有上下文加载器看得见 webapp 中的业务类。
     */
    public void testConsultsContextClassLoaderFirst() throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        RecordingClassLoader recording = new RecordingClassLoader(original);
        Thread.currentThread().setContextClassLoader(recording);
        try {
            ClassUtil.forName("java.util.LinkedHashMap");
            assertTrue("应当先问过上下文类加载器", recording.consulted);
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    /**
     * 上下文加载器找不到时要退回本类的加载器，而不是直接失败
     */
    public void testFallsBackWhenContextClassLoaderCannotFind() throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new RejectingClassLoader());
        try {
            assertEquals(ClassUtil.class, ClassUtil.forName(ClassUtil.class.getName()));
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    /**
     * 集成断言：解码用户类型时必须走到 {@link ClassUtil}，而不是裸的 Class.forName。
     * 只测 ClassUtil 本身证明不了调用方改过来了。
     */
    public void testDecodeResolvesUserTypeThroughContextClassLoader() {
        String encoded = Tsons.encode(new Payload());
        assertTrue("这份数据要带用户类型才有意义: " + encoded, encoded.contains(Payload.class.getName()));

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        RecordingClassLoader recording = new RecordingClassLoader(original);
        Thread.currentThread().setContextClassLoader(recording);
        try {
            Tsons.decode(encoded);
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
        assertTrue("解码用户类型时应当先问上下文类加载器", recording.consulted);
    }

    public static class Payload {
        private String name = "x";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private static class RecordingClassLoader extends ClassLoader {
        boolean consulted = false;

        RecordingClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            consulted = true;
            return super.loadClass(name);
        }
    }

    /** 什么都加载不了，用来验证回退分支 */
    private static class RejectingClassLoader extends ClassLoader {
        RejectingClassLoader() {
            super(null);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException(name);
        }
    }
}
