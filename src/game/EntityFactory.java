package game;

import java.io.IOException;
import java.text.ParseException;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import game.components.*;
import engine.audio.IAudioDevice;
import engine.audio.Sound;
import engine.audio.SoundData;
import engine.audio.AudioUtil;
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
import engine.util.Util;
import engine.util.factory.SpriteSheetFactory;
import engine.util.factory.TextureFactory;
import engine.util.factory.SoundFactory;
import engine.util.IDAssigner;


public class EntityFactory {
	private class EnemyMaker implements IEntityMaker {
		private int[] hittableComponentTypes;
		private IEntityComponentAdder bulletType;
		private int numCollectables;
		private int spriteIndex;
		private List<Entity> bulletMoveLocations;
		private IEntityComponentAdder additionalComponents;

		public EnemyMaker(int[] hittableComponentTypes, IEntityComponentAdder bulletType, int spriteIndex, List<Entity> bulletMoveLocations,
				IEntityComponentAdder additionalComponents) {
			this.hittableComponentTypes = hittableComponentTypes;
			this.bulletType = bulletType;
			this.numCollectables = numCollectables;
			this.spriteIndex = spriteIndex;
			this.bulletMoveLocations = bulletMoveLocations;
			this.additionalComponents = additionalComponents;
		}
		// rightWallX, 1.1, -0.3, -0.35, -0.1, 3.0, 0.005
		@Override
		public Entity makeEntity(Entity entity, double[] params) {
			int paramIndex = 0;
			double startX = params[paramIndex++];
			double startY = params[paramIndex++];
			double velX = params[paramIndex++];
			double velY = params[paramIndex++];
			double perpVel = params[paramIndex++];
			double perpAcc = params[paramIndex++];
			double bulletSpeed = params[paramIndex++];
			double shootInterval = params[paramIndex++];
			double shootVariance = params[paramIndex++];
			double startAngle = params[paramIndex++];
			double endAngle = params[paramIndex++];
			int numBullets = (int)params[paramIndex++];
			double screenShakeAmount = params[paramIndex++];
			double health = params[paramIndex++];
			int numCollectables = (int)params[paramIndex++];

			Entity e = new Entity(spatialStructure, startX, startY, 0);
			new ProjectileComponent(e,velX, velY, 0, 0, perpVel, perpAcc, 0.5, 2.0, screenArea);
			new ColliderComponent(e);
			new SpriteComponent(e, 0.075, 0.075, primarySheet, spriteIndex, Color.WHITE);
			new BulletSpawner(e, 0, -1, startAngle, endAngle, shootInterval, 0, 0, -0.075,
							new BulletSpawnProperties(bulletSpeed, 0, 0, 0, 0, 0, primarySheet),
							new BulletSpawnVariance(0,shootVariance,0,0,0,0,0,0), bulletType, screenArea, numBullets);
			new BulletSpawnerAimer(e,player);
			new EnemyComponent(e, collectables, health, screenShakeAmount, hittableComponentTypes, collectableSpawner, new double[] {0.01, 0.02}, numCollectables, bulletMoveLocations);
			new InvalidAreaRemove(e, screenArea);
			if(additionalComponents != null) {
				additionalComponents.addToEntity(e);
			}
			return e;
		}
	}

