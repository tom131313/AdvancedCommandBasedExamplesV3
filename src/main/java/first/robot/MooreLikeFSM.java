package first.robot;

import static org.wpilib.units.Units.Seconds;

import java.util.function.BooleanSupplier;

import org.wpilib.command3.Command;
import org.wpilib.command3.StateMachine;
import org.wpilib.command3.StateMachine.State;
import org.wpilib.hardware.led.LEDPattern;
import org.wpilib.system.Timer;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.units.measure.Time;
import org.wpilib.util.Color;

import first.robot.mechanisms.RobotSignals;
import first.robot.mechanisms.RobotSignals.LEDView;

/**
 * Demonstration of a Moore-Like FSM example based on the StateMachine class model in WPILib
 * Command-Based V3.
 * 
 * This FSM example sequentially displays eight red LEDs first to last then back last to first
 *   1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8 -> 7 -> 6 -> 5 -> 4 -> 3 -> 2 -> 1 -> 2 ...
 * 
 * To demonstrate the trigger for "whenComplete" a cycle counter state is added. It contributes
 * nothing to the light bar and is just to show the use of "whenComplete". The cycle count is
 * displayed in NT viewer.
 * 
 * The triggers are a user specified clock period distributed among 14 bins for 14 triggers needed
 * for this example of the Knight Rider Kitt Scanner. (An example of alternate triggering using a 
 * clock that expires for each state is included for example as comments but it doesn't seem to
 * offer much if any benefit.)
 * 
 * The scanner runs Disabled and in the example usage in Robot it is started immediately.
 * 
 * This example is a bit of a cheat - that is there are a few things wrong with it not being a
 * perfect FSM. There are several complex states but they are all identical except for a sequence
 * number - light number. That allows severe compression of code.  Normally each state would have its
 * own Functional Command combining the Entry, Exit, and Steady-state Runnables for that state.
 * 
 * There 8 states of the lights and each of those states has 2 possible exit transitions for counting
 * up or counting down with the 14 clocked triggers. An additional state to count cycles is defined
 * to show an example of the transition made if a state completes normally (internal event) and was
 * not interrupted by an external event. [The FSM could have been organized as 14 states with one
 * clocked triggered.]
 * 
 * This FSM does not demonstrate a STOP State except by cancelling the command.
 */

public class MooreLikeFSM {

  private final LEDView m_robotSignals; // LED view where the output is displayed
  private final Color m_color; // changeable color of the scanner
  private final double m_numberPeriods = 14.0; // number of periods or time bins to generate time-based triggers
  private int cyclesCounter;
  // part of the hard way to do logging (use Telemetry instead as is coded herein) 
  // private final IntegerPublisher lightBarCyclesCounter = NetworkTableInstance.getDefault()
  //     .getTable("MooreLikeFSM").getIntegerTopic("light bar cycles").publish();
  // private final StringPublisher stateName = NetworkTableInstance.getDefault()
  //     .getTable("MooreLikeFSM").getStringTopic("state name").publish();

  /**
   * Eight states of the lights in the Knight Rider Kitt Scanner.
   * Caution - anti-pattern - the ordinal of the state is used as the hardware LED index (0 based).
   * That could be made more obvious by using a class variable for each state.
   * 
   * These states only roughly correspond to the States of the StateMachine as they are the light
   * patterns only and aren't used for the counter State.
   */ 
  private enum LightState
    {Light1, Light2, Light3, Light4, Light5, Light6, Light7, Light8};

  private double m_duration; // used to determine when to change states
  
  // Display cycle counter

  // The hard way has a more complete NT publisher definition (see commented out Publishers).
  // The easy way is to use the Telemetry class - it does all the work.

  // assume no requirement is okay otherwise this would have to be a Mechanism
  private Command count = Command.noRequirements(_ ->
    {
      cyclesCounter++;
      // lightBarCyclesCounter.set(cyclesCounter); // the hard way
      Telemetry.log("light bar cycles", cyclesCounter); // the easy way
    }).named("count");

  /**
   * A Moore-Like FSM to display lights similar to the Knight Rider Kitt Scanner
   * 
   * @param robotSignals the LED View for the Scanner
   * @param period Specify the speed of the Scanner (suggest about 2.)
   * @param color Specify the color of the Scanner (suggest Color.kRed)
   */
  public MooreLikeFSM(LEDView robotSignals, Time period, Color color) {
    m_robotSignals = robotSignals;
    m_duration = m_numberPeriods/(period.in(Seconds));
    m_color = color;
  }

