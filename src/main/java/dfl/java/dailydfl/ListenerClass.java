package dfl.java.dailydfl;

import com.google.errorprone.annotations.ForOverride;
import dfl.java.statsdfl.StatsDFL;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.command.CommandExecutor;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.List;

public class ListenerClass extends DailyDFL implements Listener {

    DailyDFL dailyDFL;
    public ListenerClass(DailyDFL dailyDFL) { this.dailyDFL = dailyDFL; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
//        if(command.getName().equalsIgnoreCase("daily"))
//        {
//            if(StatsDFL.getConf(sender.getName(), "dailycompleted") == "1")
//            {
//                sender.sendMessage(ChatColor.DARK_GREEN + "You completed all daily tasks.");
//                return true;
//            }
//            else
//            {
//                //DailyDFL dailyDFL = new DailyDFL();
//                List<String> allPlayerTasks = dailyDFL.GetDailyTasks((Player) sen);
//                for (int i = 0; i < allPlayerTasks.size(); i++) {
//                    int taskNumber = i + 1;
//                    String stringTask = (String) dailyDFL.getConf(sender.getName(), "task" + taskNumber);
//                    sender.sendMessage(ChatColor.YELLOW + stringTask);
//                    return true;
//                }
//            }
//        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        event.getPlayer().sendMessage("test");
        //DailyDFL dailyDFL = new DailyDFL();
        dailyDFL.loadConf();
        dailyDFL.LoadPlayerProfile(event.getPlayer());
    }

}
