package com.github.pister.tson.common;

import junit.framework.TestCase;

import java.util.Date;
import java.util.Map;

/**
 * Created by songlihuang on 2020/1/6.
 */
public class ObjectUtilTest extends TestCase {


    public static class Person {

        private int age;

        private String name;

        private Date birth;

        private boolean married;

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Date getBirth() {
            return birth;
        }

        public void setBirth(Date birth) {
            this.birth = birth;
        }

        public boolean isMarried() {
            return married;
        }

        public void setMarried(boolean married) {
            this.married = married;
        }
    }

    public void testObjectPropertiesToMap() throws Exception {
        Person person = new Person();
        person.setAge(42);
        person.setBirth(new Date());
        person.setName("Jack");
        person.setMarried(true);
        Map<String, Object> properties = ObjectUtil.objectPropertiesToMap(person);
        System.out.println(properties);
    }

}