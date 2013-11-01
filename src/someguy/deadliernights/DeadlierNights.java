package someguy.deadliernights;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 * The core class of DeadlierNights. Starts all scheduled events and handles
 * processing.
 * 
 * @author Someguyfromcrowd
 * 
 */
public class DeadlierNights extends JavaPlugin implements Listener
{
	private boolean chat;
	private boolean log;

	private boolean potionEnable;
	private boolean mobEnable;
	private boolean scareEnable;
	private boolean fatigueEnable; // TODO: Implement fatigue
	
	private boolean configError;
	private boolean potionError;
	private boolean mobError;
	private boolean scareError;
	//private boolean fatigueError;

	private int decayRate;
	private int shortestDelay;
	private ArrayList<EffectTimePair> effects;
	private ArrayList<MobBuff> mobBuffs;
	private ArrayList<Scare> scares;

	private ArrayList<String> defined;
	private HashMap<String, Integer> playerMap;
	private HashMap<String, Boolean> exemptMap;
	private HashMap<String, HashMap<Scare, Integer>> scareMap;

	@Override
	public void onEnable()
	{
		loadConfig();
		loadEffects();
		loadMobBuffs();
		loadScares();
		playerMap = new HashMap<String, Integer>();
		exemptMap = new HashMap<String, Boolean>();
		scareMap = new HashMap<String, HashMap<Scare, Integer>>();
		shortestDelay = getShortestDelay();
		getServer().getPluginManager().registerEvents(this, this);
		for (Player player : getServer().getOnlinePlayers())
		{
			playerMap.put(player.getName(), 0);
			exemptMap.put(player.getName(), player.hasPermission("deadliernights.exempt"));
			scareMap.put(player.getName(), new HashMap<Scare, Integer>());
			for (Scare scare : scares)
			{
				scareMap.get(player.getName()).put(scare, -1 * scare.getExtraFrequencyRandom());
			}
		}

		@SuppressWarnings("unused")
		BukkitTask checker = new PlayerCheckTask(this).runTaskTimer(this, 0, 20L);
	}

	/**
	 * Goes through the list of players and increments their exposure counters
	 * if they are in total darkness. If not, their counters are either reset or
	 * decremented. Any relevant potion effects are either added or removed.
	 */
	public void checkPlayers()
	{
		for (Player player : getServer().getOnlinePlayers())
		{
			String playerName = player.getName();
			if (!playerMap.containsKey(playerName))
			{
				playerMap.put(playerName, 0);
				exemptMap.put(playerName, player.hasPermission("deadliernights.exempt"));
				scareMap.put(playerName, new HashMap<Scare, Integer>());
				for (Scare scare : scares)
				{
					scareMap.get(playerName).put(scare, -1 * scare.getExtraFrequencyRandom());
				}
			}

			if (!exemptMap.get(playerName).booleanValue() && getServer().getWorld(player.getWorld().getName()).getTime() % 24000 >= 14000 && player.getLocation().getBlock().getLightFromBlocks() == 0 && !(player.getGameMode().equals(GameMode.CREATIVE)))
			{
				playerMap.put(playerName, playerMap.get(playerName) + 1);
				int current = playerMap.get(playerName);

				if (potionEnable)
				{
					for (EffectTimePair pair : effects)
					{

						if (pair.getDelay() < current)
						{
							if (!player.hasPotionEffect(pair.getEffect()))
							{
								player.addPotionEffect(new PotionEffect(pair.getEffect(), Integer.MAX_VALUE, pair.getLevel()), true);
								if (log)
									getLogger().info("Applied " + pair.getEffect().getName() + " " + pair.getLevel() + " to " + playerName);
							}
						}
						else if (pair.getDelay() == current)
						{
							player.addPotionEffect(new PotionEffect(pair.getEffect(), Integer.MAX_VALUE, pair.getLevel()), true);
							if (log)
								getLogger().info("Applied " + pair.getEffect().getName() + " " + pair.getLevel() + " to " + playerName);
							if (chat && !pair.getText().equals(""))
								player.sendMessage(pair.getText());
						}
					}
				}
			}
			else if (playerMap.get(playerName) > 0)
			{
				if (decayRate == 0)
				{
					if (playerMap.get(playerName) >= shortestDelay)
					{
						if (log)
							getLogger().info("Debuffs removed from " + playerName);
						if (chat)
							player.sendMessage("You escape from the darkness.");
						for (EffectTimePair pair : effects)
						{
							player.removePotionEffect(pair.getEffect());
						}
					}
					playerMap.put(playerName, 0);
				}
				else
				{
					int startVal = playerMap.get(playerName);
					playerMap.put(playerName, playerMap.get(player.getName()) - decayRate);
					if (playerMap.get(playerName) < 0)
						playerMap.put(playerName, 0);

					for (EffectTimePair pair : effects)
					{
						if (player.hasPotionEffect(pair.getEffect()) && playerMap.get(playerName) < pair.getDelay() && startVal >= pair.getDelay())
						{
							player.removePotionEffect(pair.getEffect());
							if (chat && !pair.getOffText().equals(""))
								player.sendMessage(pair.getOffText());
						}
					}
				}
			}
		}
	}

	/**
	 * Reacts to monster spawns by applying potion effects if needed
	 * 
	 * @param e
	 */

