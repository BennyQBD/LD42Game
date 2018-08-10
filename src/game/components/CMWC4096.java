package game.components;

import java.util.Random;

public class CMWC4096 extends Random {
	private static int CMWC_CYCLE = 4096;
	private static int CMWC_C_MAX = 809430660;
	private int[] Q;
	private int c;
	private int i;

	private static CMWC4096 globalRNG = null;

	public static double random() {
		if(globalRNG == null) {
			globalRNG = new CMWC4096();
		}
		return globalRNG.nextDouble();
	}

	public static double random(double min, double max) {
		return random()*(max-min)+min;
	}

	public CMWC4096() {
		this((new Random()).nextLong());
	}

	public CMWC4096(long seed) {
		super();
		Q = new int[CMWC_CYCLE];
		Random rand = new Random(seed);
		for(int j = 0; j < CMWC_CYCLE; j++) {
			Q[j] = rand.nextInt();
		}
		do {
			c = rand.nextInt();
		} while(c >= CMWC_C_MAX || c < 0);
		this.i = CMWC_CYCLE - 1;
	}
	
	@Override
	protected int next(int bits) {
		final long a = 18782;
		final int m = 0xfffffffe;
		long t;
		int x;

		i = (i+1) & (CMWC_CYCLE-1);
		long Qi = ((long)Q[i]) & 0xffffffffL;
		t=a*Qi+c;
		c=(int)(t>>32);
		x=((int)t) + c;
		if (x<c && x>=0) {
            x++;
            c++;
        }
        return (Q[i]=m-x) >>> (32-bits);

//		i = (i + 1) & (CMWC_CYCLE-1);
//		t = a*Q[i]+c;
//		c = (int)((t >> 32) & 0xFFFFFFFF);
//		x = (int)(t&0xFFFFFFFF) + c;
//		if(lessThanUnsigned(x,c)) {
//			x++;
//			c++;
//		}
//
//		Q[i] = m-x;
//		return Q[i] >>> (32-bits);
	}

	private static boolean lessThanUnsigned(int n1, int n2) {
	  return (n1 & 0xffffffffL) < (n2 & 0xffffffffL);
	}
}
