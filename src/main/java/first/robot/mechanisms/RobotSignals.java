package first.robot.mechanisms;

import static org.wpilib.units.Units.Seconds;

import java.util.EnumSet;
import java.util.Optional;

import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.command3.Scheduler;
import org.wpilib.driverstation.RobotState;
import org.wpilib.hardware.led.AddressableLED;
import org.wpilib.hardware.led.AddressableLEDBuffer;
import org.wpilib.hardware.led.AddressableLEDBufferView;
import org.wpilib.hardware.led.LEDPattern;
import org.wpilib.util.Color;

import static first.robot.Constants.LEDlayout.LEDViewPlacement;
import first.robot.RobotContainer.ExamplesSelector;

/**
 * Manage the addressable LEDs as signaling mechanisms.
 *
 * <p>This is the creator and container of the LEDView mechanisms.
 *
 * <p>Buffer is not cleared (user commands may do so by turning off LEDs with BLACK).
 * 
 * <p>The design of this LED management scheme is a color-setting command is run in one iteration
 * and the class' periodic method refreshes the buffer (views) and LEDs every iteration. Thus,
 * patterns live to be displayed after their command that set them. This may be ill-advised or maybe
 * it's handy and okay to use. The downside is default commands cannot be used unless they are the
 * only commands changing the LEDs.
 * 
 * <p>There is an example of methods that utilize commands to refresh the LEDs at the bottom comments.
 * This requires that the command that triggers (in some manner) the LEDPattern command must continue
 * to run as long as the display is required. The LEDPattern command as a child cannot run longer
 * than the parent command that triggered it say by schedule() or fork().
 * Default commands could be used with those commands and likely the color would be "BLACK, off".
 * 
 * <p>An example that uses only the default command and supplies a differing pattern as desired
 * for differing signals is the enable/disable. That loses the advantages of Command management
 * interrupts, however. All you see in logs is the default command and not what color (pattern) is
 * set for it at any given time. This is not a great way to accomplish this task - it's to
 * demonstrate a default command but with a trivial use.
 */

public class RobotSignals {
  /**
   * Represents a supplier of LEDPattern for creating dynamic LEDPatterns.
   *
   * <p>Can't overload methods using generic interfaces parameters like Supplier so make our own
   * interface to use in overloads
   */
  @FunctionalInterface
  public interface LEDPatternSupplier {
    /**
     * Gets a result.
     *
     * @return a result
     */
    LEDPattern get();
  }

  private final AddressableLED m_strip;
  private final AddressableLEDBuffer m_bufferLED;
  private static int m_length = 0; // length of the buffer - last LED used + 1 for the number 0 LED

  EnumSet<ExamplesSelector> examplesSelector;
  
  // location in the LED string is defined and reserved for all examples even if not selected to run
  public final Optional<LEDViewTop>  m_top;
  public final Optional<LEDViewMain> m_main;
  public final Optional<LEDViewEnableDisable> m_enableDisable;
  public final Optional<LEDViewHistoryDemo> m_historyDemo;
  public final Optional<LEDViewAchieveHueGoal> m_achieveHueGoal;
  public final Optional<LEDViewKnightRider> m_knightRider;

