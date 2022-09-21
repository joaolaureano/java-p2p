package app.resource_manager.hash;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5HashOperator implements IHashOperator {

    @Override
    public String run(String content) {
        MessageDigest m;
        try {
            m = MessageDigest.getInstance("MD5");

            m.update(content.getBytes());
            byte[] resultBytes = m.digest();
            String result = new BigInteger(1, resultBytes).toString(16);
            
            while(result.length() < 32 ){
                result = "0"+result;
            }

            System.out.println("HASH IS " + result);
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new Error("bad hash operator");
        }
    }

}