package com.github.pister.tson.objects;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by songlihuang on 2020/1/6.
 */
public class Person {
    private int age;

    private String name;

    private Date birth;

    private boolean married;

    private long weight;

    private List<String> address;

    private List<Contact> contacts;

    private Map<String, Object> attrs;

    private String[] mobiles;

    private int[] attr1;

    private Integer[] attr2;

    public int[] getAttr1() {
        return attr1;
    }

    public void setAttr1(int[] attr1) {
        this.attr1 = attr1;
    }

    public Integer[] getAttr2() {
        return attr2;
    }

    public void setAttr2(Integer[] attr2) {
        this.attr2 = attr2;
    }

    public String[] getMobiles() {
        return mobiles;
    }

    public void setMobiles(String[] mobiles) {
        this.mobiles = mobiles;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

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

    public long getWeight() {
        return weight;
    }

    public void setWeight(long weight) {
        this.weight = weight;
    }

    public List<String> getAddress() {
        return address;
    }

    public void setAddress(List<String> address) {
        this.address = address;
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
    }
}
