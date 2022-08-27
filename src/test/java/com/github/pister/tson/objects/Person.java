package com.github.pister.tson.objects;

import java.beans.Transient;
import java.util.Arrays;
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

    private FooEnum myEnum;

    private List<String> address;

    private List<Contact> contacts;

    private Map<String, Object> attrs;

    private String[] mobiles;

    private int[] attr1;

    private Integer[] attr2;

    private int[][] myMatrix;

    private String transientValue;

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

    public int[][] getMyMatrix() {
        return myMatrix;
    }

    public void setMyMatrix(int[][] myMatrix) {
        this.myMatrix = myMatrix;
    }

    @Transient
    public String getTransientValue() {
        return transientValue;
    }

    public void setTransientValue(String transientValue) {
        this.transientValue = transientValue;
    }

    public FooEnum getMyEnum() {
        return myEnum;
    }

    public void setMyEnum(FooEnum myEnum) {
        this.myEnum = myEnum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Person person = (Person) o;

        if (age != person.age) return false;
        if (married != person.married) return false;
        if (weight != person.weight) return false;
        if (name != null ? !name.equals(person.name) : person.name != null) return false;
        if (birth != null ? !birth.equals(person.birth) : person.birth != null) return false;
        if (myEnum != person.myEnum) return false;
        if (address != null ? !address.equals(person.address) : person.address != null) return false;
        if (contacts != null ? !contacts.equals(person.contacts) : person.contacts != null) return false;
        if (attrs != null ? !attrs.equals(person.attrs) : person.attrs != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(mobiles, person.mobiles)) return false;
        if (!Arrays.equals(attr1, person.attr1)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(attr2, person.attr2)) return false;
        return Arrays.deepEquals(myMatrix, person.myMatrix);
    }

    @Override
    public int hashCode() {
        int result = age;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (birth != null ? birth.hashCode() : 0);
        result = 31 * result + (married ? 1 : 0);
        result = 31 * result + (int) (weight ^ (weight >>> 32));
        result = 31 * result + (myEnum != null ? myEnum.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (contacts != null ? contacts.hashCode() : 0);
        result = 31 * result + (attrs != null ? attrs.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(mobiles);
        result = 31 * result + Arrays.hashCode(attr1);
        result = 31 * result + Arrays.hashCode(attr2);
        result = 31 * result + Arrays.deepHashCode(myMatrix);
        return result;
    }

    @Override
    public String toString() {
        return "Person{" +
                "age=" + age +
                ", name='" + name + '\'' +
                ", birth=" + birth +
                ", married=" + married +
                ", weight=" + weight +
                ", myEnum=" + myEnum +
                ", address=" + address +
                ", contacts=" + contacts +
                ", attrs=" + attrs +
                ", mobiles=" + Arrays.toString(mobiles) +
                ", attr1=" + Arrays.toString(attr1) +
                ", attr2=" + Arrays.toString(attr2) +
                ", myMatrix=" + Arrays.toString(myMatrix) +
                ", transientValue='" + transientValue + '\'' +
                '}';
    }
}
