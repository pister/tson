package com.github.pister.tson;

import com.github.pister.tson.objects.Contact;
import com.github.pister.tson.objects.Person;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.*;

/**
 * Created by songlihuang on 2020/1/6.
 */
public class TsonsTest extends TestCase {

    public void testString() {
        String a = "abc\nxksad\"xx中午";
        String s = Tsons.encode(a);
        Object a2 = Tsons.decode(s);
        Assert.assertEquals(a, a2);
    }

    public void testLinkedList() {
        List<String> a = new LinkedList<String>();
        a.add("aa");
        a.add("bb");
        a.add("cc");
        String s = Tsons.encode(a);
        Object a2 = Tsons.decode(s);
        Assert.assertEquals(a, a2);
    }

    public void testList() {
        List<String> a = new ArrayList<String>();
        a.add("aa");
        a.add("bb");
        a.add("cc");
        String s = Tsons.encode(a);
        Object a2 = Tsons.decode(s);
        Assert.assertEquals(a, a2);
    }

    public void testSet() {
        Set<String> a = new HashSet<String>();
        a.add("aa");
        a.add("bb");
        a.add("cc");
        String s = Tsons.encode(a);
        Object a2 = Tsons.decode(s);
        Assert.assertEquals(a, a2);
    }

    public void testArray1() {
        String[] a = new String[]{"hello", "world"};
        String s = Tsons.encode(a);
        Object a2 = Tsons.decode(s);
        String[] b = (String[]) a2;
        Assert.assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            Assert.assertEquals(a[i], b[i]);
        }
    }

    public void testArray2() {
        int[] a = new int[]{1, 4, 5};
        String s = Tsons.encode(a);
        Object a2 = Tsons.decode(s);
        int[] b = (int[]) a2;
        Assert.assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            Assert.assertEquals(a[i], b[i]);
        }
    }

    public void testMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("intValue", 123);
        map.put("doubleValue", 123.456);
        map.put("floatValue", 1.24f);
        map.put("dateValue", new Date());
        List<Object> list = new ArrayList<Object>();
        list.add("hello");
        list.add(123);
        list.add(true);
        map.put("myList", list);
        String s = Tsons.encode(map);
        // {dateValue:date@"2020-01-08 17:46:12.908",intValue:i32@123,myList:[str@"hello",i32@123,bool@true],floatValue:f32@1.24,doubleValue:f64@123.456}
        System.out.println(s);
        Object map2 = Tsons.decode(s);
        Assert.assertEquals(map, map2);
    }

    public void testArray3d() {
        int[][][] array3d = new int[2][3][4];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 4; k++) {
                    array3d[i][j][k] = i * 100 + j * 10 + k;
                }
            }
        }
        String s1 = Tsons.encode(array3d);
        Object o2 = Tsons.decode(s1);
        int[][][] array3d2 = (int[][][]) o2;
        Assert.assertEquals(array3d.length, array3d2.length);
        for (int i = 0; i < array3d.length; i++) {
            int[][] a1 = array3d[i];
            int[][] a2 = array3d2[i];
            Assert.assertEquals(a1.length, a2.length);
            for (int j = 0; j < a1.length; j++) {
                int[] a11 = a1[j];
                int[] a21 = a2[j];
                Assert.assertEquals(a11.length, a21.length);
                for (int k = 0; k < a11.length; k++) {
                    int a111 = a11[k];
                    int a211 = a21[k];
                    Assert.assertEquals(a111, a211);
                }
            }
        }
    }


    public void testCycleRef() {
        try {
            List<Object> a = new ArrayList<Object>();
            List<Object> b = new ArrayList<Object>();
            a.add(new Contact("aa1", "bb1"));
            b.add("xxxx");
            b.add(a);
            a.add(b);
            Tsons.encode(a);
            Assert.fail("must not reach here");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("cycle reference"));
        }

        try {
            Map<String, Object> a = new HashMap<String, Object>();
            Map<String, Object> b = new HashMap<String, Object>();
            a.put("a1", new Contact("aa1", "bb1"));
            a.put("ab", b);
            b.put("a", a);
            Tsons.encode(a);
            Assert.fail("must not reach here");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("cycle reference"));
        }

        try {
            RefTestClass a1 = new RefTestClass();
            RefTestClass a2 = new RefTestClass();
            RefTestClass a3 = new RefTestClass();
            a1.a = a2;
            a2.a = a3;
            a3.a = a1;
            Tsons.encode(a1);
            Assert.fail("must not reach here");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("cycle reference"));
        }
    }

    public static class RefTestClass {
        RefTestClass a;

        public RefTestClass getA() {
            return a;
        }

        public void setA(RefTestClass a) {
            this.a = a;
        }
    }

    public void testMix() {
        List<Object> a = new ArrayList<Object>();
        a.add(new Contact("aa1", "bb1"));
        a.add(new Date());
        a.add(123);
        a.add("hello");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("v1", new Contact("aa2", "bb2"));
        params.put("v2", "s6");
        a.add(params);
        String s = Tsons.encode(a);
        Object a2 = Tsons.decode(s);
        Assert.assertEquals(a, a2);
    }

    public void testInt() {
        int a = -123;
        String s = Tsons.encode(a);
        Object a2 = Tsons.decode(s);
        Assert.assertEquals(a, a2);
    }

    public void testBoolean() {
        boolean a = true;
        String s = Tsons.encode(a);
        Object a2 = Tsons.decode(s);
        Assert.assertEquals(a, a2);
    }

    public void testLong() {
        long a = 123L;
        String s = Tsons.encode(a);
        Object a2 = Tsons.decode(s);
        Assert.assertEquals(a, a2);
    }

    public void testFloat() {
        float a = 123.456f;
        String s = Tsons.encode(a);
        Object a2 = Tsons.decode(s);
        Assert.assertEquals(a, a2);
    }

    public void testDouble() {
        double a = 123.456;
        String s = Tsons.encode(a);
        Object a2 = Tsons.decode(s);
        Assert.assertEquals(a, a2);
    }

    public void testVerySmallOrBigDouble() {
        double smallNumber = -1.24e-12;
        {
            String s = Tsons.encode(smallNumber);
            Object a2 = Tsons.decode(s);
            Assert.assertEquals(smallNumber, a2);
        }
        double bigNumber = 3.20e13;
        {
            String s = Tsons.encode(bigNumber);
            Object a2 = Tsons.decode(s);
            Assert.assertEquals(bigNumber, a2);
        }
    }


    public void testBinary() {
        byte[] a = new byte[]{'h', 'e', 'l', 'l', 'o'};
        String s = Tsons.encode(a);
        Object a2 = Tsons.decode(s);
        Assert.assertTrue(Arrays.equals(a, (byte[]) a2));
    }

    public void testObjects() {
        Person person = new Person();
        person.setAge(42);
        person.setBirth(new Date());
        person.setName("Jack");
        person.setMarried(true);
        person.setAddress(Arrays.asList("xx", "yy"));
        person.setMyMatrix(new int[2][3]);

        person.setContacts(Arrays.asList(new Contact("Peter", "133"), new Contact("Tom", "134")));
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("name1", 123);
        attrs.put("name2", "xxx");
        person.setAttrs(attrs);

        person.setMobiles(new String[]{"123", "555"});

        person.setAttr1(new int[]{1, 2});
        person.setAttr2(new Integer[]{3, 4});

        String s = Tsons.encode(person);
        System.out.println(s);
        Person person2 = (Person) Tsons.decode(s);
        Assert.assertTrue(person.equals(person2));
    }

    public static class FooObject {
        private String name;

        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;

            FooObject fooObject = (FooObject) object;

            if (getAge() != fooObject.getAge()) return false;
            return getName() != null ? getName().equals(fooObject.getName()) : fooObject.getName() == null;
        }

        @Override
        public int hashCode() {
            int result = getName() != null ? getName().hashCode() : 0;
            result = 31 * result + getAge();
            return result;
        }
    }


    public void testEmpty() {
        FooObject eo = new FooObject();
        String s = Tsons.encode(eo);
        System.out.println(s);
        FooObject e = (FooObject) Tsons.decode(s);
        Assert.assertTrue(e.equals(eo));
    }

    public void testObjectKeyMap() {
        Map<FooObject, String> m = new HashMap<FooObject, String>();
        {
            FooObject key1 = new FooObject();
            key1.setName("name1");
            key1.setAge(12);
            m.put(key1, "abc122");
        }
        {
            FooObject key1 = new FooObject();
            key1.setName("name2");
            key1.setAge(13);
            m.put(key1, "abc123");
        }
        String s = Tsons.encode(m);
        System.out.println(s);
        Object e = Tsons.decode(s);
        Assert.assertTrue(e.equals(m));
    }

    public void testIntegerKeyMap() {
        Map<Object, String> m = new HashMap<Object, String>();
        m.put(123, "abc");
        m.put(124, "aaa");
        String s = Tsons.encode(m);
        System.out.println(s);
        Object e = Tsons.decode(s);
        Assert.assertTrue(e.equals(m));
    }

    public void testKeyForOldVersionMap() {
        Map<Object, String> m = new HashMap<Object, String>();
        m.put("a12", "abc");
        String s = "{a12:str@\"abc\"}";
        Object e = Tsons.decode(s);
        Assert.assertTrue(e.equals(m));
    }

}