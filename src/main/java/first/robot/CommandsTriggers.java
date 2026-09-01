package first.robot;

import static org.wpilib.units.Units.Milliseconds;
import static org.wpilib.units.Units.Seconds;

import java.util.Optional;
import java.util.function.DoubleSupplier;

import org.wpilib.command3.Command;
import org.wpilib.command3.button.CommandXboxController;
import org.wpilib.command3.button.InternalButton;
import org.wpilib.hardware.led.LEDPattern;
import org.wpilib.math.filter.Debouncer.DebounceType;
import org.wpilib.system.Timer;
import org.wpilib.util.Color;

import first.robot.Constants.Alerts;
import first.robot.mechanisms.AchieveHueGoal;
import first.robot.mechanisms.HistoryFSM;
import first.robot.mechanisms.Intake;
import first.robot.mechanisms.RobotSignals;

@SuppressWarnings("resource")

public abstract class CommandsTriggers {
  private static CommandXboxController       m_operatorController;
  private static RobotSignals                m_robotSignals; // container and creator of all the LEDView mechanisms
  private static Optional<AchieveHueGoal>    m_achieveHueGoal;
  private static Optional<Boolean>           m_triggerNextCommand;
  private static Optional<HistoryFSM>        m_historyFSM;
  private static Optional<Intake>            m_intake;
  private static Optional<MooreLikeFSM>      m_lightBar;
  private static Optional<Boolean>           m_UseAutonomousSignal;
  private static Optional<Boolean>           m_UseColorWheel;

  /**
   * Commands and Triggers that require more than one mechanism should be defined here.
   * <p>Commands and Triggers that require only one mechanism should be defined in their mechanism class.
   */
  private CommandsTriggers() {}

  public static void create(RobotContainer robotContainer)
  {
    m_operatorController = robotContainer.getM_operatorController();
    m_robotSignals = robotContainer.getM_robotSignals();
    m_achieveHueGoal = robotContainer.getM_achieveHueGoal();
    m_triggerNextCommand = robotContainer.getM_triggerNextCommand();
    m_historyFSM = robotContainer.getM_historyFSM();
    m_intake = robotContainer.getM_intake();
    m_lightBar = robotContainer.getM_mooreLikeFSM();
    m_UseAutonomousSignal = robotContainer.getM_autonomousSignal();
    m_UseColorWheel = robotContainer.getM_useColorWheel();

    configureGameControllersBindings();
  }

  /**
   * Configure Commands
   */


  // Configuration of the trigger's action decorator must be executed somewhere. If the next
  // command in the action does not reference the trigger then it can be configured here.
  // If the action next command references the trigger, then set it in some initializing method
  // because of circular reference - trigger would reference the command and the command would
  // reference the trigger.
  // Even if the second command doesn't reference the trigger, an initializing method may be
  // advantageous to include logic not to set the trigger action, if some option is set not to use
  // it as in this example.

  // configure the action decorator must be executed somewhere else and not here because of circular
  // reference - trigger would reference the command and the command would reference the trigger
  private static InternalButton firstJobTriggersSecond;
  
  private static final Command firstJob = Command.noRequirements(coroutine ->
      {
        firstJobTriggersSecond.setPressed(false); // add this - assuming not running a .whileTrue()
        System.out.println("first job running to trigger second job");
        firstJobTriggersSecond.setPressed(true); // add this - assuming next running with some "...True()"
      }).named("firstJob");

  private static final Command secondJob = Command.noRequirements(coroutine ->
      System.out.println("second job ran")).named("secondJob");

/**
   * Setup trigger and Get first command that triggers next command
   * 
   * @return Command to be scheduled to run trigger next command test
   */
  public static Command getFirstCommandTriggersNextTest() {
    if (m_triggerNextCommand.isPresent())
    {
      firstJobTriggersSecond = new InternalButton();
      firstJobTriggersSecond.onTrue(secondJob);
      return firstJob;      
    }
    else
    {
      return Command.noRequirements(coroutine ->
          Alerts.m_alertTriggerNextCommand.set(true)).named("Alert-getFirstCommandTriggersNextTest");
    }
  }

