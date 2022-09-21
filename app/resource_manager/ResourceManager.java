package app.resource_manager;

import app.resource_manager.hash.IHashOperator;
import app.resource_manager.hash.Md5HashOperator;

public class ResourceManager {
    public String content;
	protected IHashOperator hashOperator = new Md5HashOperator();

    public ResourceManager(String content){
        this.content = content;
    }

    public String getContent(){
        return this.content;
    }

    public String getHash(){
        return hashOperator.run(this.content);
    }

    public boolean sameHash(String hash){
        return this.getHash().equals(hash);
    }
}