	public IEntityMaker boss = new IEntityMaker() {
		@Override
		public Entity makeEntity(Entity entity, double[] params) {
			// NOTE: THIS IS TERRIBLE CODE!!!!
			TestScene.screenLighting = 0.5;
			IEntityComponentAdder bulletType = bossBullet;
			SpriteSheet sprites = primarySheet;
			double screenCenterX = 0;
			double bossLocX = 0;
			double bossLocY = 0.5;
			int numCollectablesPerPhase = 250;

			// Could use!
			IEntityComponentAdder circle1 = new IEntityComponentAdder() {
				@Override
				public void addToEntity(Entity e) {
//					new DelayedRemove(e, params[0]);
					new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
					BulletSpawnVariance var = new BulletSpawnVariance(0.05,0,0.1,0.1,0,0,0,0);
					double spawnSpeed = 0.2;
					double sqrt2o2 = Math.sqrt(2.0)/8.0;
					double perpAcc = -0.125;
					double vel = 0.2;
					double startAngle = 0;
					double endAngle = Math.PI*2;
					int numBullets = 30;
					new BulletSpawner(e, 0, -1, startAngle, endAngle, spawnSpeed,
							new BulletSpawnProperties(vel, 0, 0, 0, 0, 0, sprites),
							var, bulletType, screenArea, numBullets);
					new BulletSpawnerAimer(e, player);
				}
			};

			// If used, needs rework.
			IEntityComponentAdder fourLeafClover = new IEntityComponentAdder() {
				@Override
				public void addToEntity(Entity e) {
	//				Entity e = new Entity(getStructure(), screenCenterX, -0.25, 0);
	//				new DelayedRemove(e, params[0]);
	//				new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
	//				BulletSpawnVariance var = new BulletSpawnVariance(0,0,0,0,0,0,1.0,0.01);
	//				double spawnSpeed = 0.1;
	//				double sqrt2o2 = Math.sqrt(2.0)/2.0;
	//				new BulletSpawner(e, -1, 0, spawnSpeed,
	//						new BulletSpawnProperties(0.5, 0, 0, 0, -1, 0, 2.0, 2, sprites),
	//						var);
	//				//double posX, double posY, double velX, double velY, double accX, double accY, double perpVel, double perpAcc, double despawnDelay, double speedCap
	//				//new BulletSpawnerAimer(e, createProjectile(screenCenterX, -0.25, -0.5, 0, 0, 0, -1, 0, 2, 0.5));
	//				return e;
					new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
					BulletSpawnVariance var = new BulletSpawnVariance(0,0,0,0,0,0,0,0);
					double spawnSpeed = 0.075;
					double speed = 0.4;
					double speed2 = 0.4;
					double sqrt2o2 = Math.sqrt(speed*speed*2);//Math.sqrt(2.0)/2.0;
					double startAngle = -0.4;
					double endAngle = 0.4;
					int numBullets = 2;
					new BulletSpawner(e, sqrt2o2, sqrt2o2, startAngle, endAngle, spawnSpeed,
							new BulletSpawnProperties(speed2, 0, speed, speed, 0, -1, sprites),
							var, bulletType, screenArea, numBullets);
					new BulletSpawner(e, -sqrt2o2, sqrt2o2, startAngle, endAngle, spawnSpeed,
							new BulletSpawnProperties(speed2, 0, -speed, speed, 0, -1, sprites),
							var, bulletType, screenArea, numBullets);
					new BulletSpawner(e, sqrt2o2, -sqrt2o2, startAngle, endAngle, spawnSpeed,
							new BulletSpawnProperties(speed2, 0, speed, -speed, 0, -1, sprites),
							var, bulletType, screenArea, numBullets);
					new BulletSpawner(e, -sqrt2o2, -sqrt2o2, startAngle, endAngle, spawnSpeed,
							new BulletSpawnProperties(speed2, 0, -speed, -speed, 0, -1, sprites),
							var, bulletType, screenArea, numBullets);

	//				new BulletSpawner(e, -1, 0, startAngle, endAngle, numBullets, spawnSpeed,
	//						new BulletSpawnProperties(0.5, 0, sqrt2o2, 0, 0, -1, sprites),
	//						var);
	//				new BulletSpawner(e, 0, -1, startAngle, endAngle, numBullets, spawnSpeed,
	//						new BulletSpawnProperties(0.5, 0, 0, sqrt2o2, 0, -1, sprites),
	//						var);
	//				new BulletSpawner(e, 0, 1, startAngle, endAngle, numBullets, spawnSpeed,
	//						new BulletSpawnProperties(0.5, 0, 0, -sqrt2o2, 0, -1, sprites),
	//						var);
					new BulletSpawnerAimer(e, createProjectile(e.getX(), e.getY()-0.25, 0.5, 0, 0, 0, 1, 0, 2, 0.5));
				}
			};

			// Use. Good challenging dynamic
			IEntityComponentAdder circle3 = new IEntityComponentAdder() {
				@Override
				public void addToEntity(Entity e) {
					new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
					BulletSpawnVariance var = new BulletSpawnVariance(0,0,0,0,0.1,0,0.04,0.004);
					double spawnSpeed = 1.2;
					double sqrt2o2 = Math.sqrt(2.0)/8.0;
					double perpAcc = -0.125;
					double vel = 0.2;
					double acc = -0.025;
					double startAngle = 0;
					double endAngle = Math.PI*2;
					int numBullets = 20;
					new BulletSpawner(e, 0, -1, startAngle, endAngle, spawnSpeed, 0.0, 0.0, 0.0,
							new BulletSpawnProperties(vel/1.2, acc, 0, 0, 0.1, -0.01, sprites),
							var, bulletType, screenArea, numBullets);
					new BulletSpawner(e, 0, -1, startAngle, endAngle, spawnSpeed, 0.0, 0.0, 0.0,
							new BulletSpawnProperties(vel*1.2, acc, 0, 0, -0.1, 0.01, sprites),
							var, bulletType, screenArea, numBullets);
					new BulletSpawner(e, 0, -1, startAngle, endAngle, spawnSpeed, 0.0, 0.0, 0.0,
							new BulletSpawnProperties(vel, acc, 0, 0, 0, 0, sprites),
							var, bulletType, screenArea, numBullets);
					}
			};

			// Maybe. Doable if I want the circle theme
			IEntityComponentAdder circle4 = new IEntityComponentAdder() {
				@Override
				public void addToEntity(Entity e) {
					new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
					BulletSpawnVariance var = new BulletSpawnVariance(0,0,0,0,00,0,0.001,0.001);
					double spawnSpeed = 1.2;
					double sqrt2o2 = Math.sqrt(2.0)/8.0;
					double perpAcc = -0.125;
					double vel = 0.2;
					double acc = 0;
					double startAngle = 0;
					double endAngle = Math.PI*2;
					int numBullets = 60;
					double angleOffset = (endAngle/numBullets)/16.0;///8.0;
					new BulletSpawner(e, 0, -1, startAngle, endAngle, spawnSpeed, 0.0, 0.0, 0.0,
							new BulletSpawnProperties(vel, acc, 0, 0, 0.1, -0.01, sprites),
							var, bulletType, screenArea, numBullets);
					new BulletSpawner(e, 0, -1, startAngle+angleOffset, endAngle+angleOffset, spawnSpeed, spawnSpeed/2.0, 0.0, 0.0,
							new BulletSpawnProperties(vel, acc, 0, 0, 0.1, -0.01, sprites),
							var, bulletType, screenArea, numBullets);
				}
			};

			IEntityComponentAdder circle5 = new IEntityComponentAdder() {
				@Override
				public void addToEntity(Entity e) {
					new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
					BulletSpawnVariance var = new BulletSpawnVariance(1.0,0,0,0,0,0,0.0,0.0);
					double spawnSpeed = 0.8;
					double sqrt2o2 = Math.sqrt(2.0)/8.0;
					double vel = 0.2;
					double acc = 0;
					double startAngle = 0;
					double endAngle = Math.PI*2;
					int numBullets = 16;
					double angleOffset = (endAngle/numBullets)/16.0;///8.0;
					new BulletSpawner(e, 0, -1, startAngle, endAngle, spawnSpeed, 0.0, 0.0, 0.0,
							new BulletSpawnProperties(vel, acc, 0, 0, 0, 0.0, sprites),
							var, bulletType, screenArea, numBullets);
					new BulletSpawner(e, 0, -1, startAngle+angleOffset, endAngle+angleOffset, spawnSpeed, spawnSpeed/2.0, 0.0, 0.0,
							new BulletSpawnProperties(vel, acc, 0, 0, 0, 0.0, sprites),
							var, bulletType, screenArea, numBullets);
				}
			};

			IEntityComponentAdder circle6 = new IEntityComponentAdder() {
				@Override
				public void addToEntity(Entity e) {
					new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
					BulletSpawnVariance var = new BulletSpawnVariance(0,0,0,0,0,0,0.0,0.0);
					double spawnSpeed = 0.6;
					double sqrt2o2 = Math.sqrt(2.0)/8.0;
					double vel = 0.4;
					double acc = 0;
					double startAngle = -1.0;
					double endAngle = 1.0;
					int numBullets = 8;
					double angleOffset = (endAngle/numBullets)/16.0;///8.0;
					new BulletSpawner(e, 0, -1, startAngle, endAngle, spawnSpeed, 0.0, 0.0, 0.0,
							new BulletSpawnProperties(vel, acc, 0, 0, 0, 0.0, sprites),
							var, bulletType, screenArea, numBullets);
					new BulletSpawner(e, 0, -1, startAngle+angleOffset, endAngle+angleOffset, spawnSpeed, spawnSpeed/2.0, 0.0, 0.0,
							new BulletSpawnProperties(vel, acc, 0, 0, 0, 0.0, sprites),
							var, bulletType, screenArea, numBullets);
					
				}
			};

			IEntityComponentAdder circle7 = new IEntityComponentAdder() {
				@Override
				public void addToEntity(Entity e) {
					new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
					BulletSpawnVariance var = new BulletSpawnVariance(0,1.0,0,0,1.0,0,0.0,0.0);
					double spawnSpeed = 0.3;
					double sqrt2o2 = Math.sqrt(2.0)/8.0;
					double vel = 0.3;
					double acc = 0;
					double startAngle = -1.0;
					double endAngle = 1.0;
					int numBullets = 3;
					double angleOffset = (endAngle/numBullets)/16.0;///8.0;
					new BulletSpawner(e, 0, 1, startAngle, endAngle, spawnSpeed, 0.0, 0.0, 0.0,
							new BulletSpawnProperties(vel, acc, 0, -0.35, 0, 0.0, sprites),
							var, bulletType, screenArea, numBullets);
					new BulletSpawner(e, 0, 1, startAngle+angleOffset, endAngle+angleOffset, spawnSpeed, spawnSpeed/2.0, 0.0, 0.0,
							new BulletSpawnProperties(vel, acc, 0, -0.35, 0, 0.0, sprites),
							var, bulletType, screenArea, numBullets);
				}
			};

			IEntityComponentAdder circle8 = new IEntityComponentAdder() {
				@Override
				public void addToEntity(Entity e) {
					new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
					BulletSpawnVariance var = new BulletSpawnVariance(1.0,0,0,0,0,0,0,0);
					double spawnSpeed = 3.0;
					double sqrt2o2 = Math.sqrt(2.0)/8.0;
					double vel = 0.4;
					double acc = 0;
					double startAngle = 0.0;
					double endAngle = Math.PI*2.0;
					int numBullets = 16;
					double angleOffset = (endAngle/numBullets)/16.0;///8.0;
					new BulletSpawner(e, 0, -1, startAngle, endAngle, spawnSpeed, 0.0, 0.0, 0.0,
							new BulletSpawnProperties(vel, acc, 0, 0, 0.15, -0.025, sprites),
							var, bigBossBullet, screenArea, numBullets);
					new BulletSpawner(e, 0, -1, startAngle+angleOffset, endAngle+angleOffset, spawnSpeed, spawnSpeed/2.0, 0.0, 0.0,
							new BulletSpawnProperties(vel, acc, 0, 0, -0.15, 0.025, sprites),
							var, bigBossBullet, screenArea, numBullets);
				}
			};

			IEntityComponentAdder circle9 = new IEntityComponentAdder() {
				@Override
				public void addToEntity(Entity e) {
					new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
					BulletSpawnVariance var = new BulletSpawnVariance(0.0,0,0,0,0,0,0,0);
					double spawnSpeed = 0.5;
					double sqrt2o2 = Math.sqrt(2.0)/8.0;
					double vel = 0.4;
					double acc = 0;
					double startAngle = 0.0;
					double endAngle = Math.PI*2.0;
					int numBullets = 32;
					double angleOffset = (endAngle/numBullets)/16.0;///8.0;
					new BulletSpawner(e, 0, -1, startAngle, endAngle, spawnSpeed, 0.0, 0.0, 0.0,
							new BulletSpawnProperties(vel, acc, 0, 0, 0.025, 0.0, sprites),
							var, middleBossBullet, screenArea, numBullets);
					new BulletSpawner(e, 0, -1, startAngle+angleOffset, endAngle+angleOffset, spawnSpeed, spawnSpeed/2.0, 0.0, 0.0,
							new BulletSpawnProperties(vel, acc, 0, 0, -0.025, 0.0, sprites),
							var, middleBossBullet, screenArea, numBullets);
				}
			};


			IEntityComponentAdder baseBossEntity = new IEntityComponentAdder() {
				@Override
				public void addToEntity(Entity e) {
					new ColliderComponent(e);
					new SpriteComponent(e, 0.3, 0.3, bossSheet, 4, Color.WHITE);
				}
			};

			IEntityMaker finalPhaseMaker = new IEntityMaker() {
				@Override
				public Entity makeEntity(Entity entity, double[] params) {
					Entity e = new Entity(spatialStructure, entity.getX(), entity.getY(), 0);
					baseBossEntity.addToEntity(e);
					new BulletSpawnerAimer(e, player);
					new RandomAreaMovement(e, 0.6, new AABB(-1.0, 0.2, 1.0, 1.0));
					circle5.addToEntity(e);	
					for(int i = 0; i < numCollectablesPerPhase; i++) {
						collectableSpawner2.makeEntity(e, params);
					}
					new EnemyComponent(e, collectables, 2250.0, 0.1, new int[] {HittableComponent.TYPE_ENEMY_HAZARD}, new IEntityMaker() {
						@Override
						public Entity makeEntity(Entity entity, double[] params) {
							Entity e = new Entity(spatialStructure, entity.getX(), entity.getY(), 0);
							baseBossEntity.addToEntity(e);
							new BulletSpawnerAimer(e, player);
							new RandomAreaMovement(e, 0.6, new AABB(-1.0, 0.2, 1.0, 1.0));
							circle5.addToEntity(e);
							circle6.addToEntity(e);
							for(int i = 0; i < numCollectablesPerPhase; i++) {
								collectableSpawner2.makeEntity(e, params);
							}
							new EnemyComponent(e, collectables, 1900.0, 0.1, new int[] {HittableComponent.TYPE_ENEMY_HAZARD}, new IEntityMaker() {
								@Override
								public Entity makeEntity(Entity entity, double[] params) {
									Entity e = new Entity(spatialStructure, entity.getX(), entity.getY(), 0);
									baseBossEntity.addToEntity(e);
									new BulletSpawnerAimer(e, player);
									new RandomAreaMovement(e, 0.6, new AABB(-1.0, 0.2, 1.0, 1.0));
									circle5.addToEntity(e);
									circle6.addToEntity(e);
									circle8.addToEntity(e);
									for(int i = 0; i < numCollectablesPerPhase; i++) {
										collectableSpawner2.makeEntity(e, params);
									}
									new EnemyComponent(e, collectables, 1750.0, 0.1, new int[] {HittableComponent.TYPE_ENEMY_HAZARD}, new IEntityMaker() {
										@Override
										public Entity makeEntity(Entity entity, double[] params) {
											Entity e = new Entity(spatialStructure, entity.getX(), entity.getY(), 0);
											baseBossEntity.addToEntity(e);
											new BulletSpawnerAimer(e, player);
											new RandomAreaMovement(e, 0.6, new AABB(-1.0, 0.2, 1.0, 1.0));
											circle5.addToEntity(e);
											circle6.addToEntity(e);
											circle8.addToEntity(e);
											circle7.addToEntity(e);
											for(int i = 0; i < numCollectablesPerPhase; i++) {
												collectableSpawner2.makeEntity(e, params);
											}
											new EnemyComponent(e, collectables, 1600.0, 0.1, new int[] {HittableComponent.TYPE_ENEMY_HAZARD}, new IEntityMaker() {
												@Override
												public Entity makeEntity(Entity entity, double[] params) {
													Entity e = new Entity(spatialStructure, entity.getX(), entity.getY(), 0);
													baseBossEntity.addToEntity(e);
													new BulletSpawnerAimer(e, player);
													new RandomAreaMovement(e, 0.6, new AABB(-1.0, 0.2, 1.0, 1.0));
													circle5.addToEntity(e);
													circle6.addToEntity(e);
													circle8.addToEntity(e);
													circle7.addToEntity(e);
													circle9.addToEntity(e);
													for(int i = 0; i < numCollectablesPerPhase; i++) {
														collectableSpawner2.makeEntity(e, params);
													}
													new EnemyComponent(e, collectables, 1400.0, 0.1, new int[] {HittableComponent.TYPE_ENEMY_HAZARD}, new IEntityMaker() {
														@Override
														public Entity makeEntity(Entity entity, double[] params) {
															Entity e = new Entity(spatialStructure, entity.getX(), entity.getY(), 0);
															for(int i = 0; i < numCollectablesPerPhase; i++) {
																collectableSpawner2.makeEntity(e, params);
															}
															TestScene.isGameWon = true;
															return e;
														}
													}, new double[] {0.01, 0.02, 10.0}, 1, null);
													return e;
												}
											}, new double[] {0.01, 0.02, 9.0}, 1, null);
											return e;
										}
									}, new double[] {0.01, 0.02, 8.0}, 1, null);
									return e;
								}
							}, new double[] {0.01, 0.02, 7.0}, 1, null);
							return e;
						}
					}, new double[] {0.01, 0.02, 6.0}, 1, null);
					return e;
				}
			};
			
			Entity e = new Entity(spatialStructure, bossLocX, bossLocY, 0);
			baseBossEntity.addToEntity(e);
			circle1.addToEntity(e);
			new RandomAreaMovement(e, 0.3, new AABB(-1.0, 0.2, 1.0, 1.0));
			new EnemyComponent(e, collectables, 3000.0, 0.1, new int[] {HittableComponent.TYPE_ENEMY_HAZARD}, new IEntityMaker() {
				@Override
				public Entity makeEntity(Entity entity, double[] params) {
					Entity e = new Entity(spatialStructure, bossLocX, bossLocY, 0);
					baseBossEntity.addToEntity(e);
					fourLeafClover.addToEntity(e);
					for(int i = 0; i < numCollectablesPerPhase; i++) {
						collectableSpawner2.makeEntity(e, params);
					}
					new EnemyComponent(e, collectables, 5000.0, 0.1, new int[] {HittableComponent.TYPE_ENEMY_HAZARD}, new IEntityMaker() {
						@Override
						public Entity makeEntity(Entity entity, double[] params) {
							Entity e = new Entity(spatialStructure, entity.getX(), entity.getY(), 0);
							baseBossEntity.addToEntity(e);
							circle3.addToEntity(e);
							for(int i = 0; i < numCollectablesPerPhase; i++) {
								collectableSpawner2.makeEntity(e, params);
							}
							new EnemyComponent(e, collectables, 7500.0, 0.1, new int[] {HittableComponent.TYPE_ENEMY_HAZARD}, new IEntityMaker() {
								@Override
								public Entity makeEntity(Entity entity, double[] params) {
									Entity e = new Entity(spatialStructure, entity.getX(), entity.getY(), 0);
									new RandomAreaMovement(e, 0.3, new AABB(-1.0, 0.2, 1.0, 1.0));
									baseBossEntity.addToEntity(e);
									circle4.addToEntity(e);
									for(int i = 0; i < numCollectablesPerPhase; i++) {
										collectableSpawner2.makeEntity(e, params);
									}
									new EnemyComponent(e, collectables, 3500.0, 0.1, new int[] {HittableComponent.TYPE_ENEMY_HAZARD}, finalPhaseMaker, new double[] {0.01, 0.02}, 1, null);
									return e;
								}
							}, new double[] {0.01, 0.02, 5.0}, 1, null);
							return e;
						}
					}, new double[] {0.01, 0.02, 3.0}, 1, null);
					return e;
				}
			}, new double[] {0.01, 0.02, 2.0}, 1, null);
			return e;
		}
	};

