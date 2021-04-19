package com.github.pister.tson.access;

import java.util.Map;

/**
 * Created by songlihuang on 2020/1/7.
 */
public interface ObjectVisitor {

    Map<String, Object> getFields(Object object);

    void setFields(Object object, Map<Object, Object> fields);

}
