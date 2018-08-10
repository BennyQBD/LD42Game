/** 
 * Copyright (c) 2015, Benny Bobaganoosh. All rights reserved.
 * License terms are in the included LICENSE.txt file.
 */
package engine.rendering;

import engine.util.Util;

/**
 * Represents a Color.
 * 
 * @author Benny Bobaganoosh (thebennybox@gmail.com)
 */
public class Color {
	/** Pure White. */
	public static final Color WHITE = new Color(1.0, 1.0, 1.0);
	/** Pure Black. */
	public static final Color BLACK = new Color(0.0, 0.0, 0.0);
	public static final Color RED = new Color(1.0, 0.0, 0.0);
	public static final Color GREEN = new Color(0.0, 1.0, 0.0);
	public static final Color BLUE = new Color(0.0, 0.0, 1.0);
	public static final Color YELLOW = new Color(1.0, 1.0, 0.0);
	public static final Color CYAN = new Color(0.0, 1.0, 1.0);
	public static final Color MAGENTA = new Color(1.0, 0.0, 0.0);
	public static final Color ORANGE = new Color(1.0, 0.5, 0.0);

	private static final int ARGB_COMPONENT_BITS = 8;
	private static final int ARGB_COMPONENT_MASK = (1 << ARGB_COMPONENT_BITS) - 1;
	private static final int ARGB_NUM_COMPONENTS = 4;

	private double red;
	private double green;
	private double blue;
	private double alpha;

	/**
	 * Creates a new Color.
	 * 
	 * @param red
	 *            The amount of red, in the range of (0, 1).
	 * @param green
	 *            The amount of green, in the range of (0, 1).
	 * @param blue
	 *            The amount of blue, in the range of (0, 1).
	 */
	public Color(double red, double green, double blue) {
		this(red, green, blue, 1.0);
	}

	/**
	 * Creates a new Color.
	 * 
	 * @param red
	 *            The amount of red, in the range of (0, 1).
	 * @param green
	 *            The amount of green, in the range of (0, 1).
	 * @param blue
	 *            The amount of blue, in the range of (0, 1).
	 * @param alpha
	 *            The amount of transparency, in the range of (0, 1). 0 is fully
	 *            transparent, and 1 is fully opaque.
	 */
	public Color(double red, double green, double blue, double alpha) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.alpha = alpha;
	}

	/**
	 * Creates a new Color with a shade a grey.
	 * 
	 * @param amt
	 *            The amount of grey, in the range of (0, 1).
	 */
	public Color(double amt) {
		this(amt, amt, amt);
	}

	/**
	 * Creates a 32-bit ARGB color.
	 * 
	 * @param a
	 *            The amount of alpha, in range of (0, 255).
	 * @param r
	 *            The amount of red, in range of (0, 255).
	 * @param g
	 *            The amount of green, in range of (0, 255).
	 * @param b
	 *            The amount of blue, in range of (0, 255).
	 * @return A 32-bit ARGB color.
	 */
	public static int makeARGB(int a, int r, int g, int b) {
		return ((a & ARGB_COMPONENT_MASK) << getComponentShift(0))
				| ((r & ARGB_COMPONENT_MASK) << getComponentShift(1))
				| ((g & ARGB_COMPONENT_MASK) << getComponentShift(2))
				| (b & ARGB_COMPONENT_MASK) << getComponentShift(3);
	}

	/**
	 * Creates a 32-bit ARGB color.
	 * 
	 * @param r
	 *            The amount of red, in range of (0, 255).
	 * @param g
	 *            The amount of green, in range of (0, 255).
	 * @param b
	 *            The amount of blue, in range of (0, 255).
	 * @return A 32-bit ARGB color.
	 */
	public static int makeARGB(int r, int g, int b) {
		return makeARGB(ARGB_COMPONENT_MASK, r, g, b);
	}

	/**
	 * Creates a 32-bit ARGB color.
	 * 
	 * @param a
	 *            The amount of alpha, in range of (0, 1).
	 * @param r
	 *            The amount of red, in range of (0, 1).
	 * @param g
	 *            The amount of green, in range of (0, 1).
	 * @param b
	 *            The amount of blue, in range of (0, 1).
	 * @return A 32-bit ARGB color.
	 */
	public static int makeARGB(double a, double r, double g, double b) {
		return makeARGB(doubleToComponent(a), doubleToComponent(r),
				doubleToComponent(g), doubleToComponent(b));
	}

	/**
	 * Creates a 32-bit ARGB color.
	 * 
	 * @param r
	 *            The amount of red, in range of (0, 1).
	 * @param g
	 *            The amount of green, in range of (0, 1).
	 * @param b
	 *            The amount of blue, in range of (0, 1).
	 * @return A 32-bit ARGB color.
	 */
	public static int makeARGB(double r, double g, double b) {
		return makeARGB(1.0, r, g, b);
	}

	private static int doubleToComponent(double c) {
		return (int) (Util.saturate(c) * ARGB_COMPONENT_MASK + 0.5);
	}

	/**
	 * Gets a component of an ARGB color.
	 * 
	 * @param pixel
	 *            A 32-bit ARGB color.
	 * @param component
	 *            Which component to get, in range of (0, 3). <br/>
	 *            0 -> Alpha <br/>
	 *            1 -> Red <br/>
	 *            2 -> Green <br/>
	 *            3 -> Blue
	 * @return The component of the ARGB Color.
	 */
	public static byte getARGBComponent(int pixel, int component) {
		return (byte) ((pixel >> getComponentShift(component)) & ARGB_COMPONENT_MASK);
	}

	private static int getComponentShift(int component) {
		return (ARGB_COMPONENT_BITS * (ARGB_NUM_COMPONENTS - component - 1));
	}

	/**
	 * Returns a 32-Bit ARGB version of this color, with intensity scaled by
	 * scaleAmt.
	 * 
	 * @param scaleAmt
	 *            The amount to scale the intensity of this color during
	 *            conversion.
	 * @return A 32-Bit ARGB version of this color, with intensity scaled by
	 *         scaleAmt.
	 */
	public int scaleToARGB(double scaleAmt) {
		return makeARGB(red * scaleAmt, green * scaleAmt, blue * scaleAmt);
	}

	/**
	 * Get the amount of red in this color.
	 * 
	 * @return The amount of green in this color.
	 */
	public double getRed() {
		return red;
	}

	/**
	 * Get the amount of green in this color.
	 * 
	 * @return The amount of green in this color.
	 */
	public double getGreen() {
		return green;
	}

	/**
	 * Get the amount of blue in this color.
	 * 
	 * @return The amount of blue in this color.
	 */
	public double getBlue() {
		return blue;
	}

	/**
	 * Get the amount of alpha in this color.
	 * 
	 * @return The amount of alpha in this color.
	 */
	public double getAlpha() {
		return alpha;
	}

	public void set(double r, double g, double b, double a) {
		red = r;
		green = g;
		blue = b;
		alpha = a;
	}

