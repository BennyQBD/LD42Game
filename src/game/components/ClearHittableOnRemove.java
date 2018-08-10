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


public class ClearHittableOnRemove extends RemoveComponent {
	private AABB area;
	private int type;
	private int numUpdates = 0;

	public ClearHittableOnRemove(Entity entity, AABB area, int type) {
		super(entity);
		this.area = area;
		this.type = type;
	}

	@Override
	public void onActivate() {
		List<EntityComponent> spawners = getEntity().getComponents(BulletSpawner.ID);
		for(EntityComponent e : spawners) {
			getEntity().remove(e);
		}
	}

	@Override
	public void removeUpdate(double delta) {
		AABB effectiveArea = getEntity().translateAABB(area);
		if(numUpdates > 0) {
			getEntity().visitInRange(HittableComponent.ID,
					effectiveArea, new IEntityVisitor() {
						@Override
						public void visit(Entity entity, EntityComponent component) {
							if(entity == getEntity()) {
								return;
							}
							ColliderComponent collider = (ColliderComponent) entity
									.getComponent(ColliderComponent.ID);
							if (collider != null
									&& !effectiveArea.intersects(collider.getAABB())) {
								return;
							}

							HittableComponent c = (HittableComponent) component;
							if(c.getType() == type) {
								entity.remove();
							}
						}
					});
			getEntity().forceRemove();
		}
		numUpdates++;
	}
}
