package com.github.pister.tson;

import com.github.pister.tson.common.ItemType;
import com.github.pister.tson.models.Item;

import java.util.List;
import java.util.Map;

/**
 * Created by songlihuang on 2020/1/5.
 */
public class TsonObject {

    private Item root;


    public TsonObject(Item root) {
        this.root = root;
    }

    public TsonObject(Object root) {
        this.root = Item.wrap(root);
    }

    public Item get(String propertyName) {
        if (root.getType() != ItemType.MAP) {
            return null;
        }
        Map<String, Item> map = (Map<String, Item>)root.getValue();
        return map.get(propertyName);
    }

    public Item get(int index) {
        if (root.getType() != ItemType.LIST) {
            return null;
        }
        List<Item> list = (List<Item> )root.getValue();
        return list.get(index);
    }

    public Item getRoot() {
        return root;
    }
// TODO get other

}