	private Entity createProjectile(double posX, double posY, double velX, double velY, double accX, double accY, double perpVel, double perpAcc, double despawnDelay, double speedCap) {
		Entity e = new Entity(spatialStructure, posX, posY, 0);
		new ProjectileComponent(e,velX,velY,accX,accY,perpVel, perpAcc,despawnDelay, speedCap, screenArea);
		return e;
	}

	private ISpatialStructure<Entity> spatialStructure;
	private AABB screenArea;
	private SpriteSheet primarySheet;
	private SpriteSheet bossSheet;
	private Entity player = null;
	private Collectables collectables;
	private LightMap bulletLight;
	private List<Entity> whiteHoleList = new ArrayList<>();
	private List<Entity> playerList = new ArrayList<>();

	public EntityFactory(ISpatialStructure<Entity> structure, AABB screenArea, SpriteSheet primarySheet, SpriteSheet bossSheet, Collectables collectables, IRenderDevice device) {
		this.spatialStructure = structure;
		this.screenArea = screenArea;
		this.primarySheet = primarySheet;
		this.bossSheet = bossSheet;
		this.player = player;
		this.collectables = collectables;
		this.bulletLight = new LightMap(device, 16, new Color(1.0, 0.8, 0.8));
	}

	public void setPlayer(Entity player) {
		playerList.clear();
		this.player = player;
		playerList.add(player);
	}

