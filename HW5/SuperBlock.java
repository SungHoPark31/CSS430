import java.util.*;

public class SuperBlock 
{
    private final int defaultInodeBlocks = 64;
    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList;    // the block number of the free list's head

    public SuperBlock(int diskSize) {
	//Read the superblock from disk 
	byte[] superBlock = new byte[Disk.blockSize];

	//Read from the disk
	SysLib.rawread(0, superBlock);

	//Initialize the variables
	totalBlocks = SysLib.bytes2int(superBlock, 0);
	totalInodes = SysLib.bytes2int(superBlock, 4);
	freeList = SysLib.bytes2int(superBlock, 8);

	//Check if the variables are valid and if they are not, format it.
	if(totalBlocks == diskSize && totalInodes > 0 && freeList >= 2)
	    return;
	else { 
	    totalBlocks = diskSize;
	    SysLib.cerr("Formatting");
	    format(defaultInodeBlocks);
	}
    }

    public void sync() {
	//Create a byte array to store the variables after it is converted
	//back from int to bytes.
	byte[] block = new byte[Disk.blockSize];
	SysLib.int2bytes(totalBlocks, block, 0);
	SysLib.int2bytes(totalInodes, block, 4);
	SysLib.int2bytes(freeList, block, 8);
	
	//Write it to the disk.
	SysLib.rawwrite(0, block);
    }

    public int getFreeBlock() {
	//Save a temporary head
	int temp = freeList;

	//Check if it's valid
	if(temp > 0) {
	    //Read the first free block
	    if(freeList < totalBlocks){
		byte[] block = new byte[Disk.blockSize];
		SysLib.rawread(freeList, block);
		
		//Move head index to the next space
		freeList = SysLib.bytes2int(block, 0);
		SysLib.int2bytes(0, block, 0);
		SysLib.rawwrite(temp, block);
	    }
	}
        return temp;
    }

    public void returnBlock (int blockNumber) {
	//Check if the block number is valid
	if(blockNumber < 0)
	    return; 
	else {
	    //Make a temporary byte array
	    byte[] data = new byte[Disk.blockSize];

	    //convert free list into bytes and put it in data. 
	    SysLib.int2bytes(freeList, data, 0);

	    //Write to the disk.
	    SysLib.rawwrite(blockNumber, data);

	    //Set block number to freelist.
	    freeList = blockNumber;
	}
	return;
    }

    public void format(int size) {
      	// Format inodes
        totalInodes = size;
        for (short i = 0; i < totalInodes; ++i) {
	    // Reset inode in disk
	    Inode tempINode = new Inode();
	    tempINode.flag = 0;
	    tempINode.toDisk(i);
	}

        // Format free blocks
	freeList = 2 + (totalInodes / 16);
        for (int i = freeList; i < totalBlocks; i++) {
	    // Reset all data in the block
	    byte[] info = new byte[Disk.blockSize];
      	    
	    // Write pointer to next block
	    SysLib.int2bytes(freeList + 1, info, 0);
	    
	    // Finish formatting
	    SysLib.rawwrite(freeList, info);
	}
	//Update Super block
        sync();
    }
}
