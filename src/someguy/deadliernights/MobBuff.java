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

	private int minBrightness; //Minimum brightness level this effect can occur at
	private int maxBrightness; //Maximum brightness level this effect can occur at

	/**
	 * @param delay
	 * @param mobs
	 * @param buffs
	 * @param healthMult 
	 * @param canDrown 
	 * @param autoChase 
	 * @param minBrightness 
	 * @param maxBrightness 
	 */
	public MobBuff(int delay, ArrayList<EntityType> mobs, ArrayList<PotionEffect> buffs, double healthMult, boolean canDrown, boolean autoChase, int minBrightness, int maxBrightness)
	{
		this.delay = delay;
		this.mobs = mobs;
		this.buffs = buffs;
		this.healthMult = healthMult;
		this.canDrown = canDrown;
		this.autoChase = autoChase;
		this.minBrightness = minBrightness;
		this.maxBrightness = maxBrightness;
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
	
	/**
	 * @param arg0
	 * @return whether or not the effect is currently active, given the current moon phase
	 */
	public boolean isRunning(MoonPhase arg0)
	{
		if (arg0.getBright() <= maxBrightness && arg0.getBright() >= minBrightness)
			return true;
		else
			return false;
	}
}
