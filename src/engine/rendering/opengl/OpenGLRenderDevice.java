/** 
 * Copyright (c) 2015, Benny Bobaganoosh. All rights reserved.
 * License terms are in the included LICENSE.txt file.
 */
package engine.rendering.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.EXTFramebufferObject.*;
import static org.lwjgl.opengl.ARBVertexBufferObject.*;
//import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;

import engine.rendering.ArrayBitmap;
import engine.rendering.Color;
import engine.rendering.IRenderDevice;

/**
 * A device that is capable of OpenGL rendering
 * 
 * @author Benny Bobaganoosh (thebennybox@gmail.com)
 */
public class OpenGLRenderDevice implements IRenderDevice {
	private class FramebufferData {
		public FramebufferData(int width, int height) {
			this.width = width;
			this.height = height;
		}

		private int width;
		private int height;
	}

	private class TextureData {
		public TextureData(int width, int height) {
			this.width = width;
			this.height = height;
		}

		private int width;
		private int height;
	}

	private final Map<Integer, FramebufferData> framebuffers = new HashMap<>();
	private final Map<Integer, TextureData> textures = new HashMap<>();
	private final List<Integer> releasedTextures = new ArrayList<Integer>();
	private final List<Integer> releasedFramebuffers = new ArrayList<Integer>();
	private int boundFbo;
	private int boundTex;
	private int rectVbo;
	private IRenderDevice.BlendMode currentMode = null;

