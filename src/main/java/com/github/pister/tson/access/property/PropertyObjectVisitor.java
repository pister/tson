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

    @Override
    public void setFields(Object object, Map<Object, Object> fields) {
        Map<String, Property> propertyMap = ObjectUtil.findPropertiesFromClass(object.getClass());
        for (Map.Entry<String, Property> entry : propertyMap.entrySet()) {
            Object name = entry.getKey();
            Property property = entry.getValue();
            Object value = fields.get(name);
            if (value == null) {
                continue;
            }
            try {
                property.setValue(object, value);
            } catch (IllegalArgumentException e) {
                throw e;
            }
        }
    }
}
