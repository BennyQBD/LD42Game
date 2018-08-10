package game.components;

import java.io.IOException;
import java.text.ParseException;
import java.util.Random;
import java.util.List;

import engine.audio.IAudioDevice;
import engine.audio.AudioUtilClip;
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
import engine.util.factory.SpriteSheetFactory;
import engine.util.factory.TextureFactory;
import engine.util.IDAssigner;

public class InventoryComponent extends EntityComponent {
	public static final int ID = IDAssigner.getId();
	private static final double MAX_POWER = 4.0;

	private ColliderComponent colliderComponent;
	private double startPosX = 0.0;
	private double startPosY = 0.0;
	private double invulnerabilityTimeAfterHit;
	private double invulnerabilityTimer = 0.0;
	private double deathBombTimer = 0.0;
	private double deathBombTime;
	private double bombTimeInvulnerability;
	private IButton bombButton;
	private IAxis movementX;
	private IAxis movementY;
	private IButton slowDownControl;
	private IButton fireButton;
	private boolean buttonWasDown = false;
	private boolean isHit = false;
	private double speed;
	private double fireSpeed;
	private double powerDecayPerSecond;
	private double minPowerToDecayTo;
	private double shotTimer = 0.0;
	private double powerSpeedUpFactor;
	private Collectables collectables;
	private IEntityMaker shotType;
	private double maxFireSpeed = 1.0/60.0;
	private static AudioUtilClip bombSound = null;
	private static AudioUtilClip shootSound = null;

	private ColliderComponent getColliderComponent() {
		if (colliderComponent != null) {
			return colliderComponent;
		}

		colliderComponent = (ColliderComponent) getEntity().getComponent(
				ColliderComponent.ID);
		return colliderComponent;
	}

	public InventoryComponent(Entity entity, Collectables collectables, double invulnerabilityTimeAfterHit, double bombTimeInvulnerability, double deathBombTime, double speed, double fireSpeed, double powerDecayPerSecond, double minPowerToDecayTo, IEntityMaker shotType, IAxis movementX, IAxis movementY, IButton bombButton, IButton slowDownControl, IButton fireButton) {
		super(entity, ID);
		this.collectables = collectables;
		this.startPosX = entity.getX();
		this.startPosY = entity.getY();
		this.invulnerabilityTimeAfterHit = invulnerabilityTimeAfterHit;
		this.bombButton = bombButton;
		this.deathBombTime = deathBombTime;
		this.movementX = movementX;
		this.movementY = movementY;
		this.slowDownControl = slowDownControl;
		this.speed = speed;
		this.bombTimeInvulnerability = bombTimeInvulnerability;
		this.fireButton = fireButton;
		this.fireSpeed = fireSpeed;
		this.shotType = shotType;
		this.powerDecayPerSecond = powerDecayPerSecond;
		this.minPowerToDecayTo = minPowerToDecayTo;
		this.powerSpeedUpFactor = Math.exp(Math.log(maxFireSpeed/fireSpeed)/(collectables.getMaxPower()-4.0));
		if(bombSound == null) {
			this.bombSound = AudioUtil.loadClip("./res/bomb.wav");
		}
		if(shootSound == null) {
			this.shootSound = AudioUtil.loadClip("./res/shoot.wav");
		}
	}

	@Override
	public void update(double delta) {
		handleTimers(delta);
		handleHittable();
		handleBombs();
		handleMovement(delta);
		handleShooting();

		collectables.decayPower(powerDecayPerSecond, minPowerToDecayTo, delta);
		// TODO: Better way to display score and power!
	}

	private void handleShooting() {
		if(shotTimer > 0 || !fireButton.isDown()) {
			return;
		}
		shootSound.play();
		while(shotTimer <= 0) {
			double colorFactor = 1.0;
			if(collectables.getPower() <= 4.0) {
				shotTimer += fireSpeed;
			} else {
				double powerDiff = (collectables.getPower() - 4.0);
				double speedFactor = Math.pow(powerSpeedUpFactor, powerDiff);
				double maxSpeedFactor = maxFireSpeed/fireSpeed;
				colorFactor = (speedFactor-maxSpeedFactor)/(1.0-maxSpeedFactor);
				shotTimer += fireSpeed*speedFactor;
			}
			
			shotType.makeEntity(getEntity(), new double[] {collectables.getPower(), slowDownControl.isDown() ? 1.0 : 0.0, colorFactor});
		}
	}

