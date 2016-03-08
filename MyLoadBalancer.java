import java.util.*;

public class MyLoadBalancer implements ILoadBalancer {
	public boolean canAcceptNewConnections(Map<String, Object> statusVars){
		try{

			// check some obvious params
			if(statusVars.containsKey("isOverloaded")){
				return true;
			}

			// check various params, use simple decision tree to decide
			if(statusVars.containsKey("numUsers")){
				int numUsers = (int) statusVars.get("numUsers");
				if(statusVars.containsKey("maxUsers")){
					int maxUsers = (int) statusVars.get("maxUsers");
					float currentLoadFactor = (float) numUsers / maxUsers;
					if(statusVars.containsKey("maxLoadFactor")){
						float maxLoadFactor = (float) statusVars.get("maxLoadFactor");
						return currentLoadFactor < maxLoadFactor;
					}else{
						return currentLoadFactor < .80f;
					}
				}

				// a little fallback...
				if(numUsers > 100){
					return false;
				}
			}
		}
		catch(Exception e){
			System.err.println("Error retrieving status vars");
			e.printStackTrace();
		}

		// default response
		return true;
	}
}