	public final IEntityMaker playerShot = new IEntityMaker() {
			@Override
			public Entity makeEntity(Entity entity, double[] params) {
				double slowDownAmt = params[1];
				Color playerShotColor = new Color(Util.lerp(0.0, 1.0, params[2]), 1.0, 1.0);
				IEntityMaker shotMaker = new IEntityMaker() {
					@Override
					public Entity makeEntity(Entity entity, double[] params) {
						Entity e = new Entity(spatialStructure, entity.getX()+params[0], entity.getY()+params[1], 0);
						new ProjectileComponent(e,params[2], 1.0, 0, 2.0, 0, 0, 0.5, 2.0, screenArea);
						new ColliderComponent(e);
						new SpriteComponent(e, 0.03, 0.03, primarySheet, 0, playerShotColor).setTransparency(0.5);
						new InvalidAreaRemove(e, screenArea);
						new HittableComponent(e, HittableComponent.TYPE_ENEMY_HAZARD, 5.0-slowDownAmt);
						return e;
					}
				};
				double shotXSpread = 0.05;
				double shotYSpread = 0.05;
				double shotXVel = 0.1;
				if(params[1] == 1.0) {
					shotXSpread /= 2.0;
					shotXVel /= 4.0;
					shotYSpread -= 0.1;
				}
				if(params[0] < 1.0) {
					shotMaker.makeEntity(entity, new double[] {+shotXSpread*3.0/5.0, 0, 0});
					return shotMaker.makeEntity(entity, new double[] {-shotXSpread*3.0/5.0, 0, 0});
				} else if(params[0] < 2.0) {
					shotMaker.makeEntity(entity, new double[] {+shotXSpread, -shotYSpread, -shotXVel});
					shotMaker.makeEntity(entity, new double[] {-shotXSpread, -shotYSpread, +shotXVel});
					return shotMaker.makeEntity(entity, new double[] {0.0, 0.0, 0});
				} else if(params[0] < 3.0) {
					shotMaker.makeEntity(entity, new double[] {+shotXSpread, -shotYSpread/2.0, +shotXVel});
					shotMaker.makeEntity(entity, new double[] {-shotXSpread, -shotYSpread/2.0, -shotXVel});
					shotMaker.makeEntity(entity, new double[] {+shotXSpread, -shotYSpread, -shotXVel});
					shotMaker.makeEntity(entity, new double[] {-shotXSpread, -shotYSpread, +shotXVel});
					return shotMaker.makeEntity(entity, new double[] {0.0, 0.0, 0});
				} else if(params[0] < 4.0) {
					shotMaker.makeEntity(entity, new double[] {+shotXSpread, -shotYSpread/2, +shotXVel/1.5});
					shotMaker.makeEntity(entity, new double[] {-shotXSpread, -shotYSpread/2, -shotXVel/1.5});

					shotMaker.makeEntity(entity, new double[] {+shotXSpread, -shotYSpread, +shotXVel*1.5});
					shotMaker.makeEntity(entity, new double[] {-shotXSpread, -shotYSpread, -shotXVel*1.5});
					shotMaker.makeEntity(entity, new double[] {+shotXSpread, -shotYSpread, -shotXVel});
					shotMaker.makeEntity(entity, new double[] {-shotXSpread, -shotYSpread, +shotXVel});
					return shotMaker.makeEntity(entity, new double[] {0.0, 0.0, 0});
				} else {
					shotMaker.makeEntity(entity, new double[] {+shotXSpread, -shotYSpread/3, +shotXVel/2});
					shotMaker.makeEntity(entity, new double[] {-shotXSpread, -shotYSpread/3, -shotXVel/2});
					shotMaker.makeEntity(entity, new double[] {+shotXSpread, -shotYSpread/2, +shotXVel});
					shotMaker.makeEntity(entity, new double[] {-shotXSpread, -shotYSpread/2, -shotXVel});

					shotMaker.makeEntity(entity, new double[] {+shotXSpread, -shotYSpread, +shotXVel*2});
					shotMaker.makeEntity(entity, new double[] {-shotXSpread, -shotYSpread, -shotXVel*2});
					shotMaker.makeEntity(entity, new double[] {+shotXSpread, -shotYSpread, -shotXVel});
					shotMaker.makeEntity(entity, new double[] {-shotXSpread, -shotYSpread, +shotXVel});
					return shotMaker.makeEntity(entity, new double[] {0.0, 0.0, 0});

				}
			}
		};

