package game.components;

import engine.util.IDAssigner;
import engine.core.entity.EntityComponent;
import engine.rendering.IRenderContext;
import engine.core.entity.Entity;
import engine.space.AABB;


public class ProjectileComponent extends EntityComponent {
	public static final int ID = IDAssigner.getId();
	private double velX;
	private double velY;
	private double accX;
	private double accY;
	private double perpVel;
	private double perpAcc;
	private double speedCap;

	private final double offscreenRemoveTime;
	private double timeSinceRender = 0.0;
	private AABB screenArea;

	public double getVelocityMagnitude() {
		return Math.sqrt(velX*velX+velY*velY);
	}

	public void setVelXY(double velX, double velY) {
		this.velX = velX;
		this.velY = velY;
	}

	public ProjectileComponent(Entity entity, double velX, double velY,
	double accX, double accY, double perpVel, double perpAcc, double offscreenRemoveTime, double speedCap, AABB screenArea) {
		super(entity, ID);
		this.offscreenRemoveTime = offscreenRemoveTime;
		this.velX = velX;
		this.velY = velY;
		this.accX = accX;
		this.accY = accY;
		this.perpVel = perpVel;
		this.perpAcc = perpAcc;
		this.speedCap = speedCap;
		this.screenArea = screenArea;
	}

	@Override
	public void update(double delta) {
		double perpDirX = -velY;
		double perpDirY = velX;
		double normConst = Math.sqrt(perpDirX*perpDirX+perpDirY*perpDirY);
		if(normConst != 0) {
			perpDirX /= normConst;
			perpDirY /= normConst;
		}

		perpVel += perpAcc*delta;
		velY += accY * delta;
		velX += accX * delta;
		velY += perpDirY*perpVel*delta;
		velX += perpDirX*perpVel*delta;

		float newMoveX = (float) (delta * velX);
		float newMoveY = (float) (delta * velY);
		double speedSq = velX*velX+velY*velY;
		if(speedSq > speedCap*speedCap) {
			normConst = Math.sqrt(speedSq);
			newMoveX = (float) (delta * speedCap*velX/normConst);
			newMoveY = (float) (delta * speedCap*velY/normConst);
			velX = speedCap*velX/normConst;
			velY = speedCap*velY/normConst;
		}

		getEntity().move(newMoveX, 0);
		getEntity().move(0, newMoveY);
		removeIfOffscreenTooLong(delta);
	}

	private void removeIfOffscreenTooLong(double delta) {
		if(timeSinceRender >= offscreenRemoveTime) {
			getEntity().forceRemove();
		} else {
			if(screenArea.intersects(getEntity().getAABB())) {
				timeSinceRender = 0.0;
			} else {
				timeSinceRender += delta;
			}
		}
	}
}
