/*
** A simple timeout thread to periodically
** remove clients that have timed out (for 
** async server)
*/

import java.nio.channels.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class TimeoutThread extends Thread {

	// track all deadlines
	static ConcurrentHashMap <SelectionKey, Long> deadlines;
	private Dispatcher dispatcher;

	public TimeoutThread(Dispatcher d){
		this.dispatcher = d;
		deadlines = new ConcurrentHashMap<SelectionKey, Long>();
	}

	public void run (){
		Debug.DEBUG("Timeout thread is running");

		// remove stale/timed-out connections periodically
		while(true){
			try{
				Thread.sleep(500);
			} catch (Exception e){
				System.out.println("Couldn't fall asleep");
				e.printStackTrace();
			}

			// remove any deadlines that occured before "rightNow"
			long rightNow = System.currentTimeMillis();
			for(Map.Entry<SelectionKey, Long> pair : deadlines.entrySet()){

				// when does this deadline extend?
				Long when = (Long) pair.getValue();
				SelectionKey key = (SelectionKey) pair.getKey();

				// check if thread should timeout
				if(when.longValue() < rightNow){

					// timeout; check if it's still a valid key
					if (key.isValid()){
						// abort this key
						Debug.DEBUG("Thread timed out!");
						deadlines.remove(key);
						key.cancel();
						try{
							key.channel().close();
						}catch (Exception e){
							System.err.println("Couldn't disconnect client");
							e.printStackTrace();
						}
					}else{
						// key is no longer valid, remove it
						Debug.DEBUG("Cleaning up key");
						deadlines.remove(key);
					}
				}
			}
		}
	}

	// add a deadline to be monitored
	static void addDeadline(SelectionKey s, long when){
		Debug.DEBUG(s.toString() + " has until " + when + " to respond!");
		deadlines.put(s, new Long(when));
	}

	// remove a deadline (he responded)
	static void removeDeadline(SelectionKey s){
		Debug.DEBUG(s.toString() + " has responded; remove from timeout pool");
		deadlines.remove(s);
	}
}