package game;

import java.io.IOException;
import java.text.ParseException;
import java.util.Random;
import java.util.List;

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

/*
 * NOTE: In bullet performance:
 * 66% of the time is currently taken up with the SpriteComponent (4.7-1.6)
 * 22.3% of the time is overhead of the entity system (i.e. the entity simply existing)
 * 8.5% of the time is currently taken up with collision detection/resolution
 * 2.1% of the time is currently taken up by the projectile component
 * 1.1% of the time is everything else non-bullet related the engine is doing
 */
public class TestScene extends Scene {
	private SpriteSheet font;
	private SpriteSheet primarySheet;
	private Collectables collectables = new Collectables();
	
	private AABB screenArea;

	private Entity createPlayer(double posX, double posY, double invulnerabilityTimeAfterHit, double bombTimeInvulnerability, double deathBombTime, double speed, double fireSpeed, double powerDecayPerSecond, double minPowerToDecayTo, IEntityMaker shotType, IInput input, SpriteSheet sprites) {
		IButton slowDownControl = new KeyButton(input, new int[] {
				IInput.KEY_LEFT_SHIFT, IInput.KEY_RIGHT_SHIFT });
		IButton leftKeyButtons = new KeyButton(input, new int[] {
				IInput.KEY_A, IInput.KEY_LEFT });
		IButton rightKeyButtons = new KeyButton(input, new int[] {
				IInput.KEY_D, IInput.KEY_RIGHT });
		IButton upKeyButtons = new KeyButton(input, new int[] {
				IInput.KEY_W, IInput.KEY_UP });
		IButton downKeyButtons = new KeyButton(input, new int[] {
				IInput.KEY_S, IInput.KEY_DOWN });
		
		IButton leftJoyButtons = new JoystickButton(input, 0, 7);
		IButton rightJoyButtons = new JoystickButton(input, 0, 5);
		IButton upJoyButtons = new JoystickButton(input, 0, 4);
		IButton downJoyButtons = new JoystickButton(input, 0, 6);

		IButton leftButtons = new CompoundButton(leftKeyButtons,
				leftJoyButtons);
		IButton rightButtons = new CompoundButton(rightKeyButtons,
				rightJoyButtons);
		IButton upButtons = new CompoundButton(upKeyButtons, upJoyButtons);
		IButton downButtons = new CompoundButton(downKeyButtons,
				downJoyButtons);
		
		IAxis buttonX = new ButtonAxis(leftButtons, rightButtons);
		IAxis buttonY = new ButtonAxis(upButtons, downButtons);
		
		IAxis joystickX = new JoystickAxis(input, 0, 0);
		IAxis joystickY = new JoystickAxis(input, 0, 1);

		IAxis movementX = new CompoundAxis(buttonX, joystickX);
		IAxis movementY = new CompoundAxis(buttonY, joystickY);
		return createPlayer(posX, posY, collectables, invulnerabilityTimeAfterHit, bombTimeInvulnerability, deathBombTime, speed, fireSpeed, powerDecayPerSecond, minPowerToDecayTo, shotType, movementX, movementY, new KeyButton(input, new int[] {IInput.KEY_X}), slowDownControl, new KeyButton(input, new int[] {IInput.KEY_Z}), sprites);
	}

	private Entity createPlayer(double posX, double posY, Collectables collectables, double invulnerabilityTimeAfterHit, double bombTimeInvulnerability, double deathBombTime, double speed, double fireSpeed, double powerDecayPerSecond, double minPowerToDecayTo, IEntityMaker shotType, IAxis movementX, IAxis movementY, IButton bombButton, IButton slowDownControl, IButton fireButton, SpriteSheet sprites) {
		final double size = (47.0/480.0+37.0/640.0)/2.0;
		final double sizeY = size;
		final double sizeX = size;
		posY = posY + sizeY/2.0;
		Entity e = new Entity(getStructure(), posX, posY, 0);
		new ColliderComponent(e);
		new CollisionComponent(e);
		new InventoryComponent(e, collectables, invulnerabilityTimeAfterHit, bombTimeInvulnerability, deathBombTime, speed, fireSpeed, powerDecayPerSecond, minPowerToDecayTo, shotType, movementX, movementY, bombButton, slowDownControl, fireButton);
		new SpriteComponent(e, sizeX, sizeY, sprites, 1, Color.WHITE);
		return e;
	}