	@EventHandler
	public void onCreatureSpawn(CreatureSpawnEvent e)
	{
		if (mobEnable)
		{
			for (Player player : getServer().getOnlinePlayers())
			{
				String playerName = player.getName();
				if (!playerMap.containsKey(playerName))
				{
					playerMap.put(playerName, 0);
					exemptMap.put(playerName, player.hasPermission("deadliernights.exempt"));
					scareMap.put(playerName, new HashMap<Scare, Integer>());
					for (Scare scare : scares)
					{
						scareMap.get(playerName).put(scare, -1 * scare.getExtraFrequencyRandom());
					}
				}
				if (player.getLocation().distance(e.getLocation()) <= 32)
				{
					for (MobBuff buff : mobBuffs)
					{
						if (buff.getMobs().contains(e.getEntityType()) && buff.getDelay() < playerMap.get(player.getName()))
						{
							for (PotionEffect eff : buff.getBuffs())
							{
								e.getEntity().addPotionEffect(eff, true);
							}

							if (buff.getHealthMult() != 1)
								e.getEntity().setMaxHealth(e.getEntity().getMaxHealth() * buff.getHealthMult());

							if (!buff.canDrown())
								e.getEntity().setMaximumAir(Integer.MAX_VALUE);

							if (buff.autoChase())
								((Creature) e.getEntity()).setTarget(player);

						}
					}
				}
			}
		}
	}

