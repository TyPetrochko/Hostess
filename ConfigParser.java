/**
 ** Code adopted from:
 ** 
 ** Yale CS433/533 Demo Basic Web Server
 **/
import java.io.*;
import java.net.*;
import java.util.*;

class ConfigParser{

    public int port = -1;
    public int threadPoolSize = -1;
    public int cacheSize = -1;
    public float incompleteTimeout = -1.0f;
    public ILoadBalancer loadBalancer = null;
    public List<VirtualHost> virtualHosts;

    public ConfigParser(String configFile) throws Exception {
    	virtualHosts = new ArrayList<VirtualHost>();

    	// Parse input for port, thread pool, cache size
		try{
			Scanner sc = new Scanner(new File(configFile));
	    	while (sc.hasNextLine()) {
	        	String line = sc.nextLine();
	        	String [] words = line.split("\\s+");
	        	if (words.length < 2){
	        		continue; // Probably a blank line
	        	}else if(words[0].equalsIgnoreCase("Listen")){
	        		port = Integer.parseInt(words[1]);
	        	}else if(words[0].equalsIgnoreCase("ThreadPoolSize")){
	        		threadPoolSize = Integer.parseInt(words[1]);
	        	}else if (words[0].equalsIgnoreCase("CacheSize")){
	        		cacheSize = Integer.parseInt(words[1]);
	        	}else if (words[0].equalsIgnoreCase("IncompleteTimeout")) {
	        		incompleteTimeout = Float.parseFloat(words[1]);
	        	}else if (words[0].equalsIgnoreCase("LoadBalancer")){
	        		loadBalancer = getLoadBalancer(words[1].trim());
	        	}else if(line.contains("VirtualHost")){
	        		VirtualHost v = new VirtualHost();

	        		// Read through params for a new virtual host
	        		while(sc.hasNextLine()){
	        			line = sc.nextLine();
	        			words = line.split("\\s+");
	        			if(line.toLowerCase().contains("virtualhost")){
	        				break;
	        			}else if(line.contains("DocumentRoot")){
	        				v.documentRoot = words[words.length - 1];
	        			}else if (line.contains("ServerName")){
	        				v.serverName = words[words.length - 1];
	        			}
	        		}

	        		virtualHosts.add(v);
	        	}
	      	}
	    }catch(Exception e){
	    	throw e;
	    }
    }// end of constructor

    // globally accessible getter for the default load-balancer
    private ILoadBalancer getLoadBalancer(String classPath){
    	ILoadBalancer toReturn = null;
		try {
			// try to load it
	        Class<?> c = Class.forName(classPath);
	        if (ILoadBalancer.class.isAssignableFrom(c)) {
	        	Object o = c.newInstance();
			    toReturn = ILoadBalancer.class.cast(o);
			}
	    } catch (Exception e) {
	    	System.err.println("Couldn't load class from config file");
	        e.printStackTrace();
	    }

	    return toReturn;
    }
} // end of class WebServer
