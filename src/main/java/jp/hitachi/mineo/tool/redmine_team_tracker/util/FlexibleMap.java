package jp.hitachi.mineo.tool.redmine_team_tracker.util;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

public class FlexibleMap<K extends Comparable<K>, V> {
  private TreeMap<K, V> map;

  public FlexibleMap() {
    this.map = new TreeMap<>();
  }

  public void put(K key, V value) {
    map.put(key, value);
  }

  public V getNearestOrEqualValue(K key) {
    K ceilingKey = map.floorKey(key);
    return ceilingKey != null ? map.get(ceilingKey) : null;
  }

  public List<V> getValuesInRange(K key1, K key2) {
    NavigableMap<K, V> subMap = map.subMap(key1, true, key2, true);
    return new ArrayList<>(subMap.values());
  }
}