	public TestScene(IInput input, IRenderDevice device,
			IAudioDevice audioDevice) throws IOException {
		//4: 3.394931111371444 ms per frame (294.55678692579767 fps); Standard Deviation of render time for a given frame: 4.228984560214602 ms (Worst Plausible Lag Spike: 24.539853912444457 ms)
		//8: 3.1253772902439056 ms per frame (319.9613701429179 fps); Standard Deviation of render time for a given frame: 4.039041362681109 ms (Worst Plausible Lag Spike: 23.320584103649452 ms)
		//16: 2.98484498047344 ms per frame (335.0257740492055 fps); Standard Deviation of render time for a given frame: 3.7018356383081397 ms (Worst Plausible Lag Spike: 21.49402317201414 ms)
		//32: 2.6820713679637738 ms per frame (372.8461561256661 fps); Standard Deviation of render time for a given frame: 3.1348825832518905 ms (Worst Plausible Lag Spike: 18.356484284223228 ms)
		//64: 2.434896497939961 ms per frame (410.69507506624933 fps); Standard Deviation of render time for a given frame: 2.890793921199933 ms (Worst Plausible Lag Spike: 16.888866103939627 ms)
		//128: 2.4321767236797616 ms per frame (411.1543335909613 fps); Standard Deviation of render time for a given frame: 2.971707260264137 ms (Worst Plausible Lag Spike: 17.290713025000443 ms)
		super(new QuadTree<Entity>(new AABB(-1, -1, 1, 1), 64));
		final double rightWallX = 1;//0.3;
		screenArea = new AABB(-1, -1, rightWallX, 1);
		SpriteSheetFactory sprites = new SpriteSheetFactory(
				new TextureFactory(device, "./res/"));
		primarySheet = sprites.get("spriteSheet.png", 16, 16, 0,
				IRenderDevice.FILTER_LINEAR);

		font = sprites.get("monospace.png", 16, 16, 1,
				IRenderDevice.FILTER_LINEAR);

		double screenCenterX = (-1+rightWallX)/2.0;
		Entity e2 = createPlayer(screenCenterX, -1, 3.0, 1.0, 0.1, 1.0, 0.1, 0.99, 1.0, new IEntityMaker() {
			@Override
			public Entity makeEntity(Entity entity, double[] params) {
				double slowDownAmt = params[1];
				Color playerShotColor = new Color(Util.lerp(0.0, 1.0, params[2]), 1.0, 1.0);
				//playerShotColor.set(CMWC4096.random(), CMWC4096.random(), CMWC4096.random(), 1.0);//1-params[2], 1.0);
				IEntityMaker shotMaker = new IEntityMaker() {
					@Override
					public Entity makeEntity(Entity entity, double[] params) {
						Entity e = new Entity(getStructure(), entity.getX()+params[0], entity.getY()+params[1], 0);
						new ProjectileComponent(e,params[2], 1.0, 0, 2.0, 0, 0, 0.5, 2.0, screenArea);
//							entities.createProjectile(entity.getX()+params[0], entity.getY()+params[1], params[2], 1.0, 0, 2.0, 0, 0, 0.5, 2.0);
						new ColliderComponent(e);
						new SpriteComponent(e, 0.03, 0.03, primarySheet, 0, playerShotColor).setTransparency(0.5);
						new InvalidAreaRemove(e, new AABB(-1, -1, rightWallX, 1));
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
//						shotMaker.makeEntity(entities, entity, new double[] {+shotXSpread, -shotYSpread/3, +shotXVel/2});
//						shotMaker.makeEntity(entities, entity, new double[] {-shotXSpread, -shotYSpread/3, -shotXVel/2});
//
//						shotMaker.makeEntity(entities, entity, new double[] {+shotXSpread, -shotYSpread/2, +shotXVel*2});
//						shotMaker.makeEntity(entities, entity, new double[] {-shotXSpread, -shotYSpread/2, -shotXVel*2});
//						shotMaker.makeEntity(entities, entity, new double[] {+shotXSpread, -shotYSpread, -shotXVel/1.5});
//						shotMaker.makeEntity(entities, entity, new double[] {-shotXSpread, -shotYSpread, +shotXVel/1.5});
//						shotMaker.makeEntity(entities, entity, new double[] {+shotXSpread, -shotYSpread, -shotXVel*1.5});
//						shotMaker.makeEntity(entities, entity, new double[] {-shotXSpread, -shotYSpread, +shotXVel*1.5});
					return shotMaker.makeEntity(entity, new double[] {0.0, 0.0, 0});

				}

				//return null;
			}
		}, input, primarySheet);
		createOuterWall(rightWallX);
		createSidePanel(rightWallX, primarySheet);
		LightMap bulletLight = new LightMap(device, 16, new Color(1.0, 0.8, 0.8));
		IEntityComponentAdder standardBullet = new IEntityComponentAdder() {
			@Override
			public void addToEntity(Entity e) {
				ColliderComponent c = new ColliderComponent(e);
				new HittableComponent(e,1);
				new SpriteComponent(e, 0.02, 0.02, primarySheet, 0, Color.WHITE);
//				if(bulletLight != null) {
//					new LightComponent(e, bulletLight, 0.08, 0.08, 0, 0);
//				}

			}
		};
		IEntityMaker basicEnemy1 = new IEntityMaker() {
			@Override
			public Entity makeEntity(Entity entity, double[] params) {
				Entity e = new Entity(getStructure(), params[0], params[1], 0);
				new ProjectileComponent(e,params[2], params[3], 0, 0, params[4], 0, 0.5, 2.0, screenArea);
//					entities.createProjectile(params[0], params[1], params[2], params[3], 0, 0, params[4], 0, 0.5, 2.0);
				new ColliderComponent(e);
				new SpriteComponent(e, 0.05, 0.05, primarySheet, 2, Color.WHITE);
				new BulletSpawner(e, 0, -1, params[5],
								new BulletSpawnProperties(0.5, 0, 0, 0, 0, 0, primarySheet),
								new BulletSpawnVariance(0,1.0,0,0,0,0,0,0), standardBullet, screenArea);
				new BulletSpawnerAimer(e,e2);
				new EnemyComponent(e, collectables, 10.0, new IEntityMaker() {
					@Override
					public Entity makeEntity(Entity entity, double[] params) {
						Entity e = new Entity(getStructure(), entity.getX(), entity.getY(), 0);
						new ProjectileComponent(e, CMWC4096.random(-0.2, 0.2), CMWC4096.random(0.1, 0.3), 0, -0.4, 0, 0, 0.5, 2.0, screenArea);
//							entities.createProjectile(entity.getX(), entity.getY(), 0, 0.2, 0, -0.4, 0, 0, 0.5, 2.0);
						double scale = CMWC4096.random(0.01, 0.02);
						new SpriteComponent(e, scale, scale, primarySheet, 2, Color.WHITE);
						new CollectableComponent(e, 0, scale);
						new HittableComponent(e, HittableComponent.TYPE_COLLECTABLE);
						return e;
					}
				}, null, 4);
				new InvalidAreaRemove(e, new AABB(-1, -1, rightWallX, 1));
				return e;
			}
		};
		
		Entity e = new Entity(getStructure(), 0, 0, 0);
		int numEnemies = 8;
		new DelayedSpawn(e, 0.0, basicEnemy1, new double[] { rightWallX, 1.1, -0.3, -0.35, -0.1, 1.5 }, numEnemies, 0.5);
		new DelayedSpawn(e, 0.0, basicEnemy1, new double[] { -1, 1.1, 0.3, -0.35, 0.1, 1.5 }, numEnemies, 0.5);

		IEntityComponentAdder bossBullet = new IEntityComponentAdder() {
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
//						Entity e = new Entity(getStructure(), params[params.length-1], params[params.length-2], 0);
//						new BulletSpawner(e, 0, -1, 1.0,
//								new BulletSpawnProperties(0.5, 0, 0, 0, 0, 0, primarySheet),
//								new BulletSpawnVariance(0.0,0.0,0,0,0,0,0,0), standardBullet, screenArea);
//						new BulletSpawnerAimer(e,e2);
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

		IEntityComponentAdder zoomBullet = new IEntityComponentAdder() {
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
						Entity e = new Entity(getStructure(), params[params.length-1], params[params.length-2], 0);
						new BulletSpawner(e, 0, -1, 1.0,
								new BulletSpawnProperties(0.5, 0, 0, 0, 0, 0, primarySheet),
								new BulletSpawnVariance(0.0,0.0,0,0,0,0,0,0), adder, screenArea);
						new BulletSpawnerAimer(e,e2);
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


		makeBossEntity(e2, screenCenterX, primarySheet, bossBullet);
//		AudioUtil.playBackgroundMusic("./res/music.mp3");
	}

	private Entity makeBossEntity(Entity e2, double screenCenterX, SpriteSheet sprites, IEntityComponentAdder bulletType) {
		IEntityMaker circle1 = new IEntityMaker() {
			@Override
			public Entity makeEntity(Entity entity, double[] params) {
				Entity e = new Entity(getStructure(), screenCenterX, 0, 0);
				new DelayedRemove(e, params[0]);
				new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
				BulletSpawnVariance var = new BulletSpawnVariance(0.05,0,0.1,0.1,0,0,0,0);
				double spawnSpeed = 0.2;
				double sqrt2o2 = Math.sqrt(2.0)/8.0;
				double perpAcc = -0.125;
				double vel = 0.2;
				double startAngle = 0;
				double endAngle = Math.PI*2;
				int numBullets = 30;
				new BulletSpawner(e, 0, -1, startAngle, endAngle, numBullets, spawnSpeed,
						new BulletSpawnProperties(vel, 0, 0, 0, 0, 0, sprites),
						var, bulletType, screenArea);
				new BulletSpawnerAimer(e,e2);
				return e;
			}
		};

		IEntityMaker circle2 = new IEntityMaker() {
			@Override
			public Entity makeEntity(Entity entity, double[] params) {
				Entity e = new Entity(getStructure(), screenCenterX, 0, 0);
				new DelayedRemove(e, params[0]);
				new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
				BulletSpawnVariance var = new BulletSpawnVariance(0,0,0.1,0.1,0,0,0,0);
				double spawnSpeed = 0.3;
				double sqrt2o2 = Math.sqrt(2.0)/8.0;
				double perpAcc = -0.125;
				double vel = 0.2;
				double startAngle = 0;
				double endAngle = Math.PI*2;
				int numBullets = 40;
				new BulletSpawner(e, 0, -1, startAngle, endAngle, numBullets, spawnSpeed,
						new BulletSpawnProperties(vel, 0, 0, 0, 0, 0, sprites),
						var, bulletType, screenArea);
				return e;
			}
		};

		// NOTE: This attack doesn't scale past "Hard" mode. Need harder attack to replace it on higher difficulties. Above hard, replace with CrazyCircleSpiral?
		IEntityMaker pinwheel = new IEntityMaker() {
			@Override
			public Entity makeEntity(Entity entity, double[] params) {
				Entity e = new Entity(getStructure(), screenCenterX, 0, 0);
				new DelayedRemove(e, params[0]);
				new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
				BulletSpawnVariance var = new BulletSpawnVariance(0,0,0,0,0,0,0,0);
				double spawnSpeed =params[1] == 0.0 ? 0.4 : 0.2;
				double sqrt2o2 = Math.sqrt(2.0)/8.0;
				double perpAcc = -0.125;
				double vel = 0.25;
				double startAngle = 0;
				double endAngle = Math.PI*2;
				int numBullets = params[1] == 0.0 ? 2 : (params[1] <= 1.0 ? 4 : 8);
				new BulletSpawner(e, 1, 0, startAngle, endAngle, numBullets, spawnSpeed,
						new BulletSpawnProperties(vel, 0, -sqrt2o2, 0, 0, perpAcc, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, -1, 0, startAngle, endAngle, numBullets, spawnSpeed,
						new BulletSpawnProperties(vel, 0, sqrt2o2, 0, 0, perpAcc, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, 0, -1, startAngle, endAngle, numBullets, spawnSpeed,
						new BulletSpawnProperties(vel, 0, 0, sqrt2o2, 0, perpAcc, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, 0, 1, startAngle, endAngle, numBullets, spawnSpeed,
						new BulletSpawnProperties(vel, 0, 0, -sqrt2o2, 0, perpAcc, sprites),
						var, bulletType, screenArea);
				new BulletSpawnerAimer(e, createProjectile(screenCenterX, -0.25, 0.5, 0, 0, 0, 1, 0, 2, 0.5));	
				return e;
			}
		};

		IEntityMaker oldFourLeafClover = new IEntityMaker() {
			@Override
			public Entity makeEntity(Entity entity, double[] params) {
				Entity e = new Entity(getStructure(), screenCenterX, 0, 0);
				new DelayedRemove(e, params[0]);
				new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
				BulletSpawnVariance var = new BulletSpawnVariance(0,0,0,0,0,0,0,0);
				double spawnSpeed = 0.1;
				double sqrt2o2 = Math.sqrt(2.0)/2.0;
				double startAngle = -0.4;
				double endAngle = 0.4;
				int numBullets = 4;
				new BulletSpawner(e, 1, 0, startAngle, endAngle, numBullets, spawnSpeed,
						new BulletSpawnProperties(0.5, 0, -sqrt2o2, 0, 0, -1, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, -1, 0, startAngle, endAngle, numBullets, spawnSpeed,
						new BulletSpawnProperties(0.5, 0, sqrt2o2, 0, 0, -1, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, 0, -1, startAngle, endAngle, numBullets, spawnSpeed,
						new BulletSpawnProperties(0.5, 0, 0, sqrt2o2, 0, -1, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, 0, 1, startAngle, endAngle, numBullets, spawnSpeed,
						new BulletSpawnProperties(0.5, 0, 0, -sqrt2o2, 0, -1, sprites),
						var, bulletType, screenArea);
				new BulletSpawnerAimer(e, createProjectile(screenCenterX, -0.25, 0.5, 0, 0, 0, 1, 0, 2, 0.5));
				return e;
			}
		};

		IEntityMaker crazyCircleSpiral = new IEntityMaker() {
			@Override
			public Entity makeEntity(Entity entity, double[] params) {
				Entity e = new Entity(getStructure(), screenCenterX, 0, 0);
				new DelayedRemove(e, params[0]);
				new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
				BulletSpawnVariance var = new BulletSpawnVariance(0,0,0,0,0,0,0,0);
				double spawnSpeed = 0.4;
				double sqrt2o2 = Math.sqrt(2.0)/2.0;
				new BulletSpawner(e, 1, 1, spawnSpeed,
						new BulletSpawnProperties(0.5, 0, -0.5, -0.5, 0, -1, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, -1, 1, spawnSpeed,
						new BulletSpawnProperties(0.5, 0, 0.5, -0.5, 0, -1, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, -1, -1, spawnSpeed,
						new BulletSpawnProperties(0.5, 0, 0.5, 0.5, 0, -1, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, 1, -1, spawnSpeed,
						new BulletSpawnProperties(0.5, 0, -0.5, 0.5, 0, -1, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, 1, 0, spawnSpeed,
						new BulletSpawnProperties(0.5, 0, -sqrt2o2, 0, 0, -1, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, -1, 0, spawnSpeed,
						new BulletSpawnProperties(0.5, 0, sqrt2o2, 0, 0, -1, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, 0, -1, spawnSpeed,
						new BulletSpawnProperties(0.5, 0, 0, sqrt2o2, 0, -1, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, 0, 1, spawnSpeed,
						new BulletSpawnProperties(0.5, 0, 0, -sqrt2o2, 0, -1, sprites),
						var, bulletType, screenArea);
				new BulletSpawnerAimer(e, createProjectile(screenCenterX, -0.25, -0.5, 0, 0, 0, -1, 0, 2, 0.5));
				return e;
			}
		};

		IEntityMaker fourLeafClover = new IEntityMaker() {
			@Override
			public Entity makeEntity(Entity entity, double[] params) {
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
				Entity e = new Entity(getStructure(), screenCenterX, 0, 0);
				new DelayedRemove(e, params[0]);
				new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
				BulletSpawnVariance var = new BulletSpawnVariance(0,0,0,0,0,0,0,0);
				double spawnSpeed = params[1] <= 0.0 ? 0.2 : (params[1] <= 2.0 ? 0.1 : 0.075);
				double speed = 0.4;
				double speed2 = 0.4;
				double sqrt2o2 = Math.sqrt(speed*speed*2);//Math.sqrt(2.0)/2.0;
				double startAngle = -0.4;
				double endAngle = 0.4;
				int numBullets = params[1] <= 1.0 ? 1 : (params[1] <= 3.0 ? 2 : 3);
				new BulletSpawner(e, sqrt2o2, sqrt2o2, startAngle, endAngle, numBullets, spawnSpeed,
						new BulletSpawnProperties(speed2, 0, speed, speed, 0, -1, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, -sqrt2o2, sqrt2o2, startAngle, endAngle, numBullets, spawnSpeed,
						new BulletSpawnProperties(speed2, 0, -speed, speed, 0, -1, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, sqrt2o2, -sqrt2o2, startAngle, endAngle, numBullets, spawnSpeed,
						new BulletSpawnProperties(speed2, 0, speed, -speed, 0, -1, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, -sqrt2o2, -sqrt2o2, startAngle, endAngle, numBullets, spawnSpeed,
						new BulletSpawnProperties(speed2, 0, -speed, -speed, 0, -1, sprites),
						var, bulletType, screenArea);

//				new BulletSpawner(e, -1, 0, startAngle, endAngle, numBullets, spawnSpeed,
//						new BulletSpawnProperties(0.5, 0, sqrt2o2, 0, 0, -1, sprites),
//						var);
//				new BulletSpawner(e, 0, -1, startAngle, endAngle, numBullets, spawnSpeed,
//						new BulletSpawnProperties(0.5, 0, 0, sqrt2o2, 0, -1, sprites),
//						var);
//				new BulletSpawner(e, 0, 1, startAngle, endAngle, numBullets, spawnSpeed,
//						new BulletSpawnProperties(0.5, 0, 0, -sqrt2o2, 0, -1, sprites),
//						var);
				new BulletSpawnerAimer(e, createProjectile(screenCenterX, -0.25, 0.5, 0, 0, 0, 1, 0, 2, 0.5));
				return e;

			}
		};

		IEntityMaker circle3 = new IEntityMaker() {
			@Override
			public Entity makeEntity(Entity entity, double[] params) {
				Entity e = new Entity(getStructure(), screenCenterX, 0, 0);
				new DelayedRemove(e, params[0]);
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
				new BulletSpawner(e, 0, -1, startAngle, endAngle, numBullets, spawnSpeed, 0.0,
						new BulletSpawnProperties(vel/1.2, acc, 0, 0, 0.1, -0.01, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, 0, -1, startAngle, endAngle, numBullets, spawnSpeed, 0.0,//spawnSpeed/4.0,
						new BulletSpawnProperties(vel*1.2, acc, 0, 0, -0.1, 0.01, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, 0, -1, startAngle, endAngle, numBullets, spawnSpeed, 0.0,
						new BulletSpawnProperties(vel, acc, 0, 0, 0, 0, sprites),
						var, bulletType, screenArea);
				return e;
			}
		};

		
		IEntityMaker circle4 = new IEntityMaker() {
			@Override
			public Entity makeEntity(Entity entity, double[] params) {
				Entity e = new Entity(getStructure(), screenCenterX, 0, 0);
				new DelayedRemove(e, params[0]);
				new ClearHittableOnRemove(e, new AABB(-2,-2,2,2), HittableComponent.TYPE_HAZARD);
				BulletSpawnVariance var = new BulletSpawnVariance(0,0,0,0,00,0,0.01,0.001);
				double spawnSpeed = 1.2;
				double sqrt2o2 = Math.sqrt(2.0)/8.0;
				double perpAcc = -0.125;
				double vel = 0.2;
				double acc = 0;
				double startAngle = 0;
				double endAngle = Math.PI*2;
				int numBullets = 40;
				double angleOffset = (endAngle/numBullets)/16.0;///8.0;
				new BulletSpawner(e, 0, -1, startAngle, endAngle, numBullets, spawnSpeed, 0.0,
						new BulletSpawnProperties(vel, acc, 0, 0, 0.1, -0.01, sprites),
						var, bulletType, screenArea);
				new BulletSpawner(e, 0, -1, startAngle+angleOffset, endAngle+angleOffset, numBullets, spawnSpeed, spawnSpeed/2.0,
						new BulletSpawnProperties(vel, acc, 0, 0, 0.1, -0.01, sprites),
						var, bulletType, screenArea);
				return e;
			}
		};


		Entity e = new Entity(getStructure(), screenCenterX, 0, 0);
		return makeSpawningEntity(e,
				new IEntityMaker[] { circle1, fourLeafClover, circle2, pinwheel, circle3, crazyCircleSpiral, circle4 },
				//new double[] { 20.0, 15.0, 20.0, 15.0 }, 3.0);
				new double[] { 15.0, 29.0, 19.0, 14.5, 31.0, 8.0, 35.0 }, 3.0);

		// Impractical, but looks absolutely amazing! Maybe use a brief spurt of this to warn
		// the player about the "twister" attacks to come?
//			BulletSpawnVariance var = new BulletSpawnVariance(0,0,0,0,0,0,0,0.1);
//			double spawnSpeed = 0.05;
//			new BulletSpawner(e, entities, 1, 1, spawnSpeed,
//					new BulletSpawnProperties(0.5, 0, -0.5, -0.5, 0, 1),
//					var);
//			new BulletSpawner(e, entities, -1, 1, spawnSpeed,
//					new BulletSpawnProperties(0.5, 0, 0.5, -0.5, 0, -1),
//					var);
//			new BulletSpawner(e, entities, -1, -1, spawnSpeed,
//					new BulletSpawnProperties(0.5, 0, 0.5, 0.5, 0, 1),
//					var);
//			new BulletSpawner(e, entities, 1, -1, spawnSpeed,
//					new BulletSpawnProperties(0.5, 0, -0.5, 0.5, 0, -1),
//					var);
//			new FadeRemove(e, 1.2, -1, 1.2); // Note this is just a gross hack to easily end the attack after 1.2 seconds.
//			e.remove();

		// This forces the player to strategically move close at the peak of the spiral to ensure a gap.
//			new BulletSpawner(e, entities, 0, -1, 0.01,
//					new BulletSpawnProperties(0.5, 0, 0, -0.5, 0, 0),
//					new BulletSpawnVariance(0,0,0,0,0.1,0,0,0));
//			new BulletSpawnerAimer(e, e2);
//			Entity e3 = new Entity(getStructure(), screenCenterX, 0, 0);
//			new BulletSpawner(e3, entities, 0, 1, 0.025,
//					new BulletSpawnProperties(0.5, 0, 0, 0, 0, 0),
//					new BulletSpawnVariance());
//			new BulletSpawnerAimer(e3, entities.createProjectile(screenCenterX, -0.25, 0.5, 0, 0, 0, 1, 0, 2, 0.5));

		// Interesting attack with lots of bullets in all sorts of directions
//			new BulletSpawner(e, entities, 0, -1, 0.05, new BulletSpawnProperties(0.5, 0, 0, -0.25, 0, 0, 2, 10.0),
//					new BulletSpawnVariance(3, 0, 0, 0, 0.1, 0.1, 1, 0));
//			new BulletSpawnerAimer(e, e2);

		// Spiral attack where the player needs to follow the spiral plus dodge the wall of bullets
		// coming down diagonally. NOTE: If used, there must be something to prevent the player from
		// safespotting at the top of the screen or it defeats the point of the attack
//			double speed = Math.sqrt(2)/4;
//			BulletSpawnProperties bsprops = new BulletSpawnProperties(0.75, 0, -speed, -speed, 0, 0, 2, 10.0);
//			BulletSpawnVariance bsvar = new BulletSpawnVariance(0.1, 0, 0, 0, 0, 0, 0, 0);
//			new BulletSpawner(e, entities, 0, -1, 0.05, bsprops, bsvar);
//			new BulletSpawner(e, entities, 0, 1, 0.05, bsprops, bsvar);
//			new BulletSpawner(e, entities, 1, 0, 0.05, bsprops, bsvar);
//			new BulletSpawner(e, entities, -1, 0, 0.05, bsprops, bsvar);
//			new BulletSpawnerAimer(e, entities.createProjectile(screenCenterX, -0.75/2.0, 0.5, 0, 0, 0, 0.75, 0, 2, 0.5));

		// NOTE: Awesome "Boomerang" attack that encourages the player to spiral around to beat it.
		// Depends on the spawning entity being in the center of the screen
//			BulletSpawnProperties bsprops = new BulletSpawnProperties(0.75, -0.14, 0, 0, 0, 0, 2, 10.0);
//			BulletSpawnVariance bsvar = new BulletSpawnVariance(0.8, 0, 0, 0, 0, 0, 0, 0);
//			new BulletSpawner(e, entities, 0, -1, 0.05, bsprops, bsvar);
//			new BulletSpawner(e, entities, 0, 1, 0.05, bsprops, bsvar);
//			new BulletSpawnerAimer(e, e2);

		// NOTE: This is a really cool pattern if something is rotating around the shooting entity
//			new BulletSpawner(e, entities, 0, -1, 0.05, new BulletSpawnProperties(-1, 1, 0, -0.5, 0, 0, 2, 10.0),
//					new BulletSpawnVariance(1, 0, 0, 0, 0, 0, 0, 0));
//			new BulletSpawnerAimer(e, entities.createProjectile(screenCenterX, -0.25, 0.5, 0, 0, 0, 1, 0, 2, 0.5));
	}

	private Entity makeSpawningEntity(Entity e, IEntityMaker[] spawnedEntities, double[] delays, double difficulty) {
		double delay = 0;
		for(int i = 0; i < spawnedEntities.length; i++) {
			new DelayedSpawn(e, delay, spawnedEntities[i], new double[] { delays[i], difficulty });
			delay += delays[i];
		}
		new DelayedRemove(e, delay);
		return e;
	}

	private Entity createProjectile(double posX, double posY, double velX, double velY, double accX, double accY, double perpVel, double perpAcc, double despawnDelay, double speedCap) {
		Entity e = new Entity(getStructure(), posX, posY, 0);
		new ProjectileComponent(e,velX,velY,accX,accY,perpVel, perpAcc,despawnDelay, speedCap, screenArea);
		return e;
	}

	private ColliderComponent makeColliderAndCollision(Entity e) {
		ColliderComponent result = new ColliderComponent(e);
		new CollisionComponent(e);
		return result;
	}

	private void createOuterWall(double rightWallX) {
		Entity w1 = new Entity(getStructure(), -1, 0, 0);
		Entity w2 = new Entity(getStructure(), rightWallX, 0, 0);
		Entity w3 = new Entity(getStructure(), 0, -1, 0);
		Entity w4 = new Entity(getStructure(), 0, 1, 0);

		makeColliderAndCollision(w1).fitAABB(new AABB(-1,-1,0,1));
		makeColliderAndCollision(w2).fitAABB(new AABB(0,-1,1,1));
		makeColliderAndCollision(w3).fitAABB(new AABB(-1,-1,1,0));
		makeColliderAndCollision(w4).fitAABB(new AABB(-1,0,1,1));
	}

	private Entity createSidePanel(double rightWallX, SpriteSheet sprites) {
		Entity e = new Entity(getStructure(), (rightWallX+1)/2.0, 0, 1000000);
		new SpriteComponent(e, (1-rightWallX), 2, sprites, 3, Color.WHITE);
		return e;
	}


	double starsDelta = 0.0;

	@Override
	public boolean update(double delta) {
		super.updateRange(delta, new AABB(-5, -5, 5, 5));
		starsDelta += delta;
		return false;
	}

	//private Stars3D stars = new Stars3D(800, 0.50f);
	private Stars3D stars = new Stars3D(800, 1.0, 1.0, 0.50, 0.0);
	private Color starColor = new Color(0.4f, 0.4f, 1.0f);

	@Override
	public void render(IRenderContext target) {
		target.clear(Color.BLACK);
		target.clearLighting(new Color(0.5));
		double screenShake = collectables.getScreenShake();
		double screenLocX = CMWC4096.random(-screenShake,screenShake);
		double screenLocY = CMWC4096.random(-screenShake,screenShake);
		//stars.updateAndRender(target, (float)starsDelta, primarySheet, 0, 0.1f, 0.1f, 0.1, starColor, 0.0f, 0.0f, -0.02f, (float)screenLocX, (float)screenLocY, false, false, true);
		stars.updateAndRender(target, (float)starsDelta, primarySheet, 0, 0.1, 0.1, 0.1, starColor, 0.02, 0, -0.02, (float)screenLocX, (float)screenLocY, true, true, true);
		collectables.multiplyScreenShake(0.006, starsDelta);
		starsDelta = 0.0;

		super.renderRange(target, screenLocX, screenLocY);
		target.applyLighting();

		double y = 0.925;
		target.drawString("Score: "+collectables.getScore(), font, -1, y, 0.075,
				Color.WHITE, 1.0);
//		y = target.drawString(""+collectables.getScore(), font, 0.3, y, 0.075,
//				Color.WHITE, 1.0);
		target.drawString("Souls: " + collectables.getPower(), font, 0.28, y, 0.075,
				Color.WHITE, 1.0);
		target.cleanupResources();	
	}
}

//Normal Rendering: 4.1816038947566385 ms per frame
//Minimizing Mode Setting: 3.475157966305777 ms per frame (287.7567033486646 fps)
