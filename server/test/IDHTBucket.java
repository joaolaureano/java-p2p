package server.test;

import java.util.Map;

public abstract class IDHTBucket<T> {

    Map<String, T> storage;

    public abstract boolean setIfPossible(String hash, T data);
    public abstract void get(String hash);
    
    public abstract String toString();

}