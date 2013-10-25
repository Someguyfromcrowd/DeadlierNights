package someguy.deadliernights;

import org.bukkit.entity.EntityType;

/**
 * A Scare is a harmless but startling event that occurs after an excessive
 * amount of exposure is accrued.
 * 
 * @author Someguyfromcrowd
 * 
 */
public class Scare
{
	private ScareType type;
	private int delay;
	private int frequency; //how many seconds should pass between checks
	private double probability; //chance of happening per frequency; 0..1
	
	/* FAKESOUND */
	private String soundToPlay;
	private int volume;
	private int range;
	
	/* FAKEMOB */
	EntityType mobToShow;

	/**
	 * Constructs a FAKESOUND Scare
	 * 
	 * @param type
	 * @param delay 
	 * @param frequency 
	 * @param probability 
	 * @param soundToPlay 
	 * @param volume 
	 * @param range 
	 */
	/* FAKEHURT */

	public Scare(ScareType type, int delay, int frequency, double probability, String soundToPlay, int volume, int range)
	{
		this.delay = delay;
		this.frequency = frequency;
		this.probability = probability;
		if (type.equals(ScareType.FAKESOUND))
		{
			this.soundToPlay = soundToPlay;
			this.volume = volume;
			this.range = range;
		}
		else
		{
			this.soundToPlay="";
			this.volume = 1;
			this.range = 10;
		}
	}
	
	/**
	 * @param type
	 * @param delay
	 * @param arg0
	 */
	public Scare(ScareType type, int delay, EntityType arg0)
	{
		this.delay = delay;
		if (type.equals(ScareType.FAKEMOB))
		{
			this.mobToShow = arg0;
		}
		else
		{
			this.mobToShow=EntityType.ZOMBIE;
		}
	}
	
	/**
	 * @return the type of scare
	 */
	public ScareType getType()
	{
		return type;
	}
	
	/**
	 * @return the onset delay of the scare
	 */
	public int getDelay()
	{
		return delay;
	}

	/**
	 * @return the frequency at which the scare will trigger
	 */
	public int getFrequency()
	{
		return frequency;
	}
	
	/**
	 * @return the sound which the scare will play
	 */
	public String getSoundToPlay()
	{
		if (type.equals(ScareType.FAKESOUND))
			return soundToPlay;
		else
			return "";
	}
	
	/**
	 * @return the volume at which the scale will play
	 */
	public int getVolume()
	{
		if (type.equals(ScareType.FAKESOUND))
			return volume;
		else
			return 0;
	}
	
	/**
	 * @return how far away the scare can play 
	 */
	public int getRange()
	{
		if (type.equals(ScareType.FAKESOUND))
			return range;
		else
			return 0;
	}
	
	/**
	 * @return whether or not the scare should trigger
	 */
	public boolean triggerScare()
	{
		return (Math.random() < probability);
	}
}
