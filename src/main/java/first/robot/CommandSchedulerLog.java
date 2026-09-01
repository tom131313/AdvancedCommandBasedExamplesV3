package first.robot;

import java.lang.invoke.MethodHandles;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;
import org.wpilib.command3.SchedulerEvent;
import org.wpilib.driverstation.DriverStation;
import org.wpilib.networktables.NetworkTable;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.StringEntry;
import org.wpilib.system.DataLogManager;
import org.wpilib.util.WPIUtilJNI;

/**
 * Log Command Scheduler actions for command events of scheduled, mounted, yielded, completed,
 *  completed with error, canceled, and interrupted.
 * <p>Note that there are other datalog options available besides this class and the logging of these
 *  command scheduler event may be in addition to other logging options.
 * <p>This class along with other datalog features could be used this way:
 *
<pre><code>

class Robot:
    private final Scheduler scheduler = Scheduler.getDefault();
    private final RobotContainer robotContainer = new RobotContainer(this);

    private final ProtobufLogEntry<Scheduler> schedulerLog =
      ProtobufLogEntry.create(DataLogManager.getLog(), "Scheduler", Scheduler.proto);

    // Start recording to data log
    DataLogManager.start();

    // Record DS control and joystick data.
    // Change to `false` to not record joystick data.
    DriverStation.startDataLog(DataLogManager.getLog(), true);

    // periodically:
    schedulerLog.append(scheduler); // dumps some scheduler info to NT but bug in 2027 alpha7 causes DataLog to show <invalid> instead of the good NT data that does show but passes by quickly!

class RobotContainer:
    // this is configuration code that could be put in a config file
    //Choices Of Destinations of Command Logging
    // public static EnumSet<LogsSelector> logsSelector =
    //     EnumSet.allOf(LogsSelector.class);
    // OR PICK WHICH INDIVIDUALS TO USE comment out the unwanted ones
    public static EnumSet<LogsSelector> logsSelector =
        EnumSet.of(
                // LogsSelector.useConsole,
                LogsSelector.useDataLog
        );
    // OR PICK NONE
    // public static EnumSet<LogsSelector> logsSelector = EnumSet.noneOf(LogsSelector.class);
    // end configuration code

    private CommandSchedulerLog schedulerLog = new CommandSchedulerLog(logsSelector);
</code></pre>
 */
public class CommandSchedulerLog 
{
    private static final String m_fullClassName = MethodHandles.lookup().lookupClass().getCanonicalName();
    static
    {
        System.out.println("Loading: " + m_fullClassName);
    }

    public enum LogsSelector
    {
        useConsole, useDataLog
    };

    private static final Alert COMMAND_LOGGING_OFF = new Alert("Log Commands", "1", "No Command Logging Selected", Alert.Level.LOW);
    private static final Alert MOUNTED_ERROR = new Alert("Log Commands", "2", "", Alert.Level.HIGH);

    private final long programStartTime = WPIUtilJNI.getProgramStartTime();
    private EnumSet<LogsSelector> m_logsSelector;
    private final HashMap<Command, Integer> m_currentCommands = new HashMap<Command, Integer>();
    private final NetworkTable m_nt;
    private final StringEntry m_scheduledCommandLogEntry;
    private final StringEntry m_mountedCommandLogEntry;
    private final StringEntry m_yieldedCommandLogEntry;
    private final StringEntry m_completedCommandLogEntry;
    private final StringEntry m_completedWithErrorCommandLogEntry;
    private final StringEntry m_canceledCommandLogEntry;
    private final StringEntry m_interruptedCommandLogEntry;
    public boolean logEnabled = true; // quick disconnect

