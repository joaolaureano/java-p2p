package app.bucket;

public abstract class IDHTHash {
    
    Long MAX_SIZE;
    Long MIN_SIZE;
    static Long max_hashes;

    public abstract boolean containsHash(String hash);

    // USED ONLY FOR DEVELOPMENT
    
    public abstract String getInterval();
}