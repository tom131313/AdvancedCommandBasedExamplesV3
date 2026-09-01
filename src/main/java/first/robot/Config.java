package first.robot;

import java.util.EnumSet;

import first.robot.CommandSchedulerLog.LogsSelector;
import first.robot.RobotContainer.ExamplesSelector;
import first.robot.mechanisms.HistoryFSM;

/**
 * This defines the user settable configuration
 * <p>
 * Add settings to the appropriate interface to limit visibility of those parameters
 * <p>
 * If a parameter is shared by more than one interface, then define a parameter in common in the
 * high level Config interface and parameters that reference that common parameter to all the
 * appropriate interfaces. (That does reveal that parameter, though, in {@link Config})
 * <p>
 * It is advised to statically import this class (or one of its inner classes) wherever the
 * variables are needed, to reduce verbosity.
 * <p>
 * There is some user selectable debug code scattered around this project such as a commented out
 * reference to {@link HistoryFSM#verificationPrint}. Also, commented out debug code in
 * {@link RobotContainer#configureCommandLogs} and other places.
 */
public interface Config {

    /**
     * Choices Of Destinations of Command Logging
     */    
    public interface CommandLoggingSettings {

        // public static EnumSet<CommandStageSelector> commandStageSelector =
        //     EnumSet.allOf(CommandStageSelector.class);
        
        // OR PICK WHICH INDIVIDUALS TO USE comment out the unwanted ones
        public static EnumSet<LogsSelector> logsSelector =
            EnumSet.of(
                //   LogsSelector.useConsole,
                  LogsSelector.useDataLog
            );

        // OR PICK NONE
        // public static EnumSet<LogsSelector> logsSelectors = EnumSet.noneOf(LogsSelector.class);
    }

    /**
     * Choices of Examples to Run
     */
    public interface Examples {

        // ALL OF THEM or comment out to select individuals
        public static EnumSet<ExamplesSelector> examplesSelector =
            EnumSet.allOf(ExamplesSelector.class);

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
        // public static EnumSet<ExamplesSelector> examplesSelector = EnumSet.noneOf(ExamplesSelector.class);
    }
}