	/**
	 * Checks to see if players have exceeded the scare thresholds and performs
	 * relevant events if this is the case
	 */
	public void scarePlayers()
	{
		if (scareEnable)
		{
			for (Player player : getServer().getOnlinePlayers())
			{
				String playerName = player.getName();
				if (!playerMap.containsKey(playerName))
				{
					playerMap.put(playerName, 0);
					exemptMap.put(playerName, player.hasPermission("deadliernights.exempt"));
					scareMap.put(playerName, new HashMap<Scare, Integer>());
					for (Scare scare : scares)
					{
						scareMap.get(playerName).put(scare, -1 * scare.getExtraFrequencyRandom());
					}
				}

				for (Scare scare : scares)
				{
					if (scare.getDelay() <= playerMap.get(playerName))
					{
						if (scareMap.get(playerName).get(scare) > scare.getFrequency() && scare.triggerScare())
						{
							if (scare.getType().equals(ScareType.FAKESOUND))
							{
								int randomX = player.getLocation().getBlockX() + ((int) (2 * Math.random() * scare.getRange())) - scare.getRange();
								int randomY = player.getLocation().getBlockY();
								int randomZ = player.getLocation().getBlockZ() + ((int) (2 * Math.random() * scare.getRange())) - scare.getRange();
								player.playSound(new Location(player.getWorld(), randomX, randomY, randomZ), Sound.valueOf(scare.getSoundToPlay().toUpperCase()), 1, 1);
							}
							scareMap.get(playerName).put(scare, -1 * scare.getExtraFrequencyRandom());
						}
						else
						{
							scareMap.get(playerName).put(scare, scareMap.get(playerName).get(scare) + 1);
						}
					}
				}
			}
		}
	}

	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onGamemodeChange(PlayerGameModeChangeEvent event)
	{
		if (event.getNewGameMode().equals(GameMode.CREATIVE))
		{
			if (playerMap.containsKey(event.getPlayer().getName()))
			{
				playerMap.put(event.getPlayer().getName(), 0);
				curePlayer(event.getPlayer());
			}
		}

	}

	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerRespawn(PlayerRespawnEvent event)
	{
		String playerName = event.getPlayer().getName();
		if (!playerMap.containsKey(playerName))
		{
			playerMap.put(playerName, 0);
			exemptMap.put(playerName, event.getPlayer().hasPermission("deadliernights.exempt"));
			scareMap.put(playerName, new HashMap<Scare, Integer>());
			for (Scare scare : scares)
			{
				scareMap.get(playerName).put(scare, -1 * scare.getExtraFrequencyRandom());
			}
		}
		playerMap.put(event.getPlayer().getName(), 0);
		for (EffectTimePair pair : effects)
		{
			if (event.getPlayer().hasPotionEffect(pair.getEffect()) && playerMap.get(event.getPlayer().getName()) < pair.getDelay())
			{
				event.getPlayer().removePotionEffect(pair.getEffect());
			}
		}
	}

	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerLogin(PlayerJoinEvent event)
	{
		String playerName = event.getPlayer().getName();
		if (!playerMap.containsKey(playerName))
		{
			playerMap.put(playerName, 0);
			exemptMap.put(playerName, event.getPlayer().hasPermission("deadliernights.exempt"));
			scareMap.put(playerName, new HashMap<Scare, Integer>());
			for (Scare scare : scares)
			{
				scareMap.get(playerName).put(scare, -1 * scare.getExtraFrequencyRandom());
			}
		}
		for (EffectTimePair pair : effects)
		{
			if (event.getPlayer().hasPotionEffect(pair.getEffect()) && playerMap.get(event.getPlayer().getName()) < pair.getDelay())
			{
				event.getPlayer().removePotionEffect(pair.getEffect());
			}
		}
	}

	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerLogout(PlayerQuitEvent event)
	{
		String playerName = event.getPlayer().getName();
		if (!playerMap.containsKey(playerName))
		{
			playerMap.put(playerName, 0);
			exemptMap.put(playerName, event.getPlayer().hasPermission("deadliernights.exempt"));
			scareMap.put(playerName, new HashMap<Scare, Integer>());
			for (Scare scare : scares)
			{
				scareMap.get(playerName).put(scare, -1 * scare.getExtraFrequencyRandom());
			}
		}
		for (EffectTimePair pair : effects)
		{
			if (event.getPlayer().hasPotionEffect(pair.getEffect()) && playerMap.get(event.getPlayer().getName()) < pair.getDelay())
			{
				event.getPlayer().removePotionEffect(pair.getEffect());
			}
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase("DNchat"))
		{
			if (args.length == 0)
			{
				if (chat)
					sender.sendMessage("[DeadlierNights]: Chat messages are enabled");
				else
					sender.sendMessage("[DeadlierNights]: Chat messages are disabled");
				return true;
			}
			else if (args[0].equalsIgnoreCase("true") || args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("on"))
			{
				chat = true;
				sender.sendMessage("[DeadlierNights]: Chat messages enabled.");
				return true;
			}
			else if (args[0].equalsIgnoreCase("false") || args[0].equalsIgnoreCase("disable") || args[0].equalsIgnoreCase("off"))
			{
				chat = false;
				sender.sendMessage("[DeadlierNights]: Chat messages disabled.");
				return true;
			}
			else
				return false;

		}
		else if (cmd.getName().equalsIgnoreCase("DNlog"))
		{
			if (args.length == 0)
			{
				if (log)
					sender.sendMessage("[DeadlierNights]: Log messages are enabled");
				else
					sender.sendMessage("[DeadlierNights]: Log messages are disabled");
				return true;
			}
			else if (args[0].equalsIgnoreCase("true") || args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("on"))
			{
				log = true;
				sender.sendMessage("[DeadlierNights]: Log messages enabled.");
				return true;
			}
			else if (args[0].equalsIgnoreCase("false") || args[0].equalsIgnoreCase("disable") || args[0].equalsIgnoreCase("off"))
			{
				log = false;
				sender.sendMessage("[DeadlierNights]: Log messages disabled.");
				return true;
			}
			else
			{
				return false;
			}
		}
		else if (cmd.getName().equalsIgnoreCase("DNpotion"))
		{
			if (args.length == 0)
			{
				if (potionEnable)
					sender.sendMessage("[DeadlierNights]: Potion debuffs are enabled");
				else
					sender.sendMessage("[DeadlierNights]: Potion debuffs are disabled");
				return true;
			}
			else
			{
				if (args[0].equalsIgnoreCase("true") || args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("on"))
				{
					potionEnable = true;
					sender.sendMessage("[DeadlierNights]: Potion debuffs enabled.");
					return true;
				}
				else if (args[0].equalsIgnoreCase("false") || args[0].equalsIgnoreCase("disable") || args[0].equalsIgnoreCase("off"))
				{
					potionEnable = false;
					sender.sendMessage("[DeadlierNights]: Potion debuffs disabled.");

					for (Player player : getServer().getOnlinePlayers())
					{
						String playerName = player.getName();
						if (!playerMap.containsKey(playerName))
						{
							playerMap.put(playerName, 0);
							exemptMap.put(playerName, player.hasPermission("deadliernights.exempt"));
							scareMap.put(playerName, new HashMap<Scare, Integer>());
							for (Scare scare : scares)
							{
								scareMap.get(player.getName()).put(scare, -1 * scare.getExtraFrequencyRandom());
							}
						}

						for (EffectTimePair pair : effects)
							if (player.hasPotionEffect(pair.getEffect()) && playerMap.get(player.getName()) >= pair.getDelay())
							{
								player.removePotionEffect(pair.getEffect());
							}

					}

					return true;
				}
				else
					return false;
			}
		}
		else if (cmd.getName().equalsIgnoreCase("DNmob"))
		{
			if (args.length == 0)
			{
				if (mobEnable)
					sender.sendMessage("[DeadlierNights]: Mob buffs are enabled");
				else
					sender.sendMessage("[DeadlierNights]: Mob buffs are disabled");
				return true;
			}
			else
			{
				if (args[0].equalsIgnoreCase("true") || args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("on"))
				{
					mobEnable = true;
					sender.sendMessage("[DeadlierNights]: Mob buffs enabled.");
					return true;
				}
				else if (args[0].equalsIgnoreCase("false") || args[0].equalsIgnoreCase("disable") || args[0].equalsIgnoreCase("off"))
				{
					mobEnable = false;
					sender.sendMessage("[DeadlierNights]: Mob buffs disabled.");
					return true;
				}
				else
					return false;
			}
		}
		else if (cmd.getName().equalsIgnoreCase("DNscare"))
		{
			if (args.length == 0)
			{
				if (scareEnable)
					sender.sendMessage("[DeadlierNights]: Scares are enabled");
				else
					sender.sendMessage("[DeadlierNights]: Scares are disabled");
				return true;
			}
			else
			{
				if (args[0].equalsIgnoreCase("true") || args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("on"))
				{
					scareEnable = true;
					sender.sendMessage("[DeadlierNights]: Scares enabled.");
					return true;
				}
				else if (args[0].equalsIgnoreCase("false") || args[0].equalsIgnoreCase("disable") || args[0].equalsIgnoreCase("off"))
				{
					scareEnable = false;
					sender.sendMessage("[DeadlierNights]: Scares disabled.");
					return true;
				}
				else
					return false;
			}
		}
		else if (cmd.getName().equalsIgnoreCase("DNfatigue"))
		{
			if (args.length == 0)
			{
				if (fatigueEnable)
					sender.sendMessage("[DeadlierNights]: Fatigue is enabled");
				else
					sender.sendMessage("[DeadlierNights]: Fatigue is disabled");
				return true;
			}
			else
			{
				if (args[0].equalsIgnoreCase("true") || args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("on"))
				{
					fatigueEnable = true;
					sender.sendMessage("[DeadlierNights]: Fatigue enabled.");
					return true;
				}
				else if (args[0].equalsIgnoreCase("false") || args[0].equalsIgnoreCase("disable") || args[0].equalsIgnoreCase("off"))
				{
					fatigueEnable = false;
					sender.sendMessage("[DeadlierNights]: Fatigue disabled.");
					return true;
				}
				else
					return false;
			}
		}
		else if (cmd.getName().equalsIgnoreCase("DNDecay"))
		{
			if (args.length == 0)
			{
				if (decayRate == 0)
					sender.sendMessage("[DeadlierNights]: Decay is currently disabled.");
				else
					sender.sendMessage("[DeadlierNights]: The decay is currently set to " + decayRate + "/sec");
				return true;
			}
			else
			{
				try
				{
					int input = Integer.parseInt(args[0]);
					decayRate = input;
					sender.sendMessage("[DeadlierNights]: The decay is now set to " + decayRate + "/sec");
				}
				catch (NumberFormatException e)
				{
					return false;
				}
				return true;
			}
		}
		else if (cmd.getName().equalsIgnoreCase("DNreload"))
		{
			reload();
			sender.sendMessage("[DeadlierNights]: All configuration files reloaded!");
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("DNexempt"))
		{
			if (args.length == 0)
			{
				if (exemptMap.get(sender.getName()).booleanValue())
					sender.sendMessage("[DeadlierNights]: You are immune to DeadlierNights");
				else
					sender.sendMessage("[DeadlierNights]: You are not immune to DeadlierNights");
				return true;
			}
			else if (args[0].equalsIgnoreCase("true") || args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("on"))
			{
				exemptMap.put(sender.getName(), Boolean.TRUE);
				sender.sendMessage("[DeadlierNights]: You are now immune to DeadlierNights.");
				return true;
			}
			else if (args[0].equalsIgnoreCase("false") || args[0].equalsIgnoreCase("disable") || args[0].equalsIgnoreCase("off"))
			{
				exemptMap.put(sender.getName(), Boolean.FALSE);
				sender.sendMessage("[DeadlierNights]: You are no longer immune to DeadlierNights.");
				return true;
			}
			else
			{
				return false;
			}
		}
		else if (cmd.getName().equalsIgnoreCase("DNexempto"))
		{
			if (args.length == 0)
			{
				return false;
			}
			else
			{
				String playerName = pickPlayer(args[0]);
				if (playerName.equals("<FAILED>"))
				{
					sender.sendMessage("[DeadlierNights]: Couldn't pick a player with that name.");
					return false;
				}
				else
				{
					if (args.length == 1)
					{
						if (exemptMap.get(playerName).booleanValue())
						{
							sender.sendMessage("[DeadlierNights]: " + playerName + " is exempt from DeadlierNights");
							return true;
						}
						else
						{
							sender.sendMessage("[DeadlierNights]: " + playerName + " is not exempt from DeadlierNights");
							return true;
						}
					}
					else if (args.length == 2)
					{
						if (args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("on"))
						{
							exemptMap.put(playerName, Boolean.TRUE);
							sender.sendMessage("[DeadlierNights]: " + playerName + " is now exempt from DeadlierNights");
							getServer().getPlayer(playerName).sendMessage("[DeadlierNights]: You are now immune to DeadlierNights.");
							return true;
						}
						else if (args[1].equalsIgnoreCase("false") || args[1].equalsIgnoreCase("disable") || args[1].equalsIgnoreCase("off"))
						{
							exemptMap.put(playerName, Boolean.FALSE);
							sender.sendMessage("[DeadlierNights]: " + playerName + " is no longer exempt from DeadlierNights");
							getServer().getPlayer(playerName).sendMessage("[DeadlierNights]: You are no longer immune to DeadlierNights.");
							return true;
						}
						else
							return false;
					}
					else
						return false;
				}

			}
		}
		else if (cmd.getName().equalsIgnoreCase("DNcure"))
		{
			if (args.length >= 1)
			{
				Player player = getServer().getPlayer(pickPlayer(args[0]));
				this.curePlayer(player);
				return true;
			}
			else
			{
				Player player = getServer().getPlayer(sender.getName());
				this.curePlayer(player);
				return true;
			}
		}
		else if (cmd.getName().equalsIgnoreCase("DNhelp"))
		{
			if (args.length == 0)
			{
				sender.sendMessage("[DeadlierNights]: For help, type one of the following commands:");
				sender.sendMessage("Info: /dnhelp info");
				sender.sendMessage("Version: /dnhelp version");
				return true;
			}
			else if (args.length >= 1)
			{
				if (args[0].equalsIgnoreCase("info"))
				{
					sender.sendMessage("DeadlierNights is a plugin designed to make nighttime survival more difficult. Staying in the dark for too long at night can afflict you with debuffs, make mobs stronger, and more.");
					return true;
				}
				else if (args[0].equalsIgnoreCase("version"))
				{
					sender.sendMessage("The current version of DeadlierNights is " + this.getDescription().getVersion());
					return true;
				}
				else
					return false;
			}
			else
				return false;
		}
		else if (cmd.getName().equalsIgnoreCase("dnstatus"))
		{
			if (args.length == 0)
			{
				sender.sendMessage("[DeadlierNights]: Type a command below to view DN's status:");
				sender.sendMessage("/dnstatus config: View information about config files");
				sender.sendMessage("/dnstatus players: View information about player exposure");
				return true;
			}
			else if (args.length == 1)
			{
				if (args[0].equalsIgnoreCase("config"))
				{
					sender.sendMessage("[DeadlierNights]: The following configuration files are loaded");
					if (configError)
						sender.sendMessage("config: ERROR");
					else
						sender.sendMessage("config: loaded");
					if (potionError)
						sender.sendMessage("effects: ERROR; " + effects.size() + " effects loaded");
					else if (!potionEnable)
						sender.sendMessage("effects: disabled; " + effects.size() + " effects loaded");
					else
						sender.sendMessage("effects: loaded; " + effects.size() + " effects loaded");
					if (mobError)
						sender.sendMessage("mobBuffs: ERROR; " + effects.size() + " mob buffs loaded");
					else if (!mobEnable)
						sender.sendMessage("mobBuffs: disabled; " + mobBuffs.size() + " mob buffs loaded");
					else
						sender.sendMessage("mobBuffs: loaded; " + mobBuffs.size() + " mob buffs  loaded");
					if (scareError)
						sender.sendMessage("scares: ERROR; " + scares.size() + " scares loaded");
					else if (!scareEnable)
						sender.sendMessage("scares: disabled; " + scares.size() + " scares loaded");
					else
						sender.sendMessage("scares: loaded; " + scares.size() + " scares loaded");
					return true;
				}
				return false;
			}
			else
				return false;
		}
		else
		{
			return false;
		}

	}

