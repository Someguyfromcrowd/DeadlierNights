package someguy.deadliernights;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * A simple task to run the checkPlayers() method every second
 * 
 * @author Someguyfromcrowd
 *
 */
public class PlayerCheckTask extends BukkitRunnable
{
	private final DeadlierNights plugin;

	/**
	 * Constructs the task
	 * @param plugin
	 */
	public PlayerCheckTask(DeadlierNights plugin)
	{
		this.plugin = plugin;
	}

	@Override
	public void run()
	{
		plugin.checkPlayers();
		plugin.scarePlayers();
	}
}
