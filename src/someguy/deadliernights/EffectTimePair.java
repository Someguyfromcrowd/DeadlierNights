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
	private String text;
	private String offText;

	EffectTimePair(int delay, PotionEffectType effect, int level, String text, String offText)
	{
		this.delay = delay;
		this.effect = effect;
		this.level = level;
		this.text = text;
		this.offText = offText;
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
}
