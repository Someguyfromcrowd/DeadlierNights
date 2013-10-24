package someguy.deadliernights;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 * The core method of DeadlierNights. Starts all scheduled events and handles
 * processing.
 * 
 * @author someguyfromcrowd
 * 
 */
public class DeadlierNights extends JavaPlugin implements Listener
{
	private boolean chat;
	private boolean log;

	private boolean potionEnable;
	private boolean mobEnable;

	private int decayRate;
	private int shortestDelay;
	private ArrayList<EffectTimePair> effects;
	private ArrayList<MobBuff> mobBuffs;
	private ArrayList<String> defined;
	private HashMap<String, Integer> playerMap;
	private HashMap<String, HashSet<PotionEffectType>> potionMap;
	private HashMap<String, Boolean> exemptMap;

	@Override
	public void onEnable()
	{
		loadConfig();
		loadEffects();
		loadMobBuffs();
		playerMap = new HashMap<String, Integer>();
		potionMap = new HashMap<String, HashSet<PotionEffectType>>();
		exemptMap = new HashMap<String, Boolean>();
		shortestDelay = getShortestDelay();
		getServer().getPluginManager().registerEvents(this, this);
		for (Player player : getServer().getOnlinePlayers())
		{
			playerMap.put(player.getName(), 0);
			exemptMap.put(player.getName(), player.hasPermission("deadliernights.exempt"));
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
			}

			if (!exemptMap.get(playerName).booleanValue() && player.getLocation().getBlock().getLightFromBlocks() == 0 && getServer().getWorld(player.getWorld().getName()).getTime() % 24000 >= 14000 && !(player.getGameMode().equals(GameMode.CREATIVE)))
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
						if (player.hasPotionEffect(pair.getEffect()) && playerMap.get(playerName) < pair.getDelay() && startVal > pair.getDelay())
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
	 * Reacts to entity spawns by applying potion effects if needed
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
					potionMap.put(playerName, new HashSet<PotionEffectType>());
					exemptMap.put(playerName, player.hasPermission("deadliernights.exempt"));
				}
				if (player.getLocation().distance(e.getLocation()) <= 32)
				{
					//System.out.println("sensed");
					for (MobBuff buff : mobBuffs)
					{
						if (buff.getMobs().contains(e.getEntityType()) && buff.getDelay() < playerMap.get(player.getName()))
						{
							//System.out.println("triggered");
							for (PotionEffect eff : buff.getBuffs())
							{
								//System.out.println("added");
								e.getEntity().addPotionEffect(eff);
							}
							
							if (buff.getHealthMult() != 1)
								e.getEntity().setMaxHealth(e.getEntity().getMaxHealth() * buff.getHealthMult());
							
							if (!buff.canDrown())
								e.getEntity().setMaximumAir(Integer.MAX_VALUE);
							
							if (buff.autoChase())
								((Creature) e.getEntity()).setTarget(player);
								
						}
						else
							System.out.println(buff.getDelay() - playerMap.get(player.getName()));
					}
				}
			}
		}
	}

	/**
	 * @param event
	 */
	@EventHandler
	public void onGamemodeChange(PlayerGameModeChangeEvent event)
	{
		if (playerMap.containsKey(event.getPlayer().getName()))
		{
			playerMap.put(event.getPlayer().getName(), 0);
		}
	}

	/**
	 * @param event
	 */
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event)
	{
		playerMap.put(event.getPlayer().getName(), 0);
	}

	/**
	 * @param event
	 */
	@EventHandler
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
						if (args[1].equalsIgnoreCase("true") || args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("on"))
						{
							exemptMap.put(playerName, Boolean.TRUE);
							sender.sendMessage("[DeadlierNights]: " + playerName + " is now exempt from DeadlierNights");
							getServer().getPlayer(playerName).sendMessage("[DeadlierNights]: You are now immune to DeadlierNights.");
							return true;
						}
						else if (args[1].equalsIgnoreCase("false") || args[0].equalsIgnoreCase("disable") || args[0].equalsIgnoreCase("off"))
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
					else if (input.contains("potionDebuff:"))
						potionEnable = boolConfigCheck(input, "potionDebuff:");
					else if (input.contains("mobBuff:"))
						mobEnable = boolConfigCheck(input, "mobBuff:");
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
				out.println("chat: true");
				out.println("log: true");
				out.println("potionDebuff: true");
				out.println("mobBuff: true");
				out.close();
			}
			catch (IOException ee)
			{
				getLogger().warning(ee.getMessage());
			}
		}
	}

	private void loadEffects()
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
						{
							newEffect = PotionEffectType.getByName(input.replaceAll("effect:", "").replaceAll("^[ \t]+", "").replaceAll("[ \t]+[0-9]+$", ""));
							newLevel = Integer.parseInt(input.replaceAll("effect:", "").replaceAll("^[ \t]+", "").replaceAll("^[A-Z,a-z]*[ \t]+", "")) - 1;
						}
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
			}
			catch (IOException ee)
			{
				getLogger().warning(ee.getMessage());
			}
		}
	}

	private void loadMobBuffs()
	{
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
					
					while (!done)
					{
						line++;
						input = scanner.nextLine();
						if (input.contains("delay:"))
							newDelay = Integer.parseInt(input.replaceAll("delay:", "").replaceAll("^[ \t]+", ""));
						else if (input.contains("mob:"))
							mobs.add(EntityType.valueOf(input.replaceAll("mob:", "").replaceAll("^[ \t]+", "").toUpperCase()));
						else if (input.contains("effect:"))
						{
							newEffect = PotionEffectType.getByName(input.replaceAll("effect:", "").replaceAll("^[ \t]+", "").replaceAll("[ \t]+[0-9]+$", ""));
							newLevel = Integer.parseInt(input.replaceAll("effect:", "").replaceAll("^[ \t]+", "").replaceAll("^[A-Z,a-z]*[ \t]+", "")) - 1;
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
								throw new IOException("Error: There are no mobs specified in mobs.txt on line ");
							if (newDelay == -1)
								throw new IOException("Error: There is no delay specified in mobs.txt on line ");
							mobBuffs.add(new MobBuff(newDelay, mobs, effects, healthMult, canDrown, autoChase));
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
				getLogger().warning(e.getMessage() + " " + line);
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
			}
			catch (IOException ee)
			{
				getLogger().warning(ee.getMessage());
			}
		}

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
	
	private double doubleConfigCheck(String input, String check) throws IOException
	{
		if (defined.contains(check))
			throw new IOException("Error: Setting has already been defined; redefinition occurred on line ");
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
				throw new IOException("Error: Invalid token; must be a positive real number on line ");
			}
		}
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
	}

	/**
	 * @param match
	 * @return the name of the player matched if the method succeeds or <FAILED>
	 *         if not
	 */
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
			System.out.println("Condensed effect: " + new PotionEffect(types.get(i), Integer.MAX_VALUE, maxes.get(types.get(i))));
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
}