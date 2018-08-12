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


public class EnemyComponent extends EntityComponent {
	public static final int ID = IDAssigner.getId();
	private ColliderComponent colliderComponent;
	private double initialHealth;
	private double health;
	private double screenShakeAmount;
	private int numToSpawn;
	private IEntityMaker spawnOnDeath;
	private Collectables collectables;
	private double[] spawnParams;
	private int[] hittableComponentTypes;
	private List<Entity> bulletMoveLocations;
	private boolean isDead = false;

	private ColliderComponent getColliderComponent() {
		if (colliderComponent != null) {
			return colliderComponent;
		}

		colliderComponent = (ColliderComponent) getEntity().getComponent(
				ColliderComponent.ID);
		return colliderComponent;
	}

	public EnemyComponent(Entity entity, Collectables collectables, double health, double screenShakeAmount,
			int[] hittableComponentTypes, IEntityMaker spawnOnDeath, double[] spawnParams, int numToSpawn, List<Entity> bulletMoveLocations) {
		super(entity, ID);
		this.health = health;
		this.initialHealth = health;
		this.spawnOnDeath = spawnOnDeath;
		this.spawnParams = spawnParams;
		this.collectables = collectables;
		this.numToSpawn = numToSpawn;
		this.screenShakeAmount = screenShakeAmount;
		this.hittableComponentTypes = hittableComponentTypes;
		this.bulletMoveLocations = bulletMoveLocations;
	}

	@Override
	public void update(double delta) {
		handleHittable();
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
						for(int i = 0; i < hittableComponentTypes.length; i++) {
							if(c.getType() == hittableComponentTypes[i]) {
								collectables.addScore(5);
								damage(c.getDamage());
								if(bulletMoveLocations == null || bulletMoveLocations.size() == 0) {
									entity.remove();
								} else {
									int index = Math.min((int)CMWC4096.random(0, bulletMoveLocations.size()), bulletMoveLocations.size()-1);
									Entity newLocation = bulletMoveLocations.get(index);
									entity.setXY(newLocation.getX(), newLocation.getY());
								}
								break;
							}
						}
					}
				});
	}

	private void damage(double amt) {
		health -= amt;
		if(health <= 0 && !isDead) {
			collectables.addScreenShake(screenShakeAmount);
			collectables.addScore((long)(initialHealth*10));
			if(spawnOnDeath != null) {
				for(int i = 0; i < numToSpawn; i++) {
					spawnOnDeath.makeEntity(getEntity(), spawnParams);
				}
			}
			getEntity().remove();
			isDead = true;
		}
	}
}

