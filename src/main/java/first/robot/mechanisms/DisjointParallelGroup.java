package first.robot.mechanisms;

import static org.wpilib.units.Units.Seconds;

import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.command3.Scheduler;
import org.wpilib.system.Timer;

/**
 * Example of ordinary use of command-based v3 for disjointed parallel command execution.
 * Just fork the command.
 * <p>Sequential command execution is essentially identical except use "await" instead of
 * "fork".
 * 
 * <p>Activate this example with:
 <pre><code>
 new DisjointParallelGroup();
 </code></pre>
 */
public class DisjointParallelGroup {
    public DisjointParallelGroup() {
    Command tester = Command.noRequirements
    (coroutine ->
    {
        class AM implements Mechanism {}
        AM Am = new AM();
        Command Ac  = Command.requiring(Am)
            .executing(coroutine2 -> {
                System.out.println(Timer.getTimestamp() + " A1 5 sec");
                coroutine2.wait(Seconds.of(5.));
                System.out.println(Timer.getTimestamp() + " A2 elapsed");})
            .named("Ac");
        coroutine.fork(Ac);

        class BM implements Mechanism {}
        Mechanism Bm = new BM();
        Command Bc  = Command.requiring(Bm)
            .executing(coroutine2 -> {
                System.out.println(Timer.getTimestamp() + " B1 10 sec");
                coroutine2.wait(Seconds.of(10.));
                System.out.println(Timer.getTimestamp() + " B2 elapsed");})
            .named("Bc");
        coroutine.fork(Bc);

        class CM implements Mechanism {}
        Mechanism Cm = new CM();
        Command Cc  = Command.requiring(Cm)
            .executing(coroutine2 -> {
                System.out.println(Timer.getTimestamp() + " C1 15 sec");
                coroutine2.wait(Seconds.of(15.));
                System.out.println(Timer.getTimestamp() + " C2 elapsed");})
            .named("Cc");
        coroutine.fork(Cc);

        class DM implements Mechanism {}
        Mechanism Dm = new DM();
        Command Dc  = Command.requiring(Dm)
            .executing(coroutine2 -> {
                System.out.println(Timer.getTimestamp() + " D1 50 sec");
                coroutine2.wait(Seconds.of(50.));
                System.out.println(Timer.getTimestamp() + " D2 elapsed");})
            .whenCanceled(() -> System.out.println(Timer.getTimestamp() + " Dc is being canceled"))
            .named("Dc");
        coroutine.fork(Dc);

        System.out.println(Timer.getTimestamp() +
            " all running commands: " + Scheduler.getDefault().getRunningCommands());
        coroutine.wait(Seconds.of(20));
        System.out.println(Timer.getTimestamp() +
            " all running commands: " + Scheduler.getDefault().getRunningCommands());
        System.out.println(Timer.getTimestamp() +
            " That's all folks for this example. Scheduler will take a second to clean up.");
    }
    ).named("outer")
    .andThen(Command.noRequirements(coroutine ->
        {
            coroutine.wait(Seconds.of(1.)); // make sure Scheduler had a chance to clean up

            var runningCommands = Scheduler.getDefault().getRunningCommands();

            var noneRunning = runningCommands.stream().filter(command -> 
                command.name().equals("Ac") ||
                command.name().equals("Bc") ||
                command.name().equals("Cc") ||
                command.name().equals("Dc")).findAny().isEmpty();

            System.out.println(Timer.getTimestamp() +
            " Should be nothing left of Ac, Bc, Cc, and Dc and that appears to be " + noneRunning + ".\n" +
            " All running commands:" + runningCommands);
        }
    ).named("check running")).named("disjoint parallel group example");

    Scheduler.getDefault().schedule(tester);
    }
}
/*
Test output of useDisjointParallelGroup

3136.0311441 A1 5 sec
3136.0337452 B1 10 sec
3136.0353145 C1 15 sec
3136.0369967 D1 50 sec
3136.0371405 all running commands: [SequentialGroup[name=disjoint parallel group example], outer, Ac, Bc, Cc, Dc]
3141.0388238 A2 elapsed
3146.0384753 B2 elapsed
3151.0380486 C2 elapsed
3156.0386487 all running commands: [SequentialGroup[name=disjoint parallel group example], outer, Dc]
3156.0401152 That's all folks for this example. Scheduler will take a second to clean up.
3156.041299 Dc is being canceled
3157.060602 Should be nothing left of Ac, Bc, Cc, and Dc and that appears to be true.
 All running commands:[SequentialGroup[name=disjoint parallel group example], check running] 
*/
