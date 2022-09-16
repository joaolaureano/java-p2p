package server.test;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Test {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        IDHTBucket<String> bucket = new DHTBucket<String>(4,1);

        String s = "test";
        
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(s.getBytes(), 0, s.length());

        System.out.println(bucket.containsHash(new BigInteger(1, m.digest()).toString(16)));
        
        // System.out.println(new BigInteger(1, m.digest()).longValue());
        // byte[] out = m.digest();
        // System.out.println(Base64.getEncoder().encode(out)[0]);

        
    }
}
