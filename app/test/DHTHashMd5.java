package app.test;

import java.math.BigInteger;

public class DHTHashMd5 extends IDHTHash{
    
    static {
        max_hashes = (long) Math.pow(2, 128);
    }
    
    public DHTHashMd5(int numberOfNodes, int currentPosition){
        this.calculateInterval(numberOfNodes, currentPosition);
    }

    @Override
    public boolean containsHash(String hash) {
        Long hashInt = new BigInteger(1, hash.getBytes()).longValue();
        
        return hashInt >= MIN_SIZE && hashInt <= MAX_SIZE;
    }

    private void calculateInterval(int numberOfNodes, int currentPosition){
        
        this.MAX_SIZE = ((max_hashes / numberOfNodes) * currentPosition) - 1;
        this.MIN_SIZE = ((max_hashes / numberOfNodes) * (currentPosition - 1));

    }
    

    public String getInterval(){
        String result = "MIN INTERVAL -> " + this.MIN_SIZE;
        result += "\nMAX INTERVAL -> " + this.MAX_SIZE;

        return result;
    }
}
