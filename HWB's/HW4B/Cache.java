import java.util.*;

public class Cache {
    private int blockSize;            // 512 bytes
    private Vector<byte[]> pages;     // This is actual pages that include data
    private int victim;

    private class Entry {
	public static final int INVALID = -1;
	public boolean reference;
	public boolean dirty;
	public int frame;
	public Entry( ) {
	    reference = false;
	    dirty = false;
	    frame = INVALID;
	}
    }

    private Entry[] pageTable = null; // This is a page table that includes only attributes

    public Cache( int blockSize, int cacheBlocks ) { // cacheBlocks = 10
	this.blockSize = blockSize;                  // blockSize = 512
	pages = new Vector<byte[]>( );
	for ( int i = 0; i < cacheBlocks; i++ ) {
	    byte[] p = new byte[blockSize];
	    pages.addElement( p );
	}
	victim = cacheBlocks - 1; // set the last frame as a previous victim
	pageTable = new Entry[ cacheBlocks ];
	for ( int i = 0; i < cacheBlocks; i++ )
	    pageTable[i] = new Entry( );
    }

    private int findFreePage( ) {
	// Return a free index of pageTable[]. Look through pageTable[]
	for(int i = 0; i < pageTable.length; i++) {
	    //If the frame is invalid, or free, then return this index.
	    if(pageTable[i].frame == -1)
		return i;	
	}
	//After looking through the pages and if a free space is not found, return -1
	return -1;
    }

    private int nextVictim( ) {
	// Return a next victim index of pageTable[]. Look through the pageTable[]
	int cacheBlock = 10;
      	while(true) {
	    victim = (victim + 1) % cacheBlock;

	    //if pageTable[victim]'s reference is false, return this victim
	    if(pageTable[victim].reference == false) {
		return victim;
	    }
	    
	    pageTable[victim].reference = false;
	}
    }

    private void writeBack( int victimEntry ) {
	//If there is no space in the page table and it's filled...
	if(pageTable[victimEntry].frame != -1) {
	    SysLib.rawwrite(pageTable[victimEntry].frame, pages.elementAt(victimEntry));
	
	    // Make sure the victim's dirty is cleared.
	    pageTable[victimEntry].dirty = false;
	}	
    }

    public synchronized boolean read( int blockId, byte buffer[] ) {
	if ( blockId < 0 ) {
	    SysLib.cerr( "threadOS: a wrong blockId for cread\n" );
	    return false;
	}

	// locate a valid page and copy the hit page to buffer
	for(int i = 0; i < pageTable.length; i++) {

	    //If found, copy from pages.elementAt(i) to buffer[]
	    if(pageTable[i].frame == blockId) {

		//Initialize another buffer to store pages.elementAt(i)
		byte[] p = (byte[])pages.elementAt(i);
		System.arraycopy(p, 0, buffer, 0, blockSize);

		//Make the reference true.
		pageTable[i].reference = true;
	    
		//Return true because it has been read
		return true;
	    }
	}
	//If valid page isn't found... find an invalid page or a victim
	int free = findFreePage();

	//If there is no space, call nextVictim()
       	if(free == -1) {
	    free = nextVictim();
	}

	// write back a dirty copy.
	if(pageTable[free].dirty = true)
	    writeBack(free);

	// read a requested block from disk
	SysLib.rawread(blockId, buffer);
       
	//Cache it to pages[free]
	//Copy buffer to pages.elementAt(free)
	byte[] b = new byte[blockSize];
	System.arraycopy(buffer, 0, b, 0, blockSize);

	//Set the vector page of bytes to the free space index
	pages.set(free, b);
	pageTable[free].frame = blockId;
	pageTable[free].reference = true;
	return true;
    }

    public synchronized boolean write( int blockId, byte buffer[] ) {
	if ( blockId < 0 ) {
	    SysLib.cerr( "threadOS: a wrong blockId for cwrite\n" );
	    return false;
	}

	// locate a valid page and copy buffer to the hit page
        for(int i = 0; i < pageTable.length; i++) {

            if(pageTable[i].frame == blockId) {

		//Create a new byte[] to write to pages.elementAt(i)
		byte[] p = new byte[blockSize];
		System.arraycopy(buffer, 0, p, 0, blockSize);
	       
		//Set the pages.elementAt(i) to the newByte	
		pages.set(i, p);
		pageTable[i].reference = true;
		pageTable[i].dirty = true;
	    }
        }

	//If valid page isn't found... find an invalid page or a victim
	int free = findFreePage();

        //If a free space is not found, call nextVictim() 
        if(free == -1) {
            free = nextVictim();
	}

        // write back a dirty copy.
	if(pageTable[free].dirty == true)
	    writeBack(free);
        
	//Cache it to pages[free]
        //Copy buffer to pages.elementAt(free)
	byte[] b = new byte[blockSize];
        System.arraycopy(buffer, 0, b, 0, blockSize);
       
        //Set the vector pages to the free page index.
	pages.set(free, b);       
	pageTable[free].frame = blockId;
        pageTable[free].reference = true;
        pageTable[free].dirty = true;
	return true;
    }

    public synchronized void sync( ) {
	for ( int i = 0; i < pageTable.length; i++ )
	    writeBack( i );
	SysLib.sync( );
    }

    public synchronized void flush( ) {
	for ( int i = 0; i < pageTable.length; i++ ) {
	    writeBack( i );
	    pageTable[i].reference = false;
	    pageTable[i].frame = Entry.INVALID;
	}
	SysLib.sync( );
    }
}
