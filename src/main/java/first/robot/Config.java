package first.robot;

import static org.wpilib.units.Units.Seconds;

import java.util.EnumSet;

import org.wpilib.driverstation.DriverStationDisplay;
import org.wpilib.driverstation.DriverStationDisplay.Mode;
import org.wpilib.networktables.BooleanPublisher;
import org.wpilib.networktables.BooleanSubscriber;
import org.wpilib.networktables.BooleanTopic;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.system.Timer;

import first.robot.CommandSchedulerLog.LogsSelector;
import first.robot.RobotContainer.ExamplesSelector;
import first.robot.mechanisms.HistoryFSM;

/**
 * This defines the user settable configuration
 * <p>
 * There is some user selectable debug code scattered around this project such as a commented out
 * reference to {@link HistoryFSM#verificationPrint}.
 */
public class Config {

    private static NetworkTableInstance nt = NetworkTableInstance.getDefault();

    static final BooleanTopic doneTopic = nt.getTable("Examples Configuration").getBooleanTopic("Done");
    static final BooleanPublisher donePub = doneTopic.publish(); // if setting a default
    static final BooleanSubscriber done = doneTopic.subscribe(true);

    /**
     * Choices Of Destinations of Command Logging
     */    

        // PICK ALL COMMAND LOGGING METHODS
        // public static EnumSet<LogsSelector> logsSelector =
        //     EnumSet.allOf(LogsSelector.class);
        
        // OR PICK WHICH INDIVIDUALS TO USE comment out the unwanted ones
        // public static EnumSet<LogsSelector> logsSelector =
        //     EnumSet.of(
        //         //   LogsSelector.useConsole,
        //           LogsSelector.useDataLog
        //     );

        // OR PICK NONE
        public static EnumSet<LogsSelector> logsSelector = EnumSet.noneOf(LogsSelector.class);

        static final BooleanTopic useConsoleTopic = nt.getTable("Logging Configuration").getBooleanTopic("useConsole");         
        static final BooleanPublisher useConsolePub = useConsoleTopic.publish();
        static final BooleanSubscriber useConsoleSub = useConsoleTopic.subscribe(true);

        static final BooleanTopic useDatalogTopic = nt.getTable("Logging Configuration").getBooleanTopic("useDatalog");         
        static final BooleanPublisher useDatalogPub = useDatalogTopic.publish();
        static final BooleanSubscriber useDatalogSub = useDatalogTopic.subscribe(true);


    /**
     * Choices of Examples to Run
     */
        // ALL OF THEM or comment out to select individuals
        // public static EnumSet<ExamplesSelector> examplesSelector =
        //     EnumSet.allOf(ExamplesSelector.class);

        // OR PICK WHICH INDIVIDUALS TO USE comment out the unwanted ones
        // public static EnumSet<ExamplesSelector> examplesSelector =
        //     EnumSet.of(
        //           ExamplesSelector.useAchieveHueGoal
        //         , ExamplesSelector.useDisjointParallelGroup
        //         , ExamplesSelector.useTriggerNextCommand
        //         , ExamplesSelector.useHistoryFSM
        //         , ExamplesSelector.useIntake
        //         , ExamplesSelector.useMooreLikeFSM
        //         , ExamplesSelector.useAutonomousSignal
        //         , ExamplesSelector.useColorWheel
        //         , ExamplesSelector.useEnableDisable
        //         , ExamplesSelector.useAnotherFSMtest
        //     );

        // OR PICK NONE
        public static EnumSet<ExamplesSelector> examplesSelector = EnumSet.noneOf(ExamplesSelector.class);

        static final BooleanTopic useAchieveHueGoalTopic = nt.getTable("Examples Configuration").getBooleanTopic("useAchieveHueGoal");         
        static final BooleanPublisher useAchieveHueGoalPub = useAchieveHueGoalTopic.publish();
        static final BooleanSubscriber useAchieveHueGoalSub = useAchieveHueGoalTopic.subscribe(true);

        static final BooleanTopic useDisjointParallelGroupTopic = nt.getTable("Examples Configuration").getBooleanTopic("useDisjointParallelGroup");         
        static final BooleanPublisher useDisjointParallelGroupPub = useDisjointParallelGroupTopic.publish();
        static final BooleanSubscriber useDisjointParallelGroupSub = useDisjointParallelGroupTopic.subscribe(true);

        static final BooleanTopic useTriggerNextCommandTopic = nt.getTable("Examples Configuration").getBooleanTopic("useTriggerNextCommand");         
        static final BooleanPublisher useTriggerNextCommandPub = useTriggerNextCommandTopic.publish();
        static final BooleanSubscriber useTriggerNextCommandSub = useTriggerNextCommandTopic.subscribe(true);

        static final BooleanTopic useHistoryFSMTopic = nt.getTable("Examples Configuration").getBooleanTopic("useHistoryFSM");         
        static final BooleanPublisher useHistoryFSMPub = useHistoryFSMTopic.publish();
        static final BooleanSubscriber useHistoryFSMSub = useHistoryFSMTopic.subscribe(true);

        static final BooleanTopic useIntakeTopic = nt.getTable("Examples Configuration").getBooleanTopic("useIntake");         
        static final BooleanPublisher useIntakePub = useIntakeTopic.publish();
        static final BooleanSubscriber useIntakeSub = useIntakeTopic.subscribe(true);

