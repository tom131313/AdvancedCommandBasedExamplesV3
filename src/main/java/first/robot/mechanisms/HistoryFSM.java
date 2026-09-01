package first.robot.mechanisms;

/**
 * Display a random color signal periodically and don't reuse the same color too soon (if reasonably
 * possible).
 *
 * <p>This is a FSM that depends on the current state, the transition event trigger, and the
 * historical previous states.
 *
 * <p>This demonstrates an infinite loop command. This demonstrates using persistent data to
 * periodically refresh outputs past the completion of this command. This demonstrates running a
 * command periodically based on the past time history.
 */

import static org.wpilib.units.Units.Milliseconds;
import static org.wpilib.units.Units.Seconds;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.hardware.led.LEDPattern;
import org.wpilib.units.measure.Time;
import org.wpilib.util.Color;

import first.robot.TriConsumer;
import first.robot.mechanisms.RobotSignals.LEDView;

public class HistoryFSM implements Mechanism {
  private final LEDView m_robotSignals;
  private Random m_rand = new Random();
  public LEDPattern m_persistentPatternDemo;


  // Make the history in as narrow of scope as possible. For this simple example the scope is
  // perfectly narrow (this instance scope) since the history doesn't depend on any values
  // from other mechanisms.

  // Time data is saved for how long a color is to persist in the display.

  private static final int m_computerColorWheel = 180; // max count of hues numbered 0 to 179
  // Add a color [hue number as list index] and last time used to the history so that color isn't
  // used again during a lockout period.
  // List of the last times of all the colors (hues) so try not to repeat for a long time so repeats
  // are rare
  private List<Time> m_lastTimeHistoryOfColors = new ArrayList<>(m_computerColorWheel);

  private static final Time m_beginningOfTime = Seconds.of(0.);
  private static final Time m_changeColorPeriod = Seconds.of(2.); // display color for this long
  private static final Time m_colorLockoutPeriod = Seconds.of(20.); // try not to reuse a color for this long

  public HistoryFSM(LEDView robotSignals, TriConsumer<Runnable, Double, Double> RobotAddPeriodic) {

    m_robotSignals = robotSignals;

    // RobotAddPeriodic.accept(this::verificationPrint, 5., .019); // debug print rarely 'cuz lots of output
  }

  /** Create an initialized list of hues */
  private void fillInitialTimes() {
    // initially indicate hue hasn't been used in a long time ago so available immediately
    for (int i = 0; i < m_computerColorWheel; i++) {
      m_lastTimeHistoryOfColors.add(m_beginningOfTime);
    }
  }

  /**
   * Get the next color and set the timer for the next color change
   * @return Command to do it
   */
  public Command newColor() {
    return
      run(
        coroutine -> 
          {
            fillInitialTimes(); // initialize last time used for all the hues of the color wheel
            while(true)
            {
              getHSV(); // new color
              coroutine.fork(m_robotSignals.setSignal(m_persistentPatternDemo, "new color"));
              coroutine.wait(m_changeColorPeriod);
            }
          }
      ).named("History FSM Sequence newColor")
      ;
  }

  /**
   * Sets a color and quits immediately assuming the color persists somehow (in
   * "m_persistentPatternDemo") until the next color is later requested.
   *
   * <p>Set a random color that hasn't been used in the last "m_colorLockoutPeriod"
   */
  private void getHSV() {
    Time currentTime = Milliseconds.of(System.currentTimeMillis());
    int randomHue; // to be the next color
    int loopCounter = 0; // count attempts to find a different hue
    int loopCounterLimit = 20; // limit attempts to find a different hue
    // reasonable limit related to:
    // number of colors, how often colors change, how long to lockout a color.

    do {
      // Generate random numbers for hues in range of the computer color wheel
      randomHue = m_rand.nextInt(m_computerColorWheel);
      // if hue hasn't been used recently, then use it now and update its history
      var colorTime = m_lastTimeHistoryOfColors.get(randomHue); // get the associated time
      if (colorTime.lt(currentTime.minus(m_colorLockoutPeriod))) {
        m_lastTimeHistoryOfColors.set(randomHue, currentTime);
        break;
      }
      // hue used recently so loop to get another hue
      // limit attempts - no infinite loops allowed
    } while (++loopCounter < loopCounterLimit);

    m_persistentPatternDemo = LEDPattern.solid(Color.fromHSV(randomHue, 200, 200));
  }

  int m_verificationPrintCounter = 0; // counter for verification print limit

  @SuppressWarnings("unused")
  /**
   * Debugging output
   */
  private void verificationPrint() {

    m_robotSignals.m_view.forEach((LEDaddress, red, green, blue) ->
      System.out.println(LEDaddress + " " + red + " " + green + " " + blue)); // debugging dump for this view

    System.out.println("current time " + System.currentTimeMillis());
    for (int i = 0; i < m_lastTimeHistoryOfColors.size(); i++) {
      System.out.println(i + " " + m_lastTimeHistoryOfColors.get(i).toLongString());
    }
  }    
}