  public RobotSignals(EnumSet<ExamplesSelector> examplesSelector) {

    this.examplesSelector = examplesSelector;

    // find number of LEDs used
    for(LEDViewPlacement index : LEDViewPlacement.values())
    {
      m_length = Math.max(m_length, index.last + 1); // position is zero-based; + 1 for length
    }

    final int addressableLedPort = 5; // Any SystemCore Smart DIO physical port works for LEDs
    m_strip = new AddressableLED(addressableLedPort);
    m_strip.setLength(m_length);
    m_bufferLED = new AddressableLEDBuffer(m_length); // buffer for all of the LEDs

    // Create the LEDViews of the LED buffer as mechanisms.
    // Some views are shared between two examples. That's a confusing bad idea and one has to
    // assure there aren't actually any conflicts in the usage.
    m_top            = examplesSelector.contains(ExamplesSelector.useAutonomousSignal) ||
                       examplesSelector.contains(ExamplesSelector.useColorWheel) ?
                          Optional.of(new LEDViewTop(LEDViewPlacement.TOP)) : Optional.empty();
    m_main           = examplesSelector.contains(ExamplesSelector.useAutonomousSignal) ||
                       examplesSelector.contains(ExamplesSelector.useIntake) ?
                          Optional.of(new LEDViewMain(LEDViewPlacement.MAIN)) : Optional.empty();
    m_enableDisable  = examplesSelector.contains(ExamplesSelector.useEnableDisable) ?
                          Optional.of(new LEDViewEnableDisable(LEDViewPlacement.ENABLEDISABLE)) : Optional.empty();
    m_historyDemo    = examplesSelector.contains(ExamplesSelector.useHistoryFSM) ?
                          Optional.of(new LEDViewHistoryDemo(LEDViewPlacement.HISTORYDEMO)) : Optional.empty();
    m_achieveHueGoal = examplesSelector.contains(ExamplesSelector.useAchieveHueGoal) ?
                          Optional.of(new LEDViewAchieveHueGoal(LEDViewPlacement.ACHIEVEHUEGOAL)) : Optional.empty();
    m_knightRider    = examplesSelector.contains(ExamplesSelector.useMooreLikeFSM) ?
                          Optional.of(new LEDViewKnightRider(LEDViewPlacement.KNIGHTRIDER)) : Optional.empty();

    Scheduler.getDefault().addPeriodic(this::runBeforeTheCommands);
  }

  /**
   * Run before commands and triggers
   */
  public void runBeforeTheCommands() {

    // run periodically to send LEDPattern changes to the views (buffer segments)
    if (m_top.isPresent()) m_top.get().setViewData();
    if (m_main.isPresent()) m_main.get().setViewData();
    if (m_enableDisable.isPresent()) m_enableDisable.get().setViewData();
    if (m_historyDemo.isPresent()) m_historyDemo.get().setViewData();
    if (m_achieveHueGoal.isPresent()) m_achieveHueGoal.get().setViewData();
    if (m_knightRider.isPresent()) m_knightRider.get().setViewData();

    // run periodically to send the complete buffer changes to the LEDs
    m_strip.setData(m_bufferLED);
  }

  /** LED view resource -- a segment within the whole LED device */
  public class LEDView implements Mechanism {

    final AddressableLEDBufferView m_view;
    
    public static LEDPattern OFF = LEDPattern.OFF;

    // Some animated patterns assume the LEDPattern keeps running so provide that function with a
    // persistent variable that is used to refresh the view every iteration.
    LEDPatternSupplier persistentPattern = () -> OFF; // initially

    LEDView(LEDViewPlacement placement) {
      m_view = m_bufferLED.createView(placement.first, placement.last);
    }

    /**
     * Put the pattern into the view
     * 
     * <p>LEDPattern is supplied so each iteration can get new data from the user of the view. That
     * is, a different pattern from a pattern generator may be set each iteration. That extends
     * beyond animated patterns with the animation all within one pattern.
     */
    void setViewData() {
      persistentPattern.get().applyTo(m_view);
    }

    /*
     * Public Commands
     */

    /*
      Both setSignal example Commands run in a single iteration to set the LEDPattern in a field
      variable used by another process to refresh the LEDView at each iteration.

      If a calling command (parent) demands that the setSignal command (child) runs continuously
      while the parent command is running, then a while loop with a yield can be added to a new
      setSignal method.

      In no case can the child command run after the parent command ends.
    */
    
