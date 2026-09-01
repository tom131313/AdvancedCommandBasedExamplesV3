package first.robot.mechanisms;

/*
 * Example mechanism that acquires a game piece (simulated fake boolean by pressing "B" button). A
 * signal is displayed to indicate the status of the Intake (acquired game piece or not).
 */

import static org.wpilib.units.Units.Seconds;
import static org.wpilib.command3.Command.parallel;

import org.wpilib.hardware.led.LEDPattern;
import org.wpilib.util.Color;

import first.robot.mechanisms.RobotSignals.LEDView;

import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;

public class Intake implements Mechanism {
  private final LEDView m_robotSignals;

  /**
   * @param robotSignals Signal Mechanism
   */
  public Intake(LEDView robotSignals) {
    m_robotSignals = robotSignals;
  }

  /**
   * Signals that a game piece has been acquired
   * <p>Runs until something indicates game piece no longer acquired
   * 
   * @return command to set the signal indicating game piece acquired
   */
  public Command gamePieceIsAcquired() {
    LEDPattern gamePieceAcquiredSignal = LEDPattern.solid(Color.MAGENTA).blink(Seconds.of(0.2));
    return
      parallel(
          // this command locks the robotSignals Mechanism
          m_robotSignals.setSignal(gamePieceAcquiredSignal, "MainGamePieceAcquiredSignal"),
          
          // for an example this command locks the Intake Mechanism for the group
          idle()
      ).named("Game Piece Acquired Parallel");
  }
}