    /**
     * Command Event Loggers
     * 
     * <p>Set the command scheduler to log all the command events.
     * 
     * <p>Log to the Console/Terminal or the WPILib DataLog.
     * 
     * <p>If using DataLog tool, the recording is via NT so tell NT to send EVERYTHING to the DataLog.
     * Run DataLog tool to retrieve log from roboRIO and convert the log to a csv table that may be
     * viewed nicely in Excel.
     * 
     * <p>Note the comment in mount and yield logging that only the first time is logged unless changed.
     * 
     * <p>Note the time displayed is nanoseconds since the program started. Other clocks used
     * throughout robot programs may not be offset by the program start time and may reflect the
     * robot start time or clock time.
     * 
     * @param logsSelector
     */ 
    CommandSchedulerLog(EnumSet<LogsSelector> logsSelector)
    {
        m_logsSelector = logsSelector;

        // DataLog via NT so establish NT and the connection to DataLog
        if (m_logsSelector.contains(LogsSelector.useDataLog)) {
            DataLogManager.logNetworkTables(true); // CAUTION - this puts all NT to the DataLog
        }

        final String networkTableName = "Team4237"; //FIXME change to your favorite name
        m_nt = NetworkTableInstance.getDefault().getTable(networkTableName);
        m_scheduledCommandLogEntry = m_nt.getStringTopic("Commands/scheduled").getEntry("");
        m_mountedCommandLogEntry = m_nt.getStringTopic("Commands/mounted").getEntry("");
        m_yieldedCommandLogEntry = m_nt.getStringTopic("Commands/yielded").getEntry("");
        m_completedCommandLogEntry = m_nt.getStringTopic("Commands/completed").getEntry("");
        m_completedWithErrorCommandLogEntry = m_nt.getStringTopic("Commands/completedWithError").getEntry("");
        m_canceledCommandLogEntry = m_nt.getStringTopic("Commands/canceled").getEntry("");
        m_interruptedCommandLogEntry = m_nt.getStringTopic("Commands/interrupted").getEntry("");

       
        // Start recording to data log
        DataLogManager.start();

        // Record DS control and joystick data.
        // Change to `false` to not record joystick data.
        DriverStation.startDataLog(DataLogManager.getLog(), true);

        configureCommandLogs();
    }

    /**
     * A command was queued to run
     * 
     * <p>If the command is scheduled before this case is instantiated, this callback doesn't happen
     * and mounted will throw an exception since the scheduled key is missing. Mounted has logic to
     * accommodate that case.
     * 
     * <p>Datalog doesn't update the log timestamp if new data has the same value as previous data.
     * This isn't particularly concerning for mounted and yielded but for others it could be
     * significant for troubleshooting. So a timestamp is added to the text to make it unique. For
     * number of runs it's assumed that rarely will the number of runs be identical for two
     * consecutive entries.
     * 
     * @param cmd
     * @param time
     */
    public void logCommandScheduled(Command cmd, long time)
    {
        time-= programStartTime;

        // System.out.println(time + " Scheduled " + cmd.name());

        Command key = cmd;
        String requirements = cmd.requirements().stream()
            .map(mechanism -> mechanism.getClass().getSimpleName())
            .collect(Collectors.joining(", ", "{", "}"));

        if (m_logsSelector.contains(LogsSelector.useConsole)) {
            System.out.println(time + " " + cmd.name() + "[" + Scheduler.getDefault().runId(cmd) + "] scheduled requiring " + requirements);                    

        }
        if (m_logsSelector.contains(LogsSelector.useDataLog)) {
            m_scheduledCommandLogEntry.set(key.name() + " at " + time + " " + requirements);                    
        } 

        m_currentCommands.put(key, 0);
    }

