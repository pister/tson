package com.github.pister.tson.utils;

import com.github.pister.tson.access.ObjectVisitor;
import com.github.pister.tson.access.property.PropertyObjectVisitor;
import com.github.pister.tson.common.ItemType;
import com.github.pister.tson.common.Types;
import com.github.pister.tson.models.Item;
import com.github.pister.tson.parse.Parser;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by songlihuang on 2020/1/6.
 */
public final class ItemUtil {

    private ItemUtil() {
    }

    private static final ObjectVisitor objectVisitor = new PropertyObjectVisitor();

    /**
     * 基本类型的名字。老版本 boolean[] 的组件类型名被写成 "boolean" 走到了
     * 用户类型路径，Class.forName 加载不了 —— 那份存量数据从来没被任何版本
     * 读出来过，这里认得基本类型名，把它救活。
     */
    private static final Map<String, Class<?>> PRIMITIVE_CLASSES;

    static {
        Map<String, Class<?>> m = new HashMap<String, Class<?>>();
        m.put("boolean", Boolean.TYPE);
        m.put("byte", Byte.TYPE);
        m.put("char", Character.TYPE);
        m.put("short", Short.TYPE);
        m.put("int", Integer.TYPE);
        m.put("long", Long.TYPE);
        m.put("float", Float.TYPE);
        m.put("double", Double.TYPE);
        PRIMITIVE_CLASSES = Collections.unmodifiableMap(m);
    }

    public static Item wrapItem(Object o) {
        return wrapItemImpl(o, new ArrayList<Object>());
    }

