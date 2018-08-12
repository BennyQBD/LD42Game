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

public class Collectables {
	private long score = 0;
	private int lives;
	private double power;
	private double maxPower;
	private double screenShake = 0.0;

	private double initialMaxPower;
	private double initialPower;
	private int initialLives;

	public Collectables() {
		this(99.9, 1.0, 5);
	}
	
	public Collectables(double maxPower, double initialPower, int numStartingLives) {
		this.maxPower = maxPower;
		this.power = initialPower;
		this.lives = numStartingLives;

		this.initialMaxPower = this.maxPower;
		this.initialPower = this.power;
		this.initialLives = this.lives;
	}

	public void reset() {
		this.maxPower = this.initialMaxPower;
//		this.power = this.initialPower;
		this.lives = this.initialLives;
		this.score = 0;
	}

	public void addScore(long amt) {
		score += amt;
	}

	public void addScreenShake(double amt) {
		screenShake += amt;
	}

	public void multiplyScreenShake(double factorPerSecond, double delta) {
		screenShake *= Math.pow(factorPerSecond, delta);
		if (screenShake <= 0.0001) {
			screenShake = 0.0;
		}
	}

	public void addPower(double amt) {
		double newPower = power + amt;
		if(newPower < 0) {
			newPower = 0;
		}
		power = newPower;
	}

	public void addLives(int numLives) {
		this.lives += numLives;
	}

	public void decayPower(double factorPerSecond, double minPower, double delta) {
		if(power <= minPower) {
			return;
		}
		if(power > maxPower) {
			delta = delta*(1.0+(maxPower-power));
		}
		power = (power-minPower)*Math.pow(factorPerSecond, delta)+minPower;
	}

	public long getScore() {
		return score;
	}
	
	public int getLives() {
		return lives;
	}

	public double getPower() {
		return Math.min(Math.round(power*100)/100.0, maxPower);
	}

	public double getMaxPower() {
		return maxPower;
	}

	public double getScreenShake() {
		return screenShake;
	}
}