	private void loadConfig()
	{
		configError = false;
		defined = new ArrayList<String>();
		Scanner scanner;
		int line = 0;
		try
		{
			scanner = new Scanner(new File("plugins\\DeadlierNights\\config.txt"));
			try
			{
				String input = "";
				while (scanner.hasNext())
				{
					try
					{
						line++;
						input = scanner.nextLine();
						if (input.contains("decay rate:"))
						{
							decayRate = intConfigCheck(input, "decay rate:");
							if (decayRate < 0)
								throw new DNConfigException("Error: decay rate must have a value greater or equal to zero on line " + line);
						}

						else if (input.contains("chat:"))
							chat = boolConfigCheck(input, "chat:");
						else if (input.contains("log:"))
							log = boolConfigCheck(input, "log:");
						else if (input.contains("potionDebuff:"))
							potionEnable = boolConfigCheck(input, "potionDebuff:");
						else if (input.contains("mobBuff:"))
							mobEnable = boolConfigCheck(input, "mobBuff:");
						else if (input.contains("scare:"))
							scareEnable = boolConfigCheck(input, "scare:");
						else if (input.contains("fatigue:"))
							fatigueEnable = boolConfigCheck(input, "fatigue:");
					}
					catch (DNConfigException e)
					{
						configError=true;
						getLogger().warning(e.getMessage() + line);
					}
					catch (NumberFormatException e)
					{
						configError=true;
						getLogger().warning("Error: Can't read an entry in config.txt (is there a typo on line " + line + "?)");
					}
				}
			}
			finally
			{
				scanner.close();
			}
		}
		catch (FileNotFoundException e)
		{
			try
			{
				getLogger().info(new java.io.File(".").getCanonicalPath());
			}
			catch (IOException e1)
			{
				configError=true;
				e1.printStackTrace();
			}
			try
			{
				(new File("plugins\\DeadlierNights")).mkdirs();
				PrintWriter out = new PrintWriter(new File("plugins\\DeadlierNights\\config.txt"));
				out.println("decay rate: 0");
				out.println("chat: true");
				out.println("log: true");
				out.println("potionDebuff: true");
				out.println("mobBuff: true");
				out.close();
				loadConfig();
			}
			catch (IOException ee)
			{
				configError=true;
				getLogger().warning(ee.getMessage());
			}
		}
	}

