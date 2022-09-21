package app.command;

import app.bucket.IDHTBucket;
import app.bucket.BucketResource;

public class RegisterCommand implements ICommand<Boolean>{
    IDHTBucket<BucketResource> bucket;
    BucketResource bResource;

    public RegisterCommand(IDHTBucket<BucketResource> bucket, BucketResource bResource){
        
        this.bucket = bucket;
        this.bResource = bResource;

    }

    public Boolean run(){

        boolean result = this.bucket.setIfPossible(bResource.getHash(), bResource);
        return result;

    }
}
