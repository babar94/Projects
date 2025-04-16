package com.gateway.utils;


import net.spy.memcached.MemcachedClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MemcachedService {

    @Autowired
    private MemcachedClient memcachedClient;

    // Set value with TTL
    public void set(String key, String value, int ttlSeconds) {
        memcachedClient.set(key, ttlSeconds, value);
    }

    // Get value
    public String get(String key) {
        Object value = memcachedClient.get(key);
        return value != null ? value.toString() : null;
    }

    // Get and remove
    public String getAndDelete(String key) {
        Object value = memcachedClient.get(key);
        if (value != null) {
            memcachedClient.delete(key);
            return value.toString();
        }
        return null;
    }


    // Delete key
    public void delete(String key) {
        memcachedClient.delete(key);
    }
}
