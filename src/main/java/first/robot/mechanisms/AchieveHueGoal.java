package first.robot.mechanisms;

/*
 * Example of a mechanism that uses a (PID) controller to achieve a goal (setpoint).
 * 
 * There are several possible ways to implement this function.
 * 
 * Some teams use the structure of a perpetually running mechanism or other class that is the
 * controller that periodically updates. No commands are used to calculate or schedule
 * calculations. It responds to commands only to accept the goal (setpoint). There are a few ways
 * the periodic update can be scheduled:
 *
 *  use the Scheduler.addPeriodic() or Robot.addPeriodic() methods (Robot allows update faster or
 *  slower than the normal loop),
 * 
 *  run off the Robot.periodic() method say with team supplied runBeforeCommands() methods .
 * 
 * Those goal-setting commands or other schemes needed in the above controllers may or may not be
 * protected by use of their own mechanism.
 * 
 * There are few ways to run a controller scheduled from the Command Loop.
 * 
 * The default command will run perpetually if there is no other overriding command issued. That
 * default command can encompass the controller and manage the goal setting (setpoint). The default
 * command has an advantage of automatically restarting if interrupted for any reason.
 * 
 * A normal command can run perpetually and encompass the controller and manage the goal (setpoint).
 * Normal commands do not restart if interrupted (unless some periodic monitoring process is added
 * such as a default command or Robot.periodic() method to restart the interrupted command).
 * 
 * The best alternative to all the above schemes usually is a controller is run by command when it
 * is needed.
 * 
 * A suggestion by CD @Amicus1 for those against using commands except to set the goal:
 * "If this is a verbosity of code issue, I suggest writing the logic as a private mechanism method
 * and exposing it as a command factory."
 * 
 * That is the bulk of the controller does not have to be coded "inline" making a very long command.
 * The logic of the controller doesn't even have to be within a command or mechanism but can reside
 * in its own class. The example of using a PID controller in this example mechanism works like
 * that. The WPILib PIDController is an essentially independent class with all the PID logic. It is
 * used by a command that accepts a setpoint and calls the appropriate calculation and setpoint
 * methods. The command and thus the calculations run continuously to the goal as they are needed.
 * 
 * In this example a WPILib PID controller class is used on demand of a command to converge to the
 * selected color. The iterative command loop paces the calculations.
 */

import static org.wpilib.units.Units.Seconds;

import java.util.function.DoubleSupplier;

import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.hardware.led.LEDPattern;
import org.wpilib.math.controller.PIDController;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.tunable.Tunables;
import org.wpilib.util.Color;

import first.robot.Constants.HueGoal;
import first.robot.TriConsumer;
import first.robot.mechanisms.RobotSignals.LEDView;

/**
 * Use a PID controller to achieve (slowly by over-damped kP gain) a color hue goal set by joystick
 * right trigger axis and display progress toward goal on the LEDs.
 * 
 * <p>The current value of the controller is indicated by the first few LEDs and the current setpoint
 * shows in the last couple of LEDs.
 */
public class AchieveHueGoal implements Mechanism {

  private final PIDController m_hueController;
  private double m_currentStateHue; // PID input state then updated to new state
  private LEDPattern m_notSeekingGoalSignal = LEDPattern.solid(Color.GRAY); // controller off signal
  private final LEDView m_robotSignals; // where the output is displayed
  private final static double minimumHue = 0.0;
  private final static double maximumHue = 180.0;
  private DoubleSupplier hueSetpoint;
  /**
   * Constructor
   *
   * @param robotSignals LED mechanism used as output by this mechanism
   */
  public AchieveHueGoal(LEDView robotSignals, TriConsumer<Runnable, Double, Double> RobotAddPeriodic, DoubleSupplier hueSetpoint) {
    m_robotSignals = robotSignals;
    this.hueSetpoint = hueSetpoint;
    /**
     *  PID initialization.
     * 
     *  The PID controller is ready but not running initially until a command is issued with a
     *  setpoint.
     * 
     *  LEDView mechanism will display continuously in the "background" as patterns change.
     */
    m_hueController = new PIDController(HueGoal.kP, HueGoal.kI, HueGoal.kD);
    Tunables.publish("Hue PID Controller", m_hueController); // allows tuning inputs from dashboard
    m_hueController.setTolerance(HueGoal.tolerance);
    m_robotSignals.setSignal(m_notSeekingGoalSignal);
     // high speed PID calculation good for low momentum or high speed processes
     // runs forever as there is no way to stop an addPeriodic with the WPILib robot design.
     // Do your own "background" running similar to the addPeriodic is possible for more flexibility
     // on when to run but that is rarely of use.
    RobotAddPeriodic.accept(controllerCalculation(), HueGoal.loopSpeed.in(Seconds), 0.);
  }

  /**
   * Breakout the PID calculation from the command loop so the PID can run faster in the addPeriodic.
   * The command loop is doing the display of the calculation results and there is no need for that
   * to be faster.
   * 
   * <p>The goal (setpoint) is dynamically supplied hue 0 to 180 (computer version of a color wheel)
   * @return the PID calculation to run to and hold the setpoint
   */
  private final Runnable controllerCalculation() {
    return
    () ->
    m_currentStateHue = // compute the new current state - stay within bounds
      Math.clamp(
        m_currentStateHue +
            m_hueController.calculate(m_currentStateHue, hueSetpoint.getAsDouble()),
        minimumHue,
        maximumHue);
  }

