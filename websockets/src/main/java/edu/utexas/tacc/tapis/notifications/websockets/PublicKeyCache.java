package edu.utexas.tacc.tapis.notifications.websockets;

import org.jvnet.hk2.annotations.Service;

import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PublicKeyCache {

    private ConcurrentHashMap<String, PublicKey> keymap = new ConcurrentHashMap<>();

    public void add(String tenant, PublicKey key) {
        keymap.put(tenant, key);
    }

    public PublicKey get(String tenant) {
       return keymap.get(tenant);
    }


}
