/*
 * 11/19/04 1.0 moved to LGPL.
 * 
 * 04/01/00 Fixes for running under build 23xx Microsoft JVM. mdm.
 * 
 * 19/12/99 Performance improvements to compute_pcm_samples(). Mat McGowan. mdm@techie.com.
 * 
 * 16/02/99 Java Conversion by E.B , javalayer@javazoom.net
 * 
 * @(#) synthesis_filter.h 1.8, last edit: 6/15/94 16:52:00
 * 
 * @(#) Copyright (C) 1993, 1994 Tobias Bading (bading@cs.tu-berlin.de)
 * 
 * @(#) Berlin University of Technology
 * 
 * ----------------------------------------------------------------------- This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU Library General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * ----------------------------------------------------------------------
 */

package javazoom.jl.decoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;

/**
 * A class for the synthesis filter bank. This class does a fast downsampling from 32, 44.1 or 48 kHz to 8 kHz, if ULAW is
 * defined. Frequencies above 4 kHz are removed by ignoring higher subbands.
 */
public  final class SynthesisFilter {
	private float[] v1;
	private float[] v2;
	private float[] actual_v; // v1 or v2
	private int actual_write_pos; // 0-15
	private float[] samples; // 32 new subband samples
	private int channel;
	private float scalefactor;

	/**
	 * Quality value for controlling CPU usage/quality tradeoff.
	 */
	/*
	 * private int quality;
	 * 
	 * private int v_inc;
	 * 
	 * 
	 * 
	 * public static final int HIGH_QUALITY = 1; public static final int MEDIUM_QUALITY = 2; public static final int LOW_QUALITY =
	 * 4;
	 */

	/**
	 * Contructor. The scalefactor scales the calculated float pcm samples to short values (raw pcm samples are in [-1.0, 1.0], if
	 * no violations occur).
	 */
	public SynthesisFilter (int channelnumber, float factor, float[] eq0) {
		if (d == null) {
			d = load_d();
			d16 = splitArray(d, 16);
		}

		v1 = new float[512];
		v2 = new float[512];
		samples = new float[32];
		channel = channelnumber;
		scalefactor = factor;

		reset();
	}

	/*
	 * private void setQuality(int quality0) { switch (quality0) { case HIGH_QUALITY: case MEDIUM_QUALITY: case LOW_QUALITY: v_inc
	 * = 16 quality0; quality = quality0; break; default : throw new IllegalArgumentException("Unknown quality value"); } }
	 * 
	 * public int getQuality() { return quality; }
	 */

	/**
	 * Reset the synthesis filter.
	 */
	public void reset () {
		// float[] floatp;
		// float[] floatp2;

		// initialize v1[] and v2[]:
		// for (floatp = v1 + 512, floatp2 = v2 + 512; floatp > v1; )
		// *--floatp = *--floatp2 = 0.0;
		for (int p = 0; p < 512; p++)
			v1[p] = v2[p] = 0.0f;

		// initialize samples[]:
		// for (floatp = samples + 32; floatp > samples; )
		// *--floatp = 0.0;
		for (int p2 = 0; p2 < 32; p2++)
			samples[p2] = 0.0f;

		actual_v = v1;
		actual_write_pos = 15;
	}

	/**
	 * Inject Sample.
	 */
	public void input_sample (float sample, int subbandnumber) {
		samples[subbandnumber] = sample;
	}

	public void input_samples (float[] s) {
		for (int i = 31; i >= 0; i--)
			samples[i] = s[i];
	}

