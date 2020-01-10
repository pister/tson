package com.github.pister.tson.parse;

import com.github.pister.tson.Tsons;
import com.github.pister.tson.objects.Contact;
import com.github.pister.tson.objects.Person;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by songlihuang on 2020/1/7.
 */
public class ParserTest extends TestCase {

    public void testParse() {
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

        person.setMobiles(new String[] {"123", "555"});

        person.setAttr1(new int[]{1,2});
        person.setAttr2(new Integer[]{3, 4});

        String s = Tsons.toTsonString(person);

        Object object = Tsons.parseForObject(s);
        Assert.assertTrue(person.equals(object));
    }

}