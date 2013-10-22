package someguy.deadliernights;

import org.bukkit.scheduler.BukkitRunnable;

public class PlayerCheckTask extends BukkitRunnable
{
	private final DeadlierNights plugin;

	public PlayerCheckTask(DeadlierNights plugin)
	{
		this.plugin = plugin;
	}

	@Override
	public void run()
	{
		plugin.checkPlayers();
	}
}
