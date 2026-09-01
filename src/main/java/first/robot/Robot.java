/**
 * Example program that shows a variety of command based and programming "best practices."
 * 
 * Includes eight different techniques useful in Command-Based V3 programming. In addition all
 * examples are written in a similar suggested style of handling commands and triggers with
 * suggested variable naming style and minimal scope.
 *  1. Goal-Oriented mechanism to feed setpoints to a command-scheduled control calculation. (PID
 *     example)
 *  2. Use of historical data in addition to current state and events as input to a Finite State
 *     Machine. (Random, non-repeating colors)
 *  3. Example of splitting an apparent single resource into pieces for independent use.
 *     (Addressable LED strip)
 *  4. Minimal example of a robot mechanism. (Command triggered by an event)
 *  5. Examples of using a Moore-like FSM structure using an input state and a triggering event to
 *     transition to a new state.
 *  6. Another example of using a Moore-like FSM structure using easy-to-use methods to define the
 *     StateMachine class usage.
 *  7. Now inherent with simple syntax in V3 is the disjoint command group. The example starts in
 *     {@link #telopInit()}.
 *  8. Successive commands run by one command triggering the next command.
 *
 * Because all but two demonstrations use an addressable LED strip as output (two have console
 * output) there is significant overlap and depth in demonstrating style of using the advanced
 * addressable LED classes and methods.
 * 
 * Demonstration output is on six sets of eight (mostly) identical LEDs to show the program is
 * operating; operator input is Xbox controller. The other demonstrations outputs are the terminal
 * console "prints" and NT table.
 * 
 * Set the simulated LEDs to yes running; 8 columns; Row Major; Upper Left and 6 high.
 *
 * 1. LED set 1 usage Top LEDView mechanism.
 *  {@link #autonomousInit()} mode command brown fast blink.
 *  Non-autonomous displays colors slowly around the color wheel initiated by pressing "X" button.
 *
 * 2. LED set 2 usage Main LEDView mechanism default cyan.
 *  Game Piece Intake Acquired mechanism signal intake game piece acquired magenta fast blink
 *  (simulate game piece intake acquired by pressing "B" button).
 *  Autonomous mode command light green after 5 seconds (no requirement for Game Piece Intake Acquired).
 *
 * 3. LED set 3 usage EnableDisable LEDView mechanism.
 *  Enabled mode green slow blink; disabled mode red slow blink.
 *
 * 4. LED set 4 usage HistoryDemo LEDView mechanism.
 *  HistoryFSM mechanism displays random colors that don't repeat for awhile (time history).
 *  Periodic color changing initiated by pressing "Y" button then infinite loop.
 *
 * 5. LED set 5 usage AchieveHueGoal LEDView mechanism.
 *  AchieveHueGoal mechanism controller command to achieve the goal set by the goal supplier.
 *  Colors on color wheel position show PID controller converging on a color selected by Xbox right
 *  trigger axis. Press trigger axis a little to start and modulate to select hue goal. Press "A"
 *  button to interrupt controller before the goal has been achieved. The selected color blinks
 *  shortly at the end to indicate the controller is off and then gray. The rightmost LEDs show the
 *  setpoint and the left LEDs show the PID convergence to the setpoint.
 * 
 * 6. LED set 6 usage MooreLikeFSM LEDView mechanism.
 *  Moore Like FSM structured StateMachine runs Disabled to display a KnightRider Kitt red LED
 *  Scanner. It starts in {@link #disabledInit()}
 * 
 *  In addition to the LED output the NT variables display the actions of the FSM states.
 *
 * 7. First Job triggers Second Job initiated by {@link #utilityExit()}. This is a simple activation
 *  of a command by another command by using a trigger. This is a method of disjointing commands and
 *  breaking long commands into small parts.
 * 
 * 8. Another method that may be preferred for disjointed parallel or sequential compositions is
 *   using a coroutine. This example is activated by {@link #teleopInit()}.
 * 
 * 9. Yet another FSM test again using the StateMachine is activated in {@link #utilityInit()}
 * 
 *
 * There are user-selectable options set in {@link Config#Examples} to run the various examples.
 * 
 * There are user-selectable options set in {@link Config#CommandLoggingSettings} to run various
 * logging protocols.
 * 
 * All commands are interruptible except the enable/disable demonstration always runs as a default
 * command. That can be interrupted but would reappear immediately, if stopped.
 * 
 * <p>Some button presses are debounced.
 * 
 * The previous version of this project utilizing Command-Based V2 had complex examples of disjointed
 * parallel and sequential execution of commands with proxies. Those examples show how to isolate
 * commands' subsystems from being requirements of the entire duration of composite commands. The
 * effect of requirements being for the entire duration of Command.parallel and Command.sequence is
 * still true in V3. But the use of coroutines, instead, includes an inherent, simple way to isolate
 * commands' requirements from each other. Use fork and await for controlling command flow and not
 * have conflicting requirements that suppress the default commands. An example is included now as
 * a simple, ordinary feature of Command-Based v3.
 */

