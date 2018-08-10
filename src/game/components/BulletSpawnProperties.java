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

public class BulletSpawnProperties {
	private double acc;
	private double vel;
	private double gravityX;
	private double gravityY;
	private double perpVel;
	private double perpAcc;
	private double speedCap;
	private double despawnDelay;
	private SpriteSheet bulletSprite;

	public BulletSpawnProperties(double vel, double acc, double gravityX, double gravityY, double perpVel, double perpAcc, SpriteSheet bulletSprite) {
		this(vel, acc, gravityX, gravityY, perpVel, perpAcc, 2.0, 5.0, bulletSprite);
	}

	public BulletSpawnProperties(double vel, double acc, double gravityX, double gravityY, double perpVel, double perpAcc, double speedCap, double despawnDelay, SpriteSheet bulletSprite) {
		this.vel = vel;
		this.acc = acc;
		this.gravityX = gravityX;
		this.gravityY = gravityY;
		this.perpVel = perpVel;
		this.perpAcc = perpAcc;
		this.speedCap = speedCap;
		this.despawnDelay = despawnDelay;
		this.bulletSprite = bulletSprite;
	}

	public void spawnBullet(BulletSpawnVariance variance, double posX, double posY, double[] dirsX, double[] dirsY, BulletSpawnerAimer aimer, IEntityComponentAdder entityAdder, ISpatialStructure<Entity> spatialStructure, double initialDelta, AABB screenArea) {
		
		double angle = variance.randomizeAngle(0);
		double sina = Math.sin(angle);
		double cosa = Math.cos(angle);
		posX = variance.randomizePosX(posX);
		posY = variance.randomizePosY(posY);
		double effectiveVel = variance.randomizeVel(vel);
		double effectiveAcc = variance.randomizeAcc(acc);
		double effectivePerpAcc = variance.randomizePerpAcc(perpAcc);
		double effectivePerpVel = variance.randomizePerpVel(perpVel);

		for(int i = 0; i < dirsX.length; i++) {
			double dirX = dirsX[i];
			double dirY = dirsY[i];
			// Apply Random Rotation
			double oldDirX = dirX;
			dirX = dirX*cosa - dirY*sina;
			dirY = oldDirX*sina + dirY*cosa;

			// Find target to aim at
			double targetVectorX = 0.0;
			double targetVectorY = -1.0;

			if(aimer != null) {
				targetVectorX = aimer.getTargetDirX(posX, posY);
				targetVectorY = aimer.getTargetDirY(posX, posY);
			}

			// Rotate towards target
			oldDirX = dirX;
			dirX = -dirX*targetVectorY - dirY*targetVectorX;
			dirY = oldDirX*targetVectorX - dirY*targetVectorY;
			Entity bullet = createBullet(posX, posY,
						effectiveVel*dirX,effectiveVel*dirY,
						effectiveAcc*dirX+gravityX,effectiveAcc*dirY+gravityY,
						effectivePerpVel, effectivePerpAcc, despawnDelay, speedCap, spatialStructure, screenArea);
			entityAdder.addToEntity(bullet);
			bullet.update(initialDelta);
		}
	}

	private Entity createBullet(double posX, double posY, double velX, double velY, double accX, double accY, double perpVel, double perpAcc, double despawnDelay, double speedCap, ISpatialStructure<Entity> spatialStructure, AABB screenArea) {
		
		Entity e = createProjectile(posX, posY, velX, velY, accX, accY, perpVel, perpAcc, despawnDelay, speedCap, spatialStructure, screenArea);
//		ColliderComponent c = new ColliderComponent(e);
//		new HittableComponent(e,1);
//		new SpriteComponent(e, 0.02, 0.02, bulletSprite, 0, Color.WHITE);
//		if(bulletLight != null) {
//			new LightComponent(e, bulletLight, 0.08, 0.08, 0, 0);
//		}
//		AABB spriteAABB = bulletSprite.getAABB(0, 0.2, 0.2);
//		c.fitAABB(spriteAABB);
		return e;
	}

	private Entity createProjectile(double posX, double posY, double velX, double velY, double accX, double accY, double perpVel, double perpAcc, double despawnDelay, double speedCap, ISpatialStructure<Entity> spatialStructure, AABB screenArea) {
		Entity e = new Entity(spatialStructure, posX, posY, 0);
		new ProjectileComponent(e,velX,velY,accX,accY,perpVel, perpAcc,despawnDelay, speedCap, screenArea);
		return e;
	}
}
