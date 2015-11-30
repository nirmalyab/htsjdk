package nirmalya;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;

import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedOutputStream;

public class RandomBamSampler {
	
	String bamFile = null;
	String poiFile = null;
	String outFile = null;
	BlockCompressedInputStream bcis = null;
	RandomAccessFile poiReader = null;
	BlockCompressedOutputStream bcosWriter = null;
	long poiFileLen = 0;
	
	public final int MAXLEN = 65536;
	byte[] inBuffer = new byte[MAXLEN];
	
	ArrayList<Long> posList = new ArrayList<Long>();
	
	public RandomBamSampler(String bamFile, String poiFile, String outFile) throws IOException {
		this.bamFile = bamFile;
		this.poiFile = poiFile;
		
		bcis = new BlockCompressedInputStream(new File(bamFile));
		File poiFileF = new File(poiFile);
		poiFileLen = poiFileF.length();
		poiReader = new RandomAccessFile(poiFileF, "r");
		bcosWriter = new BlockCompressedOutputStream(outFile);
		
	}
	
	/**
	 * Copy the bam header from the 
	 * @throws IOException 
	 */
	public void writeHeader() throws IOException {
		
		int magic_len = 4;
		String magic = readWriteBytes(magic_len);

		String magicStr = "BAM\\1";
		if (magic.equals(magicStr)) {
			String err = "Illegal bam file magic  string: " + magic;
			throw new RuntimeException(err);
		}		
		
		int l_text = readWriteInt32();		
		readWriteBytes(l_text);		
		int n_ref = readWriteInt32();
		
		// Now transfer the remaining part of the header
		
		for (int j = 0; j < n_ref; j++) {
			int l_name = readWriteInt32();
			readWriteBytes(l_name);
			readWriteInt32();
		}
		
		long pos = bcis.getPosition();
		System.out.println(pos);
		
		//closeWriter();
		
	}
	
	void readRandomPos(int gap) throws NumberFormatException, IOException {
		String line = null;
		
		int longSize = 8;
		int pair = 2;
		//int jumpFreq = 5;
		
		int jumpStep = longSize * pair * gap;
		
		for (long j = 0; j < poiFileLen - jumpStep; j += jumpStep) {
			poiReader.seek(j);
			
			long firstPos = poiReader.readLong();
			long secPos = poiReader.readLong();
			posList.add(firstPos);
			posList.add(secPos);
		}
		Collections.sort(posList);		
	}
	
	void writeRandom() throws IOException {
		
		int count = 0;
		for (int i = 0; i < posList.size(); i++) {
			
			long lPos = posList.get(i);
			bcis.seek(lPos);
			int lSize = readWriteInt32();
			readWriteBytes2(lSize);
			
			count++;
			
			if (count % 1000000 == 0)
				System.out.println("Completed writing " + count + " reads.");
		}		
	}
	
	

	public static void main(String[] args) throws IOException {
		
		String bamFile = args[0];
		String poiFile = args[1];
		String  outFile = args[2];
		
		RandomBamSampler obj = new RandomBamSampler(bamFile, poiFile, outFile);
		obj.writeHeader();
		
		int gap = 20;
		obj.readRandomPos(gap);
		obj.writeRandom();
		obj.closeFiles();
		
	}
	
	private void closeFiles() throws IOException {
		bcis.close();
		poiReader.close();
		bcosWriter.close();
		
	}
	
	private String readWriteBytes(int l_text) throws IOException {
		readFromBcis(bcis, inBuffer, l_text);
		bcosWriter.write(inBuffer, 0, l_text);	
		
		String str = new String(inBuffer, 0, l_text);
		return str;
		
	}
	
	private void readWriteBytes2(int l_text) throws IOException {
		readFromBcis(bcis, inBuffer, l_text);
		bcosWriter.write(inBuffer, 0, l_text);			
		
		
	}

	private int readWriteInt32 () throws IOException {
		readFromBcis(bcis, inBuffer, 4);
		int val = RandomSamplingUtils.unpackInt32(inBuffer, 0);
		bcosWriter.write(inBuffer, 0, 4);		
		return val;
		
	}
	
	private void readFromBcis(BlockCompressedInputStream bcis, byte[] buffer, int length) throws IOException {
		
		int count = bcis.read(buffer, 0, length);
		if (-1 == count) 
		{
			String err = "Info: End of stream has reached!";
			throw new EOFException(err);
		}
	}
}
