package app.command;

import app.bucket.IDHTBucket;

public class RegisterCommand implements ICommand<Boolean>{
    IDHTBucket<String> bucket;
    String hash;
    String data;

    public RegisterCommand(IDHTBucket<String> bucket, String hash, String data){
        
        this.bucket = bucket;
        this.hash = hash;
        this.data = data;

    }

    public Boolean run(){

        boolean result = this.bucket.setIfPossible(hash, data);
        System.out.println(this.bucket);
        return result;

    }
}
