# 一、测试代码
```java
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
/*
演示GC日志生成与解读
*/
public class GCLogAnalysis {
private static Random random = new Random();
public static void main(String[] args) {
// 当前毫秒时间戳
long startMillis = System.currentTimeMillis();
// 持续运行毫秒数; 可根据需要进行修改
long timeoutMillis = TimeUnit.SECONDS.toMillis(1);
// 结束时间戳
long endMillis = startMillis + timeoutMillis;
LongAdder counter = new LongAdder();
System.out.println("正在执行...");
// 缓存一部分对象; 进入老年代
int cacheSize = 2000;
Object[] cachedGarbage = new Object[cacheSize];
// 在此时间范围内,持续循环
while (System.currentTimeMillis() < endMillis) {
// 生成垃圾对象
Object garbage = generateGarbage(100*1024);
counter.increment();
int randomIndex = random.nextInt(2 * cacheSize);
if (randomIndex < cacheSize) {
cachedGarbage[randomIndex] = garbage;
}
}
System.out.println("执行结束!共生成对象次数:" + counter.longValue());
}

    // 生成对象
    private static Object generateGarbage(int max) {
        int randomSize = random.nextInt(max);
        int type = randomSize % 4;
        Object result = null;
        switch (type) {
            case 0:
                result = new int[randomSize];
                break;
            case 1:
                result = new byte[randomSize];
                break;
            case 2:
                result = new double[randomSize];
                break;
            default:
                StringBuilder builder = new StringBuilder();
                String randomString = "randomString-Anything";
                while (builder.length() < randomSize) {
                    builder.append(randomString);
                    builder.append(max);
                    builder.append(randomSize);
                }
                result = builder.toString();
                break;
        }
        return result;
    }
}
```

# 二、测试环境
> jdk8
> 
> CPU 4核
> 
> 内存 16G

# 三、测试过程&结果
##1、串行GC(SerialGC)
### 1.1、分析
**java -XX:+UseSerialGC -Xms128m -Xmx128m -XX:+PrintGCDetails GCLogAnalysis**

|Xms|Xmx|YongGc时间|Full Gc时间|
|:--|:--|:--|:--|
|512M|512M|30ms|30ms|
|1G|1G|40~70ms|...|
|2G|2G|90~120ms|...|
|4G|4G|140~170ms|...|

### 1.2、总结
> 在堆内存越来越大时候，创建对象数量先变大大概在1g的时候数量是最多的，young gc次数也慢慢变多
gc时间越来越长，几乎是指数上升

## 2、并行GC(ParallelGC)
|-Xms|-Xmx|YongGc时间|Full Gc时间|
|:--|:--|:--|:--|
|512M|512M|30ms|30ms|
|1G|1G|40~70ms|...|
|2G|2G|90~120ms|...|
|4G|4G|140~170ms|...|