	/**
	 * Compute new values via a fast cosine transform.
	 */
	private void compute_new_v () {
		// p is fully initialized from x1
		// float[] p = _p;
		// pp is fully initialized from p
		// float[] pp = _pp;

		// float[] new_v = _new_v;

		// float[] new_v = new float[32]; // new V[0-15] and V[33-48] of Figure 3-A.2 in ISO DIS 11172-3
		// float[] p = new float[16];
		// float[] pp = new float[16];

		/*
		 * for (int i=31; i>=0; i--) { new_v[i] = 0.0f; }
		 */

		float new_v0, new_v1, new_v2, new_v3, new_v4, new_v5, new_v6, new_v7, new_v8, new_v9;
		float new_v10, new_v11, new_v12, new_v13, new_v14, new_v15, new_v16, new_v17, new_v18, new_v19;
		float new_v20, new_v21, new_v22, new_v23, new_v24, new_v25, new_v26, new_v27, new_v28, new_v29;
		float new_v30, new_v31;

		// float[] new_v = new float[32]; // new V[0-15] and V[33-48] of Figure 3-A.2 in ISO DIS 11172-3
		// float[] p = new float[16];
		// float[] pp = new float[16];

		float[] s = samples;

		float s0 = s[0];
		float s1 = s[1];
		float s2 = s[2];
		float s3 = s[3];
		float s4 = s[4];
		float s5 = s[5];
		float s6 = s[6];
		float s7 = s[7];
		float s8 = s[8];
		float s9 = s[9];
		float s10 = s[10];
		float s11 = s[11];
		float s12 = s[12];
		float s13 = s[13];
		float s14 = s[14];
		float s15 = s[15];
		float s16 = s[16];
		float s17 = s[17];
		float s18 = s[18];
		float s19 = s[19];
		float s20 = s[20];
		float s21 = s[21];
		float s22 = s[22];
		float s23 = s[23];
		float s24 = s[24];
		float s25 = s[25];
		float s26 = s[26];
		float s27 = s[27];
		float s28 = s[28];
		float s29 = s[29];
		float s30 = s[30];
		float s31 = s[31];

		float p0 = s0 + s31;
		float p1 = s1 + s30;
		float p2 = s2 + s29;
		float p3 = s3 + s28;
		float p4 = s4 + s27;
		float p5 = s5 + s26;
		float p6 = s6 + s25;
		float p7 = s7 + s24;
		float p8 = s8 + s23;
		float p9 = s9 + s22;
		float p10 = s10 + s21;
		float p11 = s11 + s20;
		float p12 = s12 + s19;
		float p13 = s13 + s18;
		float p14 = s14 + s17;
		float p15 = s15 + s16;

		float pp0 = p0 + p15;
		float pp1 = p1 + p14;
		float pp2 = p2 + p13;
		float pp3 = p3 + p12;
		float pp4 = p4 + p11;
		float pp5 = p5 + p10;
		float pp6 = p6 + p9;
		float pp7 = p7 + p8;
		float pp8 = (p0 - p15) * cos1_32;
		float pp9 = (p1 - p14) * cos3_32;
		float pp10 = (p2 - p13) * cos5_32;
		float pp11 = (p3 - p12) * cos7_32;
		float pp12 = (p4 - p11) * cos9_32;
		float pp13 = (p5 - p10) * cos11_32;
		float pp14 = (p6 - p9) * cos13_32;
		float pp15 = (p7 - p8) * cos15_32;

		p0 = pp0 + pp7;
		p1 = pp1 + pp6;
		p2 = pp2 + pp5;
		p3 = pp3 + pp4;
		p4 = (pp0 - pp7) * cos1_16;
		p5 = (pp1 - pp6) * cos3_16;
		p6 = (pp2 - pp5) * cos5_16;
		p7 = (pp3 - pp4) * cos7_16;
		p8 = pp8 + pp15;
		p9 = pp9 + pp14;
		p10 = pp10 + pp13;
		p11 = pp11 + pp12;
		p12 = (pp8 - pp15) * cos1_16;
		p13 = (pp9 - pp14) * cos3_16;
		p14 = (pp10 - pp13) * cos5_16;
		p15 = (pp11 - pp12) * cos7_16;

		pp0 = p0 + p3;
		pp1 = p1 + p2;
		pp2 = (p0 - p3) * cos1_8;
		pp3 = (p1 - p2) * cos3_8;
		pp4 = p4 + p7;
		pp5 = p5 + p6;
		pp6 = (p4 - p7) * cos1_8;
		pp7 = (p5 - p6) * cos3_8;
		pp8 = p8 + p11;
		pp9 = p9 + p10;
		pp10 = (p8 - p11) * cos1_8;
		pp11 = (p9 - p10) * cos3_8;
		pp12 = p12 + p15;
		pp13 = p13 + p14;
		pp14 = (p12 - p15) * cos1_8;
		pp15 = (p13 - p14) * cos3_8;

		p0 = pp0 + pp1;
		p1 = (pp0 - pp1) * cos1_4;
		p2 = pp2 + pp3;
		p3 = (pp2 - pp3) * cos1_4;
		p4 = pp4 + pp5;
		p5 = (pp4 - pp5) * cos1_4;
		p6 = pp6 + pp7;
		p7 = (pp6 - pp7) * cos1_4;
		p8 = pp8 + pp9;
		p9 = (pp8 - pp9) * cos1_4;
		p10 = pp10 + pp11;
		p11 = (pp10 - pp11) * cos1_4;
		p12 = pp12 + pp13;
		p13 = (pp12 - pp13) * cos1_4;
		p14 = pp14 + pp15;
		p15 = (pp14 - pp15) * cos1_4;

		// this is pretty insane coding
		float tmp1;
		new_v19/* 36-17 */= -(new_v4 = (new_v12 = p7) + p5) - p6;
		new_v27/* 44-17 */= -p6 - p7 - p4;
		new_v6 = (new_v10 = (new_v14 = p15) + p11) + p13;
		new_v17/* 34-17 */= -(new_v2 = p15 + p13 + p9) - p14;
		new_v21/* 38-17 */= (tmp1 = -p14 - p15 - p10 - p11) - p13;
		new_v29/* 46-17 */= -p14 - p15 - p12 - p8;
		new_v25/* 42-17 */= tmp1 - p12;
		new_v31/* 48-17 */= -p0;
		new_v0 = p1;
		new_v23/* 40-17 */= -(new_v8 = p3) - p2;

		p0 = (s0 - s31) * cos1_64;
		p1 = (s1 - s30) * cos3_64;
		p2 = (s2 - s29) * cos5_64;
		p3 = (s3 - s28) * cos7_64;
		p4 = (s4 - s27) * cos9_64;
		p5 = (s5 - s26) * cos11_64;
		p6 = (s6 - s25) * cos13_64;
		p7 = (s7 - s24) * cos15_64;
		p8 = (s8 - s23) * cos17_64;
		p9 = (s9 - s22) * cos19_64;
		p10 = (s10 - s21) * cos21_64;
		p11 = (s11 - s20) * cos23_64;
		p12 = (s12 - s19) * cos25_64;
		p13 = (s13 - s18) * cos27_64;
		p14 = (s14 - s17) * cos29_64;
		p15 = (s15 - s16) * cos31_64;

		pp0 = p0 + p15;
		pp1 = p1 + p14;
		pp2 = p2 + p13;
		pp3 = p3 + p12;
		pp4 = p4 + p11;
		pp5 = p5 + p10;
		pp6 = p6 + p9;
		pp7 = p7 + p8;
		pp8 = (p0 - p15) * cos1_32;
		pp9 = (p1 - p14) * cos3_32;
		pp10 = (p2 - p13) * cos5_32;
		pp11 = (p3 - p12) * cos7_32;
		pp12 = (p4 - p11) * cos9_32;
		pp13 = (p5 - p10) * cos11_32;
		pp14 = (p6 - p9) * cos13_32;
		pp15 = (p7 - p8) * cos15_32;

		p0 = pp0 + pp7;
		p1 = pp1 + pp6;
		p2 = pp2 + pp5;
		p3 = pp3 + pp4;
		p4 = (pp0 - pp7) * cos1_16;
		p5 = (pp1 - pp6) * cos3_16;
		p6 = (pp2 - pp5) * cos5_16;
		p7 = (pp3 - pp4) * cos7_16;
		p8 = pp8 + pp15;
		p9 = pp9 + pp14;
		p10 = pp10 + pp13;
		p11 = pp11 + pp12;
		p12 = (pp8 - pp15) * cos1_16;
		p13 = (pp9 - pp14) * cos3_16;
		p14 = (pp10 - pp13) * cos5_16;
		p15 = (pp11 - pp12) * cos7_16;

		pp0 = p0 + p3;
		pp1 = p1 + p2;
		pp2 = (p0 - p3) * cos1_8;
		pp3 = (p1 - p2) * cos3_8;
		pp4 = p4 + p7;
		pp5 = p5 + p6;
		pp6 = (p4 - p7) * cos1_8;
		pp7 = (p5 - p6) * cos3_8;
		pp8 = p8 + p11;
		pp9 = p9 + p10;
		pp10 = (p8 - p11) * cos1_8;
		pp11 = (p9 - p10) * cos3_8;
		pp12 = p12 + p15;
		pp13 = p13 + p14;
		pp14 = (p12 - p15) * cos1_8;
		pp15 = (p13 - p14) * cos3_8;

		p0 = pp0 + pp1;
		p1 = (pp0 - pp1) * cos1_4;
		p2 = pp2 + pp3;
		p3 = (pp2 - pp3) * cos1_4;
		p4 = pp4 + pp5;
		p5 = (pp4 - pp5) * cos1_4;
		p6 = pp6 + pp7;
		p7 = (pp6 - pp7) * cos1_4;
		p8 = pp8 + pp9;
		p9 = (pp8 - pp9) * cos1_4;
		p10 = pp10 + pp11;
		p11 = (pp10 - pp11) * cos1_4;
		p12 = pp12 + pp13;
		p13 = (pp12 - pp13) * cos1_4;
		p14 = pp14 + pp15;
		p15 = (pp14 - pp15) * cos1_4;

		// manually doing something that a compiler should handle sucks
		// coding like this is hard to read
		float tmp2;
		new_v5 = (new_v11 = (new_v13 = (new_v15 = p15) + p7) + p11) + p5 + p13;
		new_v7 = (new_v9 = p15 + p11 + p3) + p13;
		new_v16/* 33-17 */= -(new_v1 = (tmp1 = p13 + p15 + p9) + p1) - p14;
		new_v18/* 35-17 */= -(new_v3 = tmp1 + p5 + p7) - p6 - p14;

		new_v22/* 39-17 */= (tmp1 = -p10 - p11 - p14 - p15) - p13 - p2 - p3;
		new_v20/* 37-17 */= tmp1 - p13 - p5 - p6 - p7;
		new_v24/* 41-17 */= tmp1 - p12 - p2 - p3;
		new_v26/* 43-17 */= tmp1 - p12 - (tmp2 = p4 + p6 + p7);
		new_v30/* 47-17 */= (tmp1 = -p8 - p12 - p14 - p15) - p0;
		new_v28/* 45-17 */= tmp1 - tmp2;

		// insert V[0-15] (== new_v[0-15]) into actual v:
		// float[] x2 = actual_v + actual_write_pos;
		float dest[] = actual_v;

		int pos = actual_write_pos;

		dest[0 + pos] = new_v0;
		dest[16 + pos] = new_v1;
		dest[32 + pos] = new_v2;
		dest[48 + pos] = new_v3;
		dest[64 + pos] = new_v4;
		dest[80 + pos] = new_v5;
		dest[96 + pos] = new_v6;
		dest[112 + pos] = new_v7;
		dest[128 + pos] = new_v8;
		dest[144 + pos] = new_v9;
		dest[160 + pos] = new_v10;
		dest[176 + pos] = new_v11;
		dest[192 + pos] = new_v12;
		dest[208 + pos] = new_v13;
		dest[224 + pos] = new_v14;
		dest[240 + pos] = new_v15;

		// V[16] is always 0.0:
		dest[256 + pos] = 0.0f;

		// insert V[17-31] (== -new_v[15-1]) into actual v:
		dest[272 + pos] = -new_v15;
		dest[288 + pos] = -new_v14;
		dest[304 + pos] = -new_v13;
		dest[320 + pos] = -new_v12;
		dest[336 + pos] = -new_v11;
		dest[352 + pos] = -new_v10;
		dest[368 + pos] = -new_v9;
		dest[384 + pos] = -new_v8;
		dest[400 + pos] = -new_v7;
		dest[416 + pos] = -new_v6;
		dest[432 + pos] = -new_v5;
		dest[448 + pos] = -new_v4;
		dest[464 + pos] = -new_v3;
		dest[480 + pos] = -new_v2;
		dest[496 + pos] = -new_v1;

		// insert V[32] (== -new_v[0]) into other v:
		dest = actual_v == v1 ? v2 : v1;

		dest[0 + pos] = -new_v0;
		// insert V[33-48] (== new_v[16-31]) into other v:
		dest[16 + pos] = new_v16;
		dest[32 + pos] = new_v17;
		dest[48 + pos] = new_v18;
		dest[64 + pos] = new_v19;
		dest[80 + pos] = new_v20;
		dest[96 + pos] = new_v21;
		dest[112 + pos] = new_v22;
		dest[128 + pos] = new_v23;
		dest[144 + pos] = new_v24;
		dest[160 + pos] = new_v25;
		dest[176 + pos] = new_v26;
		dest[192 + pos] = new_v27;
		dest[208 + pos] = new_v28;
		dest[224 + pos] = new_v29;
		dest[240 + pos] = new_v30;
		dest[256 + pos] = new_v31;

		// insert V[49-63] (== new_v[30-16]) into other v:
		dest[272 + pos] = new_v30;
		dest[288 + pos] = new_v29;
		dest[304 + pos] = new_v28;
		dest[320 + pos] = new_v27;
		dest[336 + pos] = new_v26;
		dest[352 + pos] = new_v25;
		dest[368 + pos] = new_v24;
		dest[384 + pos] = new_v23;
		dest[400 + pos] = new_v22;
		dest[416 + pos] = new_v21;
		dest[432 + pos] = new_v20;
		dest[448 + pos] = new_v19;
		dest[464 + pos] = new_v18;
		dest[480 + pos] = new_v17;
		dest[496 + pos] = new_v16;
		/*
		 * } else { v1[0 + actual_write_pos] = -new_v0; // insert V[33-48] (== new_v[16-31]) into other v: v1[16 + actual_write_pos]
		 * = new_v16; v1[32 + actual_write_pos] = new_v17; v1[48 + actual_write_pos] = new_v18; v1[64 + actual_write_pos] = new_v19;
		 * v1[80 + actual_write_pos] = new_v20; v1[96 + actual_write_pos] = new_v21; v1[112 + actual_write_pos] = new_v22; v1[128 +
		 * actual_write_pos] = new_v23; v1[144 + actual_write_pos] = new_v24; v1[160 + actual_write_pos] = new_v25; v1[176 +
		 * actual_write_pos] = new_v26; v1[192 + actual_write_pos] = new_v27; v1[208 + actual_write_pos] = new_v28; v1[224 +
		 * actual_write_pos] = new_v29; v1[240 + actual_write_pos] = new_v30; v1[256 + actual_write_pos] = new_v31;
		 * 
		 * // insert V[49-63] (== new_v[30-16]) into other v: v1[272 + actual_write_pos] = new_v30; v1[288 + actual_write_pos] =
		 * new_v29; v1[304 + actual_write_pos] = new_v28; v1[320 + actual_write_pos] = new_v27; v1[336 + actual_write_pos] =
		 * new_v26; v1[352 + actual_write_pos] = new_v25; v1[368 + actual_write_pos] = new_v24; v1[384 + actual_write_pos] =
		 * new_v23; v1[400 + actual_write_pos] = new_v22; v1[416 + actual_write_pos] = new_v21; v1[432 + actual_write_pos] =
		 * new_v20; v1[448 + actual_write_pos] = new_v19; v1[464 + actual_write_pos] = new_v18; v1[480 + actual_write_pos] =
		 * new_v17; v1[496 + actual_write_pos] = new_v16; }
		 */
	}

