Thank you for downloading DeadlierNights!

This plugin is currently in a very early alpha stage, so expect frequent bugs, unexpected behavior, and all-around silliness until things are more stable.

In essence, the longer the player remains in complete darkness at night, the higher his or get exposure level becomes. It increases at a rate of one per second.

Once the level gets too high, various potion debuffs and mob buffs can be triggered.

The syntax for config files are as follows:

config.txt:

decay rate: how quickly exposure should decline per second when in lit areas. If set to zero, exposure returns to zero instantly.
chat: whether or not chat messages to players are enabled when they suffer potion debuffs
log: whether or not to log various status messages (these will vary wildly between updates)
potionDebuff: whether to use potion debuffs
mobBufF: whether to use mob buffs

effects.txt:

delay: how long it takes for the effect to occur
effect: a potion effect in the form of <name> <level>
text: a chat message to display to the afflicted player when the effect occurs
offtext: a chat message to display to the player when the effect is lifted (only if decay rate > 0)
minMoon: the darkest moon phase that this effect should function at- NEW/CRESCENT/QUARTER/GIBBOUS/FULL
maxMoon: the brightest moon phase that this effect should function at- NEW/CRESCENT/QUARTER/GIBBOUS/FULL
---: ends an effect-set

*NOTE*: You can only specify one potion effect per effect-set, but can specify as many sets as you would like

Moon phases are inclusive. For example, if you set minMoon to NEW and maxMoon to CRESCENT, the effects will work if the moon is new or crescent (both waning and waxing)

mobBuffs.txt:

delay: how long it takes for the mob buff to begin
effect: a potion effect in the form of <name> <level>
mob: a mob to be affected by the potion effect
healthMult: a number by which to multiply the affected mob's health
canDrown: whether or not the mob can drown
autoChase: whether or not the mob should automatically give chase to the player who caused it to be buffed (experimental)

Multiple mobs and effects can be specified; one per line

scares.txt:

type: the type of scare (currently only FAKESOUND)
delay: how long it takes until the scare can occur
frequency: the minimum interval between scares in seconds
extrafrequency: a random amount to add to the interval in seconds

FAKESOUND-

sound: the type of sound to play (currently, bukkit's enums must be used: http://jd.bukkit.org/rb/apidocs/org/bukkit/Sound.html)
range: how far away the sound can play in both the x and z directions


=====

If anything breaks, you can just delete the config files and new ones will be created in their place.

Permissions:

   deadliernights.set.chat:
     description: Allows toggling of chat messages for players
   deadliernights.set.log:
     description: Allows toggling of log messages
   deadliernights.set.potion:
     description: Allows for the toggling of potion debuffs
   deadliernights.set.mob:
     description: Allows for the toggling of mob buffs
   deadliernights.set.scare:
     description: Allows for the toggling of scares
   deadliernights.set.fatigue:
     description: Allows for the toggling of fatigue
   deadliernights.set.moon:
     description: Allows for the toggling of moon effects
   deadliernights.set.decay:
     description: Allows for the exposure decay rate to be set
   deadliernights.reload:
     description: Allows for reloading of DeadlierNights' configuration files
   deadliernights.exempt:
     description: Allows the player to make him/her self immune to DeadlierNights' debuffs
   deadliernights.exempt.others:
     description: As with exempt, but also allows for other players to be affected
   deadliernights.cure:
     description: Allows for the curing of players
   deadliernights.help:
     description: Allows for the lookup of information about DeadlierNights
     default: true
   deadliernights.status:
     description: Allows for the lookup of status info
     
You can also use deadliernights.* to grant all permissions and deadliernights.set.* to grant all set permissions
     
=====

Changelog:

v0.05a:
	-Added moon effects

v0.04a:
	-Added scares
	-Added first scare type: fake sounds
	-Improved config loading to be more fault-tolerant
	-Improved line-number reporting of errors
	-Added further commands- /dncure, etc.

v0.03a:
	-Created this readme
	-Added mob buffs
	-Improved configuration file loading with regular expression magic
	-Added more commands and permissions
	-Refactored code to reduce bugginess
	-Cleared up some chat messages
	-