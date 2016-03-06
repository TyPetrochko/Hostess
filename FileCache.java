import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.*;

class FileCache {
	private ConcurrentHashMap<String, FileWithTimestamp> cache;
    private long cacheSize;
    private long cacheCapacity;

    public FileCache(long capacity){
    	cache = new ConcurrentHashMap<String, FileWithTimestamp>();
    	cacheCapacity = capacity;
    }

    public boolean cacheIfPossible(String fileName, 
    	byte[] bytes){

        if(hasFile(fileName)){
            return false;
        }else if (bytes.length <= cacheCapacity - cacheSize){
    		cache.put(fileName, new FileWithTimestamp(bytes));
    		return true;
    	}else{
    		return false;
    	}
    }

    public boolean hasFile(String fileName){
    	return cache.containsKey(fileName);
    }

    public byte[] getFile(String fileName){
    	return cache.get(fileName).file;
    }

    public long cachedTimeMillis(String fileName){
        return cache.get(fileName).cachedTime;
    }
}

class FileWithTimestamp{
	public FileWithTimestamp(byte[] file){
		this.file = file;
		this.cachedTime = System.currentTimeMillis();
	}
	public byte[] file;
	public long cachedTime;
}