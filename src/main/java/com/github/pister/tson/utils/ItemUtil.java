package com.github.pister.tson.utils;

import com.github.pister.tson.access.ObjectVisitor;
import com.github.pister.tson.access.property.PropertyObjectVisitor;
import com.github.pister.tson.common.ItemType;
import com.github.pister.tson.common.Types;
import com.github.pister.tson.models.Item;
import com.github.pister.tson.parse.Parser;

import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by songlihuang on 2020/1/6.
 */
public final class ItemUtil {

    private ItemUtil() {
    }

    private static final ObjectVisitor objectVisitor = new PropertyObjectVisitor();

    public static Item wrapItem(Object o) {
        return wrapItemImpl(o, new ArrayList<Object>());
    }

    private static Item wrapItemImpl(Object o, List<Object> parents) {
        if (o == null) {
            return null;
        }
        checkCycleReference(o, parents);
        List<Object> clonedParents = copyList(parents, o);
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
            return new Item(ItemType.CHAR, o);
        }
        if (o instanceof Date) {
            return new Item(ItemType.DATE, o);
        }
        if (o instanceof Map) {
            if (o instanceof HashMap) {
                return mapToItem((Map) o, null, clonedParents);
            } else {
                return mapToItem((Map) o, o.getClass().getCanonicalName(), clonedParents);
            }
        }
        if (o instanceof Iterable) {
            if (o instanceof ArrayList) {
                return iterableToItem((Iterable) o, null, clonedParents);
            } else {
                return iterableToItem((Iterable) o, o.getClass().getCanonicalName(), clonedParents);
            }
        }
        if (o.getClass().isArray()) {
            if (o.getClass().getComponentType().equals(Byte.TYPE)) {
                return new Item(ItemType.BINARY, o);
            } else {
                return arrayToItem(o, clonedParents);
            }
        }
        // plain object
        Map<String, Object> properties = objectVisitor.getFields(o);
        return mapToItem(properties, o.getClass().getName(), clonedParents);
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
            case CHAR:
                return item.getValue().toString().charAt(0);
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
                return item.getValue();
            case LIST:
                return toListObject(item);
            case MAP:
                return toMapObject(item);
            default:
                throw new RuntimeException("unknown type:" + item.getType());
        }
    }

    private static Class<?> getUserClass(Item item) {
        // use java.util.Arrays.ArrayList treat as default List
        if ("java.util.Arrays.ArrayList".equals(item.getUserTypeName())) {
            return null;
        }
        if (!StringUtil.isEmpty(item.getUserTypeName())) {
            try {
                return Class.forName(item.getUserTypeName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private static class ArrayWidthDimensions {
        Object array;
        int[] dimensions;

        public ArrayWidthDimensions(Object array, int... dimensions) {
            this.array = array;
            this.dimensions = dimensions;
        }
    }



    private static ArrayWidthDimensions createArray(Item item, Class<?> clazzComponent) {
        List<Item> data = (List<Item>) item.getValue();
        int dimensions = item.getArrayDimensions();
        if (dimensions == 1) {
            Object array = Array.newInstance(clazzComponent, data.size());
            int i = 0;
            for (Item subItem : data) {
                Object subObject = itemToObject(subItem);
                Array.set(array, i, subObject);
                i++;
            }
            return new ArrayWidthDimensions(array, 1);
        }
        if (data == null || data.size() == 0) {
            return new ArrayWidthDimensions(null, 0);
        }

        List<Object> subObjects = new ArrayList<Object>(data.size());
        int[] subDimensions = null;
        for (Item subItem : data) {
            ArrayWidthDimensions awd = createArray(subItem, clazzComponent);
            if (subDimensions == null) {
                subDimensions = awd.dimensions;
            } else {
                if (!ArrayUtil.isArrayEquals(subDimensions, awd.dimensions)) {
                    throw new RuntimeException("there was deference dimensions under same array.");
                }
            }
            subObjects.add(awd.array);
        }
        int[] resultDimensions = new int[subDimensions.length + 1];
        resultDimensions[0] = subObjects.size();
        System.arraycopy(subDimensions, 0, resultDimensions, 1, subDimensions.length);
        Object ret = Array.newInstance(clazzComponent, resultDimensions);
        for (int i = 0, len = subObjects.size(); i < len; i++) {
            Array.set(ret, i, subObjects.get(i));
        }
        return new ArrayWidthDimensions(ret, resultDimensions);
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
            ArrayWidthDimensions arrayWidthDimensions = createArray(item, clazz);
            return arrayWidthDimensions.array;
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
        Map<Object, Item> srcMap = (Map<Object, Item>) item.getValue();

        Map<Object, Object> destMap = new HashMap<Object, Object>();
        for (Map.Entry<Object, Item> entry : srcMap.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof Item) {
                destMap.put(itemToObject((Item)key), itemToObject(entry.getValue()));
            } else {
                destMap.put(entry.getKey(), itemToObject(entry.getValue()));

            }
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

    private static List<Object> copyList(List<Object> objectList, Object ... additialObjects) {
        List<Object> ret = new ArrayList<Object>(objectList);
        for (Object obj : additialObjects) {
            ret.add(obj);
        }
        return ret;
    }

    private static void checkCycleReference(Object o, List<Object> parents) {
        for (Object parent : parents) {
            if (parent == o) {
                throw new RuntimeException("cycle reference for: " + o.getClass());
            }
        }
    }

    private static ArrayType getArrayTypeAndDimensions(Class<?> arrayType) {
        int dimensions = 0;
        Class<?> type = arrayType;
        while (type.isArray()) {
            dimensions++;
            type = type.getComponentType();
        }
        ArrayType ret = new ArrayType();
        ret.dimensions = dimensions;
        ret.componentType = type;
        return ret;
    }

    static class ArrayType {
        Class<?> componentType;
        int dimensions;
    }

    private static Item arrayToItem(Object arrayObject, List<Object> parents) {
        List<Item> items = new ArrayList<Item>();
        for (int i = 0, len = Array.getLength(arrayObject); i < len; i++) {
            Object o = Array.get(arrayObject, i);
            Item value = wrapItemImpl(o, copyList(parents));
            items.add(value);
        }
        Item item = new Item(ItemType.LIST, items);

        ArrayType arrayType = getArrayTypeAndDimensions(arrayObject.getClass());
        ItemType itemType = Types.getArrayComponentType(arrayType.componentType);
        if (itemType != null) {
            item.setArrayComponentType(itemType);
        } else {
            item.setArrayComponentUserTypeName(arrayType.componentType.getCanonicalName());
        }
        item.setArrayDimensions(arrayType.dimensions);
        item.setArray(true);
        return item;
    }

    private static Item iterableToItem(Iterable it, String userTypeName, List<Object> parents) {
        List<Item> items = new ArrayList<Item>();
        for (Object o : it) {
            Item value = wrapItemImpl(o, copyList(parents));
            items.add(value);
        }
        return new Item(ItemType.LIST, items, userTypeName);
    }

    private static final Pattern MAP_KEY_PATTERN = Pattern.compile("[a-zA-Z_$][a-zA-Z_$\\d]*");

    private static Item mapToItem(Map<?, ?> m, String userTypeName, List<Object> parents) {
        Map<Item, Item> tsonMap = new LinkedHashMap<Item, Item>();
        for (Map.Entry<?, ?> entry : m.entrySet()) {
            Object keyObject = entry.getKey();
            if (keyObject == null) {
                continue;
            }

            /*
            if (!(keyObject instanceof String)) {
                throw new RuntimeException("the key of map only supports identify string, regex patterns are [a-zA-Z_$][a-zA-Z_$\\d]*");
            }
            String key = keyObject.toString();
            if (!MAP_KEY_PATTERN.matcher(key).matches()) {
                throw new RuntimeException("the key of map only supports identify string, regex patterns are [a-zA-Z_$][a-zA-Z_$\\d]*");
            }
            */

            Object o = entry.getValue();
            Item value = wrapItemImpl(o, copyList(parents));
            if (keyObject instanceof String) {
                tsonMap.put(wrapItem(keyObject.toString()), value);
            } else {
                tsonMap.put(wrapItem(keyObject), value);
            }


        }
        return new Item(ItemType.MAP, tsonMap, userTypeName);
    }


}
