package com.github.pister.tson.utils;

import com.github.pister.tson.access.ObjectVisitor;
import com.github.pister.tson.access.property.ObjectUtil;
import com.github.pister.tson.access.property.PropertyObjectVisitor;
import com.github.pister.tson.common.ItemType;
import com.github.pister.tson.common.Types;
import com.github.pister.tson.models.Item;
import com.sun.org.apache.xpath.internal.operations.Bool;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by songlihuang on 2020/1/6.
 */
public final class ItemUtil {

    private ItemUtil() {
    }

    private static final ObjectVisitor objectVisitor = new PropertyObjectVisitor();

    public static Item wrapItem(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            return new Item(ItemType.STRING, o);
        }
        if (o instanceof Number) {
            return new Item(Types.numberTypeToItemType(o.getClass()), o);
        }
        if (o instanceof Boolean) {
            return new Item(ItemType.BOOL, o);
        }
        if (o instanceof Character) {
            return new Item(ItemType.INT16, (short) ((Character) o).charValue());
        }
        if (o instanceof Date) {
            return new Item(ItemType.DATE, o);
        }
        if (o instanceof Map) {
            return mapToItem((Map) o, null);
        }
        if (o instanceof Iterable) {
            return iterableToItem((Iterable) o, null);
        }
        if (o.getClass().isArray()) {
            return arrayToItem(o, o.getClass().getComponentType());
        }
        // plain object
        Map<String, Object> properties = objectVisitor.getFields(o);
        return mapToItem(properties, o.getClass().getName());
    }

    public static Object itemToObject(Item item) {
        if (item == null) {
            return null;
        }
        switch (item.getType()) {
            case BOOL:
            case DATE:
            case STRING:
                return item.getValue();
            case INT8:
                return ((Number) item.getValue()).byteValue();
            case INT16:
                return ((Number) item.getValue()).shortValue();
            case INT32:
                return ((Number) item.getValue()).intValue();
            case INT64:
                return ((Number) item.getValue()).longValue();
            case FLOAT32:
                return ((Number) item.getValue()).floatValue();
            case FLOAT64:
                return ((Number) item.getValue()).doubleValue();
            case BINARY:
                // TODO
                return null;
            case LIST:
                return toListObject(item);
            case MAP:
                return toMapObject(item);
            default:
                throw new RuntimeException("unknown type:" + item.getType());
        }
    }

    private static Class<?> getUserClass(Item item) {
        if (!StringUtil.isEmpty(item.getUserTypeName())) {
            try {
                return Class.forName(item.getUserTypeName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private static Object toListObject(Item item) {
        List<Item> data = (List<Item>) item.getValue();
        if (item.isArray()) {
            Class<?> clazz;
            if (!StringUtil.isEmpty(item.getArrayComponentUserTypeName())) {
                try {
                    clazz = Class.forName(item.getArrayComponentUserTypeName());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else {
                ItemType itemType = item.getArrayComponentType();
                if (itemType == null) {
                    throw new RuntimeException("miss type for array: " + item);
                }
                clazz = itemType.getType();
                if (clazz == null) {
                    throw new RuntimeException("not support array for: " + itemType);
                }
            }
            Object array = Array.newInstance(clazz, data.size());
            int i = 0;
            for (Item subItem : data) {
                Object subObject = itemToObject(subItem);
                Array.set(array, i, subObject);
                i++;
            }
            return array;
        } else {
            Collection collection;
            Class<?> userClass = getUserClass(item);
            if (userClass == null) {
                // default use ArrayList
                collection = new ArrayList();
            } else {
                if (!Collection.class.isAssignableFrom(userClass)) {
                    throw new RuntimeException(item.getUserTypeName() + " is not a Collection type");
                }
                try {
                    collection = (Collection) userClass.newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            for (Item subItem : data) {
                Object subObject = itemToObject(subItem);
                collection.add(subObject);
            }
            return collection;
        }
    }

    private static Object toMapObject(Item item) {
        Map<String, Item> srcMap = (Map<String, Item>) item.getValue();

        Map<String, Object> destMap = new HashMap<String, Object>();
        for (Map.Entry<String, Item> entry : srcMap.entrySet()) {
            destMap.put(entry.getKey(), itemToObject(entry.getValue()));
        }
        Class<?> userClass = getUserClass(item);
        if (userClass == null) {
            return destMap;
        } else {
            Object object;
            try {
                object = userClass.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (Map.class.isAssignableFrom(userClass)) {
                Map m = (Map) object;
                m.putAll(destMap);
                return m;
            } else {
                // raw object
                objectVisitor.setFields(object, destMap);
                return object;
            }
        }
    }

    private static Item arrayToItem(Object arrayObject, Class<?> componentType) {
        List<Item> items = new ArrayList<Item>();
        for (int i = 0, len = Array.getLength(arrayObject); i < len; i++) {
            Object o = Array.get(arrayObject, i);
            Item value = wrapItem(o);
            items.add(value);
        }
        Item item = new Item(ItemType.LIST, items);
        ItemType itemType = Types.getArrayComponentType(componentType);
        if (itemType != null) {
            item.setArrayComponentType(itemType);
        } else {
            item.setArrayComponentUserTypeName(componentType.getCanonicalName());
        }
        item.setArray(true);
        return item;
    }

    private static Item iterableToItem(Iterable it, String userTypeName) {
        List<Item> items = new ArrayList<Item>();
        for (Object o : it) {
            Item value = wrapItem(o);
            items.add(value);
        }
        return new Item(ItemType.LIST, items, userTypeName);
    }

    private static Item mapToItem(Map<?, ?> m, String userTypeName) {
        Map<String, Item> tsonMap = new LinkedHashMap<String, Item>();
        for (Map.Entry<?, ?> entry : m.entrySet()) {
            Object keyObject = entry.getKey();
            if (keyObject == null) {
                continue;
            }
            String key = keyObject.toString();
            Item value = wrapItem(entry.getValue());
            tsonMap.put(key, value);
        }
        return new Item(ItemType.MAP, tsonMap, userTypeName);
    }


}
