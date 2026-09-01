package first.robot.mechanisms;

import static org.wpilib.units.Units.Seconds;

import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.hardware.led.LEDPattern;
import org.wpilib.util.Color;

import first.robot.mechanisms.RobotSignals.LEDViewMain;
import first.robot.mechanisms.RobotSignals.LEDViewTop;

public class AutoStub implements Mechanism {   
    LEDViewTop m_top;
    LEDViewMain m_main;

    public AutoStub(LEDViewTop m_top, LEDViewMain m_main) {
        this.m_top = m_top;
        this.m_main = m_main;
        }

    /**
     * Create a command to signal Autonomous mode
     *
     * <p>Example of setting two signals by contrived example of composed commands
     *
     * @return LED pattern signal for autonomous mode
     */
    public Command setAutonomousSignalOn() {
        // statements before the return are run early at initialization time

        LEDPattern autoTopSignal =
                LEDPattern.solid(new Color(0.1, 0.2, 0.2))
                .blend(LEDPattern.solid(new Color(0.7, 0.2, 0.2)).blink(Seconds.of(0.1)));
                
        LEDPattern autoMainSignal = LEDPattern.solid(new Color(0.3, 1.0, 0.3));

        return
            Command.parallel(
                    m_top.setSignalCmd(autoTopSignal, "auto top signal"),
                    Command.waitFor(Seconds.of(5.)).named("auto wait") // timeout ends but the group continues and
                        .andThen(m_main.setSignalCmd(autoMainSignal, "auto main signal"))
                        .withAutomaticName())
            .withAutomaticName();
        }
 
  public Command setAutonomousSignalOff() {
      return Command.noRequirements(coroutine ->
        {
          m_top.setSignal(LEDPattern.OFF);
          m_main.setSignal(LEDPattern.OFF);
        }).named("auto top and main LEDs off");
  }
}
