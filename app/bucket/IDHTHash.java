package app.bucket;

import java.math.BigInteger;

public abstract class IDHTHash {
    
    BigInteger MAX_SIZE;
    BigInteger MIN_SIZE;
    static BigInteger max_hashes;

    public abstract boolean containsHash(String hash);

    // USED ONLY FOR DEVELOPMENT
    
    public abstract String getInterval();
}