package server.test;
import java.security.NoSuchAlgorithmException;

public class Test {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        IDHTBucket<String> bucket_1 = new DHTBucket<String>(4,1);
        IDHTBucket<String> bucket_2 = new DHTBucket<String>(4,2);
        IDHTBucket<String> bucket_3 = new DHTBucket<String>(4,3);
        IDHTBucket<String> bucket_4 = new DHTBucket<String>(4,4);

        // String s = "test";
        // MessageDigest m = MessageDigest.getInstance("MD5");
        // m.update(s.getBytes(), 0, s.length());


        System.out.println(bucket_1);
        System.out.println(bucket_2);
        System.out.println(bucket_3);
        System.out.println(bucket_4);
        
    }
}