/*
 * Example program demonstrating:
 *
 * Splitting a common resource (string of LEDs into multiple separately used resources).
 * Configure button trigger.
 * Triggers.
 * Use of command parameters set at command creation time.
 * Use of command parameters set at dynamically at runtime (Suppliers).
 * Use of method reference.
 * Some commentary on composite commands and mode changes.
 * Command logging.
 * Configuring an autonomous command.
 * Use of Xbox controller to produce fake events.
 * Use of Xbox controller to trigger an event.
 * Use of public command factories in mechanisms.
 * Overloading method parameter types.
 * No commands with the word Command in the name. (But Alerts with the word Alert in them.)
 * (Almost) No triggers with the word Trigger in the name.
 * Supplier of dynamic LED pattern.
 * Static LED pattern.
 * Controller mechanism scheduled by a command to reach a Goal.
 * Commands run in sequence by triggering successive commands.
 *  [option set within code to invoke this technique]
 * Commands run in parallel by triggering successive commands after the first command completes.
 *  [test case run by entering test mode]
 * Use of Time.
 * Use of sequential and parallel composed command groups to perform tasks.
 * Use of a reusable Moore-Like FSM structure of current state, trigger, new state transitions.
 * Use of a perpetually running command to accept "goals".
 * Use of Alerts.
 * Use of the StateMachine class including a ceiling fan pull-chain or push-button type trigger.
 * Use of a Config interface for user settable options.
 * Example Tunables
 * Example Telemetry
 * Example NT Publisher commented out as the hard way to do simple Telemetry
 * Example Scheduler real-time info to NT is good but bug in 2027 alpha7 DataLog only logs "<invalid>"
 * Epilogue @Logged does not naturally happen for classes that implement OpMode. A bit of a kludge
 * is an example of how to do it in AutoExamples with additions to Robot. It might be just as easy
 * and clearer to publish variables to NT or better to use Telemetry.
 * Maybe somebody can automate the "patch" statements.
 * 
 *  Note this program uses Optional for the classes of examples. @Logged does not log and follow the
 *  class tree of Optionals - the real classes are hiding behind the Optional. To automatically log
 *  all the classes, reveal the classes something like this: MyClass variable = optionalMyClass.get()
 *  Beware that Optional implies the class may not exist; structure of program maybe different in
 *  order to accommodate @Logged restrictions.
 */

/*
 * Default Commands can be useful but they normally do not run within grouped, composite commands
 * even if their associated mechanisms are not active at all times within the composition.
 *
 * There are several possibilities to accommodate that restriction:
 *  1. do without default commands at any time but then you lose the benefits of default commands.
 *  2. do not rely on the default command within the group.
 *  3. manually code the function of the default command within a group.
 *  4. break groups into smaller groups and use Triggers to sequence multiple groups.
 *  5. use coroutine forking/awaiting out of the group restriction.
 * 
 * <p>Default Command can be set more than once but only the last one set is active. It might not be
 * obvious which Default Command is being used. If a default command is no longer desired, then set
 * it to idle() since 2027 alpha7 doesn't have a function to delete the default command.
 */

/**
 * These examples include the use of OpModes. There are many ways to structure a program with OpModes
 * and this program examples is a sort of middle-of-the-road version.
 * 
 * The extremes could vary from a single class annotated with all three OpModes to many classes - one
 * for each example and each class annotated with only the OpMode used for that one example. This
 * program has an "average" of the extremes - one class for each OpMode and all the examples that
 * run in that OpMode are in that one class.
 * 
 * If a class is annotated for all three OpModes, then to discriminate between modes there need to be
 * checks - RobotState.isUtility()  RobotState.isTeleop()  RobotState.isAutonomous()  RobotState.isEnabled()).
 * This is simply IterativeRobot the hard way. So why use this? Several independent programs, for
 * example, code from different people can each have their own OpMode class. Of course, the several
 * different people could also have multiple different OpModes, if desired.
 */

/*
 * This example program runs in real (untested) or simulated (tested) mode of the 2027 alpha7 WPILib.
 *
 * This is a refactor and extension of code donated by ChiefDelphi @illinar. It is intended to
 * demonstrate good programming based on @Oblarg's rules and comments by @Amicus1.
 * 
 * Errors and confusions are the fault of ChiefDelphi @SLAB-Mr.Thomas; github tom131313.
 */

