package someguy.deadliernights;

import org.bukkit.potion.PotionEffectType;

/**
 * Stores an effect, its delay, its level, and relevant strings
 * 
 * @author Someguyfromcrowd
 * 
 */
public class EffectTimePair
{
	private int delay;
	private PotionEffectType effect;
	private int level;
	private String text; //Displayed when effect turns on
	private String offText; //Display when effect turns off
	private int minBrightness; //Minimum brightness level this effect can occur at
	private int maxBrightness; //Maximum brightness level this effect can occur at
	
	EffectTimePair(int delay, PotionEffectType effect, int level, String text, String offText, int minBrightness, int maxBrightness)
	{
		this.delay = delay;
		this.effect = effect;
		this.level = level;
		this.text = text;
		this.offText = offText;
		this.minBrightness = minBrightness;
		this.maxBrightness = maxBrightness;
	}

	/**
	 * @return the delay for the effect
	 */
	public int getDelay()
	{
		return delay;
	}

	/**
	 * @return the effect's type
	 */
	public PotionEffectType getEffect()
	{
		return effect;
	}

	/**
	 * @return the effect's level
	 */
	public int getLevel()
	{
		return level;
	}

	/**
	 * @return the effect's activation text
	 */
	public String getText()
	{
		return text;
	}

	/**
	 * @return the effect's deactivation text
	 */
	public String getOffText()
	{
		System.out.println(offText);
		return offText;
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
