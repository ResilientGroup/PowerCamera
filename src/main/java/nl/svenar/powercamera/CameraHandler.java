package nl.svenar.powercamera;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.svenar.powercamera.data.CameraMode;
import nl.svenar.powercamera.data.PlayerCameraData;
import nl.svenar.powercamera.event.PowerCameraFinishEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
@SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidDuplicateLiterals", "PMD.AvoidLiteralsInIfCondition", "PMD.AvoidReassigningParameters", "PMD.CallSuperInConstructor", "PMD.CognitiveComplexity", "PMD.CommentRequired", "PMD.CyclomaticComplexity", "PMD.GodClass", "PMD.LiteralsFirstInComparisons", "PMD.LocalVariableCouldBeFinal", "PMD.LooseCoupling", "PMD.MethodArgumentCouldBeFinal", "PMD.NPathComplexity", "PMD.RedundantFieldInitializer", "PMD.UnnecessaryCast", "PMD.UseDiamondOperator", "PMD.UselessParentheses"})
public class CameraHandler extends BukkitRunnable {

    private static final int SINGLE_FRAME_DURATION_MS = 50;

    private final PowerCamera plugin;

    private final Player player;

    private final String cameraName;

    private final ArrayList<Location> cameraPathPoints = new ArrayList<Location>();

    private final HashMap<Integer, ArrayList<String>> cameraPathCommands = new HashMap<>();

    private int ticks = 0;

    public CameraHandler(PowerCamera plugin, Player player, String cameraName) {
        this.plugin = plugin;
        this.player = player;
        this.cameraName = cameraName;
    }

    public Player getPlayer() {
        return player;
    }

    public String getCameraName() {
        return cameraName;
    }

    public CameraHandler generatePath() {
        int maxPoints = (this.plugin.getConfigCameras().getDuration(this.cameraName) * 1000) / SINGLE_FRAME_DURATION_MS;

        List<String> rawCameraPoints = this.plugin.getConfigCameras().getPoints(this.cameraName);
        List<String> rawCameraMovePoints = getMovementPoints(rawCameraPoints);

        if (rawCameraMovePoints.size() - 1 == 0) {
            for (int j = 0; j < maxPoints - 1; j++) {
                this.cameraPathPoints.add(Util.deserializeLocation(rawCameraMovePoints.get(0).split(":", 2)[1]));
            }
        } else {
            for (int i = 0; i < rawCameraMovePoints.size() - 1; i++) {
                String rawPoint = rawCameraMovePoints.get(i).split(":", 2)[1];
                String rawPointNext = rawCameraMovePoints.get(i + 1).split(":", 2)[1];
                String easing = rawCameraMovePoints.get(i + 1).split(":", 2)[0];

                Location point = Util.deserializeLocation(rawPoint);
                Location pointNext = Util.deserializeLocation(rawPointNext);

                this.cameraPathPoints.add(point);
                final int maxProgress = maxPoints / (rawCameraMovePoints.size() - 1) - 1;
                for (int j = 0; j < maxProgress; j++) {
                    if (easing.equalsIgnoreCase("linear")) {
                        this.cameraPathPoints.add(translateLinear(point, pointNext, (float) j / (float) maxProgress));
                    }
                    if (easing.equalsIgnoreCase("teleport")) {
                        this.cameraPathPoints.add(pointNext);
                    }
                }
            }
        }

        int commandIndex = 0;
        for (String rawPoint : rawCameraPoints) {
            String type = rawPoint.split(":", 3)[0];
//			String easing = rawPoint.split(":", 3)[1];
            String data = rawPoint.split(":", (type.equals("location") ? 3 : 2))[type.equals("location") ? 2 : 1];

            if (type.equalsIgnoreCase("location")) {
                commandIndex += 1;
            }

            if (type.equalsIgnoreCase("command")) {
                int index = ((commandIndex) * maxPoints / (rawCameraMovePoints.size()) - 1);
                index = commandIndex == 0 ? 0 : index - 1;
                index = Math.max(index, 0);
                if (!this.cameraPathCommands.containsKey(index)) {
                    this.cameraPathCommands.put(index, new ArrayList<>());
                }
                this.cameraPathCommands.get(index).add(data);
//				this.cameraPathCommands.put(index, rawCameraPoints.get(0));
            }
        }

        return this;
    }

    private List<String> getMovementPoints(List<String> rawCameraPoints) {
        List<String> output = new ArrayList<String>();
        for (String rawPoint : rawCameraPoints) {
            String[] pointData = rawPoint.split(":", 2);
            if (pointData[0].equalsIgnoreCase("location")) {
                output.add(pointData[1]);
            }
        }
        return output;
    }

    private Location translateLinear(Location point, Location pointNext, float progress) {
        if (!point.getWorld().getUID().toString().equals(pointNext.getWorld().getUID().toString())) {
            return pointNext;
        }

        Location newPoint = new Location(pointNext.getWorld(), point.getX(), point.getY(), point.getZ());

        newPoint.setX(lerp(point.getX(), pointNext.getX(), progress));
        newPoint.setY(lerp(point.getY(), pointNext.getY(), progress));
        newPoint.setZ(lerp(point.getZ(), pointNext.getZ(), progress));
        newPoint.setYaw((float) lerpYaw(point.getYaw(), pointNext.getYaw(), progress));
        newPoint.setPitch((float) lerp(point.getPitch(), pointNext.getPitch(), progress));

        return newPoint;
    }

