package app.command;

import app.bucket.BucketResource;
import app.bucket.IDHTBucket;

public class ListResourseCommand implements ICommand<String> {
    IDHTBucket<BucketResource> resouseList;

    public ListResourseCommand(IDHTBucket<BucketResource> resouseList){
        this.resouseList = resouseList;
    }

    public String run(){
        String content = "";
        for(BucketResource name : resouseList.toList())
            content += name.getHash() + "-" + name.getPeerPort() + ";";
        return content;
    }
}