	/**
	 * Creates a new OpenGLRenderDevice
	 * 
	 * @param width The width of the primary render target.
	 * @param height The height of the primary render target.
	 */
	public OpenGLRenderDevice(int width, int height) {
		boundFbo = -1;
		boundTex = -1;

		framebuffers.put(0, new FramebufferData(width, height));
		bindRenderTarget(0);
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_TEXTURE_2D);
		glEnable(GL_BLEND);

//		IntBuffer buffer = BufferUtils.createIntBuffer(1);
//		glGenBuffersARB(buffer);
//		rectVbo = buffer.get(0);
//		
//		float[] data = new float[] {
//			0.0f, 0.0f,
//			0.0f, 1.0f,
//			1.0f, 1.0f,
//			1.0f, 0.0f,
//		};
//		FloatBuffer vertexData = BufferUtils.createFloatBuffer(data.length*4);
//		for(int i = 0; i < data.length; i++) {
//			vertexData.put(data[i]);
//		}
//		vertexData.flip();
//		glBindBufferARB(GL_ARRAY_BUFFER_ARB, rectVbo);
//		glBufferDataARB(GL_ARRAY_BUFFER_ARB, vertexData, GL_STATIC_DRAW_ARB);
//
//		glEnableClientState(GL_VERTEX_ARRAY);
//		glEnableClientState(GL_TEXTURE_COORD_ARRAY);
//		glBindBufferARB(GL_ARRAY_BUFFER_ARB, rectVbo);
//		glVertexPointer(2, GL_FLOAT, 8, 0);
//		glTexCoordPointer(2, GL_FLOAT, 8, 0);
	}

	@Override
	public void cleanupResources() {
		synchronized(releasedTextures) {
			synchronized(releasedFramebuffers) {
				for(int i = 0; i < releasedTextures.size(); i++) {
					releaseActualTexture(releasedTextures.get(i));
				}
				for(int i = 0; i < releasedFramebuffers.size(); i++) {
					releaseActualRenderTarget(releasedFramebuffers.get(i));
				}
				releasedTextures.clear();
				releasedFramebuffers.clear();
			}
		}
	}

	@Override
	public void dispose() {
		cleanupResources();
		// Do nothing
	}

	@Override
	public int createTexture(int width, int height, ArrayBitmap image,
			int filter) {
		return createTexture(width, height, image, filter, GL_RGBA8);
	}

	@Override
	public int releaseTexture(int id) {
		if(id != 0) {
			synchronized(releasedTextures) {
				releasedTextures.add(id);
			}
		}
		return 0;
	}

	private int releaseActualTexture(int id) {
		if (id != 0) {
			glDeleteTextures(id);
			textures.remove(id);
			if (id == boundTex) {
				boundTex = -1;
			}
		}
		return 0;
	}

	@Override
	public ArrayBitmap getTexture(int id, int x, int y, int width, int height) {
		int[] dest = new int[width * height];
		TextureData tex = textures.get(id);
		ByteBuffer buffer = BufferUtils.createByteBuffer(tex.width * tex.height
				* 4);
		bindTexture(id);
		glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

		dest = byteBufferToInt(dest, buffer, tex.width, tex.height);
		return new ArrayBitmap(width, height, dest, x, y, width);
	}

	@Override
	public int createRenderTarget(int width, int height, int texId) {
		int fbo = glGenFramebuffersEXT();
		FramebufferData data = new FramebufferData(width, height);
		framebuffers.put(fbo, data);
		if (texId != 0 && texId != -1) {
			bindRenderTarget(fbo);
			glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT,
					GL_COLOR_ATTACHMENT0_EXT, GL_TEXTURE_2D, texId, 0);
		}
		return fbo;
	}

	@Override
	public int releaseRenderTarget(int fbo) {
		if(fbo != 0 && fbo != -1) {
			synchronized(releasedFramebuffers) {
				releasedFramebuffers.add(fbo);
			}
		}
		return 0;
	}

	private int releaseActualRenderTarget(int fbo) {
		if (fbo != 0 && fbo != -1) {
			glDeleteFramebuffersEXT(fbo);
			framebuffers.remove(fbo);
			if (fbo == boundFbo) {
				boundFbo = -1;
			}
		}
		return 0;
	}

	@Override
	public void clear(int fbo, Color color) {
		bindRenderTarget(fbo);
		glClearColor((float) color.getRed(), (float) color.getGreen(),
				(float) color.getBlue(), (float) color.getAlpha());
		glClear(GL_COLOR_BUFFER_BIT);
	}

	@Override
	public void drawRect(int fbo, int texId, BlendMode mode, double startX,
			double startY, double endX, double endY, double texStartX,
			double texStartY, double texEndX, double texEndY, Color c,
			double transparency) {
		bindRenderTarget(fbo);

		glColor4f((float) c.getRed(), (float) c.getGreen(),
				(float) c.getBlue(), (float) (c.getAlpha() * transparency));
		setBlendMode(mode);
		bindTexture(texId);

//		glMatrixMode(GL_TEXTURE);
//		glLoadIdentity();
//		glTranslatef((float)texStartX, (float)texStartY, 0.0f);
//		glScalef((float)(texEndX-texStartX),(float)(texEndY-texStartY),1.0f);
//
//		glMatrixMode(GL_MODELVIEW);
//		glLoadIdentity();
//		glTranslatef((float)startX, (float)startY, 0.0f);
//		glScalef((float)(endX-startX),(float)(endY-startY),1.0f);
//		
//		glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

		// Immediate Mode Performance: 7.066068789698165 ms per frame (141.52140741368527 fps); Standard Deviation of render time for a given frame: 4.896560383291652 ms (Worst Plausible Lag Spike: 31.54887070615642 ms)
		// VBO Performance: 5.09177794778067 ms per frame (196.39505301598342 fps); Standard Deviation of render time for a given frame: 4.129456077751917 ms (Worst Plausible Lag Spike: 25.739058336540257 ms)
		// Immediate Mode w/o transform pipeline: 2.930125249402049 ms per frame (341.28233945087163 fps); Standard Deviation of render time for a given frame: 3.3345218007886004 ms (Worst Plausible Lag Spike: 19.60273425334505 ms)

		glBegin(GL_TRIANGLE_FAN);
		{
			glTexCoord2f((float) texStartX, (float) texStartY);
			glVertex2f((float) startX, (float) startY);
			glTexCoord2f((float) texStartX, (float) texEndY);
			glVertex2f((float) startX, (float) endY);
			glTexCoord2f((float) texEndX, (float) texEndY);
			glVertex2f((float) endX, (float) endY);
			glTexCoord2f((float) texEndX, (float) texStartY);
			glVertex2f((float) endX, (float) startY);
		}
		glEnd();


		
//		glBegin(GL_TRIANGLE_FAN);
//		{
//			//glTexCoord2f((float) texStartX, (float) texStartY);
//			glTexCoord2f(0.0f, 0.0f);
//			glVertex2f(0.0f, 0.0f);
//			//glVertex2f((float) startX, (float) startY);
//			//glTexCoord2f((float) texStartX, (float) texEndY);
//			glTexCoord2f(0.0f, 1.0f);
//			glVertex2f(0.0f, 1.0f);
//			//glVertex2f((float) startX, (float) endY);
//			//glTexCoord2f((float) texEndX, (float) texEndY);
//			glTexCoord2f(1.0f, 1.0f);
//			glVertex2f(1.0f, 1.0f);
//			//glVertex2f((float) endX, (float) endY);
//			//glTexCoord2f((float) texEndX, (float) texStartY);
//			glTexCoord2f(1.0f, 0.0f);
//			glVertex2f(1.0f, 0.0f);
//			//glVertex2f((float) endX, (float) startY);
//		}
//		glEnd();
	}

	private void setBlendMode(IRenderDevice.BlendMode mode) {
		if(mode == currentMode) {
			return;
		}
		currentMode = mode;
		switch (mode) {
		case ADD_LIGHT:
			glBlendFunc(GL_ONE, GL_ONE);
			break;
		case APPLY_LIGHT:
			glBlendFunc(GL_DST_COLOR, GL_ZERO);
			break;
		case SPRITE:
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			break;
		}
	}

	private void bindRenderTarget(int fbo) {
		if (fbo == boundFbo) {
			return;
		}
		FramebufferData data = framebuffers.get(fbo);
		glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, fbo);
		boundFbo = fbo;

		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glMatrixMode(GL_MODELVIEW);

		glViewport(0, 0, data.width, data.height);
	}

	private void bindTexture(int texId) {
		if (texId == boundTex) {
			return;
		}
		glBindTexture(GL_TEXTURE_2D, texId);
		boundTex = texId;
	}

	private int createTexture(int width, int height, ArrayBitmap image,
			int filter, int format) {
		int id = glGenTextures();
		bindTexture(id);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
		glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, GL_RGBA,
				GL_UNSIGNED_BYTE, makeRGBABuffer(image));
		textures.put(id, new TextureData(width, height));
		return id;
	}

	private static ByteBuffer makeRGBABuffer(ArrayBitmap image) {
		if (image == null) {
			return null;
		}
		final ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth()
				* image.getHeight() * 4);
		image.visitAll(new ArrayBitmap.IVisitor() {
			@Override
			public void visit(int x, int y, int pixel) {
				buffer.put(Color.getARGBComponent(pixel, 1));
				buffer.put(Color.getARGBComponent(pixel, 2));
				buffer.put(Color.getARGBComponent(pixel, 3));
				buffer.put(Color.getARGBComponent(pixel, 0));
			}
		});
		buffer.flip();
		return buffer;
	}

	private static int[] byteBufferToInt(int[] data, ByteBuffer buffer,
			int width, int height) {
		for (int i = 0; i < width * height; i++) {
			int r = buffer.get() & 0xFF;
			int g = buffer.get() & 0xFF;
			int b = buffer.get() & 0xFF;
			int a = buffer.get() & 0xFF;
			data[i] = Color.makeARGB(a, r, g, b);
		}
		return data;
	}
}
