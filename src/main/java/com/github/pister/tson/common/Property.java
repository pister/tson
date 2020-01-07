package com.github.pister.tson.common;

import com.github.pister.tson.utils.ObjectUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by songlihuang on 2020/1/6.
 */
public class Property {

    private String name;

    private Class propertyClass;

    private Method readMethod;

    private Method writeMethod;

    public Property() {

    }

    public Property(String name, Class propertyClass, Method readMethod, Method writeMethod) {
        super();
        this.name = name;
        this.propertyClass = propertyClass;
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
    }

    /**
     * 是否可读
     * @return
     */
    public boolean isReadable() {
        return readMethod != null;
    }

    /**
     * 是否可写
     * @return
     */
    public boolean isWritable() {
        return writeMethod != null;
    }

    /**
     * 属性名
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * 获取属性对应的class
     * @return
     */
    public Class getPropertyClass() {
        return propertyClass;
    }

    /**
     * 获取目标对象的属性值
     * @param owner
     * @return
     */
    public Object getValue(Object owner) {
        if (!isReadable()) {
            throw new UnsupportedOperationException("can not read the property: " + name);
        }
        try {
            return readMethod.invoke(owner, ObjectUtil.EMPTY_OBJECT_ARRAY);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
        }
    }

    /**
     * 设置目标对象的属性值
     * @param owner
     * @param newValue
     */
    public void setValue(Object owner, Object newValue) {
        if (!isWritable()) {
            throw new UnsupportedOperationException("can not write the property: " + name);
        }
        try {
            writeMethod.invoke(owner, new Object[] { newValue });
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
        }
    }

    public Method getReadMethod() {
        return readMethod;
    }

    public Method getWriteMethod() {
        return writeMethod;
    }
}
