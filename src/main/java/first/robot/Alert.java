package first.robot;

/**
 * Stub since Alert still not available WPILib 2027 alpha7 simulation
 * Alert
 */
public class Alert {
    public enum Level {LOW, MEDIUM, HIGH};

    String group = "Alert";
    String id = "";
    String text = "";
    Level level = Level.LOW;
    boolean display = false;

    Alert(String group, String id, String text, Level level) {
        this.group = group;
        this.id = id;
        this.text = text;
        this.level = level;
    }

    void set(boolean display) {
        this.display = display;
        if (display) System.out.println("[ALERT] " + group + " " + id + " " + text + " importance " + level);
    }

    void setText(String text) {
        this.text = text;
    }
}
