package com.github.pister.tson.access.property;

import com.github.pister.tson.access.ObjectVisitor;

import java.util.Map;

/**
 * Created by songlihuang on 2020/1/7.
 */
public class PropertyObjectVisitor implements ObjectVisitor {
    @Override
    public Map<String, Object> getFields(Object object) {
        return ObjectUtil.objectPropertiesToMap(object);
    }
}
