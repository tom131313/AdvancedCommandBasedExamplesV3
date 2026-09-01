package first.robot;

import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;
import org.wpilib.driverstation.RobotState;
import org.wpilib.epilogue.Logged;
import org.wpilib.epilogue.NotLogged;
import org.wpilib.framework.OpModeRobot;
import org.wpilib.opmode.Autonomous;
import org.wpilib.opmode.OpMode;

@Logged
@Autonomous(name = "Auto Examples", group = "Group 1")
public class AutoExamples implements OpMode {

  private Command m_autonomousSignal;
  @NotLogged // prevents recursion on robot logging
  private final Robot robot;
  @SuppressWarnings("unused")
  private boolean runningAutoExamples = false; // all instances start with false; not needed but something useful to display

  /** The Robot instance is passed into the opmode via the constructor().
   * <p>The constructor() is run every time this OpMode is entered as disabled such:
   * <p>select this OpMode which starts as disabled and constructor runs
   * <p>select Enable and start() and then periodic() run
   * <p>select disable and end() runs and then close() runs then constructor() runs.
   * <p>If enabled and selects a different OpMode then the new OpMode constructor runs
   */
  public AutoExamples(Robot robot) {
    this.robot = robot; // always do this to keep the compiler quiet
    // check must match the @Autonomous explicit or implicit name; bailout if not the OpMode instance
    if ( ! RobotState.getOpMode().equals("Auto Examples")) return; // not an OpMode selection so bailout
    // only OpMode selection instance makes it here so do whatever we need to run the class
    robot.autoExamplesOpMode = this;
    runningAutoExamples = true; // not needed but something useful to display
  }

 /**
   * This function is called periodically while the opmode is selected and the robot is disabled.
   * Code that should only run once when the opmode is selected should go in the opmode constructor.
   */
  public void disabledPeriodic() {}

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
  public void periodic() {}

  /**
   * This function is called asynchronously when the robot disables or switches opmodes while this
   * opmode is enabled. Implementations should stop blocking work promptly.
   */
  @Override
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
     robot.autoExamplesOpMode = robot.autoExamplesClosed;
     runningAutoExamples = false; // not needed but something useful to display
     }
}
