package first.robot;

import static org.wpilib.units.Units.Seconds;

import java.util.function.BooleanSupplier;

import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;
import org.wpilib.command3.StateMachine;
import org.wpilib.command3.StateMachine.State;
import org.wpilib.epilogue.Logged;
import org.wpilib.command3.Trigger;
import org.wpilib.hardware.discrete.DigitalInput;

/**
 * Yet Another StateMachine Test
 * <p>uses digital inputs 0, 1, and 2 for some state changes
 * <p>usage:
 * <pre><code>
 * Scheduler.getDefault().schedule(StateMachineTest.testFSM());
 * 
 * First, Set inputs 1 and 2 to low. Input 0 may be set to low here or later.
 * Set input 0 to low then high to stop the infinite loop in state5.
 * Reset input 0 to low then set to high to stop the infinite loop in state6.
 * 
 * Set input 1 to high to stop infinite loop in 5 or 6 and go to 7
 * 
 * Set input 2 to high to stop the StateMachine from any state (especially 5 or 6)
 * </code></pre>
 */
@Logged
public class StateMachineTest {

    static DigitalInput diSwitchStates = new DigitalInput(0);
    static DigitalInput diQuitUnlimited = new DigitalInput(1);
    static DigitalInput diExit = new DigitalInput(2);

    // If the transitions' conditions of consecutive states simultaneously evaluate to TRUE, the
    // states subsequent to the first will transition out immediately executing only their ENTER and
    // EXIT runnables. This is normal, good behavior of an FSM. If a one-shot condition per iteration
    // is desired, that coding is an example shown below. That is similar to a ceiling fan pull-chain
    // or a momentary push-button switch which require a reset for the next iteration before another
    // state change.

    // Note that the StateMachine has no massaging of conditions like Triggers do. A Trigger could be
    // used for a condition if that behavior is desired as in the example below.

    // Create a one-shot push-button type trigger for use in successive state changes 5 and 6
    static Trigger oneShotFor5and6Basis = new Trigger(() -> diSwitchStates.get()).risingEdge();
    static {Scheduler.getDefault().addPeriodic(() -> oneShotFor5and6Usable = true);}
    static public boolean oneShotFor5and6Usable = true;
    static BooleanSupplier oneShotFor5and6 = () -> {
            var value = oneShotFor5and6Basis.getAsBoolean() && oneShotFor5and6Usable;
            oneShotFor5and6Usable = false;
            return value;};

    public static Command testFSM() {

        StateMachine tester = new StateMachine("test machine");

        State state2 = tester.addState(testCommand("command2"));
        State state3 = tester.addState(testCommand("command3"));
        State state4 = tester.addState(testCommand("command4"));
        State state5 = tester.addState(testCommand("unlimited5"));
        State state6 = tester.addState(testCommand("unlimited6"));
        State state7 = tester.addState(testCommand("command7"));

        state2.switchTo(state3).whenComplete();
        state3.switchTo(state4).whenComplete();
        state4.switchTo(state5).whenComplete();
        state5.switchTo(state6).when(oneShotFor5and6);
        state6.switchTo(state7).when(oneShotFor5and6);
        state7.exitStateMachine().whenComplete();

        // WARNING!! switchFromAny is dangerous - this one somewhat less so!!
        // set up identical transitions to a state from selected states
        tester.switchFromAny(state5, state6).to(state7).when(() -> diQuitUnlimited.get());

        // WARNING!! switchFromAny is dangerous - this one is highly DANGEROUS!!
        // Set up identical exit transitions from all addState executed before this statement is executed
        tester.switchFromAny().toExitStateMachine().when(() -> diExit.get());

        // HIDE!!! state1 from the switchFromAny() otherwise we can't even get started with simulation
        State state1 = tester.addState(
            // splice together the initial command so the base command doesn't have to be messed with
                assureInitializeDIOforSimulation()
                .andThen(testCommand("command1"))
                .withAutomaticName());
        // could use state1.onEnter(StateMachineTest::assureInitializeDIOforSimulation); but the Runnable
        // doesn't have coroutine.yield() so the robot stops iterating while waiting for onEnter looping to complete.
        // What a horrible idea!!
        state1.switchTo(state2).whenComplete();
        tester.setInitialState(state1);

        return tester;
    }

    /**
     * Something to do
     * @param name for unique id and processing
     * @return the command
     */
    private static Command testCommand(String name) {
      return 
        Command.noRequirements(coroutine -> {
          int count = 0;
          System.out.println(name + " " + count + " initialize");
          while(count < 4 || name.startsWith("unlimited")) {
            ++count;
            System.out.println(name + " " + count);
            coroutine.yield();
          }
          System.out.println(name + " " + count + " end");            
        }
        ).named("testFSM " + name);
    }

    /**
     * make sure digital inputs start at low because in simulation they start high
     * 
     * <p>tell the user to set them low
     * 
     * <p>This method's code should be at the beginning of the first state command; there are a
     * couple different ways - code inline in the state command or splice this onto the state command
     * with sequence() or .andThen().
     */
    private static Command assureInitializeDIOforSimulation() {
        return
            Command.noRequirements(coroutine ->
            {
                var printLimit = 20;
                var printCount = 0;
                while (printCount < printLimit && (diQuitUnlimited.get() || diExit.get())) {
                    printCount++;
                    System.out.println("To Start, Set DIO 1, and 2 to low-false-off-0 [" + printCount + " of " + printLimit + "]\n");
                    coroutine.wait(Seconds.of(1.));
                }
            }).named("initialize DIO");
    }
} // end class StateMachineTest