/*
 * Caution:
 * 
 * WPILib examples often have a cancelAll() for commands. This program uses a perpetually running
 * method run by Scheduler.addPeriodic(). The example use runnables and not commands and thus
 * cannot be canceled like commands. They endure for the life of the Scheduler (which can be changed
 * and multiple schedulers can be used). The Robot.addPeriodic() behaves similarly.
 * <p>OpMode provides another periodic option that can vary with the OpMode.
 */
package first.robot;

import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;
import org.wpilib.datalog.ProtobufLogEntry;
import org.wpilib.epilogue.Epilogue;
import org.wpilib.epilogue.Logged;
import org.wpilib.epilogue.NotLogged;
import org.wpilib.framework.OpModeRobot;
import org.wpilib.system.DataLogManager;

@Logged
public class Robot extends OpModeRobot {

  // Store the scheduler in a field for Epilogue to pick up and log for us
  private final Scheduler scheduler = Scheduler.getDefault();

  private final ProtobufLogEntry<Scheduler> schedulerLog =
      ProtobufLogEntry.create(DataLogManager.getLog(), "Scheduler", Scheduler.proto);

  private RobotContainer m_robotContainer = new RobotContainer(super::addPeriodic);

  private Command lightBar;

  @NotLogged
  public final AutoExamples autoExamplesClosed = new AutoExamples(this); // have to make this fake
  // for Epilogue since the real AutoExamples isn't created until selected on the driver station.
  // This fake instance will unfortunately be logged while the real instance is not OpMode selected.
  public AutoExamples autoExamplesOpMode = autoExamplesClosed; // initially real one doesn't exist
  
  public Robot() {
    // super(0.2); // default 0.02 seconds iterative loop step period

    DataLogManager.start();

    // Note that scheduling commands before Robot Startup Completes effects command event logging
    // that had to be accommodated
    CommandsTriggers.create(m_robotContainer);

    // runs just before command execution with each Scheduler iteration
    // Use to write outputs like logging, dashboards, indicators, meh - goal-oriented mechanism
    //  periodic from the previous iteration.
    // Use to prepare inputs to the ensuing iteration to get a consistent set of all inputs including
    //  non-mechanisms not in scheduler run
    // The TimedRobot also provides a different addPeriodic() that may have a different period
    //  (faster or slower) and an offset from the Robot loop timing.
    scheduler.addPeriodic(m_robotContainer::runBeforeTheCommands);
    //FIXME patch:
    disabledInit(); // patch error in WPILib 2027 alpha7 -- remove when fixed
  }

  /**
   * Code that needs to know the DS state should go here.
   *
   * <p>Users should override this method for initialization that needs to occur after the DS is
   * connected, such as needing the alliance information.
   */
  public void driverStationConnected() {}

  
  /** Function called once during robot initialization in simulation. */
  @Override
  public void simulationInit() {}

  /** Function called periodically in simulation. */
  @Override
  public void simulationPeriodic() {}

  /** Function called periodically every loop, regardless of enabled state or OpMode selection. */
  @Override
  public void robotPeriodic() {
    scheduler.run(); // check all triggers and run all scheduled commands

    // Update telemetry
    Epilogue.update(this);
    schedulerLog.append(scheduler); // dumps info to NT but bug in 2027 alpha7 causes DataLog to show <invalid> instead of the good NT data that does show but passes by quickly!
  }

  //FIXME bug in 2027 alpha7 OpMode this method isn't run the first time.
  // must enable then disable for it to run; patch is to run it from robot constructor
  /** Function called once when the robot becomes disabled. */
  @Override
  public void disabledInit() {
    // demonstrate how to run disabled, regardless of OpMode selection.
    lightBar = CommandsTriggers.lightBar(); // save command to cancel it later
    scheduler.schedule(lightBar);
  }

  /** Function called periodically while the robot is disabled, regardless of OpMode selection. */
  @Override
  public void disabledPeriodic() {}

  /** Function called once when the robot exits disabled state, regardless of OpMode selection. */
  @Override
  public void disabledExit() {
    scheduler.cancel(lightBar); // example is to demonstrate for disabled only
  }

  /**
   * Function called periodically anytime when no opmode is selected, including when the Driver
   * Station is disconnected.
   */
  public void nonePeriodic() {}
  
  public void close() {  
    super.close();
  }

  public RobotContainer getRobotContainer() {
    return m_robotContainer;
  }  
}
