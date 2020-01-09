package com.github.pister.tson.benchmark.cases;

import com.alibaba.fastjson.JSON;
import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.github.pister.tson.Tsons;
import com.github.pister.tson.benchmark.Action;
import com.github.pister.tson.benchmark.BenchMark;
import com.github.pister.tson.benchmark.BenchMarkResult;
import com.github.pister.tson.io.FastByteArrayInputStream;
import com.github.pister.tson.io.FastByteArrayOutputStream;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by songlihuang on 2020/1/9.
 */
public class TsonCases extends TestCase {

    private static final int stringBytesLength(String s) throws UnsupportedEncodingException {
        return s.getBytes("utf-8").length;
    }

    private void actionFor(String message, BenchMark benchMark, Action action) {
        BenchMarkResult benchMarkResult = benchMark.executeAction(action);
        System.out.println(message + ": " + benchMarkResult.getTotalEscapeTimesMs());
    }

    private void testTson(int rowSize, final Object srcData) throws UnsupportedEncodingException {
        final String data = Tsons.toTsonString(srcData);
        int dataSize = stringBytesLength(data);
        System.out.println("[tson]data size:" + dataSize);
        System.out.println("[tson]size rate:" + ((float) dataSize) / rowSize);
        BenchMark benchMark = new BenchMark();
        benchMark.setTimes(100 * 10000);
        actionFor("[tons]serialize time", benchMark, new Action() {
            @Override
            public void run() {
                Tsons.toTsonString(srcData);
            }
        });
        actionFor("[tson]unserialize time", benchMark, new Action() {
            @Override
            public void run() {
                Tsons.parseForObject(data);
            }
        });
    }

    private void testFastson(int rowSize, final Object srcData) throws UnsupportedEncodingException {
        final String data = JSON.toJSONString(srcData);
        int dataSize = stringBytesLength(data);
        System.out.println("[fastjson]data size:" + dataSize);
        System.out.println("[fastjson]size rate:" + ((float) dataSize) / rowSize);
        BenchMark benchMark = new BenchMark();
        benchMark.setTimes(100 * 10000);
        actionFor("[fastjson]serialize time", benchMark, new Action() {
            @Override
            public void run() {
                JSON.toJSONString(srcData);
            }
        });
        actionFor("[fastjson]unserialize time", benchMark, new Action() {
            @Override
            public void run() {
                JSON.parseObject(data);
            }
        });
    }

    private byte[] javaObjectToBytes(Object o) throws IOException {
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(o);
        oos.close();
        return os.toByteArray();
    }


    private Object javaBytesToObject(byte[] s) throws IOException, ClassNotFoundException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(s);
        ObjectInputStream ois = new ObjectInputStream(in);
        return ois.readObject();
    }

    private void testJava(int rowSize, final Object srcData) throws IOException {
        final byte[] data = javaObjectToBytes(srcData);
        int dataSize = data.length;
        System.out.println("[java]data size:" + dataSize);
        System.out.println("[java]size rate:" + ((float) dataSize) / rowSize);
        BenchMark benchMark = new BenchMark();
        benchMark.setTimes(100 * 10000);
        actionFor("[java]serialize time", benchMark, new Action() {
            @Override
            public void run() {
                try {
                    javaObjectToBytes(srcData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        actionFor("[java]unserialize time", benchMark, new Action() {
            @Override
            public void run() {
                try {
                    javaBytesToObject(data);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private byte[] hessianObjectToBytes(Object o) throws IOException {
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        HessianOutput output = new HessianOutput(os);
        output.writeObject(o);
        os.close();
        return os.toByteArray();
    }

    private Object hessianBytesToObject(byte[] b) throws IOException {
        FastByteArrayInputStream is = new FastByteArrayInputStream(b);
        HessianInput input = new HessianInput(is);
        return input.readObject();
    }

    private void testHassian(int rowSize, final Object srcData) throws IOException {
        final byte[] data = hessianObjectToBytes(srcData);
        int dataSize = data.length;
        System.out.println("[hessian]data size:" + dataSize);
        System.out.println("[hessian]size rate:" + ((float) dataSize) / rowSize);
        BenchMark benchMark = new BenchMark();
        benchMark.setTimes(100 * 10000);
        actionFor("[hessian]serialize time", benchMark, new Action() {
            @Override
            public void run() {
                try {
                    hessianObjectToBytes(srcData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        actionFor("[hessian]unserialize time", benchMark, new Action() {
            @Override
            public void run() {
                try {
                    hessianBytesToObject(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void testHelloWorld() throws IOException {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("n1", "hello world这是一个简单的测试数据123456789");
        testTson(stringBytesLength(params.toString()), params);
        testFastson(stringBytesLength(params.toString()), params);
        testJava(stringBytesLength(params.toString()), params);
        testHassian(stringBytesLength(params.toString()), params);
    }

}
