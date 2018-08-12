package game;

import game.components.*;

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

/*
	Goals:
	+ Screen clear!
	+ Custom projectiles from bullet spawners (aka adding new components)
	- Bullets that themselves spawn bullets
	+ Circle spawner
	+ Delayed spawn entity
	+ Delayed entity remove
	+ Player invulnerability after spawning
	+ Bombs!
	+ Death bombs!
	- 
	+ Spawn enemy
	+ Enemy move and disappear after a while
	+ Able to shoot enemy
	= Different enemy types with different shot types
	- 
	+ Power up collectables
	- Score collectables
	+ Enemies drop collectables
	- Bombs pick up collectables
	- Collectables fly towards the player once the player is close enough to them.
	= Score
	- Lives and bomb count
	- Game over
	- Score & Power display
	-
	+ Use CMWC4096 RNG because I'm really picky about RNG like that
	- Replace EntityFactory with IEntityMakers. Because let's be real, that's all that EntityFactory is, just a collection of some hardcoded IEntityMakers.
	+ Score/Power counter class for the current global variables. Passed as object to those who modify it.
*/

/*
   New File idea:
   - Why not just use Java source files for generating the level? Easy to use and integrate into existing system.
	Old File idea:
	- "Elements" - Specify an entity component to add to an entity
	- "Combinations" - Template for parameterizing adding one or more entity components
	- "Abstractions" - Parameterize applying one or more templates
*/

public class Main {
	public static void main(String[] args) throws IOException, ParseException {
		Debug.init(false, true);
		IDisplay display = new OpenGLDisplay(480, 480, "My Display");
		CoreEngine engine = new CoreEngine(display, new TestScene(
				display.getInput(), display.getRenderDevice(),
				display.getAudioDevice()), 60.0);
		engine.start();
		display.dispose();
	}
}