  /**
   * Factory to create a new lightBar FSM
   * 
   * @return new lightBar FSM
   */
  public Command createLightBar()
  {
    // With the StateMachine usage each transition belongs exclusively to the current state to exit.
    // The transition is the triggering condition and the next state to transition to.

    cyclesCounter = 0; // something to display just for example

    var lightBar = new StateMachine("Kitt Light Bar Scanner");

    // First you need states as commands.

    State countCycles = lightBar.addState(count);
    State light1 = lightBar.addState(activateLight(LightState.Light1));
    State light2 = lightBar.addState(activateLight(LightState.Light2)); 
    State light3 = lightBar.addState(activateLight(LightState.Light3));
    State light4 = lightBar.addState(activateLight(LightState.Light4));
    State light5 = lightBar.addState(activateLight(LightState.Light5));
    State light6 = lightBar.addState(activateLight(LightState.Light6));
    State light7 = lightBar.addState(activateLight(LightState.Light7));
    State light8 = lightBar.addState(activateLight(LightState.Light8));

    // Need an initial state at some point
    
    lightBar.setInitialState(countCycles);

    // Then you need conditions; the conditions determine the state changes
    // These are external conditions for the "when()". The condition for "whenComplete()" is
    // internal and implied by the use of that Command ending.

    // This scheme divides the clock's least significant digits ticking cycle into 14 pieces for the
    // 14 transitions they are essentially identical due to the contrived nature of this example so
    // take advantage using a loop.

    // When the state machine starts there will be a slight hesitation from light1 to light2 while
    // the clock winds around to the light1 state exit transition time. The alternate triggering
    // method far below in comments avoids that problem at a cost of greater complexity.
    BooleanSupplier[] period = new BooleanSupplier[(int)m_numberPeriods];
    for (int i = 0; i < period.length; i++)
    {
      var slice = i;
      period[i] = () -> (int) (Timer.getTimestamp()*m_duration % m_numberPeriods) == slice;
    }

    // countCycles steals a little time from Light1 (as does the very first cycle due to startup
    // processing) assuming it's much less than the time period trigger for light1 else obvious
    countCycles.switchTo(light1).whenComplete();
    light1.switchTo(light2).when(period[0]);
    light2.switchTo(light3).when(period[1]);
    light3.switchTo(light4).when(period[2]);
    light4.switchTo(light5).when(period[3]);
    light5.switchTo(light6).when(period[4]);
    light6.switchTo(light7).when(period[5]);
    light7.switchTo(light8).when(period[6]);
    light8.switchTo(light7).when(period[7]);
    light7.switchTo(light6).when(period[8]);
    light6.switchTo(light5).when(period[9]);
    light5.switchTo(light4).when(period[10]);
    light4.switchTo(light3).when(period[11]);
    light3.switchTo(light2).when(period[12]);
    light2.switchTo(countCycles).when(period[13]);

    // insert the counter between light2 then light1; awkward looking sequence but I didn't want
    // light1 to be hit twice in a row and depend on the right clock timing
    
    // There is no exitStateMachine defined so keep scanning until the FSM is cancelled.
    // Here's the example of how to inject the whenCanceled() into the StateMachine if needed.
    return 
      Command.requiring(m_robotSignals) // whenCanceled() uses runnable and not command with its requirement so use requiring() here
        .executing(coroutine ->
        {
          coroutine.await(lightBar); // run the state machine
          m_robotSignals.setSignal(RobotSignals.LEDView.OFF); // no need to fork the command since requiring() on whole thing
        })
        .whenCanceled(() -> m_robotSignals.setSignal(RobotSignals.LEDView.OFF)) 
        .named("Kitt");
  }
  
  /**
   * Factory for Command that turns on the correct LED every state change
   * 
   * <p>Commands can't be put into the State enum because
   * enums are static and these commands in general are non-static especially with the
   * "this" mechanism requirement.
   * 
   * <p>Generally factories can be "public" but this is dedicated to this FSM and there is no
   * intention of allowing outside use of it as that can disrupt the proper function of the FSM.
   * 
   * @param state the state to enter
   * @return the command to run that defines the state - turns on the correct LED
   */
  private final Command activateLight(LightState state) {
    LEDPattern currentStateSignal = oneLEDSmeared(state.ordinal(), m_color, Color.BLACK);
    return 
      Command.noRequirements(coroutine ->
        {
          // entry actions before the loop is equivalent to the onEntry(()->{})
          // stateName.set(this + " " + state.name()); // the hard way
          Telemetry.log("state name", this + " " + state.name()); // the easy way
          coroutine.fork(m_robotSignals.setSignal(currentStateSignal, "LEDs for " + state.name()));
          while(true) {coroutine.yield();} // idle loop waiting for state-changing interrupt
        // exit actions here but usually never get here so not the same as onExit()
        }).named(state.name());
  }
 
