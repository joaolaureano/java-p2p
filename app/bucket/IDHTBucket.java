package app.bucket;

import java.util.List;
import java.util.Map;

public abstract class IDHTBucket<T> {

    Map<String, T> storage;

    public abstract boolean setIfPossible(String hash, T data);
    public abstract void get(String hash);
    public abstract List<T> toList();
    
    public abstract String toString();

}