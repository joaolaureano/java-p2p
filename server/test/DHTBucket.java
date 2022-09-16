package server.test;

import java.math.BigInteger;
import java.util.HashMap;

public class DHTBucket<T> extends IDHTBucket<T> {

    Long MAX_SIZE;
    Long MIN_SIZE;

    Long max_hashes = (long) Math.pow(2, 128); // TEST PURPOSES, MUST BE REALLY IMPLEMENTED :)

    public DHTBucket(int numberOfNodes, int currentPosition) {
        this.MIN_SIZE = minPossibleHash(numberOfNodes, currentPosition);
        this.MAX_SIZE = maxPossibleHash(numberOfNodes, currentPosition);
        this.storage = new HashMap<String, T>();
    }

    private long minPossibleHash(int numberOfNodes, int currentPosition) {
        long result = ((max_hashes / numberOfNodes) * (currentPosition - 1));

        return result;

    }

    private long maxPossibleHash(int numberOfNodes, int currentPosition) {
        long result = ((max_hashes / numberOfNodes) * currentPosition) - 1;

        return result;

    }

    @Override
    public boolean containsHash(String hash) {
        Long hashInt = new BigInteger(1, hash.getBytes()).longValue();
        System.out.println(MIN_SIZE);
        System.out.println(MAX_SIZE);
        return hashInt >= MIN_SIZE && hashInt <= MAX_SIZE;
    }

    @Override
    public boolean setIfPossible(String hash, T data) {
        if (!this.containsHash(hash))
            return false;

        this.storage.put(hash, data);
        return true;
    }

    @Override
    public void get(String hash) {
        System.out.println(this.storage.get(hash));
        return;

    }
}
