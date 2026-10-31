package lt.mcpvp.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class McpvpCore extends JavaPlugin implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::updateTabs, 20L, 40L);
        getLogger().info("mcpvp.lt core ijungtas");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String url = getConfig().getString("pack-url", "");
        String sha1 = getConfig().getString("pack-sha1", "");
        if (!url.isEmpty() && !sha1.isEmpty()) {
            p.setResourcePack(url, sha1, getConfig().getBoolean("pack-required", true),
                    MM.deserialize("<green><bold>mcpvp.lt</bold> <gray>resource pack"));
        }
        updateTab(p);
    }

    private void updateTabs() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            updateTab(p);
        }
    }

    private void updateTab(Player p) {
        Component header = MM.deserialize(
                "\n<green><bold>MCPVP.LT</bold>\n<gray>KitPvP & Practice\n");
        Component footer = MM.deserialize(
                "\n<gray>Zaidejai: <green>" + Bukkit.getOnlinePlayers().size()
                        + " <dark_gray>| <gray>Ping: <green>" + p.getPing() + "ms\n"
                        + "<dark_gray>mcpvp.lt\n");
        p.sendPlayerListHeaderAndFooter(header, footer);
    }
}
