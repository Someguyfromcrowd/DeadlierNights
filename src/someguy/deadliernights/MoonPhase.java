package someguy.deadliernights;

/**
 * Represents a moon phase as an integer.
 * 
 * @author Someguyfromcrowd
 *
 */
public enum MoonPhase
{
	/**
	 * 
	 */
	FULL(4), /**
	 * 
	 */
	GIBBOUS(3), /**
	 * 
	 */
	QUARTER(2), /**
	 * 
	 */
	CRESCENT(1), /**
	 * 
	 */
	NEW(0);
	
	private int bright;
	
	private MoonPhase(int val)
	{
		this.bright = val;
	}
	
	/**
	 * @return the brightness of the moon from 0-1
	 */
	public int getBright()
	{
		return this.bright;
	}
};