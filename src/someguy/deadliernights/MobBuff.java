package someguy.deadliernights;

import java.util.ArrayList;

import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;

/**
 * Stores a list of mobs and the potion buffs that should be applied to them at
 * a particular time
 * 
 * @author Someguyfromcrowd
 * 
 */
public class MobBuff
{
	private int delay;
	
	private double healthMult;
	private boolean canDrown;
	private boolean autoChase;
	
	private ArrayList<EntityType> mobs;
	private ArrayList<PotionEffect> buffs;

	/**
	 * @param delay
	 * @param mobs
	 * @param buffs
	 * @param healthMult 
	 * @param canDrown 
	 * @param autoChase 
	 */
	public MobBuff(int delay, ArrayList<EntityType> mobs, ArrayList<PotionEffect> buffs, double healthMult, boolean canDrown, boolean autoChase)
	{
		this.delay = delay;
		this.mobs = mobs;
		this.buffs = buffs;
		this.healthMult = healthMult;
		this.canDrown = canDrown;
		this.autoChase = autoChase;
	}

	/**
	 * @return the delay for the buffs
	 */
	public int getDelay()
	{
		return delay;
	}

	/**
	 * @return the mobs that should be buffed
	 */
	public ArrayList<EntityType> getMobs()
	{
		return mobs;
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
	 * @return whether or not the mob should automatically chase the player who triggered the mob buff
	 */
	public boolean autoChase()
	{
		return autoChase;
	}
}
