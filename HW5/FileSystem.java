import java.util.*;

public class FileSystem {
    
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    public FileSystem(int diskBlocks) {
        superblock = new SuperBlock(diskBlocks);
        directory = new Directory(superblock.totalInodes);
        filetable = new FileTable(directory);

        // read the "/" file from disk
        FileTableEntry dirEnt = open("/", "r");
        int dirSize = fsize(dirEnt);
        if (dirSize > 0){
	    // the directroy has some data
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            directory.bytes2directory(dirData);
        }
        close(dirEnt);
    }

    public FileTableEntry open(String filename, String mode) {
        FileTableEntry ftEnt = filetable.falloc(filename, mode);
        if (mode == "w"){
	    if(!deallocAllBlocks(ftEnt)){
		return null;
	    }
	}
        return ftEnt ;
    }

    public int read(FileTableEntry ftEnt, byte[] buffer) {
        // Validate that we're not in "w" or "a" modes
        if (buffer == null || ftEnt.mode.equals("w") || ftEnt.mode.equals("a")){
	    return -1; // error
	}
	int size = buffer.length;
	int trackRead = 0;
	int sizeLeft = 0;	
        synchronized (ftEnt) {
            // true as long as we have bytes left to read
            while (ftEnt.seekPtr < fsize(ftEnt) && size > 0) {
                // Get the current block
                int blockNum = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
		
		if (blockNum == -1) 
		    return -1;
		else if(blockNum < 0 || blockNum > superblock.totalBlocks)
		    break;

		byte[] tempBlock = new byte[Disk.blockSize]; // Create a temporary block to use it for copying		    
		//Read the contents of the current block into the temp block
		SysLib.rawread(blockNum, tempBlock);
		
		int offset = ftEnt.seekPtr % Disk.blockSize; // Get the current pointer offset (zero means new block)
		int remainingBlocks = Disk.blockSize - offset; // Find the bytes available in the block
		int remaining = fsize(ftEnt) - ftEnt.seekPtr; // Find the remaining bytes in the file table
		
		int smallerBlockData = Math.min(remainingBlocks, size);
		sizeLeft = Math.min(smallerBlockData, remaining);
		// finding out how many bytes remaining that need to be read.  
		System.arraycopy(tempBlock, offset, buffer, trackRead, sizeLeft);
		
		// Update bytes read so far
		size -= sizeLeft;
		// Update seek pointer
		ftEnt.seekPtr += sizeLeft;
		// Update bytes left to read
		trackRead += sizeLeft;
            }
	    seek(ftEnt, trackRead, 1);
        }
	return trackRead;
    }

    public int write(FileTableEntry ftEnt, byte[] buffer) {	
        // Validate that we are in write mode                                                           
        if (ftEnt.mode.equals( "r") || ftEnt == null || buffer == null){
            return -1;
        }
	int readBytes = 0; // already being read                                                         
	int bufferLength = buffer.length;
	int seekPtr = 0;

	synchronized (ftEnt) {
            // Continue writing when we still have length in the buffer                   
	    // true as long as we have bytes left to read  
	    if (ftEnt.mode.equals("a")) {
		seekPtr = seek(ftEnt, 0, 2);
	    } else {
		seekPtr = ftEnt.seekPtr;
	    }
            while (bufferLength > 0) {
                int currBlock = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
                if (currBlock == -1) {// invalid block                                                 
                    // Get the next free block   
                    short availableFreeblock = (short) superblock.getFreeBlock();
                    // Update the block with the inode                                              
                    int result = ftEnt.inode.registerTargetBlock(ftEnt.seekPtr, availableFreeblock);

                    // Depending on the return value, we will have different ways to handle
                    // When result = -3 , it means that the indirect block is unavaible albe            
		    // So we need to make use of it
                    if (result == -3) {
                        // Indirect is unassigned, attempt to assign it                                
                        short nextFreeBlock = (short) superblock.getFreeBlock();
                        if (!ftEnt.inode.registerIndexBlock(nextFreeBlock)){
			    return -1; //error    
                        }
                        //Attempt to update it                                                        
                        if (ftEnt.inode.registerTargetBlock(ftEnt.seekPtr, availableFreeblock) != 0) {
			    return -1; //error                                                           
                        }
                    }                  
                    else if (result == -1 || result == -2) {
			return -1;
                    }
		    currBlock = availableFreeblock;
                }

		if (currBlock >= superblock.totalBlocks) {
		    ftEnt.inode.flag = 4;
		    break;
		}

                // Create a temporary block to copy into 
                byte[] tempData = new byte[Disk.blockSize];

                // Read the contents of the current block into the temp block
		SysLib.rawread(currBlock, tempData);
                // Get the current pointer offset (zero means new block)                                        
                int offset = ftEnt.seekPtr % Disk.blockSize;

                int remaining = Disk.blockSize - offset;
                int smaller = Math.min(remaining, bufferLength);

                System.arraycopy(buffer, readBytes, tempData, offset, smaller);
		SysLib.rawwrite(currBlock, tempData);
                // Update the seek pointer poingting to the next location               
                ftEnt.seekPtr += smaller;
                readBytes += smaller;
                // Decrement the size meaning that we have used this much space in writing                       
                bufferLength -= smaller;
                // If we reached the end of the inode                                                            
                if (ftEnt.seekPtr > ftEnt.inode.length){
                    // Set the pointer to the end of the inode            
                    ftEnt.inode.length = ftEnt.seekPtr;
                }
            }
	    // if the file size increased, update Inode
	    if (seekPtr > ftEnt.inode.length) {
		ftEnt.inode.length = seekPtr;
	    }

	    // set new seekPtr
	    seek(ftEnt, readBytes, 1);

	    // set flag
	    if (ftEnt.inode.flag != 4) {
		ftEnt.inode.flag = 1;
	    }
            // Save the inode to disk                                                           
            ftEnt.inode.toDisk(ftEnt.iNumber);  
        }
	return readBytes;
    }

    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        // check if inode is being used
        if (ftEnt.inode.count < 0 || ftEnt == null) {
            return false;
        }

