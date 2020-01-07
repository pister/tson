package com.github.pister.tson.utils;

import com.github.pister.tson.access.ObjectVisitor;
import com.github.pister.tson.access.property.ObjectUtil;
import com.github.pister.tson.access.property.PropertyObjectVisitor;
import com.github.pister.tson.common.ItemType;
import com.github.pister.tson.common.Types;
import com.github.pister.tson.models.Item;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by songlihuang on 2020/1/6.
 */
public final  class ItemUtil {

    private ItemUtil() {}

    private static final ObjectVisitor objectVisitor  = new PropertyObjectVisitor();

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
            return new Item(ItemType.INT16, (short)((Character)o).charValue());
        }
        if (o instanceof Date) {
            return new Item(ItemType.DATE, o);
        }
        if (o instanceof Map) {
            return mapToItem((Map)o, null);
        }
        if (o instanceof Iterable) {
            return iterableToItem((Iterable)o, null);
        }
        if (o.getClass().isArray()) {
            return arrayToItem(o, o.getClass().getComponentType());
        }
        // plain object
        Map<String, Object> properties = objectVisitor.getFields(o);
        return mapToItem(properties, o.getClass().getName());
    }

    private static Item arrayToItem(Object arrayObject, Class<?> componentType) {
        List<Item> items = new ArrayList<Item>();
        for (int i = 0, len = Array.getLength(arrayObject); i < len; i++) {
            Object o = Array.get(arrayObject, i);
            Item value = wrapItem(o);
            items.add(value);
        }
        Item item = new Item(ItemType.LIST, items);
        ItemType itemType = Types.classToItemType(componentType);
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
        for (Map.Entry<?, ?> entry: m.entrySet()) {
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
