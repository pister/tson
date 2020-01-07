package com.github.pister.tson.common;

import com.github.pister.tson.objects.Person;
import com.github.pister.tson.access.property.ObjectUtil;
import junit.framework.TestCase;

import java.util.Date;
import java.util.Map;

/**
 * Created by songlihuang on 2020/1/6.
 */
public class ObjectUtilTest extends TestCase {

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