	private void loadEffects()
	{
		potionError = false;
		boolean ready = true;
		effects = new ArrayList<EffectTimePair>();
		Scanner scanner;
		int line = 0;
		try
		{
			scanner = new Scanner(new File("plugins\\DeadlierNights\\effects.txt"));
			try
			{
				String input = "";
				while (scanner.hasNext())
				{
					boolean done = false;
					int newDelay = -1;
					PotionEffectType newEffect = null;
					int newLevel = 0;
					String newText = "";
					String newOffText = "";
					if (!ready)
						ready = scanner.nextLine().contains("---");
					while (ready && !done)
					{
						try
						{
							line++;
							input = scanner.nextLine();
							if (input.contains("delay:"))
							{
								newDelay = Integer.parseInt(input.replaceAll("delay:", "").replaceAll("^[ \t]+", ""));
								if (newDelay <= 0)
									throw new DNConfigException("Error: delay must have a value greater than zero on line " + line);
							}

							else if (input.contains("effect:"))
							{
								newEffect = PotionEffectType.getByName(stringConfigCheck(input, "effect:").replaceAll("[ \t]+[0-9]+$", "").toUpperCase());
								newLevel = Integer.parseInt(stringConfigCheck(input, "effect:").replaceAll("^[A-Z,a-z]*[ \t]+", "")) - 1;
							}
							else if (input.contains("offtext:"))
								newOffText = stringConfigCheck(input, "offtext:");
							else if (input.contains("text:"))
								newText = stringConfigCheck(input, "text:");
							if (input.contains("---"))
							{
								done = true;
								if (newDelay == -1 || newEffect == null)
									throw new DNConfigException("Error: delay or effect is missing from an effects entry on line " + line);
								effects.add(new EffectTimePair(newDelay, newEffect, newLevel, newText, newOffText));
							}
						}
						catch (DNConfigException e)
						{
							getLogger().warning("ERROR: Can't read an entry in effects.txt (is there a typo on line " + line + "?)");
							potionError = true;
							ready = false;
						}
						catch (NumberFormatException e)
						{
							getLogger().warning("ERROR: Can't read an entry in effects.txt (is there a typo on line " + line + "?)");
							potionError = true;
							ready = false;
						}
					}
				}
			}
			finally
			{
				scanner.close();
			}
		}
		catch (FileNotFoundException e)
		{
			try
			{
				getLogger().info(new java.io.File(".").getCanonicalPath());
			}
			catch (IOException e1)
			{
				potionError = true;
				e1.printStackTrace();
			}
			try
			{
				(new File("plugins\\DeadlierNights")).mkdirs();
				PrintWriter out = new PrintWriter(new File("plugins\\DeadlierNights\\effects.txt"));
				out.println("delay: 5");
				out.println("effect: SLOW");
				out.println("level: 1");
				out.println("text: Example Text 1");
				out.println("offtext: Example Off Text 1");
				out.println("---");
				out.println("delay: 10");
				out.println("effect: WEAKNESS");
				out.println("level: 2");
				out.println("text: Example Text 2");
				out.println("offtext: Example Off Text 2");
				out.println("---");
				out.println("delay: 15");
				out.println("effect: BLINDNESS");
				out.println("level: 3");
				out.println("text: Example Text 3");
				out.println("---");
				out.close();
				loadEffects();
			}
			catch (IOException ee)
			{
				potionError = true;
				getLogger().warning(ee.getMessage());
			}
		}
	}

