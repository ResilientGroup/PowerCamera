package nl.svenar.powercamera;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
@SuppressWarnings({"PMD.CommentRequired", "PMD.FieldDeclarationsShouldBeAtStartOfClass", "PMD.LocalVariableCouldBeFinal", "PMD.MethodArgumentCouldBeFinal", "PMD.ShortClassName", "PMD.ShortVariable"})
public final class Util {

    private static final Pattern REGEX_INT = Pattern.compile("^\\d+[^a-zA-Z]?$");

    private static final Pattern REGEX_SECONDS = Pattern.compile("\\d+[sS]");

    private static final Pattern REGEX_MINUTES = Pattern.compile("\\d+[mM]");

    private static final Pattern REGEX_HOURS = Pattern.compile("\\d+[hH]");

    private Util() {
    }

    public static String serializeLocation(Location loc) {
        return loc.getWorld().getUID() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch();
    }

    public static Location deserializeLocation(String input) {
        String[] inputSplit = input.split(";");

        UUID worldUid = UUID.fromString(inputSplit[0]);

        double x = Double.parseDouble(inputSplit[1]);
        double y = Double.parseDouble(inputSplit[2]);
        double z = Double.parseDouble(inputSplit[3]);

        float yaw = Float.parseFloat(inputSplit[4]);
        float pitch = Float.parseFloat(inputSplit[5]);

        World world = Bukkit.getServer().getWorld(worldUid);
        if (world == null) {
            world = Bukkit.getServer().getWorlds().get(0);
        }

        return new Location(world, x, y, z, yaw, pitch);
    }

    public static int timeStringToSecondsConverter(String timeInput) {
        Matcher regexInt = REGEX_INT.matcher(timeInput);

        Matcher regexSeconds = REGEX_SECONDS.matcher(timeInput);
        Matcher regexMinutes = REGEX_MINUTES.matcher(timeInput);
        Matcher regexHours = REGEX_HOURS.matcher(timeInput);

        int seconds = 0;

        if (regexInt.find()) {
            seconds = Integer.parseInt(timeInput);
        } else {
            if (regexSeconds.find()) {
                seconds += Integer.parseInt(timeInput.substring(regexSeconds.start(), regexSeconds.end() - 1));
            }

            if (regexMinutes.find()) {
                seconds += Integer.parseInt(timeInput.substring(regexMinutes.start(), regexMinutes.end() - 1)) * 60;
            }

            if (regexHours.find()) {
                seconds += Integer.parseInt(timeInput.substring(regexHours.start(), regexHours.end() - 1)) * 3600;
            }
        }

        return seconds;
    }
}
