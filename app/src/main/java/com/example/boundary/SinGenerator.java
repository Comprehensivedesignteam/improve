package com.example.boundary;

public class SinGenerator {

	public void gen(short[] buffer, double[] random) {
		int duration = 2;
		int begin = 4000;
		int end = 20000;
		int step = 400;
		int samplerate = 44100;
		int n = 32767;

		int genRate;
		int k = 0;
		for (genRate = begin; genRate <= end; genRate = genRate + step) {
			for (int i = 0; i <= duration * samplerate; ++i) {
				int out = (int) (Math.sin(2 * Math.PI * genRate * i
						/ samplerate)
						* n * random[k]);
				buffer[i] += (short) out;
			}
			k++;
		}
	}

}
