package com.andreamazzon.handout5.haltonsequence;

/**
 * This class is used to get elements of a d-dimensional Halton sequence of base
 * (b_1,...,b_d), where b_i, b_j are coprime for every 1 <= i,j <= n: the i-th
 * point of the sequence is represented by a d-dimensional vector whose j-th
 * element is the i-th element of a Van der Corput sequence of base b_j. The
 * elements of the base have to be coprime in order for the elements of the
 * vectors produced by the sequence to be uncorrelated.
 *
 * @author Andrea Mazzon, from the implementation of Christian Fries
 *
 */
public class HaltonSequence {

	private int[] base;

	public HaltonSequence(int[] base) {
		this.base = base;
	}

	public double[] getSamplePoint(int index) {
		
		int baseLength = base.length;
		double[] x = new double[baseLength];
		for (int dimensionIndex = 0; dimensionIndex < baseLength; dimensionIndex++) {
			x[dimensionIndex] = VanDerCorputSequence.getVanDerCorputNumber(index, base[dimensionIndex]);
		}
		return x;
	}
}