	private void loadMobBuffs()
	{
		mobError = false;
		boolean ready = true;
		mobBuffs = new ArrayList<MobBuff>();
		Scanner scanner;
		int line = 0;
		try
		{
			scanner = new Scanner(new File("plugins\\DeadlierNights\\mobBuffs.txt"));
			try
			{
				String input = "";
				while (scanner.hasNext())
				{
					boolean done = false;
					int newDelay = -1;
					PotionEffectType newEffect = null;
					int newLevel = 0;
					ArrayList<EntityType> mobs = new ArrayList<EntityType>();
					ArrayList<PotionEffect> effects = new ArrayList<PotionEffect>();
					double healthMult = 1;
					boolean canDrown = true;
					boolean autoChase = false;

					if (!ready)
						ready = scanner.nextLine().contains("---");

					while (!done && ready)
					{
						try
						{
							line++;
							input = scanner.nextLine();
							if (input.contains("delay:"))
								newDelay = Integer.parseInt(input.replaceAll("delay:", "").replaceAll("^[ \t]+", ""));
							else if (input.contains("mob:"))
								mobs.add(EntityType.valueOf(stringConfigCheck(input, "mob:").toUpperCase()));
							else if (input.contains("effect:"))
							{
								newEffect = PotionEffectType.getByName(stringConfigCheck(input, "effect:").replaceAll("[ \t]+[0-9]+$", ""));
								newLevel = Integer.parseInt(stringConfigCheck(input, "effect:").replaceAll("^[A-Z,a-z]*[ \t]+", "")) - 1;
								effects.add(new PotionEffect(newEffect, Integer.MAX_VALUE, newLevel));
							}
							else if (input.contains("healthMult:"))
								healthMult = doubleConfigCheck(input, "healthMult:");
							else if (input.contains("canDrown:"))
								canDrown = boolConfigCheck(input, "canDrown:");
							else if (input.contains("autoChase:"))
								autoChase = boolConfigCheck(input, "autoChase:");
							if (input.contains("---"))
							{
								done = true;
								if (mobs.size() == 0)
									throw new DNConfigException("Error: There are no mobs specified in mobs.txt on line ");
								if (newDelay == -1)
									throw new DNConfigException("Error: There is no delay specified in mobs.txt on line ");
								mobBuffs.add(new MobBuff(newDelay, mobs, effects, healthMult, canDrown, autoChase));
							}
						}
						catch (DNConfigException e)
						{
							getLogger().warning(e.getMessage() + " " + line);
							mobError = true;
							ready = false;
						}
						catch (NumberFormatException e)
						{
							getLogger().warning("Error: Can't read an entry in mobBuffs.txt (is there a typo on line " + line + "?)");
							mobError = true;
							ready = false;
						}
						catch (IllegalArgumentException e)
						{
							getLogger().warning("Error: Can't read an entry in mobBuffs.txt (is there a typo on line " + line + "?)");
							mobError = true;
							ready = false;
						}
					}
				}
			}

			finally
			{
				scanner.close();
			}
		}
		catch (FileNotFoundException e)
		{
			try
			{
				getLogger().info(new java.io.File(".").getCanonicalPath());
			}
			catch (IOException e1)
			{
				mobError = true;
				e1.printStackTrace();
			}
			try
			{
				(new File("plugins\\DeadlierNights")).mkdirs();
				PrintWriter out = new PrintWriter(new File("plugins\\DeadlierNights\\mobBuffs.txt"));
				out.println("delay: 5");
				out.println("mob: skeleton");
				out.println("effect: speed 1");
				out.println("---");
				out.println("delay: 15");
				out.println("mob: zombie");
				out.println("mob: skeleton");
				out.println("effect: jump 3");
				out.println("healthMult: 1.5");
				out.println("canDrown: false");
				out.println("---");
				out.close();
				loadMobBuffs();
			}
			catch (IOException ee)
			{
				mobError = true;
				getLogger().warning(ee.getMessage());
			}
		}

	}

