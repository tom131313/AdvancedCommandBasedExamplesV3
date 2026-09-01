package first.robot;

import org.wpilib.opmode.OpMode;
import org.wpilib.opmode.Teleop;

import first.robot.Constants.Alerts;
import first.robot.mechanisms.DisjointParallelGroup;

@Teleop(name = "Teleop Examples", group = "Group 1")
public class TeleopExamples implements OpMode {

    @SuppressWarnings("unused")
    private final Robot robot;
    RobotContainer m_robotContainer;

  /** The Robot instance is passed into the opmode via the constructor. */
  public TeleopExamples(Robot robot) {
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
    if (m_robotContainer.getM_disjointParallelGroup().isPresent()) {
      new DisjointParallelGroup();
    }
    else {
      Alerts.DISJOINTED_GROUP.set(true);
    }
  }

  @Override
  public void periodic() {
    /* Called on every robot period while the robot is enabled. */
  }

  @Override
  public void end() {
    /* Called when the robot is disabled (after previously being enabled). */
 }

  @Override
  public void close() {
    /* Called when the opmode is de-selected / no additional methods will be called. */
  }
}
