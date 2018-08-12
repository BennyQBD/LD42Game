package game.components;

import java.util.List;
import engine.core.entity.Entity;
import engine.components.RemoveComponent;

public class ListRemoveComponent extends RemoveComponent {
	private List<Entity> list;
	public ListRemoveComponent(Entity entity, List<Entity> list) {
		super(entity);
		this.list = list;
	}

	@Override
	public void onActivate() {
		list.remove(getEntity());
		getEntity().forceRemove();
	}

	@Override
	public void removeUpdate(double delta) {}
}
