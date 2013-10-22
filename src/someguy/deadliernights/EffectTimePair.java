package someguy.deadliernights;

import org.bukkit.potion.PotionEffectType;

public class EffectTimePair
{
	private int delay;
	private PotionEffectType effect;
	private int level;
	private String text;
	private String offText;
	
	EffectTimePair(int delay, PotionEffectType effect, int level, String text, String offText)
	{
		this.delay=delay;
		this.effect=effect;
		this.level=level;
		this.text=text;
		this.offText=offText;
	}
	
	public int getDelay()
	{
		return delay;
	}
	
	public PotionEffectType getEffect()
	{
		return effect;
	}
	
	public int getLevel()
	{
		return level;
	}
	
	public String getText()
	{
		return text;
	}
	
	public String getOffText()
	{
		System.out.println(offText);
		return offText;
	}
}