  /**
   * Display the operation of the PID controller.
   *
   * <p>Runs until the goal has been achieved within the tolerance at which time the end is
   * indicated and the controller/command stops. (But the PID controller would still be running but
   * its results are being ignored if this command is not running.)
   * 
   * @return command used to set and achieve the goal
   */
  public Command achieveHue() {
    return
        run(coroutine ->
            {
              LEDPattern m_currentStateSignal = m_notSeekingGoalSignal; // soon but not this instant
              reset();

              // run to the setpoint displaying state progress as it runs
              while(!m_hueController.atSetpoint())
                {
                  Telemetry.log("Hue PID Controller", m_hueController); // log outputs
                  m_currentStateSignal = displayPID(
                      Color.fromHSV((int) m_currentStateHue, 200, 200),
                      Color.fromHSV((int) hueSetpoint.getAsDouble(), 200, 200));
                  m_robotSignals.setSignal(m_currentStateSignal);
                  coroutine.yield();
                }
              // PID controller effectively stopped; momentum stable or not
              // command still running for the indicator signal and cleanup

              // set LED pattern to blink to show controller stopped at setpoint                
              m_currentStateSignal = m_currentStateSignal.blink(Seconds.of(0.1));
              m_robotSignals.setSignal(m_currentStateSignal);
              coroutine.wait(Seconds.of(2.)); // the LEDs blink awhile by the LED display
              reset(); // turn off everything
            }
            ).whenCanceled(() -> reset()).named("PID");
  }
          // if the controller needs to keep running, put it in parallel and check atSetpoint to
          // trigger subsequent commands

  /**
   * Reset or stop the controller
   * 
   * <p>Used initially to assure known stopped state. Used finally to stop anything that needs to.
   */
  public void reset() {
    // also stop other devices as needed but not needed in this example
    m_hueController.reset();
    m_currentStateHue = 0.; // also considered the initial and previous state
    m_robotSignals.setSignal(m_notSeekingGoalSignal);
  }

  /**
   * Immediately stop the controller command.
   * 
   * <p>Command works merely by its existence interrupts and cancels the PID command running for this
   * Mechanism.
   * 
   * <p>The default command, if any, for the Mechanism (and there is not) would run after this brief
   * command. (Not to be confused with any default command for the LEDView which may run the
   * display (and there is not).)
   * 
   * @return Command to cancel and stop the controller while it's running as a command
   */
  public final Command cancel() {
    return
      run(coroutine -> {}).named("stop PID");
  }

  /**
   * Color the first few LEDs with current position and last couple with setpoint
   * 
   * <p>Could have made the last couple of LEDs a new view but that has a lot of overhead, too. And
   * it's more fun to try another view reader-writer with greater flexibility to move LEDs around.
   * 
   * @param currentPoint
   * @param setPoint
   * @return Pattern to apply to the LED view
   */
  private static final LEDPattern displayPID(Color currentPoint, Color setPoint) {
    return (reader, writer) -> {
      int bufLen = reader.getLength();
      int setpointLEDs = 2; // number of right-most LEDS that display the setpoint

      for (int led = 0; led < bufLen-setpointLEDs; led++) {
          writer.setLED(led, currentPoint);
      }
      for (int led = bufLen-setpointLEDs; led < bufLen; led++) {
          writer.setLED(led, setPoint);
      }
    };
  }
}

/** The above example code of a PID controller usage follows #1 of @Oblarg post on Chief Delphi
 * https://www.chiefdelphi.com/t/command-based-help/446143/19
 * <pre><code>
 * // Example of two ways to create a PID controller Command as factory in a mechanism
 * // (Don't use the WPILib PIDControllerCommand; create your own with a factory)
 *
 * // in mechanism scope
 * // we need a motor and sensor for feedback
 * private final MotorController motor = new FooMotor();
 * private final Encoder encoder = new Encoder(...);
 *
 * // Example 1 we could have a PIDController as a field in the mechanism itself...
 * private final PIDController controller = new PIDController(...);
 * // this command captures the mechanism's PIDController, like PIDMechanism
 * public Command moveToPosition(double position) {
 *   return runOnce(controller::reset)
 *         .andThen(run(() -> {
 *                   motor.set(controller.calculate(
 *                   encoder.getPosition(),
 *                   position
 *                   ));
 *         }).finallyDo(motor::stop);
 * }
 *
 * // Example 2 if we don't want to persist the controller in the mechanism after the command ends...
 * // this command captures its *own* controller, like PIDCommand
 * public Command moveToPosition(double position) {
 *   PIDController controller = new PIDController(...);
 *  // we don't have to reset a fresh controller
 *   return run(() -> {
 *                   motor.set(controller.calculate(
 *                   encoder.getPosition(),
 *                   position
 *                   ));
 *         }).finallyDo(motor::stop);
 * }
 * </code></pre>
 */