    /**
     * A command was mounted - started or resumed - by the scheduler
     * 
     * <p>This can generate a lot of events so logging is suppressed except for the first
     * occurrence. Total count is logged at command end.
     * 
     * <p>Recompile without the if/else to get all logged.
     * @param cmd
     * @param time
     */
    public void logCommandMounted(Command cmd, long time)
    {
        time-= programStartTime;

        // System.out.println(time + " Mounted " + cmd.name());
        // m_currentCommands.forEach((cmdKey, cmdCount) -> System.out.println("Mounted " + cmdKey + " " + cmdCount));

        Command key = cmd;

        Integer keyRuns = m_currentCommands.get(key);
        if (keyRuns == null)
        {
            MOUNTED_ERROR.setText("Mounted Error - likely command " + key +
                                    " was scheduled before instantiating CommandSchedulerLog. Making approximate fix.");
            MOUNTED_ERROR.set(true);
            logCommandScheduled(cmd, time); // assume the scheduled log wasn't run but can be now for reasonable fix
            keyRuns = m_currentCommands.get(key);
        }

        if (keyRuns == 0) // suppress all but first
        {
            if (m_logsSelector.contains(LogsSelector.useConsole)) {
                System.out.println(time + " " + cmd.name() + "[" + Scheduler.getDefault().runId(cmd) + "] mounted");                        
            }
            if (m_logsSelector.contains(LogsSelector.useDataLog)) {
                m_mountedCommandLogEntry.set(key.name());             
            }
        }

        m_currentCommands.put(key, m_currentCommands.get(key) + 1);
    }

    /**
     * A command paused with Coroutine.yield()
     * 
     * <p>Log only the yield after the command was first mounted
     * @param cmd
     * @param time
     */
    public void logCommandYielded(Command cmd, long time)
    {
        time-= programStartTime;

        // System.out.println(time + " Yielded " + cmd.name());

        Command key = cmd;

        if (m_currentCommands.getOrDefault(key, 0) == 1) // suppress all but first
        {
            if (m_logsSelector.contains(LogsSelector.useConsole)) {
                System.out.println(time + " " + cmd.name() + "[" + Scheduler.getDefault().runId(cmd) + "] yielded");                        
            }
            if (m_logsSelector.contains(LogsSelector.useDataLog)) {
                m_yieldedCommandLogEntry.set(key.name());             
            }
        }
    }

    /**
     * A command successfully completed
     * @param cmd
     * @param time
     */
    public void logCommandCompleted(Command cmd, long time)
    {
        time-= programStartTime;

        // System.out.println(time + " Completed " + cmd.name());

        Command key = cmd;
        String runs = " after " + m_currentCommands.getOrDefault(key, 0) + " runs";

        if (m_logsSelector.contains(LogsSelector.useConsole)) {
            System.out.println(time + " " + cmd.name() + "[" + Scheduler.getDefault().runId(cmd) + "] completed" + runs);                    
        }
        if (m_logsSelector.contains(LogsSelector.useDataLog)) {
            m_completedCommandLogEntry.set(key.name() + runs + " at " + time);                    
        } 

        m_currentCommands.remove(key);
    }

    /**
     * A command encountered an unhandled exception
     * @param cmd
     * @param exception
     * @param time
     */
    public void logCommandCompletedWithError(Command cmd, Throwable exception, long time)
    {
        time-= programStartTime;

        // System.out.println(time + " Completed With Error " + cmd.name() + " exception " + exception);

        Command key = cmd;
        String runs = " after " + m_currentCommands.getOrDefault(key, 0) + " runs";

        if (m_logsSelector.contains(LogsSelector.useConsole)) {
            System.out.println(time + " " + cmd.name() + "[" + Scheduler.getDefault().runId(cmd) + "] completed " + runs + " with error " + exception.getMessage() + " at " + time);                    
        }
        if (m_logsSelector.contains(LogsSelector.useDataLog)) {
            m_completedWithErrorCommandLogEntry.set(key.name() + runs + "with error " + exception.getMessage());                    
        } 

        m_currentCommands.remove(key);
    }

    /**
     * A command was canceled (various causes)
     * @param cmd
     * @param time
     */
    public void logCommandCanceled(Command cmd, long time)
    {
        time-= programStartTime;

        // System.out.println(time + " Canceled" + cmd.name());

        Command key = cmd;
        String runs = " after " + m_currentCommands.getOrDefault(key, 0) + " runs";

        if (m_logsSelector.contains(LogsSelector.useConsole)) {
            System.out.println(time + " " + cmd.name() + "[" + Scheduler.getDefault().runId(cmd) + "] canceled" + runs);                    
        }
        if (m_logsSelector.contains(LogsSelector.useDataLog)) {
            m_canceledCommandLogEntry.set(key.name() + runs + " at " + time);                    
        } 

        m_currentCommands.remove(key);
    }

