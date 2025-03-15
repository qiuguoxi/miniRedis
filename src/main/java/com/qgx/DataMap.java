package com.qgx;
import java.util.*;

public class DataMap {
    public  Map<String, String> dataMap;

    public DataMap(HashMap<String,String> map) {
        this.dataMap = map;
    }
    public String get(String key){
        return dataMap.get(key);
    }
}
