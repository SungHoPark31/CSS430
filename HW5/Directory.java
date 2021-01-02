import java.util.*;

public class Directory {
    private static int maxChars = 30; // max characters of each file name
 
    // Directory entries
    private int fsize[];        // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.
    
    public Directory( int maxInumber ) { 
	// directory constructor
	fsize = new int[maxInumber];     // maxInumber = max files
	for ( int i = 0; i < maxInumber; i++ ) 
	    fsize[i] = 0;                 // all file size initialized to 0
	fnames = new char[maxInumber][maxChars];
	String root = "/";                // entry(inode) 0 is "/"
	fsize[0] = root.length( );        // fsize[0] is the size of "/".
	root.getChars( 0, fsize[0], fnames[0], 0 ); // fnames[0] includes "/"
    }
    
    public void bytes2directory( byte data[] ) {
	//Check if the data is valid. If not, return -1.
	if(data == null || data.length == 0) {
	    return;
	}
	//Data being passed in is a byte and from the disk. To put it
	//inside of fsize, you have to change data to int starting from the 
	//offset.
	int offset = 0;
	for(int i = 0; i < fsize.length; i ++, offset += 4){ 
	    fsize[i] = SysLib.bytes2int(data, offset);
	}
	
	//Go through the fname element.
	for (int i = 0; i < fnames.length; i++, offset += maxChars * 2) {
	    //Put data into a string, break that string into char and put
	    //it inside of fname.
	    String fname = new String(data, offset, maxChars * 2);
	    fname.getChars(0, fsize[i], fnames[i], 0);
	}
    }
    
    public byte[] directory2bytes( ) {
	// converts and return Directory information into a plain byte array
	// this byte array will be written back to disk
	// note: only meaningfull directory information should be converted
	// into bytes.
	byte[] dirbyte = new byte[(fsize.length * maxChars * 2) + (fsize.length * 4)];
	int offset = 0;
	
	//For each element, change it to bytes
	for(int i = 0; i < fsize.length; i++, offset += 4)
	    SysLib.int2bytes(fsize[i], dirbyte, offset);
	
	for(int i = 0; i < fnames.length; i++, offset += maxChars * 2) {
	    //Change the fnames element to a string
	    String nameString = new String(fnames[i]);
	    
	    //Put the string you just created into bytes.
	    byte[] b = nameString.getBytes(); 
	    
	    //Copy over the byte of nameString to dirbyte
	    System.arraycopy(b, 0, dirbyte, offset, b.length);
	}
	return dirbyte;
    }
    
    public short ialloc( String filename ) {
	//Look through the fsize array.
	for(int i = 0; i < fsize.length; i++) {
	    
	    //Find an empty spot
	    if(fsize[i] == 0) {
		//Because maxChar is the maximum length that you can put inside
		//the fname, get the minimum of those two to put it in.
		fsize[i] = Math.min(filename.length(), maxChars);
		
		//Take the filename string and break into char and put it 
		//inside fnames.
		filename.getChars(0, fsize[i], fnames[i], 0);
		return (short)i;
	    }
	}
	//If you cannot add it anywhere, return -1.
	return -1;
    }
    
    public boolean ifree( short iNumber ) {	
	//Check if iNumber is valid
	if(iNumber <= 0 || iNumber >= fsize.length)
	    return false;
	else {
	    for(int i = 0; i < maxChars; i++)
		fnames[iNumber][i] = 0;

	    //Set the iNumber index on fsize to 0, indicating that it's empty
	    fsize[iNumber] = 0;
	 	    
	    //It is successfully removed. 	    
	    return true;
	}
    }
    
    public short namei( String filename ) {
	// returns the inumber corresponding to this filename
	//Go through the fsize array 
        for (short i = 0; i < fsize.length; i++) {
	    //Create a temporary string to compare filename and fnames[i]
	    //fnames[i] being the string, 0 being the offset, fsize[i] is length.	
	    String temp = new String(fnames[i], 0, fsize[i]);
	    
	    //If the filename and temp is the same then return the index.
	    if (fsize[i] > 0 && filename.equals(temp)) {
		return i;
	    }
	}
	//If nothing was found.
	return -1;
    }
}