    /**
     * Put an LEDPattern into the view.
     * Dynamic changeable LEDpattern or fixed LEDPattern (as a Supplier), Command version.
     * <p>Dynamic here means the LEDPattern is changeable and not that the LEDPattern causes the LEDs
     * to change. A fixed LEDPattern can cause the LEDs to change such as BLINK and another version
     * of setSignal() may be used.
     * @param pattern
     * @return Command to apply pattern to LED view
     */
    public Command setSignal(LEDPatternSupplier pattern, String name) {
      return
        run(coroutine -> persistentPattern = pattern).named(name);
    }

    /**
     * Put an LEDPattern into the view.
     * Dynamic changeable LEDpattern or fixed LEDPattern (as a Supplier), Non-command version.
     * <p>Dynamic here means the LEDPattern is changeable and not that the LEDPattern causes the LEDs
     * to change. A fixed LEDPattern can cause the LEDs to change such as BLINK and another version
     * of setSignal() may be used.
     * @param pattern
     */
    public void setSignal(LEDPatternSupplier pattern) {
      persistentPattern = pattern;
    }

    /**
     * Put an LEDPattern into the view.
     * Fixed LEDPattern, Command version
     * @param pattern
     * @return Command to apply pattern to LED view
     */
    public Command setSignal(LEDPattern pattern, String name) {
      return
        run(coroutine -> persistentPattern = () -> pattern).named(name);
    }

    /**
     * Put an LEDPattern into the view.
     * Fixed LEDPattern, Non-command version
     * @param pattern
     */
    public void setSignal(LEDPattern pattern) {
      persistentPattern = () -> pattern;
    }
  } // End LEDView


  /* Not required but these trivial classes give the scheduler their better names to use so they
     aren't all called LEDView. Also gives a place to put a default pattern and some logic if desired. */

  public class LEDViewTop extends LEDView
  {
    public LEDViewTop(LEDViewPlacement placement)
    {
      super(placement);
    }
  }
  
  public class LEDViewMain extends LEDView
  {
    public LEDViewMain(LEDViewPlacement placement)
    {
      super(placement);
    }
  }

  public class LEDViewEnableDisable extends LEDView
  {
    public LEDViewEnableDisable(LEDViewPlacement placement)
    {
      super(placement);
      setDefaultCommand(defaultCommand());
    }

  /**
   * This LEDView mechanism is not referenced anywhere else. All the logic for the display is in
   * the LEDPattern set once by this default command which is the only command that runs for this
   * LEDView. This is not an efficient way to set the pattern once - a single call to the non-
   * command method would suffice for this trivial example.
   * 
   * <p>Alternatives to a default command such as a perpetually running command or addPeriodic would
   * work but a default command is rescheduled if it fails. Again, none of this is needed for this
   * trivial example which demonstrates the default command with a pretend problem to solve..
   */
  private Command defaultCommand() {
    final LEDPattern disabled = LEDPattern.solid(Color.RED).breathe(Seconds.of(2.0));
    final LEDPattern enabled = LEDPattern.solid(Color.GREEN).breathe(Seconds.of(2.0));
    final LEDPatternSupplier enableDisableDefaultSignal = () -> RobotState.isDisabled() ? disabled : enabled;
    return
      run(coroutine -> {
            // only needed once
            setSignal(enableDisableDefaultSignal);
            while(true) coroutine.yield(); // nothing to do but hangout; nothing to repeat
            // can't ever get here
          })
      .whenCanceled(() ->
            System.out.println("Who interrupted or canceled me? Probably a cancelAll but I will live again immediately."))
      .named("EnableDisableDefault");
    }
  }  
  
  public class LEDViewHistoryDemo extends LEDView
  {
    public LEDViewHistoryDemo(LEDViewPlacement placement)
    {
      super(placement);
    }
  }  
  
  public class LEDViewAchieveHueGoal extends LEDView
  {
    public LEDViewAchieveHueGoal(LEDViewPlacement placement)
    {
      super(placement);
    }
  }

  public class LEDViewKnightRider extends LEDView
  {
    public LEDViewKnightRider(LEDViewPlacement placement)
    {
      super(placement);
    }
  }  
}