    private double lerp(double start, double end, float progress) { // Linear interpolation
        return progress * (end - start) + start;
    }

    private double lerpYaw(double start, double end, float progress) { // Linear interpolation
        double delta = end - start;
        if (delta > 180) {
            delta -= 360;
        } else if (delta < -180) {
            delta += 360;
        }
        return progress * delta + start;
    }

    public CameraHandler start() {
        plugin.getConfigActiveCameras().setCameraActive(cameraName, player);
        if (this.plugin.getConfigPlugin().getConfig().getBoolean("camera-effects.spectator-mode")) {
            player.setGameMode(GameMode.SPECTATOR);
        }
        if (this.plugin.getConfigPlugin().getConfig().getBoolean("camera-effects.invisible")) {
            player.setInvisible(true);
        }

        getCameraData().setCameraMode(CameraMode.VIEW);
        runTaskTimer(this.plugin, 1L, 1L);
        if (!cameraPathPoints.isEmpty()) {
            player.teleport(cameraPathPoints.get(0));
        }

        if (!this.player.hasPermission("powercamera.hidestartmessages")) {
            this.player.sendMessage(this.plugin.getPluginChatPrefix() + ChatColor.GREEN + "Viewing the path of camera '" + this.cameraName + "'!");
        }

        return this;
    }

    public CameraHandler stop() {
        getCameraData().setCameraMode(CameraMode.NONE);
        try {
            this.cancel();
        } catch (Exception ignored) {
            // ignored
        }

        final boolean resetGameMode = this.plugin.getConfigPlugin().getConfig().getBoolean("camera-effects.spectator-mode");
        final boolean resetVisible = this.plugin.getConfigPlugin().getConfig().getBoolean("camera-effects.invisible");
        plugin.getConfigActiveCameras().setCameraInactive(player, true, resetGameMode, resetVisible);

        if (!this.player.hasPermission("powercamera.hidestartmessages")) {
            player.sendMessage(plugin.getPluginChatPrefix() + ChatColor.GREEN + "The path of camera '" + cameraName + "' has ended!");
        }
        Bukkit.getPluginManager().callEvent(new PowerCameraFinishEvent(this));
        return this;
    }

    private Vector calculateVelocity(Location start, Location end) {
        return new Vector(end.getX() - start.getX(), end.getY() - start.getY(), end.getZ() - start.getZ());
    }

    @Override
    public void run() {
        if (getCameraData().getCameraMode() == CameraMode.VIEW) {
            if (this.ticks > cameraPathPoints.size() - 2) {
                this.stop();
                return;
            }

            Location currentPos = cameraPathPoints.get(this.ticks);
            Location nextPoint = cameraPathPoints.get(this.ticks + 1);

            player.teleport(cameraPathPoints.get(this.ticks));

            if (cameraPathCommands.containsKey(this.ticks)) {
                for (String cmd : cameraPathCommands.get(this.ticks)) {
                    String command = cmd.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }

            player.setVelocity(calculateVelocity(currentPos, nextPoint));

            this.ticks += 1;
        } else {
            if (getCameraData().getCameraMode() == CameraMode.NONE) {
                return;
            }

            final boolean resetGameMode = this.plugin.getConfigPlugin().getConfig().getBoolean("camera-effects.spectator-mode");
            final boolean resetVisible = this.plugin.getConfigPlugin().getConfig().getBoolean("camera-effects.invisible");
            plugin.getConfigActiveCameras().setCameraInactive(player, true, resetGameMode, resetVisible);

            getCameraData().setCameraMode(CameraMode.NONE);
            player.sendMessage(plugin.getPluginChatPrefix() + ChatColor.GREEN + "Preview ended!");

            Bukkit.getPluginManager().callEvent(new PowerCameraFinishEvent(this));
        }

    }

    public CameraHandler preview(Player player, int num, int previewTime) {
        List<String> cameraPoints = plugin.getConfigCameras().getPoints(cameraName);

        if (num < 0) {
            num = 0;
        }

        if (num > cameraPoints.size() - 1) {
            num = cameraPoints.size() - 1;
        }

        if (!cameraPoints.get(num).split(":", 2)[0].equalsIgnoreCase("location")) {
            player.sendMessage(plugin.getPluginChatPrefix() + ChatColor.RED + "Point " + (num + 1) + " is not a location!");
            return this;
        }

        player.sendMessage(plugin.getPluginChatPrefix() + ChatColor.GREEN + "Preview started of point " + (num + 1) + "!");
        player.sendMessage(plugin.getPluginChatPrefix() + ChatColor.GREEN + "Ending in " + previewTime + " seconds.");

        plugin.getConfigActiveCameras().setCameraActive(cameraName, player);
        Location point = Util.deserializeLocation(cameraPoints.get(num).split(":", 3)[2]);

        getCameraData().setCameraMode(CameraMode.PREVIEW);
        if (this.plugin.getConfigPlugin().getConfig().getBoolean("camera-effects.spectator-mode")) {
            player.setGameMode(GameMode.SPECTATOR);
        }
        if (this.plugin.getConfigPlugin().getConfig().getBoolean("camera-effects.invisible")) {
            player.setInvisible(true);
        }
        player.teleport(point);

        runTaskLater(this.plugin, previewTime * 20L);
        return this;
    }

    private PlayerCameraData getCameraData() {
        return plugin.getPlayerData().get(player);
    }

}