  /**
   * Create a command to start the Moore FSM StateMachine Light Bar
   *
   * @return command that can be scheduled to start the Light Bar
   */
  public static Command lightBar() {
    if (m_lightBar.isPresent()) {
        // statements before the return are run early at initialization time
      return
          m_lightBar.get().createLightBar();
    }
    else {
      return Command.noRequirements(coroutine ->
          Alerts.m_alertMooreFSMLightBar.set(true)).named("Alert-lightBar");
    }
  }

/**
   * Create a command to signal Autonomous mode
   *
   * <p>Example of setting two signals by contrived example of composed commands
   *
   * @return LED pattern signal for autonomous mode
   */
  public static Command setAutonomousSignal() {
    if (m_UseAutonomousSignal.isPresent()) {
      // statements before the return are run early at initialization time

      LEDPattern autoTopSignal =
            LEDPattern.solid(new Color(0.1, 0.2, 0.2))
            .blend(LEDPattern.solid(new Color(0.7, 0.2, 0.2)).blink(Seconds.of(0.1)));
            
      LEDPattern autoMainSignal = LEDPattern.solid(new Color(0.3, 1.0, 0.3));

      return
        Command.parallel(
                m_robotSignals.m_top.get().setSignal(autoTopSignal, "auto signal"),

                Command.waitFor(Seconds.of(5.)).named("auto wait") // timeout ends but the group continues and
                    .andThen(m_robotSignals.m_main.get().setSignal(autoMainSignal, "")).withAutomaticName()).withAutomaticName();
    }
    else {
      return Command.noRequirements(coroutine ->
           Alerts.m_alertAutonomousSignal.set(true)).named("Alert-AutonomousSignal");
    }
  }
 
  public static Command setAutonomousSignalOff () {
    if (m_UseAutonomousSignal.isPresent()) {
      return Command.noRequirements(coroutine ->
        {
          coroutine.fork(m_robotSignals.m_top.get().setSignal(RobotSignals.LEDView.OFF, "auto top off"));
          coroutine.fork(m_robotSignals.m_main.get().setSignal(RobotSignals.LEDView.OFF, "auto main off"));
        }).named("auto all LEDs off");
      }
    else {
      return Command.noRequirements(coroutine -> {})
            .named("no autonomous");
    }
  }

  /**
   * configure driver and operator controllers' buttons
   * 
   * <p>Note that triggering a command from a button press a second time doesn't start the command
   * again if the scheduler thinks the command is already scheduled or still running. Triggering
   * again from a different button works okay.
   */
  private static void configureGameControllersBindings() {

    /**
     * Use operator "B" button for a fake indicator game piece is acquired
     */
    if (m_intake.isPresent()) {
      m_operatorController.b().whileTrue(m_intake.get().gamePieceIsAcquired());
    }
    else {
      Alerts.m_intake.set(true);
    }

    /**
     * Start History FSM Control with the operator "Y" button
     */
    if (m_historyFSM.isPresent())
    {
      final var yButtonDebounceTime = Milliseconds.of(40.0);
      m_operatorController.y().debounce(yButtonDebounceTime).onTrue(m_historyFSM.get().newColor());      
    }
    else {
      Alerts.m_alertHistory.set(true);
    }

    /**
     * Start a color wheel display with the operator "X" button
     */
    if (m_UseColorWheel.isPresent())
    {  
      // produce a LED color pattern based on the timer current seconds of the minute
      final RobotSignals.LEDPatternSupplier colorWheel =
        () ->
          LEDPattern.solid(
              Color.fromHSV(
                  (int) (Timer.getTimestamp() % 60.0 /* one of the 60 seconds of the minute */)
                      * 3 /* scale seconds to 180 hues per color wheel */,
                  200,
                  200));

      final var xButtonDebounceTime = Milliseconds.of(30.0);
      m_operatorController
          .x()
          .debounce(xButtonDebounceTime, DebounceType.BOTH)
          .onTrue(m_robotSignals.m_top.get().setSignal(colorWheel, "colorwheel"));
    }
    else {
            Alerts.m_alertColorWheel.set(true);
    }

    /**
      * Goal setting demo control
      *
      * <p>The PID controller is not running initially until a setpoint is set by moving the operator
      * right trigger axis past the threshold at which time a command runs to achieve that goal.
      */
    if (m_achieveHueGoal.isPresent())
      {
        var triggerHueGoalDeadBand = 0.05; //triggers if past a small threshold (scale of 0 to 1)
        m_operatorController.rightTrigger(triggerHueGoalDeadBand)
            .onTrue(
                m_achieveHueGoal.get().achieveHue());

        // immediately stop controller
        m_operatorController.a()
            .onTrue(m_achieveHueGoal.get().cancel());
      }
    else {
      Alerts.m_alertAchieveHueGoal.set(true);
    }
  }

  /**
   * Supplier of the setpoint for the {@link AchieveHueGoal}
   * 
   * @return setpoint supplier
   */
  static DoubleSupplier hueSetpoint() {
    // scale joystick's 0 to 1 to computer color wheel hue 0 to 180
    return () -> m_operatorController.getRightTrigger()*180.0;
  }
}
