package first.robot;

import static first.robot.Config.CommandLoggingSettings.logsSelector;
import static first.robot.Config.Examples.examplesSelector;

import java.lang.invoke.MethodHandles;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.wpilib.command3.button.CommandXboxController;
import org.wpilib.util.Color;

import first.robot.Config.Examples;
import first.robot.Constants.Alerts;
import first.robot.Constants.LightBar;
import first.robot.mechanisms.AchieveHueGoal;
import first.robot.mechanisms.HistoryFSM;
import first.robot.mechanisms.Intake;
import first.robot.mechanisms.RobotSignals;

public class RobotContainer {
  
  public enum ExamplesSelector // All possible examples from which to choose
  {
    useAchieveHueGoal, useDisjointParallelGroup, useTriggerNextCommand, useHistoryFSM, useIntake,
    useMooreLikeFSM, useAutonomousSignal, useColorWheel, useEnableDisable, useAnotherFSMtest
  };

  @SuppressWarnings("unused")
  private CommandSchedulerLog schedulerLog = new CommandSchedulerLog(logsSelector);

  /**
   * Constructor creates most of the mechanisms and operator controller bindings
   */
  public RobotContainer(BiConsumer<Runnable, Double> RobotAddPeriodic) {

    Examples.config();

    if (!EnumSet.complementOf(examplesSelector).isEmpty()) {
      Alerts.NOT_ALL_EXAMPLES.set(true);
    }

    m_robotSignals = new RobotSignals(examplesSelector); // container and creator of all the LEDView mechanisms

    // instantiate optional classes and mechanisms

    // EnableDisable instantiation logic only in its LEDView so check existence here.
    if (!examplesSelector.contains(ExamplesSelector.useEnableDisable)) {
      Alerts.ENABLE_DISABLE.set(true);    
    }
    m_achieveHueGoal = examplesSelector.contains(ExamplesSelector.useAchieveHueGoal) ?
        Optional.of(new AchieveHueGoal(m_robotSignals.m_achieveHueGoal.get(), RobotAddPeriodic, CommandsTriggers.hueSetpoint())) :
        Optional.empty();
    m_disjointParallelGroup = examplesSelector.contains(ExamplesSelector.useDisjointParallelGroup) ?
        Optional.of(true) :
        Optional.empty();
    m_triggerNextCommand = examplesSelector.contains(ExamplesSelector.useTriggerNextCommand) ?
        Optional.of(true) :
        Optional.empty();
    m_historyFSM = examplesSelector.contains(ExamplesSelector.useHistoryFSM) ?
        Optional.of(new HistoryFSM(m_robotSignals.m_historyDemo.get(), RobotAddPeriodic)) :
        Optional.empty();
    m_intake = examplesSelector.contains(ExamplesSelector.useIntake) ?
        Optional.of(new Intake(m_robotSignals.m_main.get())) :
        Optional.empty();
    m_mooreLikeFSM = examplesSelector.contains(ExamplesSelector.useMooreLikeFSM) ?
        Optional.of(new MooreLikeFSM(m_robotSignals.m_knightRider.get(), LightBar.PERIOD, Color.RED)) :
        Optional.empty();
    m_autonomousSignal = examplesSelector.contains(ExamplesSelector.useAutonomousSignal) ?
        Optional.of(true) :
        Optional.empty();
    m_colorWheel = examplesSelector.contains(ExamplesSelector.useColorWheel) ?
        Optional.of(true) :
        Optional.empty();
    m_enableDisable = examplesSelector.contains(ExamplesSelector.useEnableDisable) ?
        Optional.of(true) :
        Optional.empty();
    m_anotherFSMtest = examplesSelector.contains(ExamplesSelector.useAnotherFSMtest) ?
        Optional.of(true ) :
        Optional.empty();
  }

  // required classes and mechanisms

  private final CommandXboxController m_operatorController = new CommandXboxController(Constants.OPERATOR_CONTROLLER_PORT);
  public CommandXboxController getM_operatorController() {
    return m_operatorController;
  }

  private final RobotSignals m_robotSignals; // container and creator of all the LEDView mechanisms
  public RobotSignals getM_robotSignals() {
    return m_robotSignals;
  }
  
  // optional classes and mechanisms

  private Optional<AchieveHueGoal> m_achieveHueGoal;
  public Optional<AchieveHueGoal> getM_achieveHueGoal() {
    return m_achieveHueGoal;
  }

  private Optional<Boolean> m_disjointParallelGroup;
  public Optional<Boolean> getM_disjointParallelGroup() {
    return m_disjointParallelGroup;
  }

  private Optional<Boolean> m_triggerNextCommand;
  public Optional<Boolean> getM_triggerNextCommand() {
    return m_triggerNextCommand;
  }

  private Optional<HistoryFSM> m_historyFSM;
  public Optional<HistoryFSM> getM_historyFSM() {
    return m_historyFSM;
  }

  private Optional<Intake> m_intake;
  public Optional<Intake> getM_intake() {
    return m_intake;
  }

  private Optional<MooreLikeFSM> m_mooreLikeFSM;
  public Optional<MooreLikeFSM> getM_mooreLikeFSM() {
    return m_mooreLikeFSM;
  }

  private Optional<Boolean> m_autonomousSignal;
  public Optional<Boolean> getM_autonomousSignal() {
    return m_autonomousSignal;
  }

  private Optional<Boolean> m_colorWheel;
  public Optional<Boolean> getM_useColorWheel() {
    return m_colorWheel;
  }
  
  private Optional<Boolean> m_enableDisable;
  public Optional<Boolean> getM_useEnableDisable() {
    return m_enableDisable;
  }

  private Optional<Boolean> m_anotherFSMtest;
  public Optional<Boolean> getM_anotherFSMtest() {
    return m_anotherFSMtest;
  }

  private static final String fullClassName = MethodHandles.lookup().lookupClass().getCanonicalName();
  static
  {
    System.out.println("Loading: " + fullClassName);
    System.out.println("WPILib version " + org.wpilib.system.WPILibVersion.Version + " (Java)");
  }

  /**
   * There are a variety of techniques to run I/O methods periodically and the example implemented
   * below in this code is a very simplistic start of a good possibility.
   * 
   * It demonstrates running before the scheduler loop to get a consistent set of sensor inputs.
   * After the scheduler loop completes all periodic outputs from mechanisms are run such as data
   * logging and dashboards. (There is additional related discussion of periodic running in AchieveHueGoal.)
   *
   * There are clever ways to register classes say using a common "MechanismTeam" class or
   * interface with a "register" method so they are automatically included in a list that can
   * easily be accessed with a loop. But this example is simplistic with no registration and no
   * loop - remember to type them in here and in any class that has multiple mechanisms such as the 
   * example "GroupDisjointTest".
   * 
   * Security to prevent unauthorized running of periodic methods could be implemented in a variety
   * of ways but that error doesn't seem to happen so these examples have all "public" periodic
   * methods. Don't run them except in the designated places in the code.
   */

  /**
   * Run before commands and triggers from the Robot.periodic()
   *
   * <p>Run periodically before commands are run to read sensors to create a consistent set of inputs.
   */
  public void runBeforeTheCommands() {} // not used for these several independent examples
      // some examples have used addPeriodic() for display and logging that could have been put here
}
