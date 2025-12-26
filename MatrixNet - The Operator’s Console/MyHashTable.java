import java.util.ArrayList;
import java.util.LinkedList;

public class MyHashTable<K, V> {

    // Represents a single key-value pair stored in the table
    private static class Entry<K, V> {
        K key;
        V value;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    // ArrayList of buckets; each bucket is a LinkedList to handle collisions
    private ArrayList<LinkedList<Entry<K, V>>> buckets;
    private int capacity; // total number of buckets
    private int size;     // number of key-value pairs stored

    public MyHashTable(int initialCapacity) {
        this.capacity = initialCapacity;
        this.size = 0;

        // Create bucket array with the given capacity
        this.buckets = new ArrayList<>(this.capacity);

        // Initialize each bucket as an empty linked list
        for (int i = 0; i < this.capacity; i++) {
            buckets.add(new LinkedList<>());
        }
    }


    public void put(K key, V value) {
        // Determine bucket index using hashed key
        int bucketIndex = getHashIndex(key);
        LinkedList<Entry<K, V>> bucket = buckets.get(bucketIndex);

        // If key exists, update its value
        for (Entry<K, V> entry : bucket) {
            if (entry.key.equals(key)) {
                entry.value = value;
                return;
            }
        }

        // Otherwise, add new key-value pair
        bucket.add(new Entry<>(key, value));
        this.size++;
    }


    public V get(K key) {
        int bucketIndex = getHashIndex(key);
        LinkedList<Entry<K, V>> bucket = buckets.get(bucketIndex);

        // Search the bucket for the matching key
        for (Entry<K, V> entry : bucket) {
            if (entry.key.equals(key)) {
                return entry.value;
            }
        }
        return null; // not found
    }

    public ArrayList<V> values() {
        // Collect all values from all buckets
        ArrayList<V> allValues = new ArrayList<>(this.size);

        for (LinkedList<Entry<K, V>> bucket : buckets) {
            for (Entry<K, V> entry : bucket) {
                allValues.add(entry.value);
            }
        }

        return allValues;
    }

    public V remove(K key) {
        int bucketIndex = getHashIndex(key);
        LinkedList<Entry<K, V>> bucket = buckets.get(bucketIndex);

        // Find the entry to remove
        Entry<K, V> entryToRemove = null;
        for (Entry<K, V> entry : bucket) {
            if (entry.key.equals(key)) {
                entryToRemove = entry;
                break;
            }
        }

        // Remove if found
        if (entryToRemove != null) {
            bucket.remove(entryToRemove);
            this.size--;
            return entryToRemove.value;
        }
        return null;
    }

    public boolean containsKey(K key) {
        int bucketIndex = getHashIndex(key);
        LinkedList<Entry<K, V>> bucket = buckets.get(bucketIndex);

        // Check if key is in the appropriate bucket
        for (Entry<K, V> entry : bucket) {
            if (entry.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return this.size; // total number of entries
    }

    public boolean isEmpty() {
        return this.size == 0; // true if no entries exist
    }

    private int getHashIndex(K key) {
        // Ensure hashCode is positive
        int hashCode = key.hashCode();
        hashCode = hashCode & 0x7fffffff;

        // Map hash to a bucket index
        return hashCode % this.capacity;
    }
}