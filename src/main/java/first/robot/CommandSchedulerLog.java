package first.robot;

import java.lang.invoke.MethodHandles;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;
import org.wpilib.driverstation.DriverStation;
import org.wpilib.networktables.NetworkTable;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.StringEntry;
import org.wpilib.system.DataLogManager;

import first.robot.Constants.Alerts;

@SuppressWarnings("resource")

/**
 * Log Command Scheduler actions for command events of scheduled, mounted, yielded, completed,
 *  completed with error, canceled, and interrupted.
 * <p>Note that there are other datalog options available besides this class and the logging of these
 *  command scheduler event may be in addition to other logging options.
 * <p>This class along with other datalog features could be used this way:
 *
<pre><code>

public Robot() {
    robotContainer = new RobotContainer();

    // Start recording to data log
    DataLogManager.start();

    // Record DS control and joystick data.
    // Change to `false` to not record joystick data.
    DriverStation.startDataLog(DataLogManager.getLog(), true);
}

class RobotContainer:
    private static final Alert m_commandLogging = new Alert("Log Command", "No Command Logging Selected", Alert.Level.LOW);

    private CommandSchedulerLog schedulerLog;

    // options for logging
    // put your choices in .of(...); hint: type LogsSelector. for list
    private static EnumSet<LogsSelector> logsSelector = EnumSet.of(
        LogsSelector.useDataLog
        ,LogsSelector.useConsole
    );

    configureCommandLogs(); // do early on otherwise log not ready for first commands

    private void configureCommandLogs()
    {
        if (logsSelector.contains(LogsSelector.useConsole) ||
            logsSelector.contains(LogsSelector.useDataLog)) {
            schedulerLog = new CommandSchedulerLog(logsSelector);
            Scheduler.getDefault().addEventListener( // Can (optionally) generate a lot of output
                event -> {
                // examples of debug logging and suppressing huge excess output so the good stuff is easier to find
                // System.out.println("[SchedulerEvent] " + event); // trivial logging to console but it works and is complete but many Mounted and Yielded
                // if (event.toString().contains("Achieve Hue Display")) return;
                switch (event) { // a smarter formatting to hold down excess output
                    case SchedulerEvent.Scheduled(var cmd, var time) -> schedulerLog.logCommandScheduled(cmd, time);
                    case SchedulerEvent.Mounted(var cmd, var time) -> schedulerLog.logCommandMounted(cmd, time);
                    case SchedulerEvent.Yielded(var cmd, var time) -> schedulerLog.logCommandYielded(cmd, time);
                    case SchedulerEvent.Completed(var cmd, var time) -> schedulerLog.logCommandCompleted(cmd, time);
                    case SchedulerEvent.CompletedWithError(var cmd, var exception, var time) -> schedulerLog.logCommandCompletedWithError(cmd, exception, time);
                    case SchedulerEvent.Canceled(var cmd, var time) -> schedulerLog.logCommandCanceled(cmd, time);
                    case SchedulerEvent.Interrupted(var cmd, var byCmd, var time) -> schedulerLog.logCommandInterrupted(cmd, byCmd, time);
                }
                }
            );
        }
        else {
            m_commandLogging.set(true);
        }
    }
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

    private EnumSet<LogsSelector> m_logsSelector;
    protected Scheduler m_scheduler;
    private final HashMap<Command, Integer> m_currentCommands = new HashMap<Command, Integer>();
    private final NetworkTable m_nt;
    private final StringEntry m_scheduledCommandLogEntry;
    private final StringEntry m_mountedCommandLogEntry;
    private final StringEntry m_yieldedCommandLogEntry;
    private final StringEntry m_completedCommandLogEntry;
    private final StringEntry m_completedWithErrorCommandLogEntry;
    private final StringEntry m_canceledCommandLogEntry;
    private final StringEntry m_interruptedCommandLogEntry;

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
    }

    /**
     * A command was queued to run.
     * <p>If the command is scheduled before this case is instantiated, this callback doesn't happen
     * and mounted will throw an exception since the scheduled key is missing. Mounted has logic to
     * accommodate that case.
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
        // System.out.println(time + " Scheduled " + cmd.name());

        Command key = cmd;
        String requirements = cmd.requirements().stream()
            .map(mechanism -> mechanism.getClass().getSimpleName())
            .collect(Collectors.joining(", ", "{", "}"));

        if (m_logsSelector.contains(LogsSelector.useConsole)) {
            System.out.println(time + " " + cmd.name() + " scheduled requiring " + requirements);                    

        }
        if (m_logsSelector.contains(LogsSelector.useDataLog)) {
            m_scheduledCommandLogEntry.set(key.name() + " at " + time + " " + requirements);                    
        } 

        m_currentCommands.put(key, 0);
    }

    /**
     * The scheduler mounted - started or resumed - a command
     * <p>This can generate a lot of events so logging is suppressed except for the first
     * occurrence. Total count is logged at command end.
     * 
     * <p>Recompile without the if/else to get all logged.
     * @param cmd
     * @param time
     */
    public void logCommandMounted(Command cmd, long time)
    {
        // System.out.println(time + " Mounted " + cmd.name());
        // m_currentCommands.forEach((cmdKey, cmdCount) -> System.out.println("Mounted " + cmdKey + " " + cmdCount));
        Command key = cmd;

        Integer keyRuns = m_currentCommands.get(key);
        if (keyRuns == null)
        {
            Alerts.m_MountedError.setText("Mounted Error - likely command " + key +
                                    " was scheduled before instantiating CommandSchedulerLog. Making approximate fix.");
            Alerts.m_MountedError.set(true);
            logCommandScheduled(cmd, time); // assume the scheduled log wasn't run but can be now for reasonable fix
            keyRuns = m_currentCommands.get(key);
        }

        if (keyRuns == 0) // suppress all but first
        {
            if (m_logsSelector.contains(LogsSelector.useConsole)) {
                System.out.println(time + " " + cmd.name() + " mounted");                        
            }
            if (m_logsSelector.contains(LogsSelector.useDataLog)) {
                m_mountedCommandLogEntry.set(key.name());             
            }
        }

        m_currentCommands.put(key, m_currentCommands.get(key) + 1);
    }

    /**
     * A command paused with Coroutine.yield()
     * Log only the yield after the command was first mounted
     * @param cmd
     * @param time
     */
    public void logCommandYielded(Command cmd, long time)
    {
        // System.out.println(time + " Yielded " + cmd.name());

        Command key = cmd;

        if (m_currentCommands.getOrDefault(key, 0) == 1) // suppress all but first
        {
            if (m_logsSelector.contains(LogsSelector.useConsole)) {
                System.out.println(time + " " + cmd.name() + " yielded");                        
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
        // System.out.println(time + " Completed " + cmd.name());

        Command key = cmd;
        String runs = " after " + m_currentCommands.getOrDefault(key, 0) + " runs";

        if (m_logsSelector.contains(LogsSelector.useConsole)) {
            System.out.println(time + " " + cmd.name() + " completed" + runs);                    
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
        // System.out.println(time + " Completed With Error " + cmd.name() + " exception " + exception);

        Command key = cmd;
        String runs = " after " + m_currentCommands.getOrDefault(key, 0) + " runs";

        if (m_logsSelector.contains(LogsSelector.useConsole)) {
            System.out.println(time + " " + cmd.name() + " completed " + runs + " with error " + exception.getMessage() + " at " + time);                    
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
        // System.out.println(time + " Canceled" + cmd.name());

        Command key = cmd;
        String runs = " after " + m_currentCommands.getOrDefault(key, 0) + " runs";

        if (m_logsSelector.contains(LogsSelector.useConsole)) {
            System.out.println(time + " " + cmd.name() + " canceled" + runs);                    
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
        // System.out.println(time + " Interrupted " + cmd.name()  + " by " + byCmd.name());

        Command key = cmd;
        String interrupter = " interrupted by command " + byCmd.name();

        if (m_logsSelector.contains(LogsSelector.useConsole)) {
            System.out.println(time + " " + key + interrupter);                    
        }
        if (m_logsSelector.contains(LogsSelector.useDataLog)) {
            m_interruptedCommandLogEntry.set(key.name() + interrupter + " at " + time);                    
        } 
    }
}
/*
When the default command is bumped out,
the execution sequence is yield, interrupt, whenCancelled, cancel.
When the interrupter ends somehow and there isn't another command for the requirements,
then the default command is scheduled and mounted to run.
If the default command ends normally (likely that shouldn't happen), it is immediately
scheduled again.
*/