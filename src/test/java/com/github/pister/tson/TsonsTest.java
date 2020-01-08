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
        String s = Tsons.toTsonString(a);
        Object a2 = Tsons.parseForObject(s);
        Assert.assertEquals(a, a2);
    }

    public void testLinkedList() {
        List<String> a = new LinkedList<String>();
        a.add("aa");
        a.add("bb");
        a.add("cc");
        String s = Tsons.toTsonString(a);
        Object a2 = Tsons.parseForObject(s);
        Assert.assertEquals(a, a2);
    }

    public void testList() {
        List<String> a = new ArrayList<String>();
        a.add("aa");
        a.add("bb");
        a.add("cc");
        String s = Tsons.toTsonString(a);
        Object a2 = Tsons.parseForObject(s);
        Assert.assertEquals(a, a2);
    }

    public void testSet() {
        Set<String> a = new HashSet<String>();
        a.add("aa");
        a.add("bb");
        a.add("cc");
        String s = Tsons.toTsonString(a);
        System.out.println(s);
        Object a2 = Tsons.parseForObject(s);
        Assert.assertEquals(a, a2);
    }

    public void testArray1() {
        String[] a = new String[]{"hello", "world"};
        String s = Tsons.toTsonString(a);
        Object a2 = Tsons.parseForObject(s);
        String[] b = (String[])a2;
        Assert.assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            Assert.assertEquals(a[i], b[i]);
        }
    }

    public void testArray2() {
        int[] a = new int[]{1, 4, 5};
        String s = Tsons.toTsonString(a);
        Object a2 = Tsons.parseForObject(s);
        int[] b = (int[])a2;
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
        String s = Tsons.toTsonString(map);
        // {dateValue:date@"2020-01-08 17:46:12.908",intValue:i32@123,myList:[str@"hello",i32@123,bool@true],floatValue:f32@1.24,doubleValue:f64@123.456}
        System.out.println(s);
        Object map2 = Tsons.parseForObject(s);
        Assert.assertEquals(map, map2);
    }


    public void testCycleRef() {
        try {
            List<Object> a = new ArrayList<Object>();
            List<Object> b = new ArrayList<Object>();
            a.add(new Contact("aa1", "bb1"));
            b.add("xxxx");
            b.add(a);
            a.add(b);
            Tsons.toTsonString(a);
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
            Tsons.toTsonString(a);
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
            Tsons.toTsonString(a1);
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
        String s = Tsons.toTsonString(a);
        System.out.println(s);
        Object a2 = Tsons.parseForObject(s);
        Assert.assertEquals(a, a2);
    }



    public void testInt() {
        int a = 123;
        String s = Tsons.toTsonString(a);
        Object a2 = Tsons.parseForObject(s);
        Assert.assertEquals(a, a2);
    }

    public void testBoolean() {
        boolean a = true;
        String s = Tsons.toTsonString(a);
        Object a2 = Tsons.parseForObject(s);
        Assert.assertEquals(a, a2);
    }

    public void testLong() {
        long a = 123L;
        String s = Tsons.toTsonString(a);
        Object a2 = Tsons.parseForObject(s);
        Assert.assertEquals(a, a2);
    }

    public void testFloat() {
        float a = 123.456f;
        String s = Tsons.toTsonString(a);
        Object a2 = Tsons.parseForObject(s);
        Assert.assertEquals(a, a2);
    }

    public void testDouble() {
        double a = 123.456;
        String s = Tsons.toTsonString(a);
        Object a2 = Tsons.parseForObject(s);
        Assert.assertEquals(a, a2);
    }

    public void testObjects() {
        Person person = new Person();
        person.setAge(42);
        person.setBirth(new Date());
        person.setName("Jack");
        person.setMarried(true);
        person.setAddress(Arrays.asList("xx", "yy"));

        person.setContacts(Arrays.asList(new Contact("Peter", "133"), new Contact("Tom", "134")));
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("name1", 123);
        attrs.put("name2", "xxx");
        person.setAttrs(attrs);

        person.setMobiles(new String[]{"123", "555"});

        person.setAttr1(new int[]{1, 2});
        person.setAttr2(new Integer[]{3, 4});

        String s = Tsons.toTsonString(person);
        Person person2 = (Person) Tsons.parseForObject(s);
        Assert.assertTrue(person.equals(person2));
    }
}