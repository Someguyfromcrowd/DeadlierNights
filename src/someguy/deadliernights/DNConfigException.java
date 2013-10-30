package someguy.deadliernights;

/**
 * A custom exception class to make debugging somewhat simpler.
 * 
 * @author Someguyfromcrowd
 *
 */
public class DNConfigException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3667198096628108216L;
	private String message;
	
	/**
	 * Creates the exception
	 * 
	 * @param e
	 */
	public DNConfigException(String e)
	{
		this.message = e;
	}
	
	@Override
	public String getMessage()
	{
		return this.message;
	}
}
