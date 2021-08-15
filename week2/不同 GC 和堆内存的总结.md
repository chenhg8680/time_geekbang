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

|-Xms|-Xmx|YongGc时间|Full Gc时间|
|:--|:--|:--|:--|
|512M|512M|30ms|30ms|
|1G|1G|40~70ms|...|
|2G|2G|90~120ms|...|
|4G|4G|140~170ms|...|

### 1.2、总结
> 在堆内存越来越大时候，创建对象数量先变大大概在1g的时候数量是最多的，young gc次数也慢慢变多
gc时间越来越长，几乎是指数上升

## 2、并行GC(ParallelGC)
### 2.1、分析
**java -XX:+UseParallelGC -Xms1g -Xmx1g  -XX:+PrintGCDetails -XX:+PrintGCDateStamps GCLogAnalysis**
|-Xms|-Xmx|YongGc时间|Full Gc时间|
|:--|:--|:--|:--|
|512M|512M|5ms|15~28ms|
|1G|1G|30ms|50ms|
|2G|2G|70ms|未发生|
|4G|4G|120ms|未发生|
### 2.2、总结
>创建对象数量随着内存的大小 逐渐变大进而变小
gc时间在堆内存大小1g开始时候 垃圾回收时间基本是串行回收期的一般时间


## 3、CMS GC(ConcMarkSweepGC)
### 3.1、分析

**java -XX:+UseConcMarkSweepGC -Xms512m -Xmx512m -XX:+PrintGCDetails -XX:+PrintGCDateStamps GCLogAnalysis**
|-Xms|-Xmx|YongGc时间|Full Gc时间|
|:--|:--|:--|:--|
|512M|512M|20~40ms|20~40ms|
|1G|1G|30~50ms|未发生|
|2G|2G|60~100ms|未发生|
|4G|4G|60~100ms|未发生|
### 3.2、总结
>可以看到 cms创建对象数量不是很多，吞吐量不如并发甚至串行收集器，垃圾回收时间与并行回收相差无异。

## 4、G1 GC(G1GC)
### 4.1、分析

**java -XX:+UseG1GC -Xms512m -Xmx512m -XX:+PrintGCDetails -XX:+PrintGCDateStamps GCLogAnalysis**
|-Xms|-Xmx|YongGc时间|Full Gc时间|
|:--|:--|:--|:--|
|512M|512M| 0.5-0.7ms|1 ~ 2ms|
|1G|1G|0.5-3ms|10ms+|
|2G|2G|10~40ms|未发生|
|4G|4G|20~50ms|未发生|
### 4.2、总结

# 四、总结
>1、内存越小不管是yong gc 和 full gc次数会频繁触发，导致大量时间在做gc；
> 
>2、同等内存大小情况下 串行gc 时间 几乎是 并行gc的二倍；
> 
>3、内存越大的情况下 G1 回收的效率越高，gc时间比其他回收器时间短、内存越大g1效果越好；
> 
>4、可以看到内存越大的情况下g1的垃圾回收基本优于串行 并行 以及 cms 不管是从吞吐量和gc时间来说；
> 
>5、cms 于 吞吐量相对于其他回收器明显存在劣势，回收时间不太稳定；
