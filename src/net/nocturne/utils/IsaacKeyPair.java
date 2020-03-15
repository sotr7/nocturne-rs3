package net.nocturne.utils;

public class IsaacKeyPair {

	private ISAACCipher inKey, outKey;
	private int[] isaacKeys;

	public IsaacKeyPair(int[] seed) {
		isaacKeys = seed;
		inKey = new ISAACCipher(seed);
		for (int i = 0; i < seed.length; i++)
			seed[i] += 50;
		outKey = new ISAACCipher(seed);
	}

	public ISAACCipher inKey() {
		return inKey;
	}

	public ISAACCipher outKey() {
		return outKey;
	}

	public int[] getIsaacKeys() {
		// TODO Auto-generated method stub
		return isaacKeys;
	}
}