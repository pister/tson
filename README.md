# tson
Java对象的持久化方案的另一种选择，对象持久化成人类可读文本字符串。

### 和其他持久化方案的对比
>#### json
> > tson无论是命名还是文本数据格式的组织都是从json发展而来，json的主要问题是类型丢失，
反序列化后所有的类型考推断，这往往会产生与预期不一致的行为。

>#### 其他注如 hassian, protobuf等二进制方案
> > 二进制方案的执行效率本身比文本方案要高，但是牺牲了数据可读性，可调试性，
以及在某些场景下的显示，传输和存储问题。

### 使用方法
```
Map<String, Object> map = new HashMap<String, Object>();
map.put("intValue", 123);
map.put("doubleValue", 123.456);
map.put("floatValue", 1.24f);
map.put("dateValue", new Date());
List<Object> list = new ArrayList<Object>();
list.add("hello");
list.add(123);
list.add(true);
map.put("myList", list);
String s = Tsons.toTsonString(map);
// {dateValue:date@"2020-01-08 17:46:12.908",intValue:i32@123,myList:[str@"hello",i32@123,bool@true],floatValue:f32@1.24,doubleValue:f64@123.456}
System.out.println(s);
Object map2 = Tsons.parseForObject(s);
Assert.assertEquals(map, map2);
```
