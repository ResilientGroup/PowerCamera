package nl.svenar.powercamera.commands.subcommand;

import java.util.List;
import nl.svenar.powercamera.PowerCamera;
import nl.svenar.powercamera.Util;
import nl.svenar.powercamera.commands.PowerCameraCommand;
import nl.svenar.powercamera.commands.structure.CommandExecutionContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SubcommandInfo extends PowerCameraCommand {

    public SubcommandInfo(PowerCamera plugin, String commandName) {
        super(plugin, commandName, CommandExecutionContext.PLAYER);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (sender.hasPermission("powercamera.cmd.info")) {
            if (args.length == 0) {
                String cameraName = plugin.playerSelectedCamera.get(((Player) sender).getUniqueId());
                if (cameraName != null) {
                    List<String> cameraPoints = plugin.getConfigCameras().getPoints(cameraName);
                    int cameraDuration = plugin.getConfigCameras().getDuration(cameraName);

                    sender.sendMessage(
                        ChatColor.BLUE + "===" + ChatColor.DARK_AQUA + "----------" + ChatColor.AQUA + plugin.getPluginDescriptionFile().getName()
                            + ChatColor.DARK_AQUA + "----------" + ChatColor.BLUE + "===");
                    sender.sendMessage(ChatColor.DARK_GREEN + "Camera name: " + ChatColor.GREEN + cameraName);
                    sender.sendMessage(ChatColor.DARK_GREEN + "Path duration: " + ChatColor.GREEN + cameraDuration + " seconds");
                    sender.sendMessage(ChatColor.DARK_GREEN + "Camera points (" + ChatColor.GREEN + cameraPoints.size() + ChatColor.DARK_GREEN + "):");

                    int index = 0;
                    for (String rawPoint : cameraPoints) {
                        index++;

                        String type = rawPoint.split(":", 3)[0];
                        String easing = rawPoint.split(":", 3)[1];
                        String point = rawPoint.split(":", 3)[2];

                        String pointInfo = "";
                        pointInfo += "#" + index + " ";
                        pointInfo += type + " (" + easing + "): ";

                        if (type.equalsIgnoreCase("location")) {
                            Location pointLocation = Util.deserializeLocation(point);

                            pointInfo += pointLocation.getWorld().getName();
                            pointInfo +=
                                ", (X: " + pointLocation.getBlockX() + ", Y: " + pointLocation.getBlockY() + ", Z: " + pointLocation.getBlockZ() + ")";
                            pointInfo += ", (Yaw: " + Math.round(pointLocation.getYaw()) + ", Pitch: " + Math.round(pointLocation.getPitch()) + ")";
                        } else {
                            pointInfo += point;
                        }

                        sender.sendMessage(ChatColor.DARK_GREEN + "- " + ChatColor.GREEN + pointInfo);
                    }
                    sender.sendMessage(ChatColor.BLUE + "===" + ChatColor.DARK_AQUA + "-------------------------------" + ChatColor.BLUE + "===");
                } else {
                    sender.sendMessage(plugin.getPluginChatPrefix() + ChatColor.RED + "No camera selected!");
                    sender.sendMessage(plugin.getPluginChatPrefix() + ChatColor.GREEN + "Select a camera by doing: /" + commandLabel + " select <name>");
                }

            } else {
                sender.sendMessage(plugin.getPluginChatPrefix() + ChatColor.DARK_RED + "Usage: /" + commandLabel + " info");
            }

        } else {
            sender.sendMessage(plugin.getPluginChatPrefix() + ChatColor.DARK_RED + "You do not have permission to execute this command");
        }

        return false;
    }
}
