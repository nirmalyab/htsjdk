package nirmalya;

import java.io.EOFException;
import java.io.IOException;

import htsjdk.samtools.util.BlockCompressedInputStream;

public class RandomSamplingUtils {
	
	
	
	
	public static String readStr(BlockCompressedInputStream bcis, int length) throws IOException {
		byte[] buffer = new byte[length];
		if (-1 == bcis.read(buffer)) {
			String err = "Reached end of stream!";
			throw new EOFException(err);
		}
		
		String lStr = new String(buffer);
		buffer = null;
		return lStr;
		
		
	}

	public static int readInt32(BlockCompressedInputStream bcis) throws IOException {

		byte[] buffer = new byte[4];
		if (-1 == bcis.read(buffer)) {
			String err = "Reached end of stream!";
			throw new EOFException(err);
		}
		
		int lVal = unpackInt32(buffer, 0);
		buffer = null;
		return lVal;

	}

	public static int unpackInt32(final byte[] buffer, final int offset) {
		return ((buffer[offset] & 0xFF) | ((buffer[offset + 1] & 0xFF) << 8) | ((buffer[offset + 2] & 0xFF) << 16)
				| ((buffer[offset + 3] & 0xFF) << 24));
	}


}