  /**
   * Turn on one bright LED in the string view.
   * Turn on its neighbors dimly. It appears smeared.
   * 
   * A simple cheat of the real Knight Rider Kitt Scanner which has a slowly
   * diminishing comet tail.  https://www.youtube.com/watch?v=usui7ECHPNQ
   * 
   * @param light index of which LED to turn on
   * @param colorForeground color of the on LED
   * @param colorBackground color of the off LEDs
   * @return Pattern to apply to the LED view
   */
  private static final LEDPattern oneLEDSmeared(int light, Color colorForeground, Color colorBackground) {
    int index = light;
    final int slightlyDim = 180;
    final int dim = 120;

    return (reader, writer) -> {
      int bufLen = reader.getLength();

      for (int led = 0; led < bufLen; led++) {
        if (led == index) {
          writer.setLED(led, colorForeground);              
        } else if ((led == index-2 && index-2 >= 0) || (led == index+2 && index+2 < bufLen)) {
          writer.setRGB(led,
           (int) (colorForeground.red * dim),
           (int) (colorForeground.green * dim),
           (int) (colorForeground.blue * dim));
        } else if ((led == index-1 && index-1 >= 0) || (led == index+1 && index+1 < bufLen)) {
          writer.setRGB(led,
           (int) (colorForeground.red * slightlyDim),
           (int) (colorForeground.green * slightlyDim),
           (int) (colorForeground.blue * slightlyDim));
        } else {
          writer.setLED(led, colorBackground);              
        }
      }
    };
  }
} // end class
/*
An alternate method to triggering state changes.

It sets a timer to expire for each state change. A second condition is required to determine which
of the two transitions for each state is required at the expiration of the timer.

It doesn't seem to offer benefit over the implementation of using the system clock for this example.

It is more complex coding with the use of additional enum and functional interface. It has only 2
different conditions, though, instead of 14.


// used to determine which triggered state to use in the back and forth cycle
enum Direction {FORTH, BACK}
private Direction direction = Direction.FORTH;


// used to determine when to change states
private Timer timer = Timer.createStarted();
private Time m_duration;
private DirectionConsumer timeElapsed = allowedDirection -> () -> {
    if (timer.hasElapsed(m_duration.in(Seconds)) && allowedDirection == direction) {
      // triggering correct transition and setup for next period
      timer.reset();
      return true;
    }
    else {
      return false; 
    } 
};


m_duration = period.div(m_numberPeriods);


countCycles.onEnter(() -> direction = Direction.FORTH);
countCycles.switchTo(light1).whenComplete();
light1.switchTo(light2).when(timeElapsed.apply(Direction.FORTH));
light2.switchTo(light3).when(timeElapsed.apply(Direction.FORTH));
light3.switchTo(light4).when(timeElapsed.apply(Direction.FORTH));
light4.switchTo(light5).when(timeElapsed.apply(Direction.FORTH));
light5.switchTo(light6).when(timeElapsed.apply(Direction.FORTH));
light6.switchTo(light7).when(timeElapsed.apply(Direction.FORTH));
light7.switchTo(light8).when(timeElapsed.apply(Direction.FORTH));
light8.onEnter(() -> direction = Direction.BACK);
light8.switchTo(light7).when(timeElapsed.apply(Direction.BACK));
light7.switchTo(light6).when(timeElapsed.apply(Direction.BACK));
light6.switchTo(light5).when(timeElapsed.apply(Direction.BACK));
light5.switchTo(light4).when(timeElapsed.apply(Direction.BACK));
light4.switchTo(light3).when(timeElapsed.apply(Direction.BACK));
light3.switchTo(light2).when(timeElapsed.apply(Direction.BACK));
light2.switchTo(countCycles).when(timeElapsed.apply(Direction.BACK));


/ **
  * For transitions need the timer expiration AND which direction the cycle is going in its back
  * and forth display. So this takes a desired direction and returns a BooleanSupplier if the timer
  * has expired AND the cycle is going the right way at the moment.
  * /
@FunctionalInterface
private interface DirectionConsumer {
  / **
    * Gets a result.
    * @param direction Desired Direction
    * @return BooleanSupplier for timer expired and correct direction
    * /
  BooleanSupplier apply(Direction direction);
}
*/