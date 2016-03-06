import java.nio.channels.*;
import java.io.IOException;
import java.util.*; // for Set and Iterator
import java.util.concurrent.*;

public class Timeout {
	private static ExecutorService pool;

	public Timeout(long time){
		pool = Executors.newFixedThreadPool(1000);
	}

	public static void addDeadline(SelectionKey key, long time){
		pool.submit(() -> {
			try{
				Debug.DEBUG(key + " is going to sleep for " + time + " ms..");
				Thread.sleep(time);
				Debug.DEBUG(key + " woke up!");
				if(key.isValid()){
					Debug.DEBUG(key + " timed out!");
					key.cancel();
					key.channel().close();
				}
			}catch (Exception e){
				Debug.DEBUG("Chances ");
			}
		});
	}
}