package first.robot;

import static org.wpilib.units.Units.Milliseconds;

import org.wpilib.units.measure.Time;
// import org.wpilib.util.Alert;

/**
 * The Constants class provides a convenient place for teams to hold robot-wide numerical or boolean
 * constants. These values are not expected to change (often). This class should not be used for any
 * other purpose. All constants should be declared globally (i.e. public static). Do not put
 * anything functional in this class.
 * 
 * <p>Maybe Alerts should be put in this class.
 * 
 * <p>
 * Variables that are considered user settable to configure a particular use should be set in the
 * {@link Config} interface.
 * <p>
 * It is advised to statically import this class and the inner classes wherever the constants are
 * needed, to reduce verbosity.
 * <p>
 * There are a few user settable constants scattered around this project. These likely will not be
 * changed and aren't in {@link Config} either. The values are usually an integral part of the
 * structure of the example and changing the "constant" would mess up the logic which was not
 * dynamically determined.
 */
public final class Constants {

    // Xbox controller port used to initiate alignment commands
    public static final int operatorControllerPort = 0;

    /**
     * Arrangement of the use of the LEDs by the examples
     */
    public static final class LEDlayout {

        /**
         * Layout by LED number of the single physical buffer into multiple logical views or mechanisms.
         * 
         * Location of view in buffer - zero-based buffer numbering.
         * 
         * Order doesn't matter.

        * A view will be reversed if the starting index is after the
        * ending index; writing front-to-back in the view will write
        * in the back-to-front direction on the underlying buffer.
        *
        * You take your chances with overlapping views.
        */
        public static enum LEDViewPlacement {
            // 6 strings of 8 LEDs each
            TOP           (0, 7),
            MAIN          (8, 15),
            ENABLEDISABLE (16, 23),
            HISTORYDEMO   (24, 31),
            ACHIEVEHUEGOAL(32, 39),
            KNIGHTRIDER   (40, 47);
        
            public int first;
            public int last;

            /**
             * Location of view in buffer [zero-based numbering]
             * 
             * @param first LED number inclusive
             * @param last LED number inclusive
             */
            private LEDViewPlacement(int first, int last)
            {
            this.first = first;
            this.last = last;
            }
        }
    }

    /**
     * AchieveHueGoal PID constants.
     * These may be changed on the fly by a dashboard such as Elastic or NT Viewer as the PID
     * controller is a Tunable.
     */
    public static final class HueGoal {
        public static final double kP = 0.002;
        public static final double kI = 0.;
        public static final double kD = 0.;
        public static final double tolerance = 2.; // hue range is 0 to 180
        public static final Time loopSpeed = Milliseconds.of(0.8);
    }

    /**
     * ALERTS
     */
    public static final class Alerts {
        
        public static final Alert m_commandLogging = new Alert("Log Commands", "1", "No Command Logging Selected", Alert.Level.LOW);
        public static final Alert m_MountedError = new Alert("Log Commands", "2", "", Alert.Level.HIGH);
    
        public static final Alert m_notAllExamples = new Alert("Example not selected", "1", "Not all; only selected ones running", Alert.Level.LOW);
        public static final Alert m_alertTriggerNextCommand = new Alert("Example not selected", "2", "First job Triggers Second", Alert.Level.LOW);
        public static final Alert m_alertMooreFSMLightBar = new Alert("Example not selected", "3", "Moore FSM Light Bar", Alert.Level.LOW);
        public static final Alert m_alertAutonomousSignal = new Alert("Example not selected", "4", "Autonomous Signal", Alert.Level.LOW);
        public static final Alert m_alertStateMachineTest = new Alert("Example not selected", "5", "State Machine Test", Alert.Level.LOW);
        public static final Alert m_alertDisjointedGroup = new Alert("Example not selected", "6", "Disjointed Group Test", Alert.Level.LOW);
        public static final Alert m_alertAchieveHueGoal = new Alert("Example not selected", "7", "Achieve Hue Goal", Alert.Level.LOW);
        public static final Alert m_alertColorWheel = new Alert("Example not selected", "8", "Color Wheel", Alert.Level.LOW);
        public static final Alert m_alertHistory = new Alert("Example not selected", "9", "History", Alert.Level.LOW);
        public static final Alert m_intake = new Alert("Example not selected", "10", "Intake", Alert.Level.LOW);
        public static final Alert m_EnableDisable = new Alert("Example not selected", "11", "Enable Disable", Alert.Level.LOW);        
    }
}
