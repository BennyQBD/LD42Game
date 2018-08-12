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

public class BulletSpawner extends EntityComponent {
	public static final int ID = IDAssigner.getId();
	private BulletSpawnVariance variance;
	private BulletSpawnProperties properties;
	private IEntityComponentAdder entityAdder;
	private double shootInterval;
	private double timeSinceShoot = 0.0;
	private double currentShootInterval;
	private double offsetX;
	private double offsetY;
	private double[] fireVectorsX;
	private double[] fireVectorsY;
	private AABB screenArea;
	
	private BulletSpawnerAimer bulletSpawnerAimer;

	private BulletSpawnerAimer getBulletSpawnerAimer() {
		if (bulletSpawnerAimer != null) {
			return bulletSpawnerAimer;
		}

		bulletSpawnerAimer = (BulletSpawnerAimer) getEntity().getComponent(
				BulletSpawnerAimer.ID);
		return bulletSpawnerAimer;
	}

	// Circle/spread shooter constructor
	public BulletSpawner(Entity entity, double fireVectorX, double fireVectorY, double startAngle, double endAngle, double shootInterval, BulletSpawnProperties properties, BulletSpawnVariance variance, IEntityComponentAdder entityAdder, AABB screenArea, int numBullets) {
		this(entity, fireVectorX, fireVectorY, startAngle, endAngle, shootInterval, 0, 0, 0, properties, variance, entityAdder, screenArea, numBullets);
	}

	public BulletSpawner(Entity entity, double fireVectorX, double fireVectorY, double startAngle, double endAngle, double shootInterval, double shootStartDelay, double offsetX, double offsetY, BulletSpawnProperties properties, BulletSpawnVariance variance, IEntityComponentAdder entityAdder, AABB screenArea, int numBullets) {
		super(entity, ID);
		double[] fireVectorsX = new double[numBullets];
		double[] fireVectorsY = new double[numBullets];

		double normConst = Math.sqrt(fireVectorX*fireVectorX+fireVectorY*fireVectorY);
		if(normConst != 0) {
			fireVectorX /= normConst;
			fireVectorY /= normConst;
		}
		
		if(numBullets == 1) {
			double angle = (startAngle+endAngle)/2.0;
			double cosa = Math.cos(angle);
			double sina = Math.sin(angle);
			fireVectorsX[0] = fireVectorX*cosa - fireVectorY*sina;
			fireVectorsY[0] = fireVectorX*sina + fireVectorY*cosa;
		} else {
			double step = (endAngle-startAngle)/(numBullets-1);
			double angle = startAngle;
			for(int i = 0; i < numBullets; i++) {
				double cosa = Math.cos(angle);
				double sina = Math.sin(angle);
				fireVectorsX[i] = fireVectorX*cosa - fireVectorY*sina;
				fireVectorsY[i] = fireVectorX*sina + fireVectorY*cosa;
				angle += step;
			}
		}

		init(entity, fireVectorsX, fireVectorsY, shootInterval, properties, variance, entityAdder, screenArea, shootStartDelay, offsetX, offsetY);
	}

	public BulletSpawner(Entity entity, double fireVectorX, double fireVectorY, double shootInterval, double shootStartDelay, double offsetX, double offsetY, BulletSpawnProperties properties, BulletSpawnVariance variance, IEntityComponentAdder entityAdder, AABB screenArea) {
		this(entity, new double[] {fireVectorX}, new double[] {fireVectorY}, shootInterval, shootStartDelay, offsetX, offsetY, properties, variance, entityAdder, screenArea);
	}

	public BulletSpawner(Entity entity, double[] fireVectorsX, double[] fireVectorsY, double shootInterval, double shootStartDelay, double offsetX, double offsetY, BulletSpawnProperties properties, BulletSpawnVariance variance, IEntityComponentAdder entityAdder, AABB screenArea) {
		super(entity, ID);
		init(entity, fireVectorsX, fireVectorsY, shootInterval, properties, variance, entityAdder, screenArea, shootStartDelay, offsetX, offsetY);
	}

	private final void init(Entity entity, double[] fireVectorsX, double[] fireVectorsY, double shootInterval, BulletSpawnProperties properties, BulletSpawnVariance variance, IEntityComponentAdder entityAdder, AABB screenArea, double shootStartDelay, double offsetX, double offsetY) {
		this.entityAdder = entityAdder;
		this.shootInterval = shootInterval;
		this.fireVectorsX = fireVectorsX;
		this.fireVectorsY = fireVectorsY;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.screenArea = screenArea;
		if(fireVectorsX.length != fireVectorsY.length) {
			throw new IllegalArgumentException();
		}
		this.variance = variance;
		this.properties = properties;
		generateNewShootInterval();

		for(int i = 0; i <fireVectorsX.length; i++) {
			double fireVectorX = fireVectorsX[i];
			double fireVectorY = fireVectorsY[i];
			double normConst = Math.sqrt(fireVectorX*fireVectorX+fireVectorY*fireVectorY);
			if(normConst != 0) {
				fireVectorX /= normConst;
				fireVectorY /= normConst;
			}
			fireVectorsX[i] = fireVectorX;
			fireVectorsY[i] = fireVectorY;
		}
		if(entityAdder == null) {
			this.entityAdder = new IEntityComponentAdder() {
				@Override
				public void addToEntity(Entity e) {}
			};
		}

		this.timeSinceShoot = -shootStartDelay;
	}

	private void generateNewShootInterval() {
		currentShootInterval = variance.randomizeShootInterval(shootInterval);
	}

	@Override
	public void update(double delta) {
		timeSinceShoot += delta;
		while(timeSinceShoot >= currentShootInterval) {
			timeSinceShoot -= currentShootInterval;
			generateNewShootInterval();
			properties.spawnBullet(variance, getEntity().getX() + offsetX, getEntity().getY() + offsetY,
					fireVectorsX, fireVectorsY, getBulletSpawnerAimer(), entityAdder, getEntity().getSpatialStructure(), timeSinceShoot, screenArea);
		}
	}
}
