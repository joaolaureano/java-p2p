package content_mngnt;
import java.math.BigInteger;
import java.security.*;

public class ContentManagement {
	protected String content;
	
	ContentManagement(String content){
		this.content = content;
	}

	public String getContent(String hash) {
		if(hash.equals(this.getHash()))
		return content;
		else
			throw new Error("Content does not exists!");
	}
	
	public String getHash(){
		MessageDigest m;
		try {
			m = MessageDigest.getInstance("MD5");
			m.update(content.getBytes(),0,content.length());
			return new BigInteger(1,m.digest()).toString(5);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