	/**
	 * Compute PCM Samples.
	 */
	private float[] _tmpOut = new float[32];

	private void compute_pcm_samples0 (OutputBuffer buffer) {
		final float[] vp = actual_v;
		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			float pcm_sample;
			final float[] dp = d16[i];
			pcm_sample = ((vp[0 + dvp] * dp[0] + vp[15 + dvp] * dp[1] + vp[14 + dvp] * dp[2] + vp[13 + dvp] * dp[3] + vp[12 + dvp]
				* dp[4] + vp[11 + dvp] * dp[5] + vp[10 + dvp] * dp[6] + vp[9 + dvp] * dp[7] + vp[8 + dvp] * dp[8] + vp[7 + dvp]
				* dp[9] + vp[6 + dvp] * dp[10] + vp[5 + dvp] * dp[11] + vp[4 + dvp] * dp[12] + vp[3 + dvp] * dp[13] + vp[2 + dvp]
				* dp[14] + vp[1 + dvp] * dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}

	private void compute_pcm_samples1 (OutputBuffer buffer) {
		final float[] vp = actual_v;
		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = ((vp[1 + dvp] * dp[0] + vp[0 + dvp] * dp[1] + vp[15 + dvp] * dp[2] + vp[14 + dvp] * dp[3] + vp[13 + dvp]
				* dp[4] + vp[12 + dvp] * dp[5] + vp[11 + dvp] * dp[6] + vp[10 + dvp] * dp[7] + vp[9 + dvp] * dp[8] + vp[8 + dvp]
				* dp[9] + vp[7 + dvp] * dp[10] + vp[6 + dvp] * dp[11] + vp[5 + dvp] * dp[12] + vp[4 + dvp] * dp[13] + vp[3 + dvp]
				* dp[14] + vp[2 + dvp] * dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}

	private void compute_pcm_samples2 (OutputBuffer buffer) {
		final float[] vp = actual_v;

		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = ((vp[2 + dvp] * dp[0] + vp[1 + dvp] * dp[1] + vp[0 + dvp] * dp[2] + vp[15 + dvp] * dp[3] + vp[14 + dvp]
				* dp[4] + vp[13 + dvp] * dp[5] + vp[12 + dvp] * dp[6] + vp[11 + dvp] * dp[7] + vp[10 + dvp] * dp[8] + vp[9 + dvp]
				* dp[9] + vp[8 + dvp] * dp[10] + vp[7 + dvp] * dp[11] + vp[6 + dvp] * dp[12] + vp[5 + dvp] * dp[13] + vp[4 + dvp]
				* dp[14] + vp[3 + dvp] * dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}

	private void compute_pcm_samples3 (OutputBuffer buffer) {
		final float[] vp = actual_v;

		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = ((vp[3 + dvp] * dp[0] + vp[2 + dvp] * dp[1] + vp[1 + dvp] * dp[2] + vp[0 + dvp] * dp[3] + vp[15 + dvp]
				* dp[4] + vp[14 + dvp] * dp[5] + vp[13 + dvp] * dp[6] + vp[12 + dvp] * dp[7] + vp[11 + dvp] * dp[8] + vp[10 + dvp]
				* dp[9] + vp[9 + dvp] * dp[10] + vp[8 + dvp] * dp[11] + vp[7 + dvp] * dp[12] + vp[6 + dvp] * dp[13] + vp[5 + dvp]
				* dp[14] + vp[4 + dvp] * dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}

	private void compute_pcm_samples4 (OutputBuffer buffer) {
		final float[] vp = actual_v;

		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = ((vp[4 + dvp] * dp[0] + vp[3 + dvp] * dp[1] + vp[2 + dvp] * dp[2] + vp[1 + dvp] * dp[3] + vp[0 + dvp]
				* dp[4] + vp[15 + dvp] * dp[5] + vp[14 + dvp] * dp[6] + vp[13 + dvp] * dp[7] + vp[12 + dvp] * dp[8] + vp[11 + dvp]
				* dp[9] + vp[10 + dvp] * dp[10] + vp[9 + dvp] * dp[11] + vp[8 + dvp] * dp[12] + vp[7 + dvp] * dp[13] + vp[6 + dvp]
				* dp[14] + vp[5 + dvp] * dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}

	private void compute_pcm_samples5 (OutputBuffer buffer) {
		final float[] vp = actual_v;

		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = ((vp[5 + dvp] * dp[0] + vp[4 + dvp] * dp[1] + vp[3 + dvp] * dp[2] + vp[2 + dvp] * dp[3] + vp[1 + dvp]
				* dp[4] + vp[0 + dvp] * dp[5] + vp[15 + dvp] * dp[6] + vp[14 + dvp] * dp[7] + vp[13 + dvp] * dp[8] + vp[12 + dvp]
				* dp[9] + vp[11 + dvp] * dp[10] + vp[10 + dvp] * dp[11] + vp[9 + dvp] * dp[12] + vp[8 + dvp] * dp[13] + vp[7 + dvp]
				* dp[14] + vp[6 + dvp] * dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}

	private void compute_pcm_samples6 (OutputBuffer buffer) {
		final float[] vp = actual_v;
		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = ((vp[6 + dvp] * dp[0] + vp[5 + dvp] * dp[1] + vp[4 + dvp] * dp[2] + vp[3 + dvp] * dp[3] + vp[2 + dvp]
				* dp[4] + vp[1 + dvp] * dp[5] + vp[0 + dvp] * dp[6] + vp[15 + dvp] * dp[7] + vp[14 + dvp] * dp[8] + vp[13 + dvp]
				* dp[9] + vp[12 + dvp] * dp[10] + vp[11 + dvp] * dp[11] + vp[10 + dvp] * dp[12] + vp[9 + dvp] * dp[13] + vp[8 + dvp]
				* dp[14] + vp[7 + dvp] * dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}

	private void compute_pcm_samples7 (OutputBuffer buffer) {
		final float[] vp = actual_v;

		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = ((vp[7 + dvp] * dp[0] + vp[6 + dvp] * dp[1] + vp[5 + dvp] * dp[2] + vp[4 + dvp] * dp[3] + vp[3 + dvp]
				* dp[4] + vp[2 + dvp] * dp[5] + vp[1 + dvp] * dp[6] + vp[0 + dvp] * dp[7] + vp[15 + dvp] * dp[8] + vp[14 + dvp]
				* dp[9] + vp[13 + dvp] * dp[10] + vp[12 + dvp] * dp[11] + vp[11 + dvp] * dp[12] + vp[10 + dvp] * dp[13] + vp[9 + dvp]
				* dp[14] + vp[8 + dvp] * dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}

	private void compute_pcm_samples8 (OutputBuffer buffer) {
		final float[] vp = actual_v;

		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = ((vp[8 + dvp] * dp[0] + vp[7 + dvp] * dp[1] + vp[6 + dvp] * dp[2] + vp[5 + dvp] * dp[3] + vp[4 + dvp]
				* dp[4] + vp[3 + dvp] * dp[5] + vp[2 + dvp] * dp[6] + vp[1 + dvp] * dp[7] + vp[0 + dvp] * dp[8] + vp[15 + dvp]
				* dp[9] + vp[14 + dvp] * dp[10] + vp[13 + dvp] * dp[11] + vp[12 + dvp] * dp[12] + vp[11 + dvp] * dp[13]
				+ vp[10 + dvp] * dp[14] + vp[9 + dvp] * dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}

	private void compute_pcm_samples9 (OutputBuffer buffer) {
		final float[] vp = actual_v;

		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = ((vp[9 + dvp] * dp[0] + vp[8 + dvp] * dp[1] + vp[7 + dvp] * dp[2] + vp[6 + dvp] * dp[3] + vp[5 + dvp]
				* dp[4] + vp[4 + dvp] * dp[5] + vp[3 + dvp] * dp[6] + vp[2 + dvp] * dp[7] + vp[1 + dvp] * dp[8] + vp[0 + dvp] * dp[9]
				+ vp[15 + dvp] * dp[10] + vp[14 + dvp] * dp[11] + vp[13 + dvp] * dp[12] + vp[12 + dvp] * dp[13] + vp[11 + dvp]
				* dp[14] + vp[10 + dvp] * dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}

	private void compute_pcm_samples10 (OutputBuffer buffer) {
		final float[] vp = actual_v;
		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = ((vp[10 + dvp] * dp[0] + vp[9 + dvp] * dp[1] + vp[8 + dvp] * dp[2] + vp[7 + dvp] * dp[3] + vp[6 + dvp]
				* dp[4] + vp[5 + dvp] * dp[5] + vp[4 + dvp] * dp[6] + vp[3 + dvp] * dp[7] + vp[2 + dvp] * dp[8] + vp[1 + dvp] * dp[9]
				+ vp[0 + dvp] * dp[10] + vp[15 + dvp] * dp[11] + vp[14 + dvp] * dp[12] + vp[13 + dvp] * dp[13] + vp[12 + dvp]
				* dp[14] + vp[11 + dvp] * dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}

	private void compute_pcm_samples11 (OutputBuffer buffer) {
		final float[] vp = actual_v;

		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = ((vp[11 + dvp] * dp[0] + vp[10 + dvp] * dp[1] + vp[9 + dvp] * dp[2] + vp[8 + dvp] * dp[3] + vp[7 + dvp]
				* dp[4] + vp[6 + dvp] * dp[5] + vp[5 + dvp] * dp[6] + vp[4 + dvp] * dp[7] + vp[3 + dvp] * dp[8] + vp[2 + dvp] * dp[9]
				+ vp[1 + dvp] * dp[10] + vp[0 + dvp] * dp[11] + vp[15 + dvp] * dp[12] + vp[14 + dvp] * dp[13] + vp[13 + dvp] * dp[14] + vp[12 + dvp]
				* dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}

	private void compute_pcm_samples12 (OutputBuffer buffer) {
		final float[] vp = actual_v;
		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = ((vp[12 + dvp] * dp[0] + vp[11 + dvp] * dp[1] + vp[10 + dvp] * dp[2] + vp[9 + dvp] * dp[3] + vp[8 + dvp]
				* dp[4] + vp[7 + dvp] * dp[5] + vp[6 + dvp] * dp[6] + vp[5 + dvp] * dp[7] + vp[4 + dvp] * dp[8] + vp[3 + dvp] * dp[9]
				+ vp[2 + dvp] * dp[10] + vp[1 + dvp] * dp[11] + vp[0 + dvp] * dp[12] + vp[15 + dvp] * dp[13] + vp[14 + dvp] * dp[14] + vp[13 + dvp]
				* dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}

	private void compute_pcm_samples13 (OutputBuffer buffer) {
		final float[] vp = actual_v;

		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = ((vp[13 + dvp] * dp[0] + vp[12 + dvp] * dp[1] + vp[11 + dvp] * dp[2] + vp[10 + dvp] * dp[3] + vp[9 + dvp]
				* dp[4] + vp[8 + dvp] * dp[5] + vp[7 + dvp] * dp[6] + vp[6 + dvp] * dp[7] + vp[5 + dvp] * dp[8] + vp[4 + dvp] * dp[9]
				+ vp[3 + dvp] * dp[10] + vp[2 + dvp] * dp[11] + vp[1 + dvp] * dp[12] + vp[0 + dvp] * dp[13] + vp[15 + dvp] * dp[14] + vp[14 + dvp]
				* dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}

	private void compute_pcm_samples14 (OutputBuffer buffer) {
		final float[] vp = actual_v;

		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = ((vp[14 + dvp] * dp[0] + vp[13 + dvp] * dp[1] + vp[12 + dvp] * dp[2] + vp[11 + dvp] * dp[3] + vp[10 + dvp]
				* dp[4] + vp[9 + dvp] * dp[5] + vp[8 + dvp] * dp[6] + vp[7 + dvp] * dp[7] + vp[6 + dvp] * dp[8] + vp[5 + dvp] * dp[9]
				+ vp[4 + dvp] * dp[10] + vp[3 + dvp] * dp[11] + vp[2 + dvp] * dp[12] + vp[1 + dvp] * dp[13] + vp[0 + dvp] * dp[14] + vp[15 + dvp]
				* dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}

	private void compute_pcm_samples15 (OutputBuffer buffer) {
		final float[] vp = actual_v;

		// int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp = 0;

		// fat chance of having this loop unroll
		for (int i = 0; i < 32; i++) {
			float pcm_sample;
			final float dp[] = d16[i];
			pcm_sample = ((vp[15 + dvp] * dp[0] + vp[14 + dvp] * dp[1] + vp[13 + dvp] * dp[2] + vp[12 + dvp] * dp[3] + vp[11 + dvp]
				* dp[4] + vp[10 + dvp] * dp[5] + vp[9 + dvp] * dp[6] + vp[8 + dvp] * dp[7] + vp[7 + dvp] * dp[8] + vp[6 + dvp]
				* dp[9] + vp[5 + dvp] * dp[10] + vp[4 + dvp] * dp[11] + vp[3 + dvp] * dp[12] + vp[2 + dvp] * dp[13] + vp[1 + dvp]
				* dp[14] + vp[0 + dvp] * dp[15]) * scalefactor);

			tmpOut[i] = pcm_sample;
			dvp += 16;
		} // for
	}

	private void compute_pcm_samples (OutputBuffer buffer) {

		switch (actual_write_pos) {
		case 0:
			compute_pcm_samples0(buffer);
			break;
		case 1:
			compute_pcm_samples1(buffer);
			break;
		case 2:
			compute_pcm_samples2(buffer);
			break;
		case 3:
			compute_pcm_samples3(buffer);
			break;
		case 4:
			compute_pcm_samples4(buffer);
			break;
		case 5:
			compute_pcm_samples5(buffer);
			break;
		case 6:
			compute_pcm_samples6(buffer);
			break;
		case 7:
			compute_pcm_samples7(buffer);
			break;
		case 8:
			compute_pcm_samples8(buffer);
			break;
		case 9:
			compute_pcm_samples9(buffer);
			break;
		case 10:
			compute_pcm_samples10(buffer);
			break;
		case 11:
			compute_pcm_samples11(buffer);
			break;
		case 12:
			compute_pcm_samples12(buffer);
			break;
		case 13:
			compute_pcm_samples13(buffer);
			break;
		case 14:
			compute_pcm_samples14(buffer);
			break;
		case 15:
			compute_pcm_samples15(buffer);
			break;
		}

		if (buffer != null) buffer.appendSamples(channel, _tmpOut);

		/*
		 * // MDM: I was considering putting in quality control for // low-spec CPUs, but the performance gain (about 10-15%) // did
		 * not justify the considerable drop in audio quality. switch (inc) { case 16: buffer.appendSamples(channel, tmpOut); break;
		 * case 32: for (int i=0; i<16; i++) { buffer.append(channel, (short)tmpOut[i]); buffer.append(channel, (short)tmpOut[i]); }
		 * break; case 64: for (int i=0; i<8; i++) { buffer.append(channel, (short)tmpOut[i]); buffer.append(channel,
		 * (short)tmpOut[i]); buffer.append(channel, (short)tmpOut[i]); buffer.append(channel, (short)tmpOut[i]); } break;
		 * 
		 * }
		 */
	}

	/**
	 * Calculate 32 PCM samples and put the into the Obuffer-object.
	 */

	public void calculate_pcm_samples (OutputBuffer buffer) {
		compute_new_v();
		compute_pcm_samples(buffer);

		actual_write_pos = actual_write_pos + 1 & 0xf;
		actual_v = actual_v == v1 ? v2 : v1;

		// initialize samples[]:
		// for (register float *floatp = samples + 32; floatp > samples; )
		// *--floatp = 0.0f;

		// MDM: this may not be necessary. The Layer III decoder always
		// outputs 32 subband samples, but I haven't checked layer I & II.
		for (int p = 0; p < 32; p++)
			samples[p] = 0.0f;
	}

	private static final double MY_PI = 3.14159265358979323846;
	private static final float cos1_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI / 64.0)));
	private static final float cos3_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 3.0 / 64.0)));
	private static final float cos5_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 5.0 / 64.0)));
	private static final float cos7_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 7.0 / 64.0)));
	private static final float cos9_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 9.0 / 64.0)));
	private static final float cos11_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 11.0 / 64.0)));
	private static final float cos13_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 13.0 / 64.0)));
	private static final float cos15_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 15.0 / 64.0)));
	private static final float cos17_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 17.0 / 64.0)));
	private static final float cos19_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 19.0 / 64.0)));
	private static final float cos21_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 21.0 / 64.0)));
	private static final float cos23_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 23.0 / 64.0)));
	private static final float cos25_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 25.0 / 64.0)));
	private static final float cos27_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 27.0 / 64.0)));
	private static final float cos29_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 29.0 / 64.0)));
	private static final float cos31_64 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 31.0 / 64.0)));
	private static final float cos1_32 = (float)(1.0 / (2.0 * Math.cos(MY_PI / 32.0)));
	private static final float cos3_32 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 3.0 / 32.0)));
	private static final float cos5_32 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 5.0 / 32.0)));
	private static final float cos7_32 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 7.0 / 32.0)));
	private static final float cos9_32 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 9.0 / 32.0)));
	private static final float cos11_32 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 11.0 / 32.0)));
	private static final float cos13_32 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 13.0 / 32.0)));
	private static final float cos15_32 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 15.0 / 32.0)));
	private static final float cos1_16 = (float)(1.0 / (2.0 * Math.cos(MY_PI / 16.0)));
	private static final float cos3_16 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 3.0 / 16.0)));
	private static final float cos5_16 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 5.0 / 16.0)));
	private static final float cos7_16 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 7.0 / 16.0)));
	private static final float cos1_8 = (float)(1.0 / (2.0 * Math.cos(MY_PI / 8.0)));
	private static final float cos3_8 = (float)(1.0 / (2.0 * Math.cos(MY_PI * 3.0 / 8.0)));
	private static final float cos1_4 = (float)(1.0 / (2.0 * Math.cos(MY_PI / 4.0)));

	// Note: These values are not in the same order
	// as in Annex 3-B.3 of the ISO/IEC DIS 11172-3
	// private float d[] = {0.000000000, -4.000442505};

	private static float d[] = null;

	/**
	 * d[] split into subarrays of length 16. This provides for more faster access by allowing a block of 16 to be addressed with
	 * constant offset.
	 **/
	private static float d16[][] = null;

	/**
	 * Loads the data for the d[] from the resource SFd.ser.
	 * @return the loaded values for d[].
	 */
	static private float[] load_d () {
		try {
			Class elemType = Float.TYPE;
			Object o = deserializeArray(SynthesisFilter.class.getResourceAsStream("/sfd.ser"), elemType, 512);
			return (float[])o;
		} catch (IOException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	/**
	 * Deserializes an array from a given <code>InputStream</code>.
	 * 
	 * @param in The <code>InputStream</code> to deserialize an object from.
	 * 
	 * @param elemType The class denoting the type of the array elements.
	 * @param length The expected length of the array, or -1 if any length is expected.
	 */
	static private Object deserializeArray (InputStream in, Class elemType, int length) throws IOException {
		if (elemType == null) throw new NullPointerException("elemType");

		if (length < -1) throw new IllegalArgumentException("length");

		Object obj = deserialize(in);

		Class cls = obj.getClass();

		if (!cls.isArray()) throw new InvalidObjectException("object is not an array");

		Class arrayElemType = cls.getComponentType();
		if (arrayElemType != elemType) throw new InvalidObjectException("unexpected array component type");

		if (length != -1) {
			int arrayLength = Array.getLength(obj);
			if (arrayLength != length) throw new InvalidObjectException("array length mismatch");
		}

		return obj;
	}

	static public Object deserialize (InputStream in) throws IOException {
		if (in == null) throw new NullPointerException("in");

		ObjectInputStream objIn = new ObjectInputStream(in);

		Object obj;

		try {
			obj = objIn.readObject();
		} catch (ClassNotFoundException ex) {
			throw new InvalidClassException(ex.toString());
		}
		return obj;
	}

	/**
	 * Converts a 1D array into a number of smaller arrays. This is used to achieve offset + constant indexing into an array. Each
	 * sub-array represents a block of values of the original array.
	 * @param array The array to split up into blocks.
	 * @param blockSize The size of the blocks to split the array into. This must be an exact divisor of the length of the array,
	 *           or some data will be lost from the main array.
	 * 
	 * @return An array of arrays in which each element in the returned array will be of length <code>blockSize</code>.
	 */
	static private float[][] splitArray (final float[] array, final int blockSize) {
		int size = array.length / blockSize;
		float[][] split = new float[size][];
		for (int i = 0; i < size; i++)
			split[i] = subArray(array, i * blockSize, blockSize);
		return split;
	}

	/**
	 * Returns a subarray of an existing array.
	 * 
	 * @param array The array to retrieve a subarra from.
	 * @param offs The offset in the array that corresponds to the first index of the subarray.
	 * @param len The number of indeces in the subarray.
	 * @return The subarray, which may be of length 0.
	 */
	static private float[] subArray (final float[] array, final int offs, int len) {
		if (offs + len > array.length) len = array.length - offs;

		if (len < 0) len = 0;

		float[] subarray = new float[len];
		for (int i = 0; i < len; i++)
			subarray[i] = array[offs + i];
		return subarray;
	}
}