	public final IEntityMaker collectableSpawner = new IEntityMaker() {
			@Override
			// Params: minScale, maxScale
			public Entity makeEntity(Entity entity, double[] params) {
				Entity e = new Entity(spatialStructure, entity.getX(), entity.getY(), 0);
				new ProjectileComponent(e, CMWC4096.random(-0.2, 0.2), CMWC4096.random(0.1, 0.3), 0, -0.4, 0, 0, 0.5, 2.0, screenArea);
				double scale = CMWC4096.random(params[0], params[1]);
				new SpriteComponent(e, scale, scale, primarySheet, 7, Color.WHITE);
				new CollectableComponent(e, 0, scale);
				new HittableComponent(e, HittableComponent.TYPE_COLLECTABLE);
				return e;
			}
		};

	public final IEntityMaker collectableSpawner2 = new IEntityMaker() {
			@Override
			// Params: minScale, maxScale
			public Entity makeEntity(Entity entity, double[] params) {
				Entity e = new Entity(spatialStructure, entity.getX(), entity.getY(), 0);
				new ProjectileComponent(e, CMWC4096.random(-0.2, 0.2), CMWC4096.random(0.1, 0.3), 0, -0.4, 0, 0, 0.5, 2.0, screenArea);
				double scale = CMWC4096.random(params[0], params[1]);
				new SpriteComponent(e, scale, scale, primarySheet, 7, Color.WHITE);
				new CollectableComponent(e, 0, scale*params[2]);
				new HittableComponent(e, HittableComponent.TYPE_COLLECTABLE);
				return e;
			}
		};