        static final BooleanTopic useMooreLikeFSMTopic = nt.getTable("Examples Configuration").getBooleanTopic("useMooreLikeFSM");         
        static final BooleanPublisher useMooreLikeFSMPub = useMooreLikeFSMTopic.publish();
        static final BooleanSubscriber useMooreLikeFSMSub = useMooreLikeFSMTopic.subscribe(true);

        static final BooleanTopic useAutonomousSignalTopic = nt.getTable("Examples Configuration").getBooleanTopic("useAutonomousSignal");         
        static final BooleanPublisher useAutonomousSignalPub = useAutonomousSignalTopic.publish();
        static final BooleanSubscriber useAutonomousSignalSub = useAutonomousSignalTopic.subscribe(true);

        static final BooleanTopic useColorWheelTopic = nt.getTable("Examples Configuration").getBooleanTopic("useColorWheel");         
        static final BooleanPublisher useColorWheelPub = useColorWheelTopic.publish();
        static final BooleanSubscriber useColorWheelSub = useColorWheelTopic.subscribe(true);

        static final BooleanTopic useEnableDisableTopic = nt.getTable("Examples Configuration").getBooleanTopic("useEnableDisable");         
        static final BooleanPublisher useEnableDisablePub = useEnableDisableTopic.publish();
        static final BooleanSubscriber useEnableDisableSub = useEnableDisableTopic.subscribe(true);
        
        static final BooleanTopic useAnotherFSMtestTopic = nt.getTable("Examples Configuration").getBooleanTopic("useAnotherFSMtest");         
        static final BooleanPublisher useAnotherFSMtestPub = useAnotherFSMtestTopic.publish();
        static final BooleanSubscriber useAnotherFSMtestSub = useAnotherFSMtestTopic.subscribe(true);
       
        /**
         * Make selection of command logging and examples to run from Network Tables (Elastic, TableViewer, etc.)
         * 
         *  <p>User has 2 minutes to select the options to use, then the program moves on with
         *  whatever has been selected up to that point. User may press "Done" to continue before
         *  2 minutes have expired.
         */
        public static void config()
        {
            donePub.setDefault(false);

            useConsolePub.setDefault(false);
            useDatalogPub .setDefault(true);
            
            useAchieveHueGoalPub.setDefault(true);
            useDisjointParallelGroupPub.setDefault(true);
            useTriggerNextCommandPub.setDefault(true);
            useHistoryFSMPub.setDefault(true);
            useIntakePub.setDefault(true);
            useMooreLikeFSMPub.setDefault(true);
            useAutonomousSignalPub.setDefault(true);
            useColorWheelPub.setDefault(true);
            useEnableDisablePub.setDefault(true);         
            useAnotherFSMtestPub.setDefault(true);

            var timeOut = Timer.getTimestamp() + Constants.optionsEntryTimeout;
            DriverStationDisplay.setMode(Mode.RawAnsi);

            DriverStationDisplay.writeRawAnsi(
                "\033[2J\033[H"
                + "Select examples to run on the Dashboard within seconds\n");

            while ((Timer.getTimestamp() < timeOut) && (!done.get()))
            {
                // show the user how much time is left to make choices
                // Better to show the timer on the same Elastic tab as the inputs but do it this way
                // as an example (of what can be done but don't do it this way?).
                DriverStationDisplay.writeRawAnsi(
                    String.format("\033[3;18H\033[38;5;%dm%5d\033[0m", 196, (int)(timeOut - Timer.getTimestamp())));

                Timer.delay(Seconds.of(1.));
            }

            logsSelector = EnumSet.noneOf(LogsSelector.class); // start fresh - nothing
            if (useConsoleSub.get()) logsSelector.add(LogsSelector.useConsole);
            if (useDatalogSub.get()) logsSelector.add(LogsSelector.useDataLog);

            examplesSelector = EnumSet.noneOf(ExamplesSelector.class); // start fresh - nothing
            if (useAchieveHueGoalSub.get()) examplesSelector.add(ExamplesSelector.useAchieveHueGoal);
            if (useDisjointParallelGroupSub.get()) examplesSelector.add(ExamplesSelector.useDisjointParallelGroup);
            if (useTriggerNextCommandSub.get()) examplesSelector.add(ExamplesSelector.useTriggerNextCommand);
            if (useHistoryFSMSub.get()) examplesSelector.add(ExamplesSelector.useHistoryFSM);
            if (useIntakeSub.get()) examplesSelector.add(ExamplesSelector.useIntake);
            if (useMooreLikeFSMSub.get()) examplesSelector.add(ExamplesSelector.useMooreLikeFSM);
            if (useAutonomousSignalSub.get()) examplesSelector.add(ExamplesSelector.useAutonomousSignal);
            if (useColorWheelSub.get()) examplesSelector.add(ExamplesSelector.useColorWheel);
            if (useEnableDisableSub.get()) examplesSelector.add(ExamplesSelector.useEnableDisable);
            if (useAnotherFSMtestSub.get()) examplesSelector.add(ExamplesSelector.useAnotherFSMtest);
        }
}
