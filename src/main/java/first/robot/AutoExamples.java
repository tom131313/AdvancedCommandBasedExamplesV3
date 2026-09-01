package first.robot;

import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;
import org.wpilib.epilogue.Epilogue;
import org.wpilib.epilogue.Logged;
import org.wpilib.framework.OpModeRobot;
import org.wpilib.opmode.Autonomous;
import org.wpilib.opmode.OpMode;
@Logged
@Autonomous(name = "Auto Examples", group = "Group 1")
public class AutoExamples implements OpMode {
  private Command m_autonomousSignal;
  @SuppressWarnings("unused")
  private int anInt = 0; // test variable for testing logged
  @SuppressWarnings("unused")
  private final Robot robot;

  /** The Robot instance is passed into the opmode via the constructor.
   * <p>The constructor is run every time this OpMode is entered as disabled such:
   * <p>select this OpMode which starts as disabled and constructor runs
   * <p>select Enable and periodic runs
   * <p>select disable and end runs, close runs, constructor runs to be ready to enable auto again.
   * <p>If enabled and selects a different OpMode then the new OpMode constructor runs
   */
  public AutoExamples(Robot robot) {
    this.robot = robot;
  }

 /**
   * This function is called periodically while the opmode is selected and the robot is disabled.
   * Code that should only run once when the opmode is selected should go in the opmode constructor.
   */
  public void disabledPeriodic() {
    anInt--;
    Epilogue.first_robot_AutoExamplesLogger
    .update(Epilogue.getConfig()
    .table
    .getTable(this.getClass().getName()), this);
  }

  /** Called once when this opmode transitions to enabled. */
  public void start() {
    Scheduler.getDefault().cancelAll(); // start auto clean - defaults are also cancelled but restart immediately
    m_autonomousSignal = CommandsTriggers.setAutonomousSignal();
    Scheduler.getDefault().schedule(m_autonomousSignal);
  }

  /**
   * This function is called periodically while the opmode is enabled at the rate returned by {@link
   * OpModeRobot#getPeriod()}.
   * 
   * This method runs periodically, using the same period as the Robot instance.
   *
   * Additional periodic methods may be configured with addPeriodic(),
   * which can have periods that differ from the main Robot instance.
   */
  @Override
  public void periodic() {
    anInt++;
    // example of how to activate @Logged for an implements OpMode; need @Logged in Robot, too.
    Epilogue.first_robot_AutoExamplesLogger
    .update(Epilogue.getConfig()
    .table
    .getTable(this.getClass().getName()), this);
  }

  /**
   * This function is called asynchronously when the robot disables or switches opmodes while this
   * opmode is enabled. Implementations should stop blocking work promptly.
   */
  public void end() {
    // Commands running from another mode haven't been cancelled directly except the one below.
    if (m_autonomousSignal != null) { // check null in case not initialized in auto mode
      Scheduler.getDefault().cancel(m_autonomousSignal); // cancel in case still running
    }

    // This cancels the auto command in case it's still running.
    // It cancels because of the overlapping requirements of the LEDViews of m_autonomousSignal and
    // setAutonomousSignalOff.
    Scheduler.getDefault().schedule(CommandsTriggers.setAutonomousSignalOff());
  }

  /**
   * This function is called when the opmode is no longer selected on the DS or after an enabled run
   * ends. The object will not be reused after this is called.
   */
  @Override
  public void close() {
  }
}