        // Deallocate the indirect blocks,
        byte[] data;
	int indirectData = ftEnt.inode.indirect;

	if (indirectData != -1) {
            data = new byte[Disk.blockSize];
            SysLib.rawread(indirectData, data );
            ftEnt.inode.indirect = -1;
          
        } else {
            data = null;
	}
        
        // First check if there was data
        if (data != null)
        {
            byte offset = 0;
            // Get all the block that is pointed to by the indirect block
            short blockID = SysLib.bytes2short(data, offset);
            // And make it free. 
            while (blockID != -1)
            {
                superblock.returnBlock(blockID);
                blockID = SysLib.bytes2short(data, offset);
            }
        }
	for (int i = 0; i < 11; i++) {
            if (ftEnt.inode.direct[i] != -1) {
                // deallocate direct blocks if the block hasn't been deallocated yet     
                this.superblock.returnBlock(ftEnt.inode.direct[i]);
                // set the block to deallocated                                          
                ftEnt.inode.direct[i] = -1;
            }
        }

        // Write the inode to disk
        ftEnt.inode.toDisk(ftEnt.iNumber);
        return true;
    }

    public void sync()
    {
        FileTableEntry tempEntry = open("/", "w");
        // Get all the information from the directory first, including all files name and size
        byte[] temp = directory.directory2bytes();
        // Open the Table Entry that correspond to the directory

        // Write back to the disk all the info from the directory
        write(tempEntry, temp);
        close(tempEntry);
        // Write back to the disk all the info
        superblock.sync();
    }

    boolean close(FileTableEntry ftEnt){
        if(ftEnt == null)
	    return false;

	synchronized (ftEnt)
        {
            ftEnt.count--; // decrement since the thread's no longer using it
            if (ftEnt.count <= 0){ // not being used
                return filetable.ffree(ftEnt);
            }
            return true;
        }
    }

    public boolean delete(String fn) {
        FileTableEntry ftEnt = open(fn, "w");
	short iNum = ftEnt.iNumber;
	if(ftEnt == null)
	    return false;
	// only if closed and deallocate the inode number so it will, fails otherwise
	return close(ftEnt) && directory.ifree(iNum);
    }

    public int seek(FileTableEntry ftEnt, int offset, int whence){
        if (ftEnt == null){
	    return -1;
	}
	// Invalid mode to set the pointer, can only be 1,2,3
        if (whence != 0 && whence != 1 && whence != 2) {
	    return -1;
	}

        synchronized (ftEnt){
            if (whence == SEEK_SET){
		// The file's seek pointer is set to offset bytes from the 
                // beginning of the file.
                if (offset <= fsize(ftEnt) && offset >= 0){
                    ftEnt.seekPtr = offset;
                }
            } else if (whence == SEEK_CUR){
                // The file's seek pointer is set to its current value plus 
                // the offset. The offset can be positive or negative.
                if (ftEnt.seekPtr + offset <= fsize(ftEnt) && ((ftEnt.seekPtr + offset) >= 0)){
                    ftEnt.seekPtr += offset;
                }
            } else if (whence == SEEK_END){
		// The file's seek pointer is set to the size of the file 
                // plus the offset. The offset can be positive or negative.
                if (fsize(ftEnt) + offset >= 0 && fsize(ftEnt) + offset <= fsize(ftEnt)){
                    ftEnt.seekPtr = fsize(ftEnt) + offset;
                } else {
                    return -1; //error
                }
            }
            // return the updated seek pointer
            return ftEnt.seekPtr;
        }
    }

    public boolean format(int files){
        if (files > 0){
            // Call the superblock's format
            this.superblock.format(files);
	    directory = new Directory(superblock.totalInodes);
            filetable = new FileTable(directory);
            return true;
        }
        return false;//error
    }

    public int fsize(FileTableEntry ftEnt){
        if (ftEnt == null){
            return -1;
        }
        synchronized (ftEnt){
            return ftEnt.inode.length;
        }
    }
}
