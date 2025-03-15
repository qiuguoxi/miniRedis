package com.qgx;
import java.util.*;
public class Data {
    public HashMap<String, String> dataMap = new HashMap<>();
    public HashMap<String, Long> expireMap = new HashMap<>();


    public Data(){
        this.dataMap = Disk.loadDataFromDisk();
        this.expireMap = Disk.loadExpFromDisk();
    }
    public String get(String key) {
        // 检查键是否存在
        if (!dataMap.containsKey(key)) {
            return "-ERR key not found\r\n";
        }

        // 检查键是否已过期
        if (expireMap.containsKey(key)) {
            if (expireMap.get(key) < System.currentTimeMillis()) {
                expireMap.remove(key);
                dataMap.remove(key);
                return "-ERR key not found\r\n";
            }
        }

        // 返回键的值
        return "$" + dataMap.get(key).length() + "\r\n" + dataMap.get(key) + "\r\n";
    }

    public void cleanExpiredKeys(){
        for(Map.Entry<String,Long> entry:expireMap.entrySet()){
            if(entry.getValue() < System.currentTimeMillis()){
                dataMap.remove(entry.getKey());
                expireMap.remove(entry.getKey());
            }
        }
    }
}
