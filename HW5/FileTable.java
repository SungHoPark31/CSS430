import java.util.*;

public class FileTable {

    //Variables to check inode.flag
    public final static int UNUSED = 0; //unused 
    public final static int USED = 1; //If flag is used
    public final static int READ = 2; //If it is read
    public final static int WRITE = 3; //If you need to write
    public final static int DELETE = 4; //If needed to deleted.

    private Vector table;         // the actual entity of this file table
    private Directory dir;        // the root directory 

    public FileTable( Directory directory) { // constructor
	table = new Vector( );     // instantiate a file (structure) table
	dir = directory;           // receive a reference to the Director
    }                             // from the file system

    // major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ) {
	// allocate a new file (structure) table entry for this file name
	// allocate/retrieve and register the corresponding inode using dir
	// increment this inode's count
	// immediately write back this inode to the disk
	// return a reference to this file (structure) table entry
	short iNumber = -1;
	Inode inode = null;

	//Whlie directory is root or the file exists...
	while(true) {
	    iNumber = (filename.equals("/") ? (short)0 : dir.namei(filename));
	    //If the file exists...
	    if(iNumber >= 0) {
		inode = new Inode(iNumber);
		
		//Check if the flag is read or being used/unused.
		if(mode.compareTo("r") == 1) {
		    if(inode.flag == READ || inode.flag == USED || inode.flag == UNUSED) {
			//Set flag to read.
			inode.flag = READ;
			break;
		    }
		    else if(inode.flag == WRITE) {
			//Wait until writing is done.
			try{
			    wait();
			} catch (InterruptedException e){ }
		    }
		    else if(inode.flag == DELETE) {
			//delete then set to null
			iNumber = -1;
			return null;
		    }
		}
		else { // If the file wnats to write, w+, or append: "w" "w+" "a"
		    //Check if the flag being used/unused.  
		    if(inode.flag == USED || inode.flag == UNUSED) {
			//If not read, then write it. 
			inode.flag = WRITE;
			break;
		    }
		    else {
			//If the flag is read or write, wait  until writing is done.  
			try{
			    wait();
			} catch(InterruptedException e) {}
		    }
		}
	    }
	    else {
		//iNumber is negative so create it.
		iNumber = dir.ialloc(filename);
		inode = new Inode();
		break;
	    }
	}
	
	//Increase inode count
	inode.count++;
	inode.toDisk(iNumber);
	
	//Create a new file table entry and add it to the file table
	FileTableEntry e = new FileTableEntry(inode, iNumber, mode);
	table.addElement(e); 
	return e;
    }
    
    public synchronized boolean ffree( FileTableEntry e ) {
	// receive a file table entry reference
	// save the corresponding inode to the disk
	// free this file table entry.
	// return true if this file table entry found in my table
	if(table.remove(e)) {
	    //Decrement count...	
	    e.inode.count--;
	    
	    //Set flag to 0 for unused
	    e.inode.flag = 0;
	    
	    //Write to disk
	    e.inode.toDisk(e.iNumber);
	    
	    //Set e to null
	    e = null;
	    
	    //Wake up
	    notify();
	    return true;
	}
	return false;
    }

    public synchronized boolean fempty( ) {
	return table.isEmpty( );  // return if table is empty 
    }                            // should be called before starting a format
}
