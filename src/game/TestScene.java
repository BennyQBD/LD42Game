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
	private IButton continueButton = null;
	private IButton startGameButton = null;
	private boolean isGameStarted = false;
	private EntityFactory entityFactory;
	
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
		final double size = ((47.0/480.0+37.0/640.0)/2.0)*2.0;
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
				IRenderDevice.FILTER_NEAREST);
		entityFactory = new EntityFactory(getStructure(), screenArea, primarySheet, sprites.get("finalBoss.png", 1, 1, 0,
				IRenderDevice.FILTER_NEAREST), collectables, device);
		continueButton = new KeyButton(input, new int[] { IInput.KEY_C });
		startGameButton = new KeyButton(input, new int[] { IInput.KEY_Z });

		font = sprites.get("monospace.png", 16, 16, 1,
				IRenderDevice.FILTER_LINEAR);

		double screenCenterX = (-1+rightWallX)/2.0;
		Entity e2 = createPlayer(screenCenterX, -1, 3.0, 1.0, 0.1, 1.0, 0.1, 0.99, 1.0, entityFactory.playerShot, input, primarySheet);
		entityFactory.setPlayer(e2);
		createOuterWall(rightWallX);
		createSidePanel(rightWallX, primarySheet);
		
		
		Entity e = new Entity(getStructure(), 0, 0, 0);
		int numEnemies = 4;
		double startSpawnDelay = 0.0;
		// INTRO FOR LEVEL
		new DelayedSpawn(e, startSpawnDelay, entityFactory.basicEnemy, new double[] { rightWallX, 1.1, -0.3, -0.35, -0.1, 0.0, 0.5, 1.6, 1.0, -0.25, 0.25, 1, 0.005, 10.0, 4 }, 4, 1.5);
		new DelayedSpawn(e, startSpawnDelay, entityFactory.basicEnemy, new double[] { -1, 1.1, 0.3, -0.35, 0.1, 0.0, 0.5, 1.6, 1.0, -0.25, 0.25, 1, 0.005, 10.0, 4 }, 4, 1.5);

		// SUDDEN INTENSITY 
		new DelayedSpawn(e, startSpawnDelay+10.0, entityFactory.basicEnemy2, new double[] { 0.5, 1.1, 0.0, -1.0, -0.2, 0.0, 0.5, 1.0, 0.0, 0, Math.PI*2.0, 8, 0.005, 10.0, 6 }, 4, 0.75);
		new DelayedSpawn(e, startSpawnDelay+10.0, entityFactory.basicEnemy2, new double[] { -0.5, 1.1, 0.0, -1.0, 0.2, 0.0, 0.5, 1.0, 0.0, 0, Math.PI*2.0, 8, 0.005, 10.0, 6 }, 4, 0.75);

		new DelayedSpawn(e, startSpawnDelay+15.0, entityFactory.basicEnemy, new double[] { 0.75, 1.1, -0.3, -0.35, -0.1, 0.0, 0.5, 1.6, 1.0, -0.25, 0.25, 1, 0.005, 10.0, 4 }, 4, 1.5);
		new DelayedSpawn(e, startSpawnDelay+15.0, entityFactory.basicEnemy, new double[] { -0.75, 1.1, 0.3, -0.35, 0.1, 0.0, 0.5, 1.6, 1.0, -0.25, 0.25, 1, 0.005, 10.0, 4 }, 4, 1.5);
		new DelayedSpawn(e, startSpawnDelay+15.0, entityFactory.basicEnemy, new double[] { 1, -0.8, -0.5, 0.6, -0.15, 0.0, 0.5, 0.9, 1.0, 0.0, 0.0, 1, 0.005, 10.0, 4 }, 4, 1.5);
		new DelayedSpawn(e, startSpawnDelay+15.0, entityFactory.basicEnemy, new double[] { -1, -0.8, 0.5, 0.6, 0.15, 0.0, 0.5, 0.9, 1.0, 0.0, 0.0, 1, 0.005, 10.0, 4 }, 4, 1.5);

		new DelayedSpawn(e, startSpawnDelay+23.0, entityFactory.basicEnemy2, new double[] { 0.75, 1.1, -0.3, -0.35, -0.3, 0.2, 0.5, 1.6, 1.0, -0.25, 0.25, 3, 0.0125, 50.0, 10 }, 4, 1.5);
		new DelayedSpawn(e, startSpawnDelay+23.0, entityFactory.basicEnemy2, new double[] { -0.75, 1.1, 0.3, -0.35, 0.3, -0.2, 0.5, 1.6, 1.0, -0.25, 0.25, 3, 0.0125, 50.0, 10 }, 4, 1.5);
		new DelayedSpawn(e, startSpawnDelay+23.0, entityFactory.basicEnemy2, new double[] { 1.1, 0.75, -1.0, 0.0, 0.6, -0.6, 0.75, 1.2, 1.0, -0.25, 0.25, 1, 0.005, 10.0, 4 }, 6, 1.0);
		new DelayedSpawn(e, startSpawnDelay+23.0, entityFactory.basicEnemy2, new double[] { -1.1, 0.75, 1.0, 0.0, -0.6, 0.6, 0.75, 1.2, 1.0, -0.25, 0.25, 1, 0.005, 10.0, 4 }, 6, 1.0);

		// INTRO FOR BLACK/WHITE HOLES
		new DelayedSpawn(e, startSpawnDelay+33.0, entityFactory.blackHoleEnemy, new double[] { 1, -0.5, -0.3, 0.35, -0.1, 0.0, 0.25, 1.3, 1.0, 0.0, 0.0, 1, 0.005, 10.0, 4 }, 4, 3.0);
		new DelayedSpawn(e, startSpawnDelay+33.0, entityFactory.whiteHoleEnemy, new double[] { -1, -0.5, 0.3, 0.35, 0.1, 0.0, 0.25, 1.3, 1.0, 0.0, 0.0, 1, 0.005, 10.0, 4 }, 4, 3.0);

		new DelayedSpawn(e, startSpawnDelay+45.0, entityFactory.blackHoleEnemy, new double[] { 1, -0.25, -0.2, 0.5, 0.24, 0.0, 0.25, 4.0, 2.0, 0.0, 0.0, 1, 0.005, 10.0, 4 }, 6, 2.0);
		new DelayedSpawn(e, startSpawnDelay+46.0, entityFactory.whiteHoleEnemy, new double[] { 1, -0.25, -0.2, 0.5, 0.24, 0.0, 0.25, 4.0, 2.0, 0.0, 0.0, 1, 0.005, 10.0, 4 }, 6, 2.0);
		new DelayedSpawn(e, startSpawnDelay+45.0, entityFactory.blackHoleEnemy, new double[] { -1, -0.25, 0.2, 0.5, -0.24, 0.0, 0.25, 4.0, 2.0, 0.0, 0.0, 1, 0.005, 10.0, 4 }, 6, 2.0);
		new DelayedSpawn(e, startSpawnDelay+46.0, entityFactory.whiteHoleEnemy, new double[] { -1, -0.25, 0.2, 0.5, -0.24, 0.0, 0.25, 4.0, 2.0, 0.0, 0.0, 1, 0.005, 10.0, 4 }, 6, 2.0);
		new DelayedSpawn(e, startSpawnDelay+45.0, entityFactory.basicEnemy, new double[] { 1, -0.5, -0.3, 0.35, -0.1, 0.0, 0.5, 1.5, 1.0, -0.5, 0.5, 4, 0.005, 10.0, 4 }, 6, 1.0);
		new DelayedSpawn(e, startSpawnDelay+46.0, entityFactory.basicEnemy, new double[] { -1, -0.5, 0.3, 0.35, 0.1, 0.0, 0.5, 1.5, 1.0, -0.5, 0.5, 4, 0.005, 10.0, 4 }, 6, 1.0);

		new DelayedSpawn(e, startSpawnDelay+59.0, entityFactory.basicEnemy2, new double[] { 1.1, 0.75, -1.0, 0.0, 0.6, -0.0, 0.75, 1.2, 1.0, -0.25, 0.25, 1, 0.005, 20.0, 5 }, 8, 1.0);
		new DelayedSpawn(e, startSpawnDelay+59.0, entityFactory.basicEnemy2, new double[] { -1.1, 0.75, 1.0, 0.0, -0.6, 0.0, 0.75, 1.2, 1.0, -0.25, 0.25, 1, 0.005, 20.0, 5 }, 8, 1.0);
		new DelayedSpawn(e, startSpawnDelay+62.0, entityFactory.basicEnemy2, new double[] { 0.9, 1.1, -0.45, -0.45, -0.2, -0.0, 0.5, 1.2, 1.0, -0.25, 0.25, 1, 0.0075, 30.0, 6 }, 8, 1.0);
		new DelayedSpawn(e, startSpawnDelay+62.0, entityFactory.basicEnemy2, new double[] { -0.9, 1.1, 0.45, -0.45, 0.2, 0.0, 0.5, 1.2, 1.0, -0.25, 0.25, 1, 0.0075, 30.0, 6 }, 8, 1.0);
		new DelayedSpawn(e, startSpawnDelay+65.0, entityFactory.basicEnemy2, new double[] { 1.0, 0.9, -0.4, -0.0, -0.0, -0.0, 0.25, 1.2, 1.0, -0.4, 0.4, 7, 0.015, 60.0, 8}, 8, 1.0);
		new DelayedSpawn(e, startSpawnDelay+65.0, entityFactory.basicEnemy2, new double[] { -1.0, 0.9, 0.4, -0.0, 0.0, 0.0, 0.25, 1.2, 1.0, -0.4, 0.4, 7, 0.015, 60.0, 8 }, 8, 1.0);
		new DelayedSpawn(e, startSpawnDelay+70.0, entityFactory.blackHoleEnemy, new double[] { 1.0, -0.9, -0.4, 0.3, -0.0, -0.0, 0.125, 1.2, 1.0, -0.25, 0.25, 1, 0.015, 60.0, 4 }, 8, 1.0);
		new DelayedSpawn(e, startSpawnDelay+70.0, entityFactory.whiteHoleEnemy, new double[] { -1.0, -0.9, 0.4, 0.3, 0.0, 0.0, 0.125, 1.2, 1.0, -0.25, 0.25, 1, 0.015, 60.0, 4 }, 8, 1.0);

		new DelayedSpawn(e, startSpawnDelay+84.0, entityFactory.basicEnemy2, new double[] { 1, 0.9, -0.3, -0.1, -0.04, 0.0, 0.5, 1.6, 1.0, -0.25, 0.25, 1, 0.005, 35.0, 7 }, 8, 1.0);
		new DelayedSpawn(e, startSpawnDelay+84.0, entityFactory.basicEnemy2, new double[] { -1, 0.9, 0.3, -0.1, 0.04, 0.0, 0.5, 1.6, 1.0, -0.25, 0.25, 1, 0.005, 35.0, 7 }, 8, 1.0);
		new DelayedSpawn(e, startSpawnDelay+84.0, entityFactory.basicEnemy2, new double[] { 1, 0.7, -0.3, 0.1, 0.04, 0.0, 0.5, 1.6, 1.0, -0.25, 0.25, 1, 0.005, 35.0, 7 }, 8, 1.0);
		new DelayedSpawn(e, startSpawnDelay+84.0, entityFactory.basicEnemy2, new double[] { -1, 0.7, 0.3, 0.1, -0.04, 0.0, 0.5, 1.6, 1.0, -0.25, 0.25, 1, 0.005, 35.0, 7 }, 8, 1.0);

		new DelayedSpawn(e, startSpawnDelay+83.0, entityFactory.seekingEnemy, new double[] { 1.0, 0.9, -0.15, 0.075, -0.0, -0.0, 0.38, 2.0, 1.0, -0.35, 0.35, 3, 0.025, 120.0, 16 }, 1, 3.0);
		new DelayedSpawn(e, startSpawnDelay+83.0, entityFactory.seekingEnemy, new double[] { -1.0, 0.9, 0.15, 0.075, 0.0, 0.0, 0.38, 2.0, 1.0, -0.35, 0.35, 3, 0.025, 120.0, 16 }, 1, 3.0);
		new DelayedSpawn(e, startSpawnDelay+86.0, entityFactory.seekingEnemy, new double[] { 1.0, 0.4, -0.15, 0.075, -0.0, -0.0, 0.38, 2.0, 1.0, -0.35, 0.35, 3, 0.025, 120.0, 16 }, 1, 3.0);
		new DelayedSpawn(e, startSpawnDelay+86.0, entityFactory.seekingEnemy, new double[] { -1.0, 0.4, 0.15, 0.075, 0.0, 0.0, 0.38, 2.0, 1.0, -0.35, 0.35, 3, 0.025, 120.0, 16 }, 1, 3.0);

		new DelayedSpawn(e, startSpawnDelay+96.0, entityFactory.seekingEnemy, new double[] { 1.0, -0.9, -0.15, 0.075, -0.0, -0.0, 0.25, 1.2, 1.0, -0.25, 0.25, 3, 0.025, 120.0, 16 }, 2, 3.0);
		new DelayedSpawn(e, startSpawnDelay+96.0, entityFactory.seekingEnemy, new double[] { -1.0, -0.9, 0.15, 0.075, 0.0, 0.0, 0.25, 1.2, 1.0, -0.25, 0.25, 3, 0.025, 120.0, 16 }, 2, 3.0);
		new DelayedSpawn(e, startSpawnDelay+98.0, entityFactory.basicEnemy2, new double[] { 1.0, 0.9, -0.4, -0.1, -0.0, -0.0, 0.5, 1.2, 1.0, -0.25, 0.25, 1, 0.0075, 25.0, 5 }, 12, 0.75);
		new DelayedSpawn(e, startSpawnDelay+98.0, entityFactory.basicEnemy2, new double[] { -1.0, 0.9, 0.4, -0.1, 0.0, 0.0, 0.5, 1.2, 1.0, -0.25, 0.25, 1, 0.0075, 25.0, 5 }, 12, 0.75);
		new DelayedSpawn(e, startSpawnDelay+105.0, entityFactory.blackHoleEnemy, new double[] { 1.0, 0.9, -0.4, 0.0, -0.0, -0.0, 0.15, 6.0, 2.0, -0.25, 0.25, 1, 0.0075, 25.0, 5 }, 12, 0.75);
		new DelayedSpawn(e, startSpawnDelay+105.0, entityFactory.blackHoleEnemy, new double[] { -1.0, 0.9, 0.4, 0.0, 0.0, 0.0, 0.15, 6.0, 2.0, -0.25, 0.25, 1, 0.0075, 25.0, 5 }, 12, 0.75);
		new DelayedSpawn(e, startSpawnDelay+105.0, entityFactory.whiteHoleEnemy, new double[] { 1.0, -0.9, -0.4, 0.0, -0.0, -0.0, 0.15, 6.0, 2.0, -0.25, 0.25, 1, 0.0075, 25.0, 5 }, 12, 0.75);
		new DelayedSpawn(e, startSpawnDelay+105.0, entityFactory.whiteHoleEnemy, new double[] { -1.0, -0.9, 0.4, 0.0, 0.0, 0.0, 0.15, 6.0, 2.0, -0.25, 0.25, 1, 0.0075, 25.0, 5 }, 12, 0.75);

		new DelayedSpawn(e, startSpawnDelay+120.0, entityFactory.basicEnemy2, new double[] { 0.5, 1.1, 0, -0.7, 0.15, 0.0, 0.75, 1.2, 1.0, 0.0, Math.PI*2.0, 10, 0.005, 15.0, 50 }, 8, 1.0);
		new DelayedSpawn(e, startSpawnDelay+120.0, entityFactory.basicEnemy2, new double[] { -0.5, 1.1, 0, -0.7, -0.15, 0.0, 0.75, 1.2, 1.0, 0.0, Math.PI*2.0, 10, 0.005, 15.0, 50 }, 8, 1.0);
		new DelayedSpawn(e, startSpawnDelay+120.0, entityFactory.basicEnemy2, new double[] { 1.1, 0.5, -0.7, 0, -0.15, 0.0, 0.75, 1.2, 1.0, 0.0, Math.PI*2.0, 10, 0.005, 15.0, 50 }, 8, 1.0);
		new DelayedSpawn(e, startSpawnDelay+120.0, entityFactory.basicEnemy2, new double[] { -1.1, 0.5, 0.7, 0, 0.15, 0.0, 0.75, 1.2, 1.0, 0.0, Math.PI*2.0, 10, 0.005, 15.0, 50 }, 8, 1.0);
