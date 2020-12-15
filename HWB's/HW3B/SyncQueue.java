import java.util.*;
import java.lang.reflect.*;
import java.io.*;

public class SyncQueue {
    //Have a queue array of QueueNode objects.
    private QueueNode queue[];

    public SyncQueue() {
	//Initialize queue[0] ~ queue[9]
        helperConstructor(10);
    }

    public SyncQueue(int condMax) {
	//Initialize queue[0] ~ queue[condMax - 1]
	helperConstructor(condMax);
    }

    public void helperConstructor(int number)
    {
	//Use helper function
	queue = new QueueNode[number];
        for(int i = 0; i < number; i++) {
            queue[i] = new QueueNode();
        }
    }

    public int enqueueAndSleep(int condition) {
	//Check the validity of condition.
	if(condition >= 0 && condition < queue.length) {
	    //Sleep method will enqueue and put this thread to sleep.  	
	    return queue[condition].sleep();
	}
	else {
	    //Return -1 if invalid 
	    return -1;
	}
    }

    public void dequeueAndWakeup(int condition, int tid) {
	//Check the validity of condition.
        if(condition >= 0 && condition < queue.length) {
            //wakeup method will dequeue the condition
	    queue[condition].wakeup(tid);
	}
    }

    public void dequeueAndWakeup(int condition) {
	dequeueAndWakeup(condition, 0);
    }
}
