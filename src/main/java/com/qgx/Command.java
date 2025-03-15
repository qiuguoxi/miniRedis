package com.qgx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Command {
    public static String handleCommand(List<String> args, Data data) {
        // 获取命令并转换为大写
        String command = args.get(0).toUpperCase();
        Map<String, String> dataMap = data.dataMap;
        Map<String, Long> expireMap = data.expireMap;
        // 根据命令类型执行相应操作
        switch (command) {
            case "PING":
                return "+PONG,I'm Qedis\r\n"; // 返回 PONG
            case "ECHO":
                return "$" + args.get(1).length() + "\r\n" + args.get(1) + "\r\n"; // 返回输入的字符串
            case "SET":
                // 如果过期时间存在且已过期，删除过期时间
                if (expireMap.containsKey(args.get(1)) && expireMap.get(args.get(1)) < System.currentTimeMillis()) {
                    expireMap.remove(args.get(1));
                }
                // 如果有第三个参数，则设置过期时间
                if (args.size() == 5) {
                    long expirationTime = args.get(3).equalsIgnoreCase("EX") ? Long.parseLong(args.get(4)) * 1000 : Long.parseLong(args.get(4));
                    expireMap.put(args.get(1), System.currentTimeMillis() + expirationTime);
                }
                // 更新或插入数据
                dataMap.put(args.get(1), args.get(2));
                Task.count++;
                return "+OK\r\n"; // 返回 OK
            case "TTLS":
                //遍历expiremap删除过期键
                data.cleanExpiredKeys();
                return "*" + dataMap.size() + "\r\n" + dataMap.keySet().stream()
                        .map(key -> "$" + key.length() + "\r\n" + key + "\r\n")
                        .collect(Collectors.joining());
            case "GET":
                data.get(args.get(1));
            case "DEL":
                if(dataMap.containsKey(args.get(1))){
                    dataMap.remove(args.get(1));
                    Task.count++;
                    return "+OK\r\n"; // 返回 OK
                }else{
                    return "-ERR key not found\r\n";
                }
            case "EXPIRE":
                if(!dataMap.containsKey(args.get(1))){
                    return "-ERR key not found\r\n";
                }
                if(expireMap.containsKey(args.get(1))){
                    if(expireMap.get(args.get(1)) < System.currentTimeMillis()){
                        return "-ERR key not found\r\n";
                    }
                }
                expireMap.put(args.get(1),1000*Long.parseLong(args.get(2))+System.currentTimeMillis());
                Task.count++;
                return "+OK\r\n"; // 返回 OK

            case "KEYS":
                //遍历expiremap删除过期键
                for(Map.Entry<String,Long> entry:expireMap.entrySet()){
                    if(entry.getValue() < System.currentTimeMillis()){
                        dataMap.remove(entry.getKey());
                        expireMap.remove(entry.getKey());
                    }
                }
                return "*" + dataMap.size() + "\r\n" + dataMap.keySet().stream()
                        .map(key -> "$" + key.length() + "\r\n" + key + "\r\n")
                        .collect(Collectors.joining());
            case "SAVE":
                Disk.saveToDiskAsync(dataMap,expireMap);
                return "+OK\r\n";
            case "LOAD":
                dataMap = Disk.loadDataFromDisk();
                expireMap = Disk.loadExpFromDisk();
                return  "+OK\r\n";
            case "TTL":
                if(!dataMap.containsKey(args.get(1))){
                    return "-ERR key not found\r\n";
                }
                if(expireMap.containsKey(args.get(1))){
                    if(expireMap.get(args.get(1)) < System.currentTimeMillis()){
                        return "-ERR key not found\r\n";
                    }
                    return ":" + (expireMap.get(args.get(1))-System.currentTimeMillis())/1000 + "\r\n";
                }else{
                    // 没有设置过期时间
                    return "-ERR key has no ttl\r\n";
                }
            default:
                return "-ERR unknown command\r\n"; // 返回错误信息
        }
    }
}