	// TODO: Add documentation to config files

	// TODO: Make server commands write to the config files

	private void loadScares()
	{
		scareError = false;
		boolean ready = true;
		scares = new ArrayList<Scare>();
		Scanner scanner;
		int line = 0;
		try
		{
			scanner = new Scanner(new File("plugins\\DeadlierNights\\scares.txt"));
			try
			{
				String input = "";
				while (scanner.hasNext())
				{
					boolean done = false;
					ScareType newType = null;
					int newDelay = -1;
					int newFrequency = -1;
					int newExtraFrequency = 0;
					double newProbability = -1;

					String newSoundToPlay = null;
					int newVolume = 1;
					int newRange = 10;

					if (!ready)
						ready = scanner.nextLine().contains("---");

					while (!done && ready)
					{
						try
						{
							line++;
							input = scanner.nextLine();
							if (input.contains("type:"))
								newType = ScareType.valueOf(stringConfigCheck(input, "type:").toUpperCase());
							else if (input.contains("delay:"))
							{
								newDelay = Integer.parseInt(stringConfigCheck(input, "delay:"));
								if (newDelay <= 0)
									throw new DNConfigException("Error: Delay must have a value of greater than zero on line ");
							}
							else if (input.contains("extrafrequency:"))
							{
								newExtraFrequency = Integer.parseInt(stringConfigCheck(input, "extrafrequency:"));
								if (newExtraFrequency < 0)
									throw new DNConfigException("Error: Extra frequency must have a value of greater or equal to zero on line ");
							}
							else if (input.contains("frequency:"))
							{
								newFrequency = Integer.parseInt(stringConfigCheck(input, "frequency:"));
								if (newFrequency <= 0)
									throw new DNConfigException("Error: Frequency must have a value of greater than zero on line ");
							}
							else if (input.contains("probability"))
								newProbability = Double.parseDouble(input.replaceAll("probability:", "").replaceAll("^[ \t]+", ""));
							else if (input.contains("sound:"))
								newSoundToPlay = input.replaceAll("sound:", "").replaceAll("^[ \t]+", "");
							else if (input.contains("volume:"))
								newVolume = intConfigCheck(input, "volume:");
							else if (input.contains("range:"))
								newRange = intConfigCheck(input, "range:");
							if (input.contains("---"))
							{
								done = true;
								if (newType == null)
									throw new DNConfigException("Scare is missing a type; ends on line ");
								else
								{
									if (newDelay == -1)
										throw new DNConfigException("Error: FakeSound scare is missing a delay on line ");
									if (newFrequency == -1)
										throw new DNConfigException("Error: FakeSound scare is missing a frequency on line ");
									if (newProbability == -1)
										throw new DNConfigException("Error: FakeSound scare is missing a probability on line ");

									if (newType.equals(ScareType.FAKESOUND))
									{
										if (newSoundToPlay == null)
											throw new DNConfigException("Error: FakeSound scare is missing a sound on line ");
										scares.add(new Scare(ScareType.FAKESOUND, newDelay, newFrequency, newExtraFrequency, newProbability, newSoundToPlay, newVolume, newRange));
									}
								}
							}
						}
						catch (DNConfigException e)
						{
							getLogger().warning(e.getMessage() + " " + line);
							scareError = true;
							ready = false;
						}
						catch (NumberFormatException e)
						{
							getLogger().warning("ERROR: Can't read an entry in scares.txt (is there a typo on line " + line + "?)");
							scareError = true;
							ready = false;
						}
					}
				}
			}

			finally
			{
				scanner.close();
			}
		}
		catch (FileNotFoundException e)
		{
			try
			{
				getLogger().info(new java.io.File(".").getCanonicalPath());
			}
			catch (IOException e1)
			{
				scareError = true;
				e1.printStackTrace();
			}
			try
			{
				(new File("plugins\\DeadlierNights")).mkdirs();
				PrintWriter out = new PrintWriter(new File("plugins\\DeadlierNights\\scares.txt"));
				out.println("type: fakesound");
				out.println("sound: mob.skeleton.say");
				out.println("delay: 10");
				out.println("frequency: 5");
				out.println("probability: 0.75");
				out.println("---");
				out.close();
			}
			catch (IOException ee)
			{
				scareError = true;
				getLogger().warning(ee.getMessage());
			}
		}

	}