	public final IEntityComponentAdder standardBullet = new IEntityComponentAdder() {
			@Override
			public void addToEntity(Entity e) {
				ColliderComponent c = new ColliderComponent(e);
				new HittableComponent(e,1, 10.0);
				new SpriteComponent(e, 0.03, 0.03, primarySheet, 0, Color.WHITE);
			}
		};

	public final IEntityComponentAdder whiteHoleAdder = new IEntityComponentAdder() {
			@Override
			public void addToEntity(Entity e) {
				whiteHoleList.add(e);
				ColliderComponent c = new ColliderComponent(e);
				new ColliderComponent(e);
				new SpriteComponent(e, 0.075, 0.075, primarySheet, 4, Color.WHITE);
				new InvalidAreaRemove(e, screenArea);
				new ListRemoveComponent(e, whiteHoleList);				
			}
		};

	public final IEntityComponentAdder blackHoleAdder = new IEntityComponentAdder() {
			@Override
			public void addToEntity(Entity e) {
				ColliderComponent c = new ColliderComponent(e);
				new ColliderComponent(e);
				new SpriteComponent(e, 0.075, 0.075, primarySheet, 3, Color.WHITE);
				new EnemyComponent(e, collectables, 1000.0, 0.005, new int[] {HittableComponent.TYPE_ENEMY_HAZARD, HittableComponent.TYPE_HAZARD}, collectableSpawner, new double[] {0.01, 0.02}, 4, whiteHoleList);
				new InvalidAreaRemove(e, screenArea);
			}
		};

