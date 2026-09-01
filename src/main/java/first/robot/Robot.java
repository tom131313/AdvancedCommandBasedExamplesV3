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
import org.wpilib.driverstation.DriverStation;
import org.wpilib.framework.TimedRobot;
import org.wpilib.system.DataLogManager;

import first.robot.Constants.Alerts;
import first.robot.mechanisms.DisjointParallelGroup;

public class Robot extends TimedRobot {
  private RobotContainer m_robotContainer = new RobotContainer(super::addPeriodic);
  private Command m_autonomousSignal;
  private Command lightBar;

  public Robot() {
    // super(0.2); // default 0.02 seconds iterative loop step period

    // Start recording to data log
    DataLogManager.start();

    // Record DS control and joystick data.
    // Change to `false` to not record joystick data.
    DriverStation.startDataLog(DataLogManager.getLog(), true);
    
    // Note that scheduling commands before Robot Startup Completes effects command event logging
    // that had to be accommodated
    CommandsTriggers.create(m_robotContainer);

    // runs just before command execution with each Scheduler iteration
    // Use to write outputs like logging, dashboards, indicators, meh - goal-oriented mechanism
    //  periodic from the previous iteration.
    // Use to prepare inputs to the ensuing iteration to get a consistent set of all inputs including
    //  non-mechanisms not in scheduler run.
    // The TimedRobot also provides a different addPeriodic() that may have a different period
    //  (faster or slower) and an offset from the Robot loop timing.
    Scheduler.getDefault().addPeriodic(m_robotContainer::runBeforeTheCommands);
  }

  @Override
  public void robotPeriodic() {
    // check all triggers and run all scheduled commands
    Scheduler.getDefault().run();
  }
  
  @Override
  public void disabledInit() {
    // demonstrate how to run disabled
    lightBar = CommandsTriggers.lightBar(); // save command to cancel it later
    Scheduler.getDefault().schedule(lightBar);
  }

  @Override
  public void disabledPeriodic() {}

  @Override
  public void disabledExit() {
    Scheduler.getDefault().cancel(lightBar); // example is to demonstrate for disabled only
  }

  @Override
  public void autonomousInit() {
    Scheduler.getDefault().cancelAll(); // start auto clean - defaults are also cancelled but restart immediately
    m_autonomousSignal = CommandsTriggers.setAutonomousSignal();
    Scheduler.getDefault().schedule(m_autonomousSignal);
  }

  @Override
  public void autonomousPeriodic() {}

  @Override
  public void autonomousExit() {
    // This cancels the auto command in case it's still running.
    // It cancels because of the overlapping requirements of the LEDViews of m_autonomousSignal and
    // setAutonomousSignalOff.
    Scheduler.getDefault().schedule(CommandsTriggers.setAutonomousSignalOff());
  }

  @Override
  public void teleopInit() {
    // Commands running from another mode haven't been cancelled directly except the one below.
    if (m_autonomousSignal != null) { // check null in case not initialized in auto mode
      Scheduler.getDefault().cancel(m_autonomousSignal); // cancel in case still running
    }

    if (m_robotContainer.getM_disjointParallelGroup().isPresent()) {
      new DisjointParallelGroup();
    }
    else {
      Alerts.m_alertDisjointedGroup.set(true);
    }
  }

  @Override
  public void teleopPeriodic() {}

  @Override
  public void teleopExit() {}

  @Override
  public void utilityInit() {
    if (m_robotContainer.getM_anotherFSMtest().isPresent()) {
      Scheduler.getDefault().schedule(StateMachineTest.testFSM());
    }
    else {
      Alerts.m_alertStateMachineTest.set(true);
    }
  }

  @Override
  public void utilityPeriodic() {}

  @Override
  public void utilityExit() {
    // Exiting utility-enabled starts a sequence of two jobs using the internal button from
    // first job to second job
    Scheduler.getDefault().schedule(CommandsTriggers.getFirstCommandTriggersNextTest());
    System.out.println("utilityExit completed");
    // output:
    // utilityExit completed
    // first job running to trigger second job
    // second job ran
  }
}
