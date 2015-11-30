package nirmalya;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import htsjdk.samtools.util.BlockCompressedInputStream;

//import nirmalya.RandomSamplingUtils;;

/**
 * This class creates an indexes for the starting and ending positions of the alignments
 * in the bam file.
 *  
 * @author nirmalya
 *
 */
public class IndexBuilder {

	BlockCompressedInputStream bcis = null;

	public final int MAXLEN = 65536;
	private int l_text = 0;
	private String text = null;
	private int n_ref = 0;
	private String bamFile = null;
	private String outFile = null;
	// size of integer in bytes
	private final int IntSize = 4;

	HashMap<String, Pos> posMap = new HashMap<String, Pos>();

	public IndexBuilder(String inFile, String outFile) throws IOException {
		this.bamFile = inFile;
		this.outFile = outFile;
		this.bcis = new BlockCompressedInputStream(new File(bamFile));
	}

	void processHeader() throws IOException {

		// Check if the BAM\1 magic string is there.
		String magic = null;

		magic = RandomSamplingUtils.readStr(bcis, 4);
		String magicStr = "BAM\\1";
		if (magic.equals(magicStr)) {
			String err = "Illegal bam file magic  string: " + magic;
			throw new RuntimeException(err);
		}

		l_text = RandomSamplingUtils.readInt32(bcis);
		text = RandomSamplingUtils.readStr(bcis, l_text);
		n_ref = RandomSamplingUtils.readInt32(bcis);

		/*
		 * System.out.println(l_text); System.out.println(text);
		 * System.out.println(n_ref);
		 */
	}

	void processReferences() throws IOException {

		if (n_ref == 0) {
			String err = "Number of reference is zero";
			throw new RuntimeException(err);
		}

		for (int j = 0; j < n_ref; j++) {
			int l_name = RandomSamplingUtils.readInt32(bcis);
			String name = RandomSamplingUtils.readStr(bcis, l_name);
			int l_ref = RandomSamplingUtils.readInt32(bcis);

			/*
			 * System.out.println(l_name); System.out.println(name);
			 * System.out.println(l_ref);
			 */
		}
	}

	void processAlignments() throws IOException {
		byte[] tempBuf = new byte[MAXLEN];

		//PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));
		
		// Do the writing using binary format in the pair: firstPos, secondPos each in long 
		// For this purpose use Java's default endianness.
		
		DataOutputStream outS = new DataOutputStream(new FileOutputStream(outFile));

		while (true) {

			// Assuming this is the start of an alignment
			long lPos = bcis.getPosition();

			int block_size = 0;
			try {
				block_size = RandomSamplingUtils.readInt32(bcis);
				int totalBlockSize = IntSize + block_size;

				bcis.read(tempBuf, 0, block_size);

				int startPos = 32;
				int count = 0;

				while (tempBuf[startPos + count] != 0) {
					count++;
				}

				String read_name = new String(tempBuf, startPos, count);

				if (posMap.containsKey(read_name)) {
					Pos firstPosObj = posMap.get(read_name);					
					
					//writer.println(firstPosObj.pos + " " + firstPosObj.size + " " + lPos + " " + totalBlockSize);
					outS.writeLong(firstPosObj.pos);
					outS.writeLong(lPos);
					posMap.remove(read_name);
				} else {
					Pos pObj = new Pos(lPos, totalBlockSize);
					posMap.put(read_name, pObj);
				}

			} catch (EOFException e) {
				//System.out.println("BlockSize: " + (block_size + 4) + " mapSize: " + posMap.size() + " Position: "
				//		+ BlockCompressedFilePointerUtil.getBlockAddress(lPos) + " Pos2: "
				//		+ BlockCompressedFilePointerUtil.getBlockOffset(lPos));
				outS.close();
				break;
			}
		}
	}

	public static void main(String[] args) throws IOException {

		String bamFile = args[0];
		String outFile = args[1];
		IndexBuilder obj = new IndexBuilder(bamFile, outFile);
		obj.processHeader();
		obj.processReferences();
		obj.processAlignments();	

	}
}

class Pos {
	long pos;
	int size;
	
	public Pos (long pos, int size) {
		this.pos = pos;
		this.size = size;
	}	
}