	// TODO: Make this more forgiving

	private boolean boolConfigCheck(String input, String check) throws DNConfigException
	{
		if (input.replaceAll(check, "").replaceAll("^[ \t]+", "").equalsIgnoreCase("true"))
		{
			defined.add(check);
			return true;
		}
		else if (input.replaceAll(check, "").replaceAll("^[ \t]+", "").equalsIgnoreCase("false"))
		{
			defined.add(check);
			return false;
		}
		else
			throw new DNConfigException("Error: Invalid token; can only be true/false on line ");
	}

	// TODO: Make this more forgiving

	private int intConfigCheck(String input, String check) throws DNConfigException
	{
		try
		{
			int temp = Integer.parseInt((input.replaceAll(check, "").replaceAll("^[ \t]+", "")));
			if (temp < 0)
				throw new NumberFormatException();
			else
				return temp;
		}
		catch (NumberFormatException e)
		{
			throw new DNConfigException("Error: Invalid token; must be a positive integer on line ");
		}
	}

	private double doubleConfigCheck(String input, String check) throws DNConfigException
	{
		if (defined.contains(check))
			throw new DNConfigException("Error: Setting has already been defined; redefinition occurred on line ");
		else
		{
			try
			{
				double temp = Double.parseDouble((input.replaceAll(check, "").replaceAll("^[ \t]+", "")));
				if (temp < 0)
					throw new NumberFormatException();
				else
					return temp;
			}
			catch (NumberFormatException e)
			{
				throw new DNConfigException("Error: Invalid token; must be a positive real number on line ");
			}
		}
	}

	private String stringConfigCheck(String input, String check)
	{
		return (input.replaceAll(check, "").replaceAll("^[ \t]+", ""));
	}

	/**
	 * Reloads the plugin's configuration files by calling the relevant internal
	 * methods.
	 */
	public void reload()
	{
		loadConfig();
		loadEffects();
		loadMobBuffs();
		loadScares();
		for (Player player : getServer().getOnlinePlayers())
		{
			playerMap.put(player.getName(), 0);
			exemptMap.put(player.getName(), player.hasPermission("deadliernights.exempt"));
			scareMap.put(player.getName(), new HashMap<Scare, Integer>());
			for (Scare scare : scares)
			{
				scareMap.get(player.getName()).put(scare, -1 * scare.getExtraFrequencyRandom());
			}
		}
	}

	/**
	 * @param match
	 * @return the name of the player matched if the method succeeds or <FAILED>
	 *         if not
	 */

	// TODO: Verify if these is needed

	public String pickPlayer(String match)
	{
		int count = 0;
		String possible = "";
		for (Player player : getServer().getOnlinePlayers())
		{
			if (player.getName().toLowerCase().startsWith(match.toLowerCase()))
			{
				possible = player.getName();
				count++;
			}
		}
		if (count == 1)
			return possible;
		else
			return "<FAILED>";
	}

	/**
	 * @param potions
	 * @return the strongest potion of each type in the set of effects
	 */

	// Currently unused
	public ArrayList<PotionEffect> condensePotions(ArrayList<PotionEffect> potions)
	{
		ArrayList<PotionEffect> toReturn = new ArrayList<PotionEffect>();
		ArrayList<PotionEffectType> types = new ArrayList<PotionEffectType>();
		HashMap<PotionEffectType, Integer> maxes = new HashMap<PotionEffectType, Integer>();
		playerMap = new HashMap<String, Integer>();
		exemptMap = new HashMap<String, Boolean>();
		scareMap = new HashMap<String, HashMap<Scare, Integer>>();
		shortestDelay = getShortestDelay();

		for (PotionEffect eff : potions)
		{
			if (!types.contains(eff.getType()))
				types.add(eff.getType());
			if (!maxes.containsKey(eff.getType()))
				maxes.put(eff.getType(), eff.getAmplifier());
			else if (maxes.get(eff.getType()) < eff.getAmplifier())
				maxes.put(eff.getType(), eff.getAmplifier());
		}
		for (int i = 0; i < maxes.size(); i++)
		{
			toReturn.add(new PotionEffect(types.get(i), Integer.MAX_VALUE, maxes.get(types.get(i))));
		}
		return toReturn;
	}

	private int getShortestDelay()
	{
		int shortest = Integer.MAX_VALUE;

		for (EffectTimePair pair : effects)
		{
			if (pair.getDelay() < shortest)
				shortest = pair.getDelay();
		}

		return shortest;
	}

	/**
	 * @param player
	 */
	public void curePlayer(Player player)
	{
		playerMap.put(player.getName(), 0);

		for (EffectTimePair effect : effects)
		{
			player.removePotionEffect(effect.getEffect());
		}

		player.sendMessage("[DeadlierNights]: You have been cured of your exposure.");
	}

	/**
	 * Gets the
	 * 
	 * @param player
	 * @param sender
	 */
	public void queryPlayer(Player player, Player sender)
	{
		sender.sendMessage(player.getName() + " currently has an exposure level of " + playerMap.get(player.getName()));

	}
}