//
		new DelayedSpawn(e, startSpawnDelay+135.0, entityFactory.boss, new double[] {}, 1, 0.0);


//

//		entityFactory.makeBossEntity(screenCenterX, entityFactory.bossBullet);
//		AudioUtil.playBackgroundMusic("./res/music.mp3");
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
	double starsDelta2 = 0.0;
	private double oldScreenLighting = 1.0;

	@Override
	public boolean update(double delta) {
		if(isGameStarted) {
			if(collectables.getLives() > 0 && gameWonCountDown > 0.0) {
				super.updateRange(delta, new AABB(-5, -5, 5, 5));
				starsDelta += delta;
				oldScreenLighting = screenLighting;
			} else {
				screenLighting = 0.25;
				collectables.multiplyScreenShake(0.00, 1.0);
				if(continueButton.isDown()) {
					collectables.reset();
					screenLighting = oldScreenLighting;
					numContinuesUsed++;
				}
			}

			if(isGameWon) {
				gameWonCountDown -= delta;
			}
		} else {
			starsDelta2 += delta;
			if(startGameButton.isDown()) {
				isGameStarted = true;
			}
		}
		return false;
	}

	//private Stars3D stars = new Stars3D(800, 1.0, 1.0, 0.50, 0.0);
	private Stars3D thunderCloud = new Stars3D(800, 1.0, 1.0, 0.50, 0.0);
	private Stars3D stars = new Stars3D(200, 5.0, 5.0, 5.0, 2.0);
	private Stars3D nebula = new Stars3D(2400, 1.0, 1.0, 1.0, 0.0);
	private Stars3D nebula2 = new Stars3D(800, 1.0, 1.0, 1.0, 0.0);
	private Color starColor = new Color(1.0f, 1.0f, 1.0f);
	private Color nebulaColor = new Color(0.4f, 0.4f, 1.0f);
	private Color nebula2Color = new Color(1.0f, 0.5f, 1.0f);
	public static double screenLighting = 1.0;
	public static boolean isGameWon = false;
	private static double gameWonCountDown = 3.0;
	private int numContinuesUsed = 0;

	@Override
	public void render(IRenderContext target) {
		target.clear(Color.BLACK);
		if(isGameStarted) {
			target.clearLighting(new Color(screenLighting));
			double screenShake = collectables.getScreenShake();
			double screenLocX = CMWC4096.random(-screenShake,screenShake);
			double screenLocY = CMWC4096.random(-screenShake,screenShake);
			//stars.updateAndRender(target, (float)starsDelta, primarySheet, 0, 0.1f, 0.1f, 0.1, starColor, 0.0f, 0.0f, -0.02f, (float)screenLocX, (float)screenLocY, false, false, true);	
	//		stars.updateAndRender(target, (float)starsDelta, primarySheet, 0, 0.1, 0.1, 0.1, starColor, 0.02, 0, -0.02, (float)screenLocX, (float)screenLocY, true, true, true);
			stars.updateAndRender(target, (float)starsDelta, primarySheet, 11, 0.02, 0.02, 0.9, starColor, 0.2, -0.2, 0, (float)screenLocX, (float)screenLocY, true, true, true);
			nebula.updateAndRender(target, (float)starsDelta, primarySheet, 12, 0.05, 0.05, 0.0125, nebulaColor, 0.04, 0, -0.04, (float)screenLocX, (float)screenLocY, true, true, true);
			nebula2.updateAndRender(target, (float)starsDelta, primarySheet, 12, 0.05, 0.05, 0.0125, nebula2Color, 0.04, 0, -0.04, (float)screenLocX, (float)screenLocY, true, true, true);

			collectables.multiplyScreenShake(0.006, starsDelta);
			starsDelta = 0.0;

			super.renderRange(target, screenLocX, screenLocY);
			target.applyLighting();

			double y = 0.925;
			target.drawString("Score: "+collectables.getScore(), font, -1, y, 0.075,
					Color.WHITE, 1.0);
	//		y = target.drawString(""+collectables.getScore(), font, 0.3, y, 0.075,
	//				Color.WHITE, 1.0);
			target.drawString("Entropy: " + collectables.getPower(), font, 0.1, y, 0.075,
					Color.WHITE, 1.0);
			target.drawString("Lives: " + collectables.getLives(), font, -1, -1, 0.075,
					Color.WHITE, 1.0);

			if(collectables.getLives() <= 0) {
				y = 0;
				y = target.drawString("Game Over!", font, -0.5, y, 0.15,
					Color.WHITE, 1.0);
				y = target.drawString("Press C to continue", font, -0.49, y, 0.075,
					Color.WHITE, 1.0);
				y = target.drawString("", font, -0.5, y, 0.075, Color.WHITE, 1.0);
				y = target.drawString("", font, -0.5, y, 0.075, Color.WHITE, 1.0);
				y = target.drawString("", font, -0.5, y, 0.075, Color.WHITE, 1.0);
				y = target.drawString("Continues used: " + numContinuesUsed, font, -0.44, y, 0.075,
					Color.WHITE, 1.0);
			} else if(gameWonCountDown <= 0.0) {
				y = 0;
				y = target.drawString("You Won!", font, -0.5, y, 0.15,
					Color.WHITE, 1.0);
				y = target.drawString("The evil behemoth known only as 'The frickin blob' has finally been subdued. Congratulation! A winner is you!", font, -1.0, y, 0.075,
					Color.WHITE, 1.0);
			}
		} else {
			thunderCloud.updateAndRender(target, (float)starsDelta2, primarySheet, 0, 0.1, 0.1, 0.1, new Color(0.5f, 0.5f, 1.0f), 0.02, 0, -0.02, (float)0.0f, (float)0.0f, true, true, true);
			starsDelta2 = 0.0;
			double y = 0.75;
			y = target.drawString("Celestial Lacuna", font, -0.95, y, 0.15, Color.WHITE, 1.0);
			y = - 0.5;
			y = target.drawString("Press Z to begin", font, -0.49, y, 0.075,
					Color.WHITE, 1.0);
			y = target.drawString("", font, -0.89, y, 0.075, Color.WHITE, 1.0);
			y = target.drawString("Controls:", font, -0.89, y, 0.075, Color.WHITE, 1.0);
			y = target.drawString("Arrow Keys - Movement", font, -0.89, y, 0.075, Color.WHITE, 1.0);
			y = target.drawString("Z - Shoot", font, -0.89, y, 0.075, Color.WHITE, 1.0);
			y = target.drawString("Shift - Slow Down", font, -0.89, y, 0.075, Color.WHITE, 1.0);
			y = target.drawString("X - Entropy Burst", font, -0.89, y, 0.075, Color.WHITE, 1.0);

		}
		target.cleanupResources();
	}
}

//Normal Rendering: 4.1816038947566385 ms per frame
//Minimizing Mode Setting: 3.475157966305777 ms per frame (287.7567033486646 fps)
