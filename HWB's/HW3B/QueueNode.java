import java.util.*;
import java.lang.reflect.*;
import java.io.*;

public class QueueNode {
    //Initiate the vector of tid
    private Vector<Integer> tidQueue;

    public QueueNode() {
	//Initialize tidQueue
	tidQueue = new Vector<Integer>();
    }

    public synchronized int sleep() {
	//If there is nothing in the tidQueue or if there is no
	//child tid in the queue, 
	if(tidQueue.size() == 0) {
	    //Put this thread to sleep.

	    //Use Try/Catch to keep the compiler happy.
	    try {
		wait();
	    }
	    catch (InterruptedException e) {
	    }
	}
	//Then get the first element in the queue and remove it.
        int tid = tidQueue.remove(0);
	return tid;
    }

    public synchronized void wakeup(int tid) {
	//Add the tid into the queue.
	tidQueue.add(tid);
	//Wake up another sleeping thread in the monitor.
	notify();
    }
}
