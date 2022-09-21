package app.bucket;

import java.math.BigInteger;

public class DHTHashMd5 extends IDHTHash{
    
    static {
        max_hashes = new BigInteger("2").pow(128); 
    }
    
    public DHTHashMd5(int numberOfNodes, int currentPosition){
        this.calculateInterval(numberOfNodes, currentPosition);
    }

    @Override
    public boolean containsHash(String hash) {
        BigInteger hashInt = new BigInteger(hash, 16);
        boolean biggerEqualMinimum = MIN_SIZE.compareTo(hashInt) <= 0;
        boolean smallerEqualMaximum = MAX_SIZE.compareTo(hashInt) >= 0;
        return  biggerEqualMinimum && smallerEqualMaximum;
    }

    private void calculateInterval(int numberOfNodes, int currentPosition){

        this.MAX_SIZE = (max_hashes.divide(BigInteger.valueOf(numberOfNodes)).multiply(BigInteger.valueOf(currentPosition)).subtract(BigInteger.valueOf(1)));
        this.MIN_SIZE = (max_hashes.divide(BigInteger.valueOf(numberOfNodes)).multiply(BigInteger.valueOf(currentPosition - 1)));

    }
    

    public String getInterval(){
        String result = "MIN INTERVAL -> " + this.MIN_SIZE;
        result += "\nMAX INTERVAL -> " + this.MAX_SIZE;

        return result;
    }
}
