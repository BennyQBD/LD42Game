package game.components;

import java.util.List;
import engine.core.entity.Entity;
import engine.components.RemoveComponent;

public class SpawnOnRemove extends RemoveComponent {
	private IEntityMaker entityToSpawn;
	private double[] params;
	public SpawnOnRemove(Entity entity, IEntityMaker entityToSpawn, double[] params) {
		super(entity);
		this.entityToSpawn = entityToSpawn;
		this.params = params;
	}

	@Override
	public void onActivate() {
		System.out.println("Activating!");
		entityToSpawn.makeEntity(getEntity(), params);
		getEntity().forceRemove();
	}

	@Override
	public void removeUpdate(double delta) {}
}
