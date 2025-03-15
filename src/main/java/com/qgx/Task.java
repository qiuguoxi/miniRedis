package com.qgx;
//定时任务类，定时生成rdb
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
public class Task {
    public static int time = 5000;//毫秒
    public static int count = 0;
    public static int threshold = 2;
    // 创建定时任务调度器

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    // 定时检查并生成 RDB 快照
    public static void rdbStart(Data data) {
        scheduler.scheduleAtFixedRate(() -> {
            if (count >= threshold) {
                Disk.saveToDiskAsync(data.dataMap,data.expireMap);
                count = 0; // 重置计数器
            }
        }, 0, time, TimeUnit.MILLISECONDS);
        System.out.println("rdb Started...");
    }

}
