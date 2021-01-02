import java.util.*;

public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers
    public int length;                          // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; //direct pointers
    public short indirect;                         // a indirect pointer

    Inode( ) {                                     // a default constructor
	length = 0;
	count = 0;
	flag = 1;
	for ( int i = 0; i < directSize; i++ )
	    direct[i] = -1;
	indirect = -1;
    }

    Inode( short iNumber ) {                       // retrieving inode from disk
	// design it by yourself.

	//16 nodes in a block 
	int blockNumber = 1 + iNumber / 16;

	//Read the disk    
	byte[] data = new byte[Disk.blockSize];
	SysLib.rawread(blockNumber, data);
	
	//head of the nodes position
	int offset = (iNumber % 16) * 32;

	//Load the variables.
	length = SysLib.bytes2int(data, offset);
	offset += 4;
	count = SysLib.bytes2short(data, offset);
	offset += 2;
	flag = SysLib.bytes2short(data, offset);
	offset += 2;

	//Load the direct pointers. 
	for(int i = 0; i < directSize; i++, offset += 2)
	    direct[i] = SysLib.bytes2short(data, offset);

	//Load the indirect pointer
	indirect = SysLib.bytes2short(data, offset);
    }
    
    public void toDisk( short iNumber ) {  // save to disk as the i-th inode
	//Verify the parameter
	if(iNumber < 0)
	    return;

	int block = iNumber / 16 + 1; 

	//Array for the data read values in the disk.
	byte[] inodeD = new byte[Disk.blockSize];
	SysLib.rawread(block, inodeD);

	//Head of the nodes position
	int offset = (iNumber % 16) * 32;

	//Put the variables inside of inodeD
	SysLib.int2bytes(length, inodeD, offset);
	offset += 4;
	SysLib.short2bytes(count, inodeD, offset);
	offset += 2;
	SysLib.short2bytes(flag, inodeD, offset);
	offset += 2;

	//For loop for the writing to the disk for direct variable
	for(int i = 0; i < directSize; i++, offset += 2)
	    SysLib.short2bytes(direct[i], inodeD, offset);

	//Indirect...
	SysLib.short2bytes(indirect, inodeD, offset);

	//Write the inodeD array into the disk.
	SysLib.rawwrite(block, inodeD);
    }

    public short getIndexBlockNumber() {
	//return indirect
	return indirect;
    }

    public boolean setIndexblock(short indexBlockNumber) {
        if (indexBlockNumber <= 0) {
            return false;
        } else {
            indirect = indexBlockNumber;
            return true;
	}
    }

    public short findTargetBlock(int offset) {
	int blockNum = offset / Disk.blockSize;

	//If the blockNum is still in the direct block of iNode
	if(blockNum < directSize)
	    return direct[blockNum];
	else if(indirect < 0)
	    return -1;
	else { 
	    byte[] tempData = new byte[Disk.blockSize];
	    //Get the number of blocks that the indirect block is pointing to 
	    SysLib.rawread(indirect, tempData);
	    int difference = blockNum - directSize;
	    //The difference is in int, transfer it
	    return SysLib.bytes2short(tempData, (difference * 2));
	}
    }

    public boolean registerIndexBlock(short indexBlockNumber) {
	for (int i = 0; i < 11; i++){
	    if (direct[i] == -1){
		return false;
	    }
	}
	
        if (indirect != -1){
	    return false;
	}
	
        indirect = indexBlockNumber;
        byte[ ] data = new byte[Disk.blockSize];
	
        for(int i = 0; i < (Disk.blockSize/2); i++){
	    SysLib.short2bytes((short) -1, data, i * 2);
	}
        SysLib.rawwrite(indexBlockNumber, data);
        return true;
    }
    

    public int registerTargetBlock(int entry, short offset){
        //How far are we into the block?
	int target = entry/Disk.blockSize;
        if (target < directSize) {
	    if(direct[target] >= 0){
		//There is already a block here.
		return -1;
	    }
	    else if ((target > 0 ) && (direct[target - 1 ] == -1)) {
		//The previous position is invalid
		return -2;
	    }
	    else {
		//Set the block number to the current location in the direct pointer.
		direct[target] = offset;
		return 0;
	    }
	}
        else if(indirect < 0){
	    //Indirect is not available
	    return -3;
	}
        else {
	    //We have an indirect location
            byte[] data = new byte[Disk.blockSize];
	    //read the data of the block in the indirect pointer
            SysLib.rawread(indirect,data);

            int blockSpace = (target - 11) * 2;
            if ( SysLib.bytes2short(data, blockSpace) > 0) {
		return -1;
	    }
            else {
		SysLib.short2bytes(offset, data, blockSpace);
		SysLib.rawwrite(indirect, data);
		return 0;
	    }
        }
    }
}
