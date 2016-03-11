/* 
** A simple file-cache singleton to remember recently-used files
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

class FileCache {
	private ConcurrentHashMap<String, FileWithTimestamp> cache;
    private long cacheSize;
    private long cacheCapacity;

    // singleton class
    public static FileCache globalCache;

    public FileCache(long capacity){

        // if no default cache, make this it
        if(globalCache == null){
        	cache = new ConcurrentHashMap<String, FileWithTimestamp>();
        	cacheCapacity = capacity;
            globalCache = this;
        }
    }

    // cache a file if space remaining
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

// wrapper class for a buffered file
class FileWithTimestamp{
	public FileWithTimestamp(byte[] file){
		this.file = file;
		this.cachedTime = System.currentTimeMillis();
	}
	public byte[] file;
	public long cachedTime;
}