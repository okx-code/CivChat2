package vg.civcraft.mc.civchat2.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import vg.civcraft.mc.civchat2.BungeePlayers;
import vg.civcraft.mc.civchat2.CivChat2;
import vg.civcraft.mc.civchat2.CivChat2Manager;
import vg.civcraft.mc.civchat2.database.DatabaseManager;
import vg.civcraft.mc.civchat2.utility.CivChat2Log;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.group.Group;

public abstract class ChatCommand extends PlayerCommand {

	protected CivChat2 plugin = CivChat2.getInstance();

	protected CivChat2Manager chatMan = plugin.getCivChat2Manager();

	protected CivChat2Log logger = CivChat2.getCivChat2Log();

	protected DatabaseManager DBM = plugin.getDatabaseManager();

	public ChatCommand(String name) {

		super(name);
		setArguments(0, 0);
	}

	@Override
	public List<String> tabComplete(CommandSender arg0, String[] arg1) {

		return null;
	}

	protected Player argAsPlayer(int index) {

		try {
			return Bukkit.getPlayer(NameAPI.getUUID(getArgs()[index].trim()));
		} catch (Exception ex) {
			return null;
		}
	}

	protected UUID argAsGlobalPlayer(int index) {
		String arg = getArgs()[index].trim();

		UUID found = null;
		String lowerName = arg.toLowerCase();
		int delta = Integer.MAX_VALUE;
		for (UUID uuid : BungeePlayers.getInstance().getPlayers()) {
			String player = NameAPI.getCurrentName(uuid);
			if (player.equalsIgnoreCase(arg)) {
				return uuid;
			}

			if (player.toLowerCase().startsWith(lowerName)) {
				int curDelta = Math.abs(player.length() - lowerName.length());
				if (curDelta < delta) {
					found = uuid;
					delta = curDelta;
				}

				if (curDelta == 0) {
					break;
				}
			}
		}

		return found;
	}

	protected Group argAsGroup(int index) {

		try {
			return GroupManager.getGroup(getArgs()[index].trim());
		} catch (Exception ex) {
			return null;
		}
	}

	protected String getRealName(Player player) {

		try {
			return NameAPI.getCurrentName(player.getUniqueId());
		} catch (Exception ex) {
			return null;
		}
	}

	protected String getRealName(String name) {

		try {
			return NameAPI.getCurrentName(NameAPI.getUUID(name));
		} catch (Exception ex) {
			return null;
		}
	}

	protected List<String> findPlayers(String pattern) {

		List<String> players = new ArrayList<>();
		for (UUID p : BungeePlayers.getInstance().getPlayers()) {
			String name = NameAPI.getCurrentName(p);
			players.add(name);
		}
		return StringUtil.copyPartialMatches(pattern, players, new ArrayList<>());
	}

	protected List<String> findGroups(String pattern) {
		GroupManager gm = NameAPI.getGroupManager();
		List<String> groups = gm.getAllGroupNames(player().getUniqueId());
		return StringUtil.copyPartialMatches(pattern, groups, new ArrayList<>());
	}
}
