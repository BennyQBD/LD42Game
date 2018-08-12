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

public class ProjectileDirecting extends EntityComponent {
	public static final int ID = IDAssigner.getId();

	public ProjectileDirecting(Entity entity) {
		super(entity, ID);
	}

	@Override
	public void update(double delta) {
		BulletSpawnerAimer bulletSpawnerAimer = (BulletSpawnerAimer)getEntity().getComponent(
				BulletSpawnerAimer.ID);
		ProjectileComponent projectileComponent = (ProjectileComponent)getEntity().getComponent(
				ProjectileComponent.ID);
		if(projectileComponent != null && bulletSpawnerAimer != null) {
			double velocity = projectileComponent.getVelocityMagnitude();
			double dirX = bulletSpawnerAimer.getTargetDirX(getEntity().getX(), getEntity().getY());
			double dirY = bulletSpawnerAimer.getTargetDirY(getEntity().getX(), getEntity().getY());
			projectileComponent.setVelXY(dirX*velocity, dirY*velocity);
		}
	}
}