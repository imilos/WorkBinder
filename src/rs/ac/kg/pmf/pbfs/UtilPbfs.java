package rs.ac.kg.pmf.pbfs;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.*;

import yu.ac.bg.rcub.binder.BinderUtil;

/**
 * Class of static methods used in PBFS Communicator implementation
 * in package yu.ac.kg.csk.mivanovic.pbfs
 * It contains methods intended for byte array transfer and transfer of 
 * complete files, as well as utility methods for compresion/decompression
 * of archives in ZIP format
 * 
 * @author Milos Ivanovic, CSANU Kragujevac, 2008.
 */
public class UtilPbfs {

	/**
	 * Reads byte array from DataInputStream using format { length, raw_data }
	 * 
	 * @param is
	 * @return byte array
	 * @throws IOException
	 */
	public static byte[] readByteArray(DataInputStream is) throws IOException {
        int len = is.readInt();
        byte[] ba = new byte[len];
        is.readFully(ba);
        return ba;
	}
	
	/** 
	 * Writes byte array to DataOutputStream using format { length, raw_data }
	 * 
	 * @param os
	 * @param ba
	 * @throws IOException
	 */
	public static void writeByteArray(DataOutputStream os, byte[] ba) throws IOException {
		os.writeInt( ba.length );
		os.write(ba);
	}

	/**
	 * ZIP archive containing files taken as arguments in String array.
	 * Mainly used by server process in order to pack large resulting files.
	 * 
	 * @param outFilename
	 * @param filenames
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void createZIP(String outFilename, String[] filenames)
	throws FileNotFoundException, IOException
	{
		byte[] buf = new byte[1024];
	    int len;
	    
	    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));
        for (String filename : filenames) {
            FileInputStream in = new FileInputStream(filename);	    
            out.putNextEntry(new ZipEntry(filename));
            
            while ((len = in.read(buf)) > 0)
                out.write(buf, 0, len);
	            
            out.closeEntry();
            in.close();
        }
        out.close();
	}
	
	/** 
	 * Method used to unpack ZIP archive, mainly used by client class
	 * 
	 * @param zipfilename
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void unZIP(String zipfilename)
	throws FileNotFoundException, IOException
	{
		BufferedOutputStream dest = null;
		FileInputStream fis = new FileInputStream(zipfilename);
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
		ZipEntry entry;
				
		while((entry = zis.getNextEntry()) != null) {
			System.out.println("Extracting: " + entry);
			int count;
			byte data[] = new byte[1024];
			FileOutputStream fos = new FileOutputStream(entry.getName());
			dest = new BufferedOutputStream(fos, 1024);
			
			while ((count = zis.read(data, 0, 1024)) != -1)
				dest.write(data, 0, count);
			
			dest.flush();
			dest.close();
		}
		zis.close();
	}
	
	/** 
	 * Method used to send file to "out" stream
	 * IMPORTANT: begins with "read" operation, ends with "write" operation
	 * 
	 * @param filename
	 * @param in
	 * @param out
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void sendFile(String filename, DataInputStream in, DataOutputStream out)
	throws FileNotFoundException, IOException
	{
		final int BUFFER_LENGTH=1024;
		
		FileInputStream fileStream = new FileInputStream(filename);
		byte[] buffer = new byte[BUFFER_LENGTH];
		int duzina, i;
		
		while ((duzina=fileStream.read(buffer)) > 0) {
			BinderUtil.readString(in);
			byte[] buffer1 = new byte[duzina];
			for (i=0; i<buffer1.length; i++) 
				buffer1[i] = buffer[i];
			writeByteArray(out, buffer1);
			// Null buffer
			for (i=0; i<BUFFER_LENGTH; i++) 
				buffer[i] = '\0';
        }
		
		fileStream.close();
		BinderUtil.readString(in);
		BinderUtil.writeString( out, "-finished-" );
	}

	/** 
	 * Method used to receive file from "in"
	 * IMPORTANT: begins with "write" operation, ends with "read" operation
	 * 
	 * @param filename
	 * @param in
	 * @param out
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void receiveFile(String filename, DataInputStream in, DataOutputStream out)
	throws FileNotFoundException, IOException
	{
		
		String line = "";
		FileOutputStream fileStream = new FileOutputStream(filename);			
		while ( !line.equalsIgnoreCase("-finished-")  ) {
			BinderUtil.writeString(out, "-continue-");
			byte[] buffer = readByteArray(in);
			line = new String(buffer);
			if ( !line.equalsIgnoreCase("-finished-") ) 
				fileStream.write(buffer);
		}
		fileStream.close();		
	}
}
