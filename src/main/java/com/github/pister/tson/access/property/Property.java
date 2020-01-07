package com.github.pister.tson.access.property;

import com.github.pister.tson.utils.ArrayUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

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
     *
     * @return
     */
    public boolean isReadable() {
        return readMethod != null;
    }

    /**
     * 是否可写
     *
     * @return
     */
    public boolean isWritable() {
        return writeMethod != null;
    }


    /**
     * 获取目标对象的属性值
     *
     * @param owner
     * @return
     */
    public Object getValue(final Object owner) {
        if (!isReadable()) {
            throw new UnsupportedOperationException("can not read the property: " + name);
        }
        return AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    return readMethod.invoke(owner, ArrayUtil.EMPTY_OBJECT_ARRAY);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e.getTargetException());
                }
            }
        });
    }

    /**
     * 设置目标对象的属性值
     *
     * @param owner
     * @param newValue
     */
    public void setValue(final Object owner, final Object newValue) {
        if (!isWritable()) {
            throw new UnsupportedOperationException("can not write the property: " + name);
        }
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    writeMethod.invoke(owner, new Object[]{newValue});
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e.getTargetException());
                }
                return null;
            }
        });
    }

    public String getName() {
        return name;
    }

    public Method getReadMethod() {
        return readMethod;
    }

    public Method getWriteMethod() {
        return writeMethod;
    }
}
