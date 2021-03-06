package crypto;

import java.util.ArrayList;


/**
 * The cipher interface for cipher algorithms
 * used in encryption and decryption
 *
 * @author 	Eivind Vinje
 */
public interface ICipher {
	public int getBlockSize();
	public int getKeySize();
	public void cipher(byte[] input, int inOff, byte[] output, int outOff);
	public String getName();
	public void init(boolean forEncryption, byte[] key);
	public static ArrayList<ICipher> allCipherAlgorithms = new ArrayList<ICipher>();
}