	public final IEntityComponentAdder bossBullet = new IEntityComponentAdder() {
			Color bulletColor1 = new Color(1.0, 0.0, 1.0).hsvToRgb();
			Color bulletColor2 = new Color(1.0, 0.0, 1.0).hsvToRgb();
			boolean useColor1 = false;
			int counter = 0;
			@Override
			public void addToEntity(Entity e) {
				IEntityComponentAdder adder = this;
				ColliderComponent c = new ColliderComponent(e);
				new HittableComponent(e,1);
				useColor1 = counter % 5 == 0;
				new SpriteComponent(e, 0.02, 0.02, primarySheet, 0, useColor1 ? bulletColor1 : bulletColor2);
				if(useColor1) {
					new LightComponent(e, bulletLight, 0.08, 0.08, 0, 0);
				}
//				double delay = 1.0;
//				new DelayedSpawn(e, delay, new IEntityMaker() {
//					@Override
//					public Entity makeEntity(Entity entity, double[] params) {
//						Entity e = new Entity(spatialStructure, params[params.length-1], params[params.length-2], 0);
//						new BulletSpawner(e, 0, -1, 1.0,
//								new BulletSpawnProperties(0.5, 0, 0, 0, 0, 0, primarySheet),
//								new BulletSpawnVariance(0.0,0.0,0,0,0,0,0,0), standardBullet, screenArea);
//						new BulletSpawnerAimer(e,player);
//						new DelayedRemove(e, 0.1);
//						return e;
//					}
//				}, null);
//				new DelayedRemove(e, delay);
				useColor1 = !useColor1;
				counter++;
				//new Color(1.0, CMWC4096.random(), 1.0).hsvToRgb()
//				if(bulletLight != null) {
//					new LightComponent(e, bulletLight, 0.08, 0.08, 0, 0);
//				}

			}
		};

	public final IEntityComponentAdder middleBossBullet = new IEntityComponentAdder() {
			Color bulletColor1 = new Color(1.0, 0.0, 1.0).hsvToRgb();
			Color bulletColor2 = new Color(1.0, 0.0, 1.0).hsvToRgb();
			boolean useColor1 = false;
			int counter = 0;
			@Override
			public void addToEntity(Entity e) {
				IEntityComponentAdder adder = this;
				ColliderComponent c = new ColliderComponent(e);
				new HittableComponent(e,1);
				useColor1 = counter % 3 == 0;
				new SpriteComponent(e, 0.04, 0.04, primarySheet, 0, useColor1 ? bulletColor1 : bulletColor2);
				if(useColor1) {
					new LightComponent(e, bulletLight, 0.16, 0.16, 0, 0);
				}
//				double delay = 1.0;
//				new DelayedSpawn(e, delay, new IEntityMaker() {
//					@Override
//					public Entity makeEntity(Entity entity, double[] params) {
//						Entity e = new Entity(spatialStructure, params[params.length-1], params[params.length-2], 0);
//						new BulletSpawner(e, 0, -1, 1.0,
//								new BulletSpawnProperties(0.5, 0, 0, 0, 0, 0, primarySheet),
//								new BulletSpawnVariance(0.0,0.0,0,0,0,0,0,0), standardBullet, screenArea);
//						new BulletSpawnerAimer(e,player);
//						new DelayedRemove(e, 0.1);
//						return e;
//					}
//				}, null);
//				new DelayedRemove(e, delay);
				useColor1 = !useColor1;
				counter++;
				//new Color(1.0, CMWC4096.random(), 1.0).hsvToRgb()
//				if(bulletLight != null) {
//					new LightComponent(e, bulletLight, 0.08, 0.08, 0, 0);
//				}

			}
		};


