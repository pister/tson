package com.github.pister.tson.cases;

import com.github.pister.tson.Tsons;
import com.github.pister.tson.objects.Contact;
import com.github.pister.tson.objects.FooEnum;
import com.github.pister.tson.objects.Person;
import junit.framework.TestCase;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Created by songlihuang on 2020/6/12.
 */
public class TestCases extends TestCase {

    public void testBasicMap() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("boolean_v", true);
        params.put("byte_v", (byte) 10);
        params.put("short_v", (short) 20);
        params.put("char_v", 'a');
        params.put("int_v", -40);
        params.put("long_v", 80L);
        params.put("float_v", (float) Math.PI);
        params.put("double_v", Math.E);
        params.put("str_v", "hello世界");
        params.put("empty_str", "");
        params.put("date_v", new Date());
        params.put("enum_v", FooEnum.ValueTwo);
        params.put("local_datetime", LocalDateTime.now());
        params.put("local_date", LocalDate.now());
        params.put("local_time", LocalTime.now());
        String s = Tsons.encode(params);
        Map<String, Object> paramsDecoded = (Map<String, Object>) Tsons.decode(s);
        assertEquals(params.size(), paramsDecoded.size());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object v1 = entry.getValue();
            Object v2 = paramsDecoded.get(key);
            assertEquals(v1, v2);
        }
    }

    public void testLocalTimes() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("date", new Date());
        params.put("local_datetime", LocalDateTime.now());
        params.put("local_date", LocalDate.now());
        params.put("local_time", LocalTime.now());

        String s = Tsons.encode(params);
        System.out.println(s);
        Map<String, Object> paramsDecoded = (Map<String, Object>) Tsons.decode(s);
        assertEquals(params.size(), paramsDecoded.size());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object v1 = entry.getValue();
            Object v2 = paramsDecoded.get(key);
            assertEquals(v1, v2);
        }
    }

    public void testIntArray() {
        int[] intArray = new int[]{1, 2, 3, 4};
        String s = Tsons.encode(intArray);
        int[] intArray2 = (int[])Tsons.decode(s);
        assertEquals(intArray.length, intArray2.length);
        for (int i = 0; i < intArray.length; i++) {
            assertEquals(intArray[i], intArray2[i]);
        }
    }

    public void testBytesArray() throws UnsupportedEncodingException {
        byte[] buf = "hello world\u0001\u0001".getBytes("utf-8");
        String s = Tsons.encode(buf);
        byte[] buf2 = (byte[])Tsons.decode(s);
        assertEquals(buf.length, buf2.length);
        for (int i = 0; i < buf.length; i++) {
            assertEquals(buf[i], buf2[i]);
        }
    }

    public void testFloatArray() {
        float[] arr1 = new float[]{1.1f, 2.2f, 3.3f, 4.4f};
        String s = Tsons.encode(arr1);
        float[] arr2 = (float[])Tsons.decode(s);
        assertEquals(arr1.length, arr2.length);
        for (int i = 0; i < arr1.length; i++) {
            assertTrue(floatEquals(arr1[i], arr2[i]));
        }
    }

    public void testDoubleArray() {
        double[] arr1 = new double[]{1.12, 2.22, 3.32, 4.42};
        String s = Tsons.encode(arr1);
        double[] arr2 = (double[])Tsons.decode(s);
        assertEquals(arr1.length, arr2.length);
        for (int i = 0; i < arr1.length; i++) {
            assertTrue(doubleEquals(arr1[i], arr2[i]));
        }
    }

    public void testStringArray() {
        String[] arr1 = new String[]{"aa", "bb", "vccccc"};
        String s = Tsons.encode(arr1);
        String[] arr2 = (String[])Tsons.decode(s);
        assertEquals(arr1.length, arr2.length);
        for (int i = 0; i < arr1.length; i++) {
            assertEquals(arr1[i], arr2[i]);
        }
    }

    public void testList() {
        List<Object> mixList = new ArrayList<Object>();
        mixList.add("string value");
        mixList.add(1234);
        mixList.add(true);
        mixList.add(new Date());
        mixList.add(4567L);
        mixList.add((float)3.14);
        mixList.add(3.14);
        mixList.add((byte)12);
        mixList.add('x');
        mixList.add('\"');
        mixList.add((short)15);
        String s = Tsons.encode(mixList);
        System.out.println(s);
        List<Object> list2 = (List<Object>)Tsons.decode(s);
        assertEquals(mixList.size(), list2.size());
        for (int i = 0, size = mixList.size(); i < size; i++) {
            assertEquals(mixList.get(i), list2.get(i));
        }
    }

    public void testMultiDimArray() {
        int[][] myMatrix = new int[2][3];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                myMatrix[i][j] = i * j + 10 *i + 20 *j;
            }
        }
        String s = Tsons.encode(myMatrix);
        int[][] myMatrix2 = (int[][])Tsons.decode(s);
        assertNotNull(myMatrix2);
        assertEquals(myMatrix2.length, myMatrix.length);
        for (int i = 0; i < myMatrix.length; i++) {
            assertEquals(myMatrix2[i].length, myMatrix[i].length);
            for (int j = 0; j < myMatrix[i].length; j++) {
                assertEquals(myMatrix[i][j], myMatrix2[i][j]);
            }
        }
    }

    public void testObjects() {
        List<Person> personList = new ArrayList<Person>();
        for (int i = 0; i < 3; i++) {
            Person person = new Person();
            person.setAge(42);
            person.setBirth(new Date());
            person.setName("Jack" + i);
            person.setMarried(true);
            person.setMyEnum(FooEnum.ValueTwo);
            person.setContacts(Arrays.asList(new Contact("name1", "133"), new Contact("name2", "134")));
            person.setWeight(80);
            person.setTransientValue("xxx");
            personList.add(person);
        }
        String s = Tsons.encode(personList);
        List<Person> personList2 = (List<Person>)Tsons.decode(s);
        assertNotNull(personList2);
        assertEquals(personList.size(), personList2.size());
        for (int i = 0, size = personList.size(); i < size; i++) {
            assertEquals(personList.get(i), personList2.get(i));
        }
    }



    private static boolean floatEquals(float f1, float f2) {
        return Math.abs(f1 - f2) < 0.000001;
    }

    private static boolean doubleEquals(double f1, double f2) {
        return Math.abs(f1 - f2) < 0.000001;
    }
}