    private static Item wrapItemImpl(Object o, List<Object> parents) {
        if (o == null) {
            // null 也是值，返回 null 引用的话，容器只能丢弃或者写出非法文本
            return new Item(ItemType.NULL, null);
        }
        checkCycleReference(o, parents);
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
        if (o instanceof LocalDateTime) {
            return new Item(ItemType.LOCAL_DATE_TIME, o);
        }
        if (o instanceof LocalDate) {
            return new Item(ItemType.LOCAL_DATE, o);
        }
        if (o instanceof LocalTime) {
            return new Item(ItemType.LOCAL_TIME, o);
        }
        // todo add localDate, localTime
        // 带常量体的枚举常量（enum X { A { ... } }）getClass() 是匿名子类，
        // isEnum() 返回 false，会掉进下面的 plain-object 分支编成空对象，
        // 枚举值无声丢失。必须用 instanceof 判断，类型名取 getDeclaringClass()
        if (o instanceof Enum) {
            return new Item(ItemType.ENUM, o, ((Enum)o).getDeclaringClass().getName());
        }
        // 只有容器才需要往下传父节点链，标量在上面已经返回了，
        // 放在这里可以省掉每个叶子节点一次 ArrayList 的新建和丢弃
        List<Object> clonedParents = copyList(parents, o);
        if (o instanceof Map) {
            if (o instanceof HashMap) {
                return mapToItem((Map) o, null, clonedParents);
            } else {
                return mapToItem((Map) o, userTypeNameOf(o.getClass()), clonedParents);
            }
        }
        if (o instanceof Iterable) {
            if (o instanceof ArrayList) {
                return iterableToItem((Iterable) o, null, clonedParents);
            } else {
                return iterableToItem((Iterable) o, userTypeNameOf(o.getClass()), clonedParents);
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
            case LOCAL_DATE_TIME:
            case LOCAL_DATE:
            case LOCAL_TIME:
            case STRING:
            case ENUM:
                return item.getValue();
            case CHAR:
                return item.getValue().toString().charAt(0);
            case INT8:
                return (byte) requireIntInRange(item, Byte.MIN_VALUE, Byte.MAX_VALUE);
            case INT16:
                return (short) requireIntInRange(item, Short.MIN_VALUE, Short.MAX_VALUE);
            case INT32:
                return (int) requireIntInRange(item, Integer.MIN_VALUE, Integer.MAX_VALUE);
            case INT64:
                return requireIntInRange(item, Long.MIN_VALUE, Long.MAX_VALUE);
            case FLOAT32:
                return (float) requireDoubleInFloatRange(item);
            case FLOAT64:
                return ((Number) item.getValue()).doubleValue();
            case BINARY:
                return item.getValue();
            case NULL:
                return null;
            case LIST:
                return toListObject(item);
            case MAP:
                return toMapObject(item);
            default:
                throw new RuntimeException("unknown type:" + item.getType());
        }
    }

    /**
     * 窄类型整数解出的守护：必须拿到整数且在目标范围内。
     *
     * <p>修复前直接 byteValue()/shortValue()/intValue() 静默截断：
     * i8@128 回绕成 -128、i8@1.5 截成 1，坏数据看起来像好数据。
     * 编码端写出的都是真实字节，超范围只可能来自手写或损坏的文本，宁可报错。</p>
     *
     * <p>浮点字面量放行"数学上恰好是整数"的（i32@1e3 是既有的合法写法，
     * 见 NumberPrecisionTest.testExponentWithoutDot），非整值或非有限值拒绝。</p>
     */
    private static long requireIntInRange(Item item, long min, long max) {
        Object value = item.getValue();
        long v;
        if (value instanceof Long) {
            v = (Long) value;
        } else if (value instanceof Double) {
            double d = (Double) value;
            if (!Double.isFinite(d) || d != Math.rint(d)) {
                throw new RuntimeException(item.getType().getTypeName() + "@ needs an integer, but: " + value);
            }
            v = (long) d;
        } else {
            throw new RuntimeException(item.getType().getTypeName() + "@ needs an integer, but: " + value);
        }
        if (v < min || v > max) {
            throw new RuntimeException(item.getType().getTypeName() + "@" + v + " out of range [" + min + ", " + max + "]");
        }
        return v;
    }

    /**
     * f32 解出的守护：有限的 double 值转 float 变 Infinite 就是超范围。
     * NaN / Infinity 本身合法照常通过（见 SpecialDoubleTest）。
     */
    private static double requireDoubleInFloatRange(Item item) {
        double d = ((Number) item.getValue()).doubleValue();
        float f = (float) d;
        if (Double.isFinite(d) && Float.isInfinite(f)) {
            throw new RuntimeException(item.getType().getTypeName() + "@" + d + " out of range [" + -Float.MAX_VALUE + ", " + Float.MAX_VALUE + "]");
        }
        return f;
    }

    /**
     * 集合类型写进类型表时用的名字，无法还原的类型返回 null 表示退回默认集合。
     *
     * <p>用 {@link Class#getName()} 而不是 getCanonicalName()：后者把内部类的 $ 写成 .，
     * 得到的名字 Class.forName 根本加载不了（java.util.Arrays.ArrayList 就是这么来的）。</p>
     *
     * <p>另一方面，Arrays.asList、Collections.unmodifiableXxx 返回的都是私有内部类，
     * 名字写对了解码时也 newInstance 不出来。与其写一个还原不了的名字，
     * 不如不写，让解码端直接用默认的 ArrayList / HashMap —— 这也正是它们今天的实际效果。</p>
     */
    private static String userTypeNameOf(Class<?> clazz) {
        if (!Modifier.isPublic(clazz.getModifiers())) {
            return null;
        }
        try {
            Constructor<?> constructor = clazz.getConstructor();
            if (!Modifier.isPublic(constructor.getModifiers())) {
                return null;
            }
        } catch (NoSuchMethodException e) {
            return null;
        }
        return clazz.getName();
    }

    private static Class<?> getUserClass(Item item) {
        // 老版本用 getCanonicalName() 写类型名，Arrays.asList 会写成这个加载不了的名字，
        // 存量数据里还有，必须继续认。新数据由 userTypeNameOf 保证不会再写出这种名字。
        if ("java.util.Arrays.ArrayList".equals(item.getUserTypeName())) {
            return null;
        }
        if (!StringUtil.isEmpty(item.getUserTypeName())) {
            try {
                return ClassUtil.forName(item.getUserTypeName());
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
            // 空的多维数组不能返回 null：null 和空数组是两种业务语义。
            // 内层维度未知，全零维度建出的 int[0][0] 与 new int[0][]
            // 运行时同型同值，语义就是"该维度为空"
            int[] emptyDimensions = new int[Math.max(dimensions, 1)];
            return new ArrayWidthDimensions(Array.newInstance(clazzComponent, emptyDimensions));
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
                clazz = PRIMITIVE_CLASSES.get(item.getArrayComponentUserTypeName());
                if (clazz == null) {
                    try {
                        clazz = ClassUtil.forName(item.getArrayComponentUserTypeName());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
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

        // 同样用 LinkedHashMap 保持顺序。LinkedHashMap 也是 HashMap，
        // 没有类型名时直接把它返回给调用方，instanceof / 强转都不受影响
        Map<Object, Object> destMap = new LinkedHashMap<Object, Object>();
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
            // 和 userTypeNameOf 同理用 getName()：getCanonicalName() 把内部类的
            // $ 写成 .，嵌套类的枚举数组、bean 数组会写出一个 Class.forName
            // 加载不了的名字，那份数据从此读不回来
            item.setArrayComponentUserTypeName(arrayType.componentType.getName());
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

    private static Item mapToItem(Map<?, ?> m, String userTypeName, List<Object> parents) {
        Map<Item, Item> tsonMap = new LinkedHashMap<Item, Item>();
        for (Map.Entry<?, ?> entry : m.entrySet()) {
            Object keyObject = entry.getKey();

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