	public final IEntityComponentAdder bigBossBullet = new IEntityComponentAdder() {
			Color bulletColor1 = new Color(1.0, 0.0, 1.0).hsvToRgb();
			Color bulletColor2 = new Color(1.0, 0.0, 1.0).hsvToRgb();
			boolean useColor1 = false;
			int counter = 0;
			@Override
			public void addToEntity(Entity e) {
				IEntityComponentAdder adder = this;
				ColliderComponent c = new ColliderComponent(e);
				new HittableComponent(e,1);
				useColor1 = counter % 1 == 0;
				new SpriteComponent(e, 0.1, 0.1, primarySheet, 0, useColor1 ? bulletColor1 : bulletColor2);
				if(useColor1) {
					new LightComponent(e, bulletLight, 0.32, 0.32, 0, 0);
				}
//				double delay = 1.0;
//				new DelayedSpawn(e, delay, new IEntityMaker() {
//					@Override
//					public Entity makeEntity(Entity entity, double[] params) {
//						Entity e = new Entity(spatialStructure, params[params.length-1], params[params.length-2], 0);
//						new BulletSpawner(e, 0, -1, 1.0,
//								new BulletSpawnProperties(0.5, 0, 0, 0, 0, 0, primarySheet),
//								new BulletSpawnVariance(0.0,0.0,0,0,0,0,0,0), standardBullet, screenArea);
//						new BulletSpawnerAimer(e,player);
//						new DelayedRemove(e, 0.1);
//						return e;
//					}
//				}, null);
//				new DelayedRemove(e, delay);
				useColor1 = !useColor1;
				counter++;
				//new Color(1.0, CMWC4096.random(), 1.0).hsvToRgb()
//				if(bulletLight != null) {
//					new LightComponent(e, bulletLight, 0.08, 0.08, 0, 0);
//				}

			}
		};


	public final IEntityComponentAdder zoomBullet = new IEntityComponentAdder() {
			Color bulletColor1 = new Color(1.0, 0.0, 1.0).hsvToRgb();
			Color bulletColor2 = new Color(1.0, 0.5, 1.0).hsvToRgb();
			boolean useColor1 = false;
			@Override
			public void addToEntity(Entity e) {
				IEntityComponentAdder adder = this;
				ColliderComponent c = new ColliderComponent(e);
				new HittableComponent(e,1);
				new SpriteComponent(e, 0.02, 0.02, primarySheet, 0, useColor1 ? bulletColor1 : bulletColor2);
				double delay = 1.0;
				new DelayedSpawn(e, delay, new IEntityMaker() {
					@Override
					public Entity makeEntity(Entity entity, double[] params) {
						Entity e = new Entity(spatialStructure, params[params.length-1], params[params.length-2], 0);
						new BulletSpawner(e, 0, -1, 1.0, 0, 0, 0,
								new BulletSpawnProperties(0.5, 0, 0, 0, 0, 0, primarySheet),
								new BulletSpawnVariance(0.0,0.0,0,0,0,0,0,0), adder, screenArea);
						new BulletSpawnerAimer(e,player);
						new DelayedRemove(e, 0.1);
						return e;
					}
				}, null);
				new FadeComponent(e, 0.0, 1.0, delay, 0.0);
				new ScaleComponent(e, 5.0, 1.0, delay, 0.0);
				new DelayedRemove(e, delay);
				useColor1 = !useColor1;
				//new Color(1.0, CMWC4096.random(), 1.0).hsvToRgb()
//				if(bulletLight != null) {
//					new LightComponent(e, bulletLight, 0.08, 0.08, 0, 0);
//				}

			}
		};

	public IEntityMaker blackHole = new IEntityMaker() {
			@Override
			public Entity makeEntity(Entity entity, double[] params) {
				Entity e = new Entity(spatialStructure, params[0], params[1], 0);
				new ProjectileComponent(e,params[2], params[3], 0, 0, params[4], 0, 0.5, 2.0, screenArea);
				blackHoleAdder.addToEntity(e);
				return e;
			}
		};

	public IEntityMaker whiteHole = new IEntityMaker() {
			@Override
			public Entity makeEntity(Entity entity, double[] params) {
				Entity e = new Entity(spatialStructure, params[0], params[1], 0);
				new ProjectileComponent(e,params[2], params[3], 0, 0, params[4], 0, 0.5, 2.0, screenArea);
				whiteHoleAdder.addToEntity(e);
				return e;
			}
		};


	public final IEntityMaker basicEnemy = new EnemyMaker(new int[] {HittableComponent.TYPE_ENEMY_HAZARD, HittableComponent.TYPE_HAZARD}, standardBullet, 2, null, null);
	public final IEntityMaker basicEnemy2 = new EnemyMaker(new int[] {HittableComponent.TYPE_ENEMY_HAZARD}, standardBullet, 2, null, null);

	public final IEntityMaker shotWrapEnemy = new EnemyMaker(new int[] {HittableComponent.TYPE_ENEMY_HAZARD, HittableComponent.TYPE_HAZARD}, standardBullet, 8, playerList, null);
	public final IEntityMaker immuneEnemy = new EnemyMaker(new int[] {HittableComponent.TYPE_HAZARD}, standardBullet, 10, playerList, null);

	public final IEntityMaker blackHoleEnemy = new EnemyMaker(new int[] {HittableComponent.TYPE_ENEMY_HAZARD, HittableComponent.TYPE_HAZARD}, blackHoleAdder, 5, null, null);
	public final IEntityMaker whiteHoleEnemy = new EnemyMaker(new int[] {HittableComponent.TYPE_ENEMY_HAZARD, HittableComponent.TYPE_HAZARD}, whiteHoleAdder, 6, null, null);
	public final IEntityMaker seekingEnemy = new EnemyMaker(new int[] {HittableComponent.TYPE_ENEMY_HAZARD}, standardBullet, 9, null, new IEntityComponentAdder() {
		@Override
		public void addToEntity(Entity e) {
			new ProjectileDirecting(e);
		}
	});

}