	private void handleMovement(double delta) {
		if(isHit) {
			return;
		}
		double effectiveSpeed = speed*delta;
		if(slowDownControl.isDown()) {
			effectiveSpeed /= 2.0;
		}
		getEntity().move((float) (movementX.getAmount() * effectiveSpeed), 0);
		getEntity().move(0, (float) (-movementY.getAmount() * effectiveSpeed));
	}

	private void handleTimers(double delta) {
		if(isHit) {
			deathBombTimer -= delta;
			if(deathBombTimer <= 0) {
				miss();
			}
		}

		if(invulnerabilityTimer > 0) {
			invulnerabilityTimer -= delta;
		}

		if(shotTimer > 0) {
			shotTimer -= delta;
		}
	}

	private void handleBombs() {
		if(bombButton.isDown() && !buttonWasDown) {
			bomb(0.75);
		}

		buttonWasDown = bombButton.isDown();
	}

	private void handleHittable() {
		getEntity().visitInRange(HittableComponent.ID,
				getColliderComponent().getAABB(), new IEntityVisitor() {
					@Override
					public void visit(Entity entity, EntityComponent component) {
						ColliderComponent collider = (ColliderComponent) entity
								.getComponent(ColliderComponent.ID);
						if (collider != null
								&& !getColliderComponent().getAABB()
										.intersects(collider.getAABB())) {
							return;
						}

						HittableComponent c = (HittableComponent) component;
						handleHit(entity, c, collider);
					}
				});
	}

	private void bomb(double radius) {
		if(collectables.getPower() < 1.0) {
			return;
		}
		//AudioUtil.playBackgroundMusic("./res/music2.mp3");
		bombSound.play();
		collectables.addPower(-1.0);

		AABB aabb = new AABB(-radius, -radius, radius, radius);
		double radius2 = radius*radius;
		isHit = false;
		setInvulnerabilityTime(bombTimeInvulnerability);
		AABB area = getEntity().translateAABB(aabb);
		getEntity().visitInRange(HittableComponent.ID,
					area, new IEntityVisitor() {
						@Override
						public void visit(Entity entity, EntityComponent component) {
							if(entity == getEntity()) {
								return;
							}
							ColliderComponent collider = (ColliderComponent) entity
									.getComponent(ColliderComponent.ID);
							if (collider != null
									&& !area.intersects(collider.getAABB())) {
								return;
							}

							double distX = entity.getX()-getEntity().getX();
							double distY = entity.getY()-getEntity().getY();
							if((distX*distX+distY*distY) > radius2) {
								return;
							}

							HittableComponent c = (HittableComponent) component;
							if(c.getType() == HittableComponent.TYPE_HAZARD) {
								entity.remove();
							}
						}
					});

	}

	private void setInvulnerabilityTime(double val) {
		invulnerabilityTimer = Math.max(invulnerabilityTimer, val);
	}

	private void miss() {
		getEntity().setX(startPosX);
		getEntity().setY(startPosY);
		setInvulnerabilityTime(invulnerabilityTimeAfterHit);
		isHit = false;
	}

	private void handleHit(Entity entity, HittableComponent c, ColliderComponent collider) {
		if(!isHit && c.getType() == HittableComponent.TYPE_HAZARD) {
			if(invulnerabilityTimer > 0) {
				return;
			}
			// Generate smaller colliders to give some collision leniency
			double colliderScale = (5.0/37.0+5.0/47.0)/2.0;
			AABB thisCollider = getColliderComponent().getAABB().localScale(colliderScale,colliderScale);
			if(!thisCollider.intersects(collider.getAABB().localScale(0.75,0.75))) {
				return;
			}

			isHit = true;
			deathBombTimer = deathBombTime;
			entity.remove();
		} else if(c.getType() == HittableComponent.TYPE_COLLECTABLE) {
			CollectableComponent collectable = (CollectableComponent)c.getEntity().getComponent(CollectableComponent.ID);
			addPoints(collectable.getPoints());
			addPower(collectable.getPower());
			entity.remove();
		}
	}

	private void addPoints(long points) {
		// TODO: 1 up based on points
		collectables.addScore(points);
	}

	private void addPower(double amt) {
		collectables.addPower(amt);
	}
}
