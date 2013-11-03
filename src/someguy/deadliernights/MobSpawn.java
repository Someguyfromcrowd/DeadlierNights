package someguy.deadliernights;

import java.util.ArrayList;

import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;

/**
 * Similar to MobBuff, but this object is invoked to create an entirely new mob.
 * It can still apply potion effects and whatnot, however.
 * 
 * @author Someguyfromcrowd
 * 
 */
public class MobSpawn
{
	private int delay;

	private double healthMult;
	private boolean canDrown;
	private boolean autoChase;

	private EntityType mob;
	private ArrayList<PotionEffect> buffs;

	/**
	 * @param delay
	 * @param mob
	 * @param buffs
	 * @param healthMult
	 * @param canDrown
	 * @param autoChase
	 */
	public MobSpawn(int delay, EntityType mob, ArrayList<PotionEffect> buffs, double healthMult, boolean canDrown, boolean autoChase)
	{
		this.delay = delay;
		this.mob = mob;
		this.buffs = buffs;
		this.healthMult = healthMult;
		this.canDrown = canDrown;
		this.autoChase = autoChase;
	}

	/**
	 * @return the delay for the spawn
	 */
	public int getDelay()
	{
		return delay;
	}

	/**
	 * @return the mob that should be spawned
	 */
	public EntityType getMob()
	{
		return mob;
	}

	/**
	 * @return the buffs that should be applied
	 */
	public ArrayList<PotionEffect> getBuffs()
	{
		return buffs;
	}

	/**
	 * @return the health multiplier of the mob
	 */
	public double getHealthMult()
	{
		return healthMult;
	}

	/**
	 * @return whether or not the mob can drown
	 */
	public boolean canDrown()
	{
		return canDrown;
	}

	/**
	 * @return whether or not the mob should automatically chase the player who
	 *         triggered the mob spawn
	 */
	public boolean autoChase()
	{
		return autoChase;
	}

}
