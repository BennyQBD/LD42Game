package game.components;

import java.io.IOException;
import java.text.ParseException;
import java.util.Random;
import java.util.List;

import engine.audio.IAudioDevice;
import engine.components.ColliderComponent;
import engine.components.CollisionComponent;
import engine.components.LightComponent;
import engine.components.SpriteComponent;
import engine.components.FadeRemove;
import engine.components.RemoveComponent;
import engine.core.CoreEngine;
import engine.core.Scene;
import engine.core.entity.Entity;
import engine.core.entity.EntityComponent;
import engine.core.entity.IEntityVisitor;
import engine.input.ButtonAxis;
import engine.input.CompoundAxis;
import engine.input.CompoundButton;
import engine.input.IAxis;
import engine.input.IButton;
import engine.input.IInput;
import engine.input.JoystickAxis;
import engine.input.JoystickButton;
import engine.input.KeyButton;
import engine.rendering.Color;
import engine.rendering.IDisplay;
import engine.rendering.IRenderContext;
import engine.rendering.IRenderDevice;
import engine.rendering.LightMap;
import engine.rendering.SpriteSheet;
import engine.rendering.opengl.OpenGLDisplay;
import engine.space.AABB;
import engine.space.QuadTree;
import engine.space.Grid;
import engine.space.ISpatialStructure;
import engine.util.Debug;
import engine.util.factory.SpriteSheetFactory;
import engine.util.factory.TextureFactory;
import engine.util.IDAssigner;

public class BulletSpawnVariance {
	private static Random random = new CMWC4096();
	private double angleVariance;
	private double shootTimeVariance;
	private double posXVariance;
	private double posYVariance;
	private double velVariance;
	private double accVariance;
	private double perpVelVariance;
	private double perpAccVariance;

	public BulletSpawnVariance() {
		this(0,0,0,0,0,0,0,0);
	}

	public BulletSpawnVariance(double angleVariance, double shootTimeVariance, double posXVariance, double posYVariance, double velVariance, double accVariance, double perpVelVariance, double perpAccVariance) {
		this.angleVariance = angleVariance;
		this.shootTimeVariance = shootTimeVariance;
		this.posXVariance = posXVariance;
		this.posYVariance = posYVariance;
		this.velVariance = velVariance;
		this.accVariance = accVariance;
		this.perpVelVariance = perpVelVariance;
		this.perpAccVariance = perpAccVariance;
	}

	private double nextLogNorm(double scaleFactor) {
		return Math.exp(scaleFactor*random.nextGaussian());
	}

	private double normalizedRandomize(double var, double variance) {
		return var + variance*random.nextGaussian()/2.0;
	}

	private double logNormRandomize(double var, double variance) {
		return var*nextLogNorm(variance/2.0);
	}

	public double randomizeAngle(double angle) {
		return normalizedRandomize(angle, angleVariance);
	}

	public double randomizePosX(double posX) {
		return normalizedRandomize(posX, posXVariance);
	}

	public double randomizePosY(double posY) {
		return normalizedRandomize(posY, posYVariance);
	}

	public double randomizeVel(double vel) {
		return logNormRandomize(vel, velVariance);
	}

	public double randomizeAcc(double acc) {
		return logNormRandomize(acc, accVariance);
	}

	public double randomizePerpVel(double perpVel) {
		return normalizedRandomize(perpVel, perpVelVariance);
	}

	public double randomizePerpAcc(double perpAcc) {
		return normalizedRandomize(perpAcc, perpAccVariance);
	}

	public double randomizeShootInterval(double shootInterval) {
		return logNormRandomize(shootInterval, shootTimeVariance);
	}
}

