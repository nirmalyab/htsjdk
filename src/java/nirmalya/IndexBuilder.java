package nirmalya;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import htsjdk.samtools.util.BlockCompressedInputStream;

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

	HashMap<String, Long> posMap = new HashMap<String, Long>();

	public IndexBuilder(String inFile, String outFile) throws IOException {
		this.bamFile = inFile;
		this.outFile = outFile;
		this.bcis = new BlockCompressedInputStream(new File(bamFile));
	}

	void processHeader() throws IOException {

		// Check if the BAM\1 magic string is there.
		String magic = null;

		magic = readStr(bcis, 4);
		String magicStr = "BAM\\1";
		if (magic.equals(magicStr)) {
			String err = "Illegal bam file magic  string: " + magic;
			throw new RuntimeException(err);
		}

		l_text = readInt32(bcis);
		text = readStr(bcis, l_text);
		n_ref = readInt32(bcis);

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
			int l_name = readInt32(bcis);
			String name = readStr(bcis, l_name);
			int l_ref = readInt32(bcis);

			/*
			 * System.out.println(l_name); System.out.println(name);
			 * System.out.println(l_ref);
			 */
		}
	}

	void processAlignments() throws IOException {
		byte[] tempBuf = new byte[MAXLEN];

		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));

		while (true) {

			// Assuming this is the start of an alignment
			long lPos = bcis.getPosition();

			int block_size = 0;
			try {
				block_size = readInt32(bcis);

				bcis.read(tempBuf, 0, block_size);

				int startPos = 32;
				int count = 0;

				while (tempBuf[startPos + count] != 0) {
					count++;
				}

				String read_name = new String(tempBuf, startPos, count);

				if (posMap.containsKey(read_name)) {
					long firstPos = posMap.get(read_name);
					writer.println(read_name + " " + firstPos + " " + lPos);
					posMap.remove(read_name);
				} else {
					posMap.put(read_name, lPos);
				}

			} catch (EOFException e) {
				//System.out.println("BlockSize: " + (block_size + 4) + " mapSize: " + posMap.size() + " Position: "
				//		+ BlockCompressedFilePointerUtil.getBlockAddress(lPos) + " Pos2: "
				//		+ BlockCompressedFilePointerUtil.getBlockOffset(lPos));
				writer.close();
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

	private String readStr(BlockCompressedInputStream bcis, int length) throws IOException {
		byte[] buffer = new byte[length];
		if (-1 == bcis.read(buffer)) {
			String err = "Reached end of stream!";
			throw new EOFException(err);
		}
		
		String lStr = new String(buffer);
		buffer = null;
		return lStr;
	}

	private int readInt32(BlockCompressedInputStream bcis) throws IOException {

		byte[] buffer = new byte[4];
		if (-1 == bcis.read(buffer)) {
			String err = "Reached end of stream!";
			throw new EOFException(err);
		}
		
		int lVal = unpackInt32(buffer, 0);
		buffer = null;
		return lVal;

	}

	private int unpackInt32(final byte[] buffer, final int offset) {
		return ((buffer[offset] & 0xFF) | ((buffer[offset + 1] & 0xFF) << 8) | ((buffer[offset + 2] & 0xFF) << 16)
				| ((buffer[offset + 3] & 0xFF) << 24));
	}
}
