package game.components;

import engine.util.IDAssigner;
import engine.core.entity.EntityComponent;
import engine.rendering.IRenderContext;
import engine.core.entity.Entity;

public class DelayedSpawn extends EntityComponent {
	public static final int ID = IDAssigner.getId();
	private double delay;
	private double delayBetweenSpawns;
	private IEntityMaker entityMaker;
	private double[] entityMakerParams;
	private int numSpawned = 0;
	private int numToSpawn;

	public DelayedSpawn(Entity entity, double delay, IEntityMaker entityMaker, double[] entityMakerParams) {
		this(entity, delay, entityMaker, entityMakerParams, 1, 0.0);
	}

	public DelayedSpawn(Entity entity, double delay, IEntityMaker entityMaker, double[] entityMakerParams, int numEntities, double delayBetweenSpawns) {
		super(entity, ID);
		this.delay = delay;
		this.entityMaker = entityMaker;

		int numEntityMakerParams = entityMakerParams == null ? 0 : entityMakerParams.length;
		this.entityMakerParams = new double[numEntityMakerParams+2];
		for(int i = 0; i < numEntityMakerParams; i++) {
			this.entityMakerParams[i] = entityMakerParams[i];
		}

		this.numToSpawn = numEntities;
		this.delayBetweenSpawns = delayBetweenSpawns;
	}

	@Override
	public void update(double delta) {
		delay -= delta;
		if(delay <= 0 && numSpawned < numToSpawn) {
			entityMakerParams[entityMakerParams.length-1] = getEntity().getX();
			entityMakerParams[entityMakerParams.length-2] = getEntity().getY();
			entityMaker.makeEntity(getEntity(), entityMakerParams);
			numSpawned++;
			delay = delayBetweenSpawns;
		}
	}
}

