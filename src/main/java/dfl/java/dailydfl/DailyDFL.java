package dfl.java.dailydfl;

import com.earth2me.essentials.Essentials;
import jdk.internal.joptsimple.internal.Strings;
import jdk.tools.jlink.plugin.Plugin;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import dfl.java.statsdfl.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Color.*;

import java.awt.*;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.joml.Vector3f;

public class DailyDFL extends JavaPlugin implements Listener {

    private static File file;
    private static YamlConfiguration playersConfig;

    public Map<Player, Map<String, Integer>> playerTaskProgress = new HashMap<>();

    public static String dailyPrefix = ChatColor.YELLOW + "Daily: ";

    @Override
    public void onEnable() {
        // Plugin startup logic
        loadConf();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("daily").setExecutor(this);
        getCommand("dailytest").setExecutor(this);
        getCommand("dailyset").setExecutor(this);
        System.out.println(LocalTime.now().getHour());

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            // Check if it's 00:00 am
            if (LocalTime.now().getHour() == 0 && LocalTime.now().getMinute() == 0) {
                List<String> playersWhoCompleted = GetPlayersWhoCompleted();
                String playersWhoCompletedString = String.join(", ", playersWhoCompleted);
                TextComponent dailyTextClickable = new TextComponent(ChatColor.UNDERLINE + "/daily");
                dailyTextClickable.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/daily"));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage("");
                    if (playersWhoCompleted.size() != 0) {
                        p.sendMessage(ChatColor.YELLOW + "All players who completed the daily:");
                        p.sendMessage("- " + ChatColor.WHITE + playersWhoCompletedString);
                    }

                    TextComponent message = new TextComponent(ChatColor.YELLOW + "00:00; You have new daily tasks to complete. (");
                    message.addExtra(dailyTextClickable);
                    message.addExtra(ChatColor.YELLOW + ")");
                    p.spigot().sendMessage(message);
                    p.sendMessage("");

                    CenterText(p, "New day", ChatColor.YELLOW + "New daily tasks");
                }

                ResetListPlayersWhoCompleted();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    LoadPlayerProfile(p);
                }
            }
        }, 0L, 1200L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(command.getName().equalsIgnoreCase("daily"))
        {
            if(!(sender instanceof Player))
            {
                sender.sendMessage("§2[DailyDFL] Console part.");
                return true;
            }
            else
            {
                ShowDaily((Player) sender);
                return true;
            }
        }
        else if(command.getName().equalsIgnoreCase("dailytest"))
        {
            return true;
        }
        else if(command.getName().equalsIgnoreCase("dailyset"))
        {
            Player p = (Player) sender;
            p.setFoodLevel(10);
            LaunchFireworkPlayer(p);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        loadConf();
        LoadPlayerProfile(event.getPlayer());

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!HasDailyCompleted(event.getPlayer().getName())) {
                ShowDaily(event.getPlayer());
            }
        }, 4); // Delay of 5 ticks (0.25 seconds)
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        // Check if the consumed item is food
        if (isFood(event.getItem())) {
            // The player has consumed food, you can execute your desired action here
            // For example, you can send a message to the player

            if(CheckIfPlayerHasTask(event.getPlayer(), "eat"))
            {
                if(IsTaskCompleted(event.getPlayer(), "eat")) return;

                HashMap<String, Integer> taskInfo = GetTaskInfo("eat");
                int taskReach = 0;
                for (Map.Entry<String, Integer> entry : taskInfo.entrySet()) {
                    taskReach = entry.getValue();
                    // Now you can use taskName and taskValue as needed
                }
                if(GetPlayerTaskProgress(event.getPlayer(), "eat") < taskReach - 1)
                {
                    int oldValue = playersConfig.getInt("players." + event.getPlayer().getName() + ".tasks.eat");
                    setConf("players." + event.getPlayer().getName() + ".tasks.eat", oldValue + 1);
                    TextComponent text = new TextComponent("test");
                    event.getPlayer().sendMessage(ChatMessageType.ACTION_BAR);
                }
                else if(GetPlayerTaskProgress(event.getPlayer(), "eat") < taskReach)
                {
                    UpdateTaskProgress(event.getPlayer().getName(), "eat");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        // Check if the player left the bed before morning
        if (event.getBed().getWorld().getTime() >= 0 && event.getBed().getWorld().getTime() < 100) {

            if(!CheckIfPlayerHasTask(event.getPlayer(), "sleep")) return;
            if(IsTaskCompleted(event.getPlayer(), "sleep")) return;

            Player p = event.getPlayer();
            HashMap<String, Integer> taskInfo = GetTaskInfo("sleep");
            int taskReach = 0;
            for (Map.Entry<String, Integer> entry : taskInfo.entrySet()) {
                taskReach = entry.getValue();
                // Now you can use taskName and taskValue as needed
            }
            if(GetPlayerTaskProgress(event.getPlayer(), "sleep") < taskReach - 1)
            {
                int oldValue = playersConfig.getInt("players." + event.getPlayer().getName() + ".tasks.sleep");
                setConf("players." + event.getPlayer().getName() + ".tasks.sleep", oldValue + 1);
            }
            else if(GetPlayerTaskProgress(event.getPlayer(), "sleep") < taskReach)
            {
                UpdateTaskProgress(event.getPlayer().getName(), "sleep");
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntityType() == EntityType.SKELETON) {
            if (event.getEntity().getKiller() != null) {

                if(!CheckIfPlayerHasTask(event.getEntity().getKiller(), "killskeletons")) return; // if he has not
                // the task, to return
                if(IsTaskCompleted(event.getEntity().getKiller(), "killskeletons")) return; // if he has already
                // completed, to return

                Player p = event.getEntity().getKiller();

                HashMap<String, Integer> taskInfo = GetTaskInfo("killskeletons");
                int taskReach = 0;
                for (Map.Entry<String, Integer> entry : taskInfo.entrySet()) {
                    taskReach = entry.getValue();
                    // Now you can use taskName and taskValue as needed
                }
                if(GetPlayerTaskProgress(p, "killskeletons") < taskReach - 1)
                {
                    int oldValue = playersConfig.getInt("players." + p.getName() + ".tasks.killskeletons");
                    setConf("players." + p.getName() + ".tasks.killskeletons", oldValue + 1);
                }
                else if(GetPlayerTaskProgress(p, "killskeletons") < taskReach)
                {
                    UpdateTaskProgress(event.getEntity().getKiller().getName(), "killskeletons");
                }
            }
        }
    }

    private void ShowDaily(Player sender)
    {
        List<String> allPlayerTasks = GetPlayerTasks(sender.getName());
        if(!AreTasksFinished((Player) sender))
        {
            int tasksToComplete = GetPlayerRemainingTasksInt((Player) sender);
            sender.sendMessage(ChatColor.YELLOW + "You have " + tasksToComplete + " more tasks to complete:");
        }
        else
        {
            sender.sendMessage(ChatColor.DARK_GREEN + "You have completed all daily tasks:");
        }
        for (String task : allPlayerTasks) {
            HashMap<String, Integer> taskInfo = GetTaskInfo(task);
            String taskMessage = null;
            int taskReach = 0;
            for (Map.Entry<String, Integer> entry : taskInfo.entrySet()) {
                taskMessage = entry.getKey();
                taskReach = entry.getValue();
                // Now you can use taskName and taskValue as needed
            }
            if(IsTaskCompleted((Player) sender, task) == false)
            {
                sender.sendMessage(ChatColor.YELLOW + taskMessage + " (" + playersConfig.get("players." + sender.getName() + ".tasks." + task) + "/" + taskReach + ")");
            }
            else if(IsTaskCompleted((Player) sender, task) == true)
            {
                sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + taskMessage + " (" + playersConfig.get("players." + sender.getName() + ".tasks." + task) + "/" + taskReach + ")" + ChatColor.DARK_GREEN + " (completed)");
            }
        }
    }

    public void loadConf()
    {
        file = new File("plugins/DailyDFL/players.yml");

        // Create data folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        if(!file.exists())
            saveResource("players.yml", false);

        playersConfig = new YamlConfiguration();
        playersConfig.options().parseComments(true);

        try {
            playersConfig.load(file);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static void saveConf()
    {
        try {
            playersConfig.save(file);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

    }

    public static void setConf(String _path, Object _value)
    {
        playersConfig.set(_path, _value);

        saveConf();
    }

    public static Object getConf(String _player, String _id)
    {
        try
        {
            if(_id == "quests") return playersConfig.get("quests");
            else if(_id == "datecreation") return playersConfig.get("players." + _player + ".ProfileCreationDate");
            else if(_id == "level") return playersConfig.get("players." + _player + ".level");
            else if(_id == "rp") return playersConfig.get("players." + _player + ".rp");
            else if(_id == "lastjoined") return playersConfig.get("players." + _player + ".lastJoined");
            else if(_id == "task1")
            {
                List<String> strings = new ArrayList<>();
                strings = playersConfig.getStringList("tasks");
                return true;
            }
            else if(_id == "task2")
            {
                List<Map<String, Integer>> mapList = getMapListFromYaml("players." + _player + ".tasks");
                if (mapList != null && !mapList.isEmpty()) {
                    // Retrieve the first map from the list
                    Map<String, Integer> firstMap = mapList.get(1);
                    // Retrieve the first string from the first map
                    if (!firstMap.isEmpty()) {
                        return firstMap.keySet().iterator().next();
                    }
                }
                return null;
            }
            else if(_id == "task3")
            {
                List<Map<String, Integer>> mapList = getMapListFromYaml("players." + _player + ".tasks");
                if (mapList != null && !mapList.isEmpty()) {
                    // Retrieve the first map from the list
                    Map<String, Integer> firstMap = mapList.get(2);
                    // Retrieve the first string from the first map
                    if (!firstMap.isEmpty()) {
                        return firstMap.keySet().iterator().next();
                    }
                }
                return null;
            }
            else return playersConfig.get(_id);
        } catch (Exception ex)
        {
            return ex.getMessage();
        }
    }

    public static void LoadPlayerProfile(Player p)
    {
        // Get today's date
        LocalDate today = LocalDate.now();
        // Define a DateTimeFormatter for the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        // Format the date using the formatter
        String dateCreated = today.format(formatter);

        // The newbie player
        if(!playersConfig.contains("players." + p.getName()))
        {
            setConf("players." + p.getName() + ".DailyCreated", dateCreated);

            // Reset completed tasks
            List<Strings> tasksCompleted = new ArrayList<>();
            setConf("players." + p.getName() + ".completedtasks", tasksCompleted);

            setConf("players." + p.getName() + ".dailycompleted", 0);

            // This variable stores all possible tasks
            List<String> allTasks = GetAllTasks();
            Collections.shuffle(allTasks);

            // Max tasks per player
            int maxTasks = 3;

            // Generate "maxTasks" tasks for the player
            for (int i = 0; i < maxTasks; i++) {
                if (!playersConfig.contains("players." + p.getName() + ".tasks." + allTasks.get(i).toString())) {
                    setConf("players." + p.getName() + ".tasks." + allTasks.get(i).toString(), 0);
                }
            }
        }
        // Player joined another date
        else if(!getConf(p.getName(), "players." + p.getName() + ".DailyCreated").equals(dateCreated))
        {
            setConf("players." + p.getName() + ".DailyCreated", dateCreated);

            // Reset completed tasks
            List<Strings> tasksCompleted = new ArrayList<>();
            setConf("players." + p.getName() + ".completedtasks", tasksCompleted);

            setConf("players." + p.getName() + ".dailycompleted", 0);

            setConf("players." + p.getName() + ".tasks", null);

            // This variable stores all possible tasks
            List<String> allTasks = GetAllTasks();
            Collections.shuffle(allTasks);

            // Max tasks per player
            int maxTasks = 3;

            // Generate "maxTasks" tasks for the player
            for (int i = 0; i < maxTasks; i++) {
                if (!playersConfig.contains("players." + p.getName() + ".tasks." + allTasks.get(i).toString())) {
                    setConf("players." + p.getName() + ".tasks." + allTasks.get(i).toString(), 0);
                }
            }
        }
    }

    public List<String> GetPlayerTasks(String playerName) {
        List<String> playerTasks = new ArrayList<>();

        ConfigurationSection playerSection = playersConfig.getConfigurationSection("players." + playerName + ".tasks");
        if (playerSection != null) {
            playerTasks.addAll(playerSection.getKeys(false));
        }

        return playerTasks;
    }

    private static List<String> GetAllTasks() { return playersConfig.getStringList("tasks"); }

    private static List<String> GetCompletedTasks(Player p) { return playersConfig.getStringList("players." + p.getName() + ".completedtasks"); }

    public static int GetAllPlayerTasksInt(String playerName) {
        playersConfig = LoadYAML();
        int allPlayerTasks = 0;
        ConfigurationSection tasksSection = playersConfig.getConfigurationSection("players." + playerName + ".tasks");
        if (tasksSection != null) {
            for (String task : tasksSection.getKeys(false)) {
                allPlayerTasks++;
            }
        }
        return allPlayerTasks;
    }

    private static int GetPlayerRemainingTasksInt(Player p) { int remainedTasksToComplete = GetAllPlayerTasksInt(p.getName()) - GetCompletedTasksInt(p.getName()); return remainedTasksToComplete; }

    private static Boolean IsTaskCompleted(Player p, String task)
    {
        List<String> allTasksCompleted = playersConfig.getStringList("players." + p.getName() + ".completedtasks");
        if(allTasksCompleted.contains(task)) return true;
        else return false;
    }

    public static Boolean HasDailyCompleted(String _playerName)
    {
        playersConfig = LoadYAML();

        if(playersConfig.getInt("players." + _playerName + ".dailycompleted") == 1)
        {
            return true;
        }
        return false;
    }

    public void UpdateTaskProgress(String _playerName, String _task)
    {
        int oldValue = playersConfig.getInt("players." + _playerName + ".tasks." + _task);
        setConf("players." + _playerName + ".tasks." + _task, oldValue + 1);
        List<String> tasksCompleted = playersConfig.getStringList("players." + _playerName + ".completedtasks");
        tasksCompleted.add(_task);
        setConf("players." + _playerName + ".completedtasks", tasksCompleted);
        int tasksCompletedInt = GetCompletedTasksInt(_playerName);
        int randomXP = new Random().nextInt(100) + 100;
        Player player = Bukkit.getPlayer(_playerName);
        Bukkit.getPlayer(_playerName).giveExp(randomXP);
        Bukkit.getPlayer(_playerName).playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        String message = (tasksCompletedInt < 3) ? "You completed a task (+" + randomXP + " xp)." :
                "You finished all daily tasks (+" + randomXP + "xp, +1RP)";
        Bukkit.getPlayer(_playerName).sendMessage(ChatColor.DARK_GREEN + message);
        if(tasksCompletedInt == GetAllPlayerTasksInt(_playerName)) {
            setConf("players." + _playerName + ".dailycompleted", 1);
            StatsDFL.UpdateTotalDailyCompleted(_playerName);
            LaunchFirework(Bukkit.getPlayer(_playerName));
            CenterText(Bukkit.getPlayer(_playerName), ChatColor.LIGHT_PURPLE + "Congrats!", "You finished all tasks");
            StatsDFL.GivePlayerFP(_playerName);
            List<String> stringPlayersCompleted = (List<String>) getConf(_playerName, "listplayerswhocompleted");
            stringPlayersCompleted.add(_playerName);
            setConf("listplayerswhocompleted", stringPlayersCompleted);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            LaunchFireworkPlayer(Bukkit.getPlayer(_playerName));
        }
        MessageAllOnTaskCompleted(Bukkit.getPlayer(_playerName));
    }

    public static int GetCompletedTasksInt(String playerName)
    {
        playersConfig = LoadYAML();

        List<String> tasksCompleted = playersConfig.getStringList("players." + playerName + ".completedtasks");
        int tasksCompletedInt = 0;
        for (String string : tasksCompleted) {
            tasksCompletedInt++;
        }
        return tasksCompletedInt;
    }

    private static Boolean CheckIfPlayerHasTask(Player p, String task)
    {
        if(task.equals("eat"))
        {
            if(playersConfig.contains("players." + p.getName() + ".tasks.eat")) return true;
        }
        else if(task.equals("sleep"))
        {
            if(playersConfig.contains("players." + p.getName() + ".tasks.sleep")) return true;
        }
        else if(task.equals("killskeletons"))
        {
            if(playersConfig.contains("players." + p.getName() + ".tasks.killskeletons")) return true;
        }
        return false;
    }

    private Boolean AreTasksFinished(Player p)
    {
        int tasksCompleted = GetCompletedTasksInt(p.getName());
        int tasksForPlayerInt = 0;
        List<String> tasksForPlayerStrings = GetPlayerTasks(p.getName());
        for (String string : tasksForPlayerStrings) {
            tasksForPlayerInt++;
        }
        if (tasksCompleted == tasksForPlayerInt) return true;
        return false;
    }

    private static HashMap<String, Integer> GetTaskInfo(String task)
    {
        if(task.equals("sleep")) return new HashMap<>(Collections.singletonMap("Sleep in a bed", 3));
        else if(task.equals("eat")) return new HashMap<>(Collections.singletonMap("Eat food", 10));
        else if(task.equals("killskeletons")) return new HashMap<>(Collections.singletonMap("Kill Skeletons", 10));
        else if(task.equals("killspiders")) return new HashMap<>(Collections.singletonMap("Kill Spiders", 8));
        else return null;
    }

    private static int GetPlayerTaskProgress(Player p, String task) { int progress = playersConfig.getInt("players." + p.getName() + ".tasks." + task); return progress; }

    public static List<Map<String, Integer>> getMapListFromYaml(String path) {
        // Retrieve the ConfigurationSection from the specified path
        ConfigurationSection section = playersConfig.getConfigurationSection(path);
        if (section != null) {
            // Create a list to hold maps
            List<Map<String, Integer>> mapList = new ArrayList<>();
            // Iterate over each key in the ConfigurationSection
            for (String key : section.getKeys(false)) {
                // Retrieve the map from the ConfigurationSection
                Map<String, Integer> map = new HashMap<>();
                ConfigurationSection subSection = section.getConfigurationSection(key);
                if (subSection != null) {
                    for (String subKey : subSection.getKeys(false)) {
                        map.put(subKey, subSection.getInt(subKey)); // Assuming values are integers
                    }
                }
                mapList.add(map);
            }
            return mapList;
        }
        return null;
    }

    public static List<String> GetPlayersWhoCompleted()
    {
        playersConfig = LoadYAML();
        List<String> players = playersConfig.getStringList("listplayerswhocompleted");
        return players;
    }

    private static void MessageAllOnTaskCompleted(Player p)
    {
        int tasksCompleted = GetCompletedTasksInt(p.getName());
        if(tasksCompleted < 2)
        {
            for (Player eachP : Bukkit.getOnlinePlayers()) {
                eachP.sendMessage(dailyPrefix + ChatColor.YELLOW + p.getName() + " completed a task (" + tasksCompleted + "/" +
                        GetAllPlayerTasksInt(p.getName()) + ").");
                if(!HasDailyCompleted(eachP.getName()))
                {
                    eachP.sendMessage(ChatColor.YELLOW + "You have " + GetPlayerRemainingTasksInt(eachP) + " more" +
                            " tasks (/daily)");
                }
            }
        }
        else if(tasksCompleted == 3)
        {
            for (Player eachP : Bukkit.getOnlinePlayers()) {
                eachP.sendMessage(dailyPrefix + ChatColor.DARK_GREEN + p.getName() + " completed all daily tasks (" +
                        tasksCompleted + "/" + GetAllPlayerTasksInt(p.getName()) + ").");
                if(!HasDailyCompleted(eachP.getName()))
                {
                    eachP.sendMessage(ChatColor.YELLOW + "You have " + GetPlayerRemainingTasksInt(eachP) + " more" +
                            " tasks (/daily)");
                }
            }
        }
    }

    private boolean isFood(ItemStack item) {
        // Implement your logic to check if the item is food
        // For example, you can check its material type
        Material material = item.getType();
        return material.isEdible(); // This is a simple check, adjust it as per your requirements
    }

    public static YamlConfiguration LoadYAML()
    {
        File file = new File("plugins/DailyDFL/players.yml");
        YamlConfiguration _config = YamlConfiguration.loadConfiguration(file);
        return _config;
    }

    private static void LaunchFirework(Player player) {

    }

    public static void CenterText(Player player, String title, String subtitle) {
        player.sendTitle(title, subtitle, 20, 20*5, 40);
    }

    private void ResetListPlayersWhoCompleted()
    {
        List<String> listReset = new ArrayList<>();
        setConf("listplayerswhocompleted", listReset);
    }

//    public void spawnFirework(Location location, Player player) {
//        Firework firework = location.getWorld().spawn(location, Firework.class);
//        FireworkMeta fireworkMeta = firework.getFireworkMeta();
//
//        // Customize firework effect
//        FireworkEffect.Builder builder = FireworkEffect.builder();
//        builder.with(FireworkEffect.Type.BURST)
//                .withColor((Iterable<?>) Color.RED)
//                .withFade((Iterable<?>) Color.ORANGE)
//                .trail(true)
//                .flicker(false);
//
//        fireworkMeta.addEffect(builder.build());
//        fireworkMeta.setPower(1);
//        firework.setFireworkMeta(fireworkMeta);
//
//        // Apply velocity to shoot the firework upwards
//        Location start = player.getLocation(); // Get the player location and store it.
//        Vector3f vector3f = new Vector3f(0, 1, 0);
//        int x = Math.round(vector3f.x);
//        int y = Math.round(vector3f.y);
//        int z = Math.round(vector3f.z);
//        Vector vector = new Vector(x, y, z);
//        firework.setVelocity(vector);
//
//        // Schedule the firework to be removed after a few seconds
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                firework.detonate();
//            }
//        }.runTaskLater(this, 20L); // Adjust the delay (in ticks) as needed
//    }

    public void LaunchFireworkPlayer(Player player)
    {
        Firework firework = (Firework) player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();

        meta.setPower(2); // Set power to 2 for a bigger explosion

        FireworkEffect effect = FireworkEffect.builder()
                .flicker(true)
                .trail(true)
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(org.bukkit.Color.YELLOW)
                .withFade(org.bukkit.Color.ORANGE)
                .build();

        meta.addEffect(effect);
        firework.setFireworkMeta(meta);

        // Schedule the removal of the firework after 4 seconds
        Bukkit.getScheduler().runTaskLater(this, () -> {
            firework.detonate(); // Detonate the firework after 4 seconds
        }, 80L); // 4 seconds = 4 * 20 ticks/second = 80 ticks
    }
}
