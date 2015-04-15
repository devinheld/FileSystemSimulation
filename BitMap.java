/**
 * 
 * @author Devin Held ID: 26883102
 *
 */

// Created from the provided slide description of BitMaps
public class BitMap {
	public int[] bitMap;
	private int[] mask;

	BitMap() {
		// 2 int bitmap and 32 int mask
		this.bitMap = new int[2];
		this.mask = new int[32];

		// creates the diagonal ones in the mask
		mask[31] = 1;
		for (int i = 30; i >= 0; i--)
			mask[i] = mask[i + 1] << 1;

		fillPosition(0);
	}

	// Pack the 4-byte integer val into the 32 bits
	byte[] pack() {
		byte[] packed = new byte[Info.BLOCK_LENGTH];
		int firstNum = bitMap[0], secondNum = bitMap[1];
		for (int i = 31; i >= 0; i--) {
			packed[i] = (byte) (firstNum & mask[i]);
			firstNum = firstNum >> 1;
		}

		for (int i = 63; i >= 32; i--) {
			packed[i] = (byte) (secondNum & mask[i % 32]);
			secondNum = secondNum >> 1;
		}

		return packed;
	}

	// Finds empty position in Bitmap and returns the index
	public int findEmptyLocation() {
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 32; j++) {
				if ((bitMap[i] & mask[j]) == 0)
					return i * 32 + j;
			}
		}
		return Errors.FAIL;
	}

	// Finds empty block to store file
	public int findEmptyBlock() {
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 32; j++) {
				if ((bitMap[i] & mask[j + 7]) == 0)
					return i * 32 + j + 7;
			}
		}
		return Errors.FAIL;

	}

	// Returns the first integer in the bitmap
	int firstItem() {
		return bitMap[0];
	}

	// Returns the second integer in the bitmap
	int secondItem() {
		return bitMap[1];
	}

	// Sets the position to 1
	public void fillPosition(int pos) {
		bitMap[pos / 32] = bitMap[pos / 32] | mask[pos % 32];
	}

	// Sets the position to 0
	public void deletePosition(int pos) {
		bitMap[pos / 32] = bitMap[pos / 32] & ~mask[pos % 32];

	}

}
