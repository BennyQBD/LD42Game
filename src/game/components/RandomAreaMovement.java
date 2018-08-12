package game.components;

import engine.util.IDAssigner;
import engine.core.entity.EntityComponent;
import engine.rendering.IRenderContext;
import engine.core.entity.Entity;
import engine.space.AABB;

public class RandomAreaMovement extends EntityComponent {
	public static final int ID = IDAssigner.getId();
	private double speed;
	private double dirX;
	private double dirY;
	private AABB movementArea;

	public RandomAreaMovement(Entity entity, double speed, AABB movementArea) {
		super(entity, ID);
		this.speed = speed;
		this.movementArea = movementArea;
		pickDirection();
	}

	private void pickDirection() {
		dirX = CMWC4096.random(-1.0, 1.0);
		dirY = CMWC4096.random(-1.0, 1.0);
		double dirMagnitude = Math.sqrt(dirX*dirX+dirY*dirY);
		dirX /= dirMagnitude;
		dirY /= dirMagnitude;
	}

	@Override
	public void update(double delta) {
		float newMoveX = 0.0f;
		float newMoveY = 0.0f;
		do {
			double moveAmtX = dirX*speed*delta;
			double moveAmtY = dirY*speed*delta;
			newMoveX = (float) (moveAmtX);
			newMoveY = (float) (moveAmtY);
			AABB aabbAtNewLocation = getEntity().getAABB().move(moveAmtX, moveAmtY);
			if(movementArea.contains(aabbAtNewLocation)) {
				break;
			}
			pickDirection();
		} while(true);

		//If object AABB at new location is within the movementArea, then move
		//Otherwise, pick a new random direction, and try moving that way instead
		getEntity().move(newMoveX, 0);
		getEntity().move(0, newMoveY);
	}
}