//	function hsvToRgb(h, s, v){
//		var r, g, b;
//
//		var i = Math.floor(h * 6);
//		var f = h * 6 - i;
//		var p = v * (1 - s);
//		var q = v * (1 - f * s);
//		var t = v * (1 - (1 - f) * s);
//
//		switch(i % 6){
//			case 0: r = v, g = t, b = p; break;
//			case 1: r = q, g = v, b = p; break;
//			case 2: r = p, g = v, b = t; break;
//			case 3: r = p, g = q, b = v; break;
//			case 4: r = t, g = p, b = v; break;
//			case 5: r = v, g = p, b = q; break;
//		}
//
//		return [r * 255, g * 255, b * 255];
//	}

	public Color hsvToRgb() {
		double r, g, b;
		r = 0.0; g = 0.0; b = 0.0;
		double h = red;
		double s = green;
		double v = blue;

		double i = Math.floor(red * 6.0);
		double f = h * 6.0 - i;
		double p = blue * (1.0 - green);
		double q = blue * (1.0 - f * green);
		double t = blue * (1.0 - (1.0 - f)*green);
		switch(((int)i) % 6) {
			case 0: r = v; g = t; b = p; break;
			case 1: r = q; g = v; b = p; break;
			case 2: r = p; g = v; b = t; break;
			case 3: r = p; g = q; b = v; break;
			case 4: r = t; g = p; b = v; break;
			case 5: r = v; g = p; b = q; break;
		}
		red = r;
		green = g;
		blue = b;
		return this;
	}

	public Color rgbToHsv() {
		double max = Math.max(Math.max(red, green), blue);
		double min = Math.min(Math.min(red, green), blue);
		double h = max;
		double v = max;
		
		double delta = max - min;
		double s = max == 0 ? 0 : delta/max;

		if(max == min) {
			h = 0;
		} else { 
			if(max == red) {
				h = (green - blue)/delta + (green < blue ? 6.0 : 0.0);
			}
			if(max == green) {
				h = (blue - red)/delta + 2.0;
			}
			if(max == blue) {
				h = (red - green)/delta + 4.0;
			}
			h /= 6.0;
		}
		red = h;
		green = s;
		blue = v;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(blue);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(green);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(red);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Color other = (Color) obj;
		if (Double.doubleToLongBits(blue) != Double
				.doubleToLongBits(other.blue))
			return false;
		if (Double.doubleToLongBits(green) != Double
				.doubleToLongBits(other.green))
			return false;
		if (Double.doubleToLongBits(red) != Double.doubleToLongBits(other.red))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Color [red=" + red + ", green=" + green + ", blue=" + blue
				+ "]";
	}
}
