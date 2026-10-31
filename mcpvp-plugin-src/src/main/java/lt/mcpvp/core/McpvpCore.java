package lt.mcpvp.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class McpvpCore extends JavaPlugin implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int LINES = 9;

    // kills, deaths, streak
    private final Map<UUID, int[]> stats = new HashMap<>();
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private File dataFile;
    private YamlConfiguration data;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dataFile = new File(getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);

        for (Player p : Bukkit.getOnlinePlayers()) {
            load(p);
            createBoard(p);
        }
        getLogger().info("mcpvp.lt core ijungtas");
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            save(p);
        }
        saveFile();
    }

    // ---------- eventai ----------

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        load(p);
        createBoard(p);

        String url = getConfig().getString("pack-url", "");
        String sha1 = getConfig().getString("pack-sha1", "");
        if (!url.isEmpty() && !sha1.isEmpty()) {
            p.setResourcePack(url, sha1, getConfig().getBoolean("pack-required", true),
                    MM.deserialize("<green><bold>mcpvp.lt</bold> <gray>resource pack"));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        save(p);
        saveFile();
        stats.remove(p.getUniqueId());
        boards.remove(p.getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        int[] v = stats.computeIfAbsent(victim.getUniqueId(), k -> new int[3]);
        v[1]++;
        v[2] = 0;

        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            int[] k = stats.computeIfAbsent(killer.getUniqueId(), x -> new int[3]);
            k[0]++;
            k[2]++;
        }
    }

    // ---------- duomenys ----------

    private void load(Player p) {
        String path = "players." + p.getUniqueId();
        int[] s = new int[]{
                data.getInt(path + ".kills", 0),
                data.getInt(path + ".deaths", 0),
                data.getInt(path + ".streak", 0)};
        stats.put(p.getUniqueId(), s);
    }

    private void save(Player p) {
        int[] s = stats.get(p.getUniqueId());
        if (s == null) return;
        String path = "players." + p.getUniqueId();
        data.set(path + ".kills", s[0]);
        data.set(path + ".deaths", s[1]);
        data.set(path + ".streak", s[2]);
    }

    private void saveFile() {
        try {
            data.save(dataFile);
        } catch (IOException ex) {
            getLogger().warning("Nepavyko issaugoti data.yml: " + ex.getMessage());
        }
    }

    // ---------- scoreboard ----------

    private void createBoard(Player p) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("mcpvp", Criteria.DUMMY,
                MM.deserialize("<green><bold>MCPVP.LT"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (int i = 0; i < LINES; i++) {
            String entry = "§" + Integer.toHexString(i) + "§r";
            Team team = board.registerNewTeam("l" + i);
            team.addEntry(entry);
            obj.getScore(entry).setScore(LINES - i);
        }

        boards.put(p.getUniqueId(), board);
        p.setScoreboard(board);
        update(p);
    }

    private void tick() {
        if (getConfig().getBoolean("tablist-enabled", false)) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                updateTab(p);
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!boards.containsKey(p.getUniqueId())) {
                load(p);
                createBoard(p);
            }
            Scoreboard mine = boards.get(p.getUniqueId());
            if (mine != null && p.getScoreboard() != mine) {
                p.setScoreboard(mine);
            }
            update(p);
        }
    }

    private void update(Player p) {
        Scoreboard board = boards.get(p.getUniqueId());
        int[] s = stats.get(p.getUniqueId());
        if (board == null || s == null) return;

        double kd = s[1] == 0 ? s[0] : (double) s[0] / s[1];

        String[] lines = new String[]{
                "<dark_gray>-------------",
                "<gray>Vardas: <green>" + p.getName(),
                "<gray>Nuzudymai: <green>" + s[0],
                "<gray>Mirtys: <green>" + s[1],
                "<gray>K/D: <green>" + String.format("%.2f", kd),
                "<gray>Serija: <green>" + s[2],
                "<gray>Zaidejai: <green>" + Bukkit.getOnlinePlayers().size(),
                " ",
                "<green>mcpvp.lt"
        };

        for (int i = 0; i < LINES; i++) {
            Team team = board.getTeam("l" + i);
            if (team != null) {
                team.prefix(MM.deserialize(lines[i]));
            }
        }
    }

    // ---------- tablistas (pagal nutylejima isjungtas) ----------

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
