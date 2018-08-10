/**
@file
@author Benny Bobaganoosh <thebennybox@gmail.com>
@section LICENSE

Copyright (c) 2014, Benny Bobaganoosh
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer. 
2. Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package game;

import java.util.Arrays;

import game.components.CMWC4096;
import engine.rendering.Color;
import engine.rendering.IRenderContext;
import engine.rendering.SpriteSheet;



/**
 * Represents a 3D Star field that can be rendered into an image.
 */
public class Stars3D
{
	private static class Star implements Comparable<Star> {
		double x;
		double y;
		double z;

		public Star(double spreadX, double spreadY, double spreadZ, double minZ) {
			init(spreadX, spreadY, spreadZ, minZ);
		}

		public void init(double spreadX, double spreadY, double spreadZ, double minZ) {
			x = 2 * ((double)CMWC4096.random() - 0.5) * spreadX;
			y = 2 * ((double)CMWC4096.random() - 0.5) * spreadY;
			z = ((double)CMWC4096.random()) * (spreadZ-minZ)+minZ;
		}

		@Override
		public int compareTo(Star other) {
			if(other.z > z) {
				return 1;
			}
			if(other.z < z) {
				return -1;
			}
			return 0;
		}
	}
	private final Star stars[];
	/** How much the stars are spread out in 3D space, on average. */
	private final double spreadX;
	private final double spreadY;
	private final double spreadZ;
	private final double minZ;

	/**
	 * Creates a new 3D star field in a usable state.
	 *
	 * @param numStars The number of stars in the star field
	 * @param spread   How much the stars spread out, on average.
	 * @param speed    How quickly the stars move towards the camera
	 */
	public Stars3D(int numStars, double spreadX, double spreadY, double spreadZ, double minZ)
	{
		this.spreadX = spreadX;
		this.spreadY = spreadY;
		this.spreadZ = spreadZ;
		this.minZ = minZ;

		stars = new Star[numStars];
		for(int i = 0; i < stars.length; i++) {
			stars[i] = new Star(spreadX, spreadY, spreadZ, minZ);
		}
		//updateAndRender(null, 0.0f, null, 0, expectedStarWidth, expectedStarHeight, 1.0, null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, false, false, false);
	}

	/**
	 * Updates every star to a new position, and draws the starfield in a
	 * bitmap.
	 *
	 * @param target The bitmap to render to.
	 * @param delta  How much time has passed since the last update.
	 */
	public void updateAndRender(IRenderContext target, double delta, SpriteSheet sheet, int index, double spriteWidth, double spriteHeight, double transparency, Color color,
			double starSpeedX, double starSpeedY, double starSpeedZ, double starCenterX, double starCenterY, boolean wrapX, boolean wrapY, boolean wrapZ)
	{
		Arrays.sort(stars);
		final double tanHalfFOV = (double)Math.tan(Math.toRadians(90.0/2.0));
		for(int i = 0; i < stars.length; i++) {
			//Update the Star.

			//Move the star towards the camera which is at 0 on Z.
			stars[i].x += delta * starSpeedX;
			stars[i].y += delta * starSpeedY;
			stars[i].z += delta * starSpeedZ;

			//If star is at or behind the camera, generate a new position for
			//it.
			if(stars[i].z < minZ) {
				stars[i].init(spreadX, spreadY, spreadZ, minZ);
				if(wrapZ) {
					stars[i].z = spreadZ;
				}
			} else if(stars[i].z > spreadZ) {
				stars[i].init(spreadX, spreadY, spreadZ, minZ);
				if(wrapZ) {
					stars[i].z = minZ;
				}
			}

			//Render the Star.

			//Multiplying the position by (size/2) and then adding (size/2)
			//remaps the positions from range (-1, 1) to (0, size)

			//Division by z*tanHalfFOV moves things in to create a perspective effect.
			double perspectiveZ = stars[i].z * tanHalfFOV;
			double x = ((stars[i].x/perspectiveZ) + starCenterX);
			double y = ((stars[i].y/perspectiveZ) + starCenterY);
			double starWidth = spriteWidth/perspectiveZ;
			double starHeight = spriteHeight/perspectiveZ;


			//If the star is not within range of the screen, then generate a
			//new position for it.
			double startX = x-starWidth;
			double startY = y-starHeight;
			double endX = x+starWidth;
			double endY = y+starHeight;
			double threshold = 1.0;
			if(startY > threshold || endY < -threshold) {
				if(wrapY && ((startY > 1 && starSpeedY > 0) || (endY < -1 && starSpeedY < 0))) {
					double newStartX = startX;
					double newEndX = endX;
					do {
						stars[i].init(spreadX, spreadY, spreadZ, minZ);
						double newZ = (stars[i].z * tanHalfFOV);
						double newStarHeight = spriteHeight/newZ;
						double newStarWidth = spriteWidth/newZ;
						if(startY > 1) {
							stars[i].y = (-1.0 - starCenterY - newStarHeight)*newZ;
						} else if(endY < -1) {
							stars[i].y = (1.0 - starCenterY + newStarHeight)*newZ;
						}
						double newX = ((stars[i].x/(stars[i].z * tanHalfFOV)) + starCenterX);
						newStartX = newX-newStarWidth;
						newEndX = newX+newStarWidth;
					} while(newStartX > 1 || newEndX < - 1);
				} else { 
					stars[i].init(spreadX, spreadY, spreadZ, minZ);
				}
			} else if(startX > threshold || endX < -threshold) {
				if(wrapX && ((startX > 1 && starSpeedX > 0) || (endX < -1 && starSpeedX < 0))) {
					double newStartY = startY;
					double newEndY = endY;
					do {
						stars[i].init(spreadX, spreadY, spreadZ, minZ);
						double newZ = (stars[i].z * tanHalfFOV);
						double newStarHeight = spriteHeight/newZ;
						double newStarWidth = spriteWidth/newZ;
						if(startX > 1) {
							stars[i].x = (-1.0 - starCenterX - newStarWidth)*(stars[i].z * tanHalfFOV);
						} else if(endX < -1) {
							stars[i].x = (1.0 - starCenterX + newStarWidth)*(stars[i].z * tanHalfFOV);
						}
						double newY = ((stars[i].y/(stars[i].z * tanHalfFOV)) + starCenterY);
						newStartY = newY-newStarHeight;
						newEndY = newY+newStarHeight;
					} while(newStartY > 1 || newEndY < - 1);
				} else { 
					stars[i].init(spreadX, spreadY, spreadZ, minZ);
				}
			} else {
				//Otherwise, it is safe to draw this star to the screen.
				if(target != null) {
					target.drawSprite(sheet, index, startX, startY, endX, endY,transparency,false,false,color);
				}
			}
		}
	}
}