    /**
     * A command was interrupted by another
     * @param cmd
     * @param byCmd
     * @param time
     */
    public void logCommandInterrupted(Command cmd, Command byCmd, long time)
    {
        time-= programStartTime;

        // System.out.println(time + " Interrupted " + cmd.name()  + " by " + byCmd.name());

        Command key = cmd;
        String interrupter = " interrupted by command " + byCmd.name() + "[" + Scheduler.getDefault().runId(cmd) + "]";

        if (m_logsSelector.contains(LogsSelector.useConsole)) {
            System.out.println(time + " " + cmd.name() + "[" + Scheduler.getDefault().runId(cmd) + "]" + interrupter);                    
        }
        if (m_logsSelector.contains(LogsSelector.useDataLog)) {
            m_interruptedCommandLogEntry.set(key.name() + interrupter + " at " + time);                    
        } 
    }

    /**
     * Configure Command logging to Console/Terminal or DataLog
     */
    private void configureCommandLogs()
    {
        if (m_logsSelector.contains(LogsSelector.useConsole) ||
            m_logsSelector.contains(LogsSelector.useDataLog)) {
            Scheduler.getDefault().addEventListener( // Can (optionally) generate a lot of output
                event -> {
                try { // no known exceptions; this is for the unknown 'cuz stuff happens
                if (logEnabled)
                {
                    // examples of debug logging and suppressing huge excess output so the good stuff is easier to find
                    // System.out.println("[SchedulerEvent] " + event); // trivial logging to console; it works and is complete but many Mounted and Yielded
                    // if (event.toString().contains("Achieve Hue Display")) return; // suppress unneeded command data
                    switch (event) { // a smarter formatting to hold down excess output
                        case SchedulerEvent.Scheduled(var cmd, var time) -> logCommandScheduled(cmd, time);
                        case SchedulerEvent.Mounted(var cmd, var time) -> logCommandMounted(cmd, time);
                        case SchedulerEvent.Yielded(var cmd, var time) -> logCommandYielded(cmd, time);
                        case SchedulerEvent.Completed(var cmd, var time) -> logCommandCompleted(cmd, time);
                        case SchedulerEvent.CompletedWithError(var cmd, var exception, var time) -> logCommandCompletedWithError(cmd, exception, time);
                        case SchedulerEvent.Canceled(var cmd, var time) -> logCommandCanceled(cmd, time);
                        case SchedulerEvent.Interrupted(var cmd, var byCmd, var time) -> logCommandInterrupted(cmd, byCmd, time);
                    }
                }
                } catch (Exception e)
                {
                    e.printStackTrace();
                    logEnabled = false; // some error so stop the logging
                    System.out.println("ERROR STOPPED COMMAND LOGGING");
                }
                }
            );
        }
        else {
            COMMAND_LOGGING_OFF.set(true);
        }
    }
}
/*
When the default command is bumped out,
the execution sequence is yield, interrupt, whenCanceled, cancel.
When the interrupter ends somehow and there isn't another command for the requirements,
then the default command is scheduled and mounted to run.
If the default command ends normally (likely that shouldn't happen), it is immediately
scheduled again.
*/
//FIXME probably need to add ForkFailure or another event
  /**
   * An event marking when a child command could not be forked.
   *
   * @param command The command attempting to fork the child commands
   * @param failures The reasons the child commands could not be forked
   * @param timestampNanos When the child commands were attempted to be forked
   */
//   record ForkFailure(Command command, List<ScheduleResult.Failure> failures, long timestampNanos)
//       implements SchedulerEvent {}