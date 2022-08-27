package com.github.pister.tson.common;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Created by songlihuang on 2022/8/27.
 */
public class EnumUtil {

    public static Object getEnumInstance(Class enumClass, String name) throws IllegalAccessException {
        Field[] fields = enumClass.getDeclaredFields();
        for (Field field : fields) {
            int modifier = field.getModifiers();
            if (Modifier.isPublic(modifier) && Modifier.isStatic(modifier) && field.getType() == enumClass) {
                if (name.equals(field.getName())) {
                    return field.get(enumClass);
                }
            }
        }
        return null;
    }

}
