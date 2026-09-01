package first.robot;

import org.wpilib.command3.Scheduler;
import org.wpilib.opmode.OpMode;
import org.wpilib.opmode.Utility;

import first.robot.Constants.Alerts;

@Utility(name = "Utility Examples", group = "Group 1")
public class UtilityExamples implements OpMode {

  @SuppressWarnings("unused")
  private final Robot robot;
  private RobotContainer m_robotContainer;

  /** The Robot instance is passed into the opmode via the constructor. */
  public UtilityExamples(Robot robot) {
    this.robot = robot;
    m_robotContainer = robot.getRobotContainer();
  }

  @Override
  public void disabledPeriodic() {
    /* Called on every robot period while the robot is disabled. */
  }

  @Override
  public void start() {
    /* Called once when the robot is enabled. */
    if (m_robotContainer.getM_anotherFSMtest().isPresent()) {
      Scheduler.getDefault().schedule(StateMachineTest.testFSM());
    }
    else {
      Alerts.STATE_MACHINE_TEST.set(true);
    }
  }

  @Override
  public void periodic() {
    /* Called on every robot period while the robot is enabled. */
  }

  @Override
  public void end() {
    /* Called when the robot is disabled (after previously being enabled). */
    // Exiting utility-enabled starts a sequence of two jobs using the internal button from
    // first job to second job
    Scheduler.getDefault().schedule(CommandsTriggers.getFirstCommandTriggersNextTest());
    // output:
    // utilityExit completed
    // first job running to trigger second job
    // second job ran
 }

  @Override
  public void close() {
    /* Called when the opmode is de-selected / no additional methods will be called. */
  }
}
