package com.github.pister.tson.access.property;

import com.github.pister.tson.utils.ArrayUtil;
import com.github.pister.tson.utils.StringUtil;

import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by songlihuang on 2020/1/6.
 */
public final class ObjectUtil {

    private static final ConcurrentMap<Class<?>, SoftReference<Map<String, Property>>> propertyCache = new ConcurrentHashMap<Class<?>, SoftReference<Map<String, Property>>>();

    private ObjectUtil() {}

    public static Map<String, Object> objectPropertiesToMap(Object object) {
        if (object == null) {
            return null;
        }
        Map<String, Object> ret = new HashMap<String, Object>();
        Class<?> clazz = object.getClass();
        Map<String, Property> propertyMap = findPropertiesFromClass(clazz);
        for (Map.Entry<String, Property> entry : propertyMap.entrySet()) {
            String name = entry.getKey();
            Property property = entry.getValue();
            if (property.isReadable() && property.isWritable()) {
                Method method = property.getReadMethod();
                try {
                   Object value = method.invoke(object, null);
                   ret.put(name, value);
                } catch (IllegalAccessException e) {
                   throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e.getTargetException());
                }
            }
        }
        return ret;
    }

    public static Map<String, Property> findPropertiesFromClass(Class<?> targetClass) {
        SoftReference<Map<String, Property>> cachedProperties = propertyCache.get(targetClass);
        if (cachedProperties != null) {
            Map<String, Property> cachedPropertiesMap = cachedProperties.get();
            if (cachedPropertiesMap != null) {
                return cachedPropertiesMap;
            }
        }
        Method[] methods = targetClass.getMethods();
        Map<String, Method> readableMethods = new HashMap<String, Method>();
        Map<String, Method> writableMethods = new HashMap<String, Method>();
        for (Method method : methods) {
            if (isReadableMethod(method)) {
                String propertyName = getPropertyName(method.getName());
                readableMethods.put(propertyName, method);
            } else if (isWritableMethod(method)) {
                String propertyName = getPropertyName(method.getName());
                writableMethods.put(propertyName, method);
            }
        }
        Map<String, Property> ret = new HashMap<String, Property>();
        for (Map.Entry<String, Method> entry : readableMethods.entrySet()) {
            String name = entry.getKey();
            Method readMethod = entry.getValue();
            Method writeMethod = writableMethods.remove(name);
           // Class<?> propertyClass = readMethod.getReturnType();
            Property property = new Property(name, readMethod, writeMethod);
            ret.put(name, property);
        }
        for (Map.Entry<String, Method> entry : writableMethods.entrySet()) {
            String name = entry.getKey();
            Method writeMethod = entry.getValue();
           // Class<?> propertyClass = writeMethod.getParameterTypes()[0];
            Property property = new Property(name, null, writeMethod);
            ret.put(name, property);
        }
        propertyCache.put(targetClass, new SoftReference<Map<String, Property>>(ret));
        return ret;
    }

    /**
     * 是否写属性
     * @param method
     * @return
     */
    private static boolean isWritableMethod(Method method) {
        if (method == null) {
            return false;
        }
        if (method.getParameterTypes().length != 1) {
            return false;
        }
        String name = method.getName();
        return name.startsWith("set") && name.length() > 3;
    }

    /**
     * 是否为读属性
     * @param method
     * @return
     */
    private static boolean isReadableMethod(Method method) {
        if (method == null) {
            return false;
        }
        if (!ArrayUtil.isEmpty(method.getParameterTypes())) {
            return false;
        }
        if (method.getReturnType().equals(void.class) || method.getReturnType().equals(Void.class)) {
            return false;
        }
        String name = method.getName();
        if (name.startsWith("get")) {
            return name.length() > 3;
        } else if (name.startsWith("is")) {
            return name.length() > 2;
        }
        return false;
    }

    private static String getPropertyName(String methodName) {
        if (methodName.startsWith("get") || methodName.startsWith("set")) {
            return StringUtil.lowercaseFirstLetter(methodName.substring(3));
        } else if (methodName.startsWith("is")) {
            return StringUtil.lowercaseFirstLetter(methodName.substring(2));
        }
        return null;
    }
}
