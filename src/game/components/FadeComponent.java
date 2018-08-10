package game.components;

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


public class FadeComponent extends EntityComponent {
	public static final int ID = IDAssigner.getId();
	private double timer;
	private double duration;
	private double startDelay;
	private double startVal;
	private double endVal;

	public FadeComponent(Entity entity, double startVal, double endVal, double duration, double startDelay) {
		super(entity, ID);
		this.duration = duration;
		this.startDelay = startDelay;
		this.startVal = startVal;
		this.endVal = endVal;
		this.timer = 0.0;
	}

	@Override
	public void update(double delta) {
		if(timer >= duration) {
			getEntity().remove(this);
			return;
		}
		SpriteComponent sc = (SpriteComponent) getEntity().getComponent(
				SpriteComponent.ID);
		if(startDelay > 0.0) {
			if (sc != null) {
				sc.setTransparency(startVal);
			}
			startDelay -= delta;
			if(startDelay < 0) {
				delta = -startDelay;
			} else {
				return;
			}
		}
		timer += delta;
		if(timer >= duration) {
			timer = duration;
		}
		double amt = (duration - timer) / duration;
		double val = (startVal-endVal)*amt+endVal;
		if (sc != null) {
			sc.setTransparency(val);
		}
	}
}
