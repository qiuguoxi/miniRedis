package com.qgx;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Disk {
    private static final String mapfilePath = "rdb_map.dat";
    private static final String expfilePath = "rdb_exp.dat";

    // 异步保存两个 Map 到磁盘
    public static void saveToDiskAsync(Map map1, Map map2) {
        new Thread(() -> {
            saveMapToDisk(map1, mapfilePath, "dataMap");
            saveMapToDisk(map2, expfilePath, "expMap");
        }).start();
    }

    // 同步保存单个 Map 到磁盘
    private static void saveMapToDisk(Map map, String filePath, String mapName) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(map);
            System.out.println(mapName + " saved to disk successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // 从磁盘加载 dataMap（同步）
    @SuppressWarnings("unchecked")
    public static HashMap loadDataFromDisk() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(mapfilePath))) {
            System.out.println("dataMap loaded from disk successfully.");
            return (HashMap<String, String>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Failed to load dataMap from disk, creating an empty map.");
        }
        return new HashMap<String, String>();
    }

    // 从磁盘加载 expMap（同步）
    @SuppressWarnings("unchecked")
    public static HashMap loadExpFromDisk() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(expfilePath))) {
            System.out.println("expMap loaded from disk successfully.");
            return (HashMap<String, Integer>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Failed to load expMap from disk, creating an empty map.");
        }
        return new HashMap<String, Integer>();
    }
}
