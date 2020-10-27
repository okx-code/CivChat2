package vg.civcraft.mc.civchat2.command.commands;

import java.util.List;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civchat2.ChatStrings;
import vg.civcraft.mc.civchat2.CivChatMessageDispatcher;
import vg.civcraft.mc.civchat2.command.ChatCommand;
import vg.civcraft.mc.namelayer.NameAPI;

public class Mute extends ChatCommand {
	public Mute(String name) {
		super(name);
		setIdentifier("mute");
		setDescription("Mute a player");
		setUsage("/mute <player>");
		setArguments(1, 1);
	}

	@Override
	public boolean execute(CommandSender commandSender, String[] strings) {
		UUID player = argAsGlobalPlayer(0);
		if (player == null) {
			msg(ChatStrings.chatPlayerNotFound);
			return true;
		}

		if (commandSender instanceof Player && ((Player) commandSender).getUniqueId().equals(player)) {
			msg(ChatStrings.chatCantMuteSelf);
			return true;
		}

		String name = NameAPI.getCurrentName(player);
		if (!DBM.isMuted(player)) {
			CivChatMessageDispatcher.dispatchMutePlayer(player);
			DBM.mute(player);
			logger.debug(commandSender.getName() + " has muted " + name + "(" + player + ")");
			msg(ChatStrings.chatMuted, name);
		} else {
			CivChatMessageDispatcher.dispatchUnmutePlayer(player);
			DBM.unmute(player);
			logger.debug(commandSender.getName() + " has un-muted " + name + "(" + player + ")");
			msg(ChatStrings.chatUnmuted, name);
		}
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender arg0, String[] args) {
		if (args.length != 1) {
			return null;
		}
		return findPlayers(args[0]);
	}
}
