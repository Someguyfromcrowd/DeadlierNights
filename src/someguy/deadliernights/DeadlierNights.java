package someguy.deadliernights;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
//import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class DeadlierNights extends JavaPlugin implements Listener
{
	private boolean chat;
	private boolean log;
	private int shortestDelay;
	private int decayRate;
	private ArrayList<EffectTimePair> effects;
	private ArrayList<String> defined;
	HashMap<String, Integer> playerMap;
	HashMap<String, Boolean> exemptMap;

	@Override
	public void onEnable()
	{
		loadConfig();
		loadEffects();
		shortestDelay = getShortestDelay();
		playerMap = new HashMap<String, Integer>();
		exemptMap = new HashMap<String, Boolean>();
		getServer().getPluginManager().registerEvents(this, this);
		for (Player player : getServer().getOnlinePlayers())
		{
			playerMap.put(player.getName(), 0);
			exemptMap.put(player.getName(), player.hasPermission("deadliernights.exempt"));
		}

		@SuppressWarnings("unused")
		BukkitTask checker = new PlayerCheckTask(this).runTaskTimer(this, 0, 20L);
	}

	public void checkPlayers()
	{
		for (Player player : getServer().getOnlinePlayers())
		{
			String playerName = player.getName();
			if (!playerMap.containsKey(playerName))
			{
				playerMap.put(playerName, 0);
				exemptMap.put(playerName, player.hasPermission("deadliernights.exempt"));
			}

			if (!exemptMap.get(playerName).booleanValue() && player.getLocation().getBlock().getLightFromBlocks() == 0 && getServer().getWorld(player.getWorld().getName()).getTime() % 24000 >= 14000 && !(player.getGameMode().equals(GameMode.CREATIVE)))
			{
				playerMap.put(playerName, playerMap.get(playerName) + 1);
				int current = playerMap.get(playerName);

				for (EffectTimePair pair : effects)
				{

					if (pair.getDelay() < current)
					{
						if (!player.hasPotionEffect(pair.getEffect()))
						{
							player.addPotionEffect(new PotionEffect(pair.getEffect(), Integer.MAX_VALUE, pair.getLevel()));
							if (log)
								getLogger().info("Applied " + pair.getEffect().getName() + " " + pair.getLevel() + " to " + playerName);
						}
					}
					else if (pair.getDelay() == current)
					{
						player.addPotionEffect(new PotionEffect(pair.getEffect(), Integer.MAX_VALUE, pair.getLevel()));
						if (log)
							getLogger().info("Applied " + pair.getEffect().getName() + " " + pair.getLevel() + " to " + playerName);
						if (chat)
							player.sendMessage(pair.getText());
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
					boolean dropped = true;
					if (playerMap.get(playerName) < shortestDelay)
						dropped = false;
					playerMap.put(playerName, playerMap.get(player.getName()) - decayRate);
					if (playerMap.get(playerName) < 0)
						playerMap.put(playerName, 0);
					System.out.println(playerName + " is down to " + playerMap.get(player.getName()) + " seconds");
					if (playerMap.get(playerName) >= shortestDelay)
						dropped = false;
					if (dropped)
					{
						player.sendMessage("You escape from the darkness.");
					}
					for (EffectTimePair pair : effects)
					{
						if (player.hasPotionEffect(pair.getEffect()) && playerMap.get(playerName) < pair.getDelay())
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

	public void onGamemodeChange(PlayerGameModeChangeEvent event)
	{
		if (playerMap.containsKey(event.getPlayer().getName()))
		{
			playerMap.put(event.getPlayer().getName(), 0);
		}
	}

	public void onPlayerRespawn(PlayerRespawnEvent event)
	{
		playerMap.put(event.getPlayer().getName(), 0);
	}

	public void onPlayerLogout(PlayerQuitEvent event)
	{
		for (EffectTimePair pair : effects)
		{
			if (event.getPlayer().hasPotionEffect(pair.getEffect()) && playerMap.get(event.getPlayer().getName()) < pair.getDelay())
			{
				event.getPlayer().removePotionEffect(pair.getEffect());
			}
		}
	}
	
	//TODO: Implement some various creature spawn mechanics (after pushing 0.02a)
	
	/*public void onCreatureSpawn(CreatureSpawnEvent event)
	{
		for (Player player : getServer().getOnlinePlayers())
		{
			if (!playerMap.containsKey(player.getName()))
			{
				getLogger().info(player.getName() + " logged in and was added to the list of death");
				playerMap.put(player.getName(), 0);
			}
		}
	}*/

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
			else if (args[0].equalsIgnoreCase("true"))
			{
				chat = true;
				sender.sendMessage("[DeadlierNights]: Chat messages enabled.");
				return true;
			}
			else if (args[0].equalsIgnoreCase("false"))
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
			else if (args[0].equalsIgnoreCase("true"))
			{
				log = true;
				sender.sendMessage("[DeadlierNights]: Log messages enabled.");
				return true;
			}
			else if (args[0].equalsIgnoreCase("false"))
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
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("DNexempt"))
		{
			if (args.length == 0)
			{
				if (exemptMap.get(sender.getName()).booleanValue())
					sender.sendMessage("[DeadlierNights]: You are immune to negative debuffs");
				else
					sender.sendMessage("[DeadlierNights]: You are not immune to negative debuffs");
				return true;
			}
			else if (args[0].equalsIgnoreCase("true"))
			{
				exemptMap.put(sender.getName(), Boolean.TRUE);
				sender.sendMessage("[DeadlierNights]: You are now immune to negative debuffs.");
				return true;
			}
			else if (args[0].equalsIgnoreCase("false"))
			{
				exemptMap.put(sender.getName(), Boolean.FALSE);
				sender.sendMessage("[DeadlierNights]: You are no longer immune to negative debuffs.");
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
							sender.sendMessage("[DeadlierNights]: " + playerName + " is exempt from debuffs");
							return true;
						}
						else
						{
							sender.sendMessage("[DeadlierNights]: " + playerName + " is not exempt from debuffs");
							return true;
						}
					}
					else if (args.length == 2)
					{
						if (args[1].equalsIgnoreCase("true"))
						{
							exemptMap.put(playerName, Boolean.TRUE);
							sender.sendMessage("[DeadlierNights]: " + playerName + " is now exempt from debuffs");
							return true;
						}
						else if (args[1].equalsIgnoreCase("false"))
						{
							exemptMap.put(playerName, Boolean.FALSE);
							sender.sendMessage("[DeadlierNights]: " + playerName + " is no longer exempt from debuffs");
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
		else
		{
			return false;
		}

	}

	private void loadConfig()
	{
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
					line++;
					input = scanner.nextLine();
					if (input.contains("decay rate:"))
						decayRate = intConfigCheck(input, "decay rate:");
					else if (input.contains("chat:"))
						chat = boolConfigCheck(input, "chat:");
					else if (input.contains("log:"))
						log = boolConfigCheck(input, "log:");
				}
			}
			catch (NumberFormatException e)
			{
				System.out.println(e);
				getLogger().warning("Error: Can't read config.txt (is there a typo on line " + line + "?)");
			}
			catch (IOException e)
			{
				System.out.println(e.getMessage() + line);
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
				e1.printStackTrace();
			}
			try
			{
				(new File("plugins\\DeadlierNights")).mkdirs();
				PrintWriter out = new PrintWriter(new File("plugins\\DeadlierNights\\config.txt"));
				out.println("decay rate: 0");
				out.close();
			}
			catch (IOException ee)
			{
				getLogger().warning(ee.getMessage());
			}
		}
	}

	public void loadEffects()
	{
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
					while (!done)
					{
						line++;
						input = scanner.nextLine();
						if (input.contains("delay:"))
							newDelay = Integer.parseInt(input.replaceAll("delay:", "").replaceAll("^[ \t]+", ""));
						else if (input.contains("effect:"))
							newEffect = PotionEffectType.getByName(input.replaceAll("effect:", "").replaceAll("^[ \t]+", ""));
						else if (input.contains("level:"))
							newLevel = Integer.parseInt(input.replaceAll("level:", "").replaceAll("^[ \t]+", "")) - 1;
						else if (input.contains("offtext:"))
							newOffText = input.replaceAll("offtext:", "").replaceAll("^[ \t]+", "");
						else if (input.contains("text:"))
							newText = input.replaceAll("text:", "").replaceAll("^[ \t]+", "");
						if (input.contains("---"))
						{
							done = true;
							if (newDelay == -1 || newEffect == null)
								throw new IOException("Error: delay or effect is missing from an effects entry on line " + line);
							else if (newDelay <= 0)
								throw new IOException("Error: delay must have a value greater than zero on line " + line);
							effects.add(new EffectTimePair(newDelay, newEffect, newLevel, newText, newOffText));
							System.out.println(effects);
						}
					}
				}
			}
			catch (NumberFormatException e)
			{
				System.out.println(e);
				getLogger().warning("ERROR: Can't read effects.txt (is there a typo on line " + line + "?)");
			}
			catch (IOException e)
			{
				System.out.println(e);
				getLogger().warning("ERROR: Can't read effects.txt (is there a typo on line " + line + "?)");
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
				e1.printStackTrace();
			}
			try
			{
				(new File("plugins\\DeadlierNights")).mkdirs();
				PrintWriter out = new PrintWriter(new File("plugins\\DeadlierNights\\effects.txt"));
				out.println("decay: 0");
				out.println("delay: 5");
				out.println("effect: SLOW");
				out.println("level: 1");
				out.println("text: Example Text 1");
				out.println("offtext: Example Off Text 1");
				out.println("---");
				out.println("delay: 10");
				out.println("effect: SLOW");
				out.println("level: 2");
				out.println("text: Example Text 2");
				out.println("offtext: Example Off Text 2");
				out.println("---");
				out.println("delay: 15");
				out.println("effect: SLOW");
				out.println("level: 3");
				out.println("text: Example Text 1)");
				out.println("offtext: TEST2");
				out.println("---");
				out.close();
			}
			catch (IOException ee)
			{
				getLogger().warning(ee.getMessage());
			}
		}
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

	private boolean boolConfigCheck(String input, String check) throws IOException
	{
		if (defined.contains(check))
			throw new IOException("Error: Setting has already been defined; redefinition occurred on line ");
		else if (input.replaceAll(check, "").replaceAll("^[ \t]+", "").equalsIgnoreCase("true"))
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
			throw new IOException("Error: Invalid token; can only be true/false on line ");
	}

	private int intConfigCheck(String input, String check) throws IOException
	{
		if (defined.contains(check))
			throw new IOException("Error: Setting has already been defined; redefinition occurred on line ");
		else
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
				throw new IOException("Error: Invalid token; must be a positive integer on line ");
			}
		}
	}
	
	public void reload()
	{
		loadConfig();
		loadEffects();
	}
	
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
		if (count==1)
			return possible;
		else
			return "<FAILED>";
	}
}