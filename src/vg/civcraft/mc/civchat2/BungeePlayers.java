package vg.civcraft.mc.civchat2;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BungeePlayers {
	private static BungeePlayers instance;
	private final Set<UUID> players = new HashSet<>();

	public static BungeePlayers getInstance() {
		if (instance == null) {
			instance = new BungeePlayers();
		}
		return instance;
	}

	public Set<UUID> getPlayers() {
		Set<UUID> playersCopy = new HashSet<>(players);
		// add local players because they aren't included
		for (Player player : Bukkit.getOnlinePlayers()) {
			playersCopy.add(player.getUniqueId());
		}

		return playersCopy;
	}

	public void pollPlayers() {
		CivChatMessageDispatcher.dispatchPlayerList();
	}

	public void updatePlayers(Set<UUID> players) {
		this.players.clear();
		this.players.addAll(players);
	}

	public static void sendMessage(UUID to, String message) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.getUniqueId().equals(to)) {
				player.sendMessage(message);
				return;
			}
		}

		CivChatMessageDispatcher.dispatch(to, message);
	}

	public static boolean isOnline(UUID uuid) {
		return getInstance().getPlayers().contains(uuid);
	}
}
