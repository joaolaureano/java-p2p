package server.test;
import java.util.HashMap;

public class DHTBucket<T> extends IDHTBucket<T> {

    IDHTHash hashOperator;

    public DHTBucket(int numberOfNodes, int currentPosition) {
        this.hashOperator = new DHTHashMd5(numberOfNodes, currentPosition);
        
        this.storage = new HashMap<String, T>();
    }

    @Override
    public boolean setIfPossible(String hash, T data) {
        if (!this.hashOperator.containsHash(hash))
            return false;

        this.storage.put(hash, data);
        return true;
    }

    @Override
    public void get(String hash) {
        System.out.println(this.storage.get(hash));
        return;

    }

    @Override
    public String toString() {
        
        return this.hashOperator.getInterval();
    }
}
