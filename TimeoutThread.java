import java.nio.channels.*;
import java.io.IOException;
import java.util.*; // for Set and Iterator
import java.util.concurrent.*;

public class TimeoutThread extends Thread {
	static ConcurrentHashMap <SelectionKey, Long> deadlines;
	private Dispatcher dispatcher;

	public TimeoutThread(Dispatcher d){
		this.dispatcher = d;
		deadlines = new ConcurrentHashMap<SelectionKey, Long>();
	}

	public void run (){
		System.out.println("Timeout thread is running");

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
				Long when = (Long) pair.getValue();
				SelectionKey key = (SelectionKey) pair.getKey();

				if(when.longValue() < rightNow){
					// thread timed out
					if (key.isValid()){
						// abort this key
						System.out.println("Thread timed out!");
						deadlines.remove(key);
						key.cancel();
						try{
							key.channel().close();
						}catch (Exception e){
							System.out.println("Couldn't disconnect client");
							e.printStackTrace();
						}
					}else{
						System.out.println("Cleaning up key");
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