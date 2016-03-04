import java.nio.channels.*;
import java.io.IOException;
import java.util.*; // for Set and Iterator

public class TimeoutThread extends Thread {
	static List <Deadline> deadlines;
	private Dispatcher dispatcher;

	public TimeoutThread(Dispatcher d){
		this.dispatcher = d;
		deadlines = new ArrayList<Deadline>();
	}

	public void run (){
		System.out.println("Timeout thread is running");
		while(true){
			long rightNow = System.currentTimeMillis();
			for(Deadline d : deadlines){
				if (d.deadline < rightNow){
					// timed out!
					try{
						d.sk.channel().close();
						d.sk.cancel();
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}
	}

	static void addDeadline(SelectionKey s, long when){
		System.out.println(s + " has until " + when + " to respond!");
		try{
			synchronized(deadlines){
				deadlines.add(new Deadline(s, when));
			}
		}catch (Exception e){
			System.out.println("Syncing error");
			e.printStackTrace();
		}
	}

	static void removeDeadline(SelectionKey s){
		try{
			synchronized(deadlines){
				for (Deadline d : deadlines){
					if (d.sk == s){
						deadlines.remove(d);
					}
				}
			}
		}catch (Exception e){
			System.out.println("Syncing error");
			e.printStackTrace();
		}
	}
}

class Deadline {
	public SelectionKey sk;
	public long deadline;

	public Deadline(SelectionKey sk, long deadline){
		this.sk = sk;
		this.deadline = deadline;
	}
}