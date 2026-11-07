package com.lowdragmc.mbd2.test.tests.bench;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.config.ConfigHolder;
import com.lowdragmc.mbd2.test.framework.MBDTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static com.lowdragmc.mbd2.test.tests.bench.RecipeLogicBenchFixtures.*;

/**
 * Recipe logic benchmark. Opt-in: registered only when the environment variable {@code MBD2_BENCH} is set, e.g.
 * {@code MBD2_BENCH=1 ./gradlew runGameTestServer}; it then runs alongside the normal suite and logs its results
 * as {@code [MBD2-BENCH]} lines.
 *
 * <p>Each scenario places {@link #MACHINES} {@link BenchMachine}s and drives them itself — calling
 * {@link MBDMachine#serverTick()} on a simulated clock — so only these machines' own cost is measured, never the
 * rest of the level; the level's own ticks skip them. Every scenario runs with recipe search on the server thread
 * (all the work) and with async search (what the server thread itself pays), each after a throwaway pass that
 * lets the JIT compile the hot code.</p>
 *
 * <p>Two numbers per pass: wall time of the simulated ticks on the server thread, and the CPU time recipe work
 * cost on every thread it runs on (server thread, Minecraft's background executor, the common fork-join pool).</p>
 *
 * <p>It never blocks the server for long: each real tick simulates only about {@link #CHUNK_MILLIS} ms worth,
 * then puts the game time back. A server stalled for seconds falls behind its schedule, and while it catches up
 * {@code pollTask} refuses fresh tasks — which broke unrelated tests in the next batch.</p>
 *
 * <p>Tunable with {@code MBD2_BENCH_RECIPES} (default 1000), {@code MBD2_BENCH_MACHINES} (default 64, at most 81)
 * and {@code MBD2_BENCH_TICKS} (measured ticks per pass, default 200).</p>
 */
public class RecipeLogicBench {
    public static final boolean ENABLED = System.getenv("MBD2_BENCH") != null || Boolean.getBoolean("mbd2.bench");
    public static final int MACHINES = Math.min(81, intSetting("MBD2_BENCH_MACHINES", 64));
    public static final int TICKS = intSetting("MBD2_BENCH_TICKS", 200);
    public static final int WARMUP = 60;
    private static final long CHUNK_MILLIS = 20;

    static int intSetting(String env, int fallback) {
        var value = System.getenv(env);
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @GameTest(template = "empty_multiblock", templateNamespace = MBD2.MOD_ID, batch = "mbd2_bench", timeoutTicks = 20000)
    @PrefixGameTestTemplate(false)
    public static void recipe_logic_bench(GameTestHelper h) {
        var halves = halfMatchingItems();
        var plan = new Plan(h, List.of(
                // items no recipe uses: pure search overhead while idle
                new Scenario("idle_no_match", (machine, index) -> {
                    for (int slot = 0; slot < IN_SLOTS; slot++) {
                        insert(h, machine, slot, NON_POOL.get((index + slot) % NON_POOL.size()), 64);
                    }
                }, null),
                // nine items that are each half of many recipes, never both halves of one
                new Scenario("idle_partial_match", (machine, index) -> {
                    for (int slot = 0; slot < halves.size(); slot++) {
                        insert(h, machine, slot, halves.get(slot), 64);
                    }
                }, null),
                // a recipe run back to back: the steady per-tick cost of working
                new Scenario("working", (machine, index) -> {
                    var recipe = BENCH_RECIPES.get((index * 37) % BENCH_RECIPES.size());
                    insert(h, machine, 0, recipe.a(), 64);
                    insert(h, machine, 1, recipe.b(), 64);
                }, null),
                // idle machines handed a random recipe's inputs every 25 ticks: searches that find something
                new Scenario("churn", (machine, index) -> {}, (machines, tick) -> {
                    for (int i = 0; i < machines.size(); i++) {
                        if ((tick + i) % 25 != 0) continue;
                        var machine = machines.get(i);
                        if (!machine.getRecipeLogic().isIdle()) continue;
                        for (int slot = 0; slot < IN_SLOTS; slot++) {
                            MBDTestHelper.extractItem(h, machine, slot, 64);
                        }
                        var recipe = BENCH_RECIPES.get(new Random(tick * 31L + i).nextInt(BENCH_RECIPES.size()));
                        insert(h, machine, 0, recipe.a(), 1);
                        insert(h, machine, 1, recipe.b(), 1);
                    }
                })));
        h.succeedWhen(plan::step);
    }

    // region plan

    /** What a scenario does to each machine once it is placed. */
    @FunctionalInterface
    interface Setup {
        void apply(MBDMachine machine, int index);
    }

    record Scenario(String name, Setup setup, @Nullable BiConsumer<List<MBDMachine>, Integer> perTick) {}

    record Result(String scenario, boolean async, double wallMsPerTick, double p50Ms, double p95Ms, double maxMs,
                  double cpuMsPerTick, int finished, int searches) {
        String line() {
            return String.format("[MBD2-BENCH] %-20s search=%-5s | server thread ms/tick: mean %7.3f p50 %7.3f p95 %7.3f max %7.3f"
                            + " | recipe cpu ms/tick %8.3f (us/machine-tick %8.2f) | recipes finished %d | full searches %d",
                    scenario, async ? "async" : "sync", wallMsPerTick, p50Ms, p95Ms, maxMs,
                    cpuMsPerTick, cpuMsPerTick * 1000 / MACHINES, finished, searches);
        }
    }

    /** All scenarios, as passes advanced a slice per real tick by {@link #step()}. */
    private static final class Plan {
        private final GameTestHelper h;
        private final List<Runnable> queue = new ArrayList<>();
        private final List<Result> results = new ArrayList<>();
        private final boolean asyncBefore = ConfigHolder.ASYNC_RECIPE_SEARCHING.get();
        @Nullable
        private Pass current;
        private int queued;
        private boolean done;

        Plan(GameTestHelper h, List<Scenario> scenarios) {
            this.h = h;
            for (var scenario : scenarios) {
                // a throwaway pass of each first, so the measured ones run JIT-compiled code
                for (boolean measured : new boolean[]{false, true}) {
                    for (boolean async : new boolean[]{false, true}) {
                        queue.add(() -> current = new Pass(h, scenario, async, measured ? results : null));
                    }
                }
            }
            queued = queue.size();
        }

        /** One real tick's slice; throws until every pass is done, which is how {@code succeedWhen} waits. */
        void step() {
            if (done) return;
            var level = h.getLevel();
            var levelData = (ServerLevelData) level.getLevelData();
            long realTime = level.getGameTime();
            long sliceEnd = System.nanoTime() + CHUNK_MILLIS * 1_000_000;
            BenchMachine.driving = true;
            try {
                if (current == null) {
                    if (queue.isEmpty()) {
                        finish();
                        return;
                    }
                    queue.remove(0).run();
                    // placing the machines is this whole slice, so setup never lands in a measured one
                    throw new GameTestAssertException("benchmark set up a pass");
                }
                // A slice is all warm-up or all measured, and its server-thread CPU is read once for the whole
                // slice: per tick would be pure noise, as Windows only reports thread CPU in 15.6 ms steps.
                boolean measured = current.isMeasuring();
                var threads = ManagementFactory.getThreadMXBean();
                long cpuBefore = threads.getCurrentThreadCpuTime();
                do {
                    current.simulateTick(levelData);
                } while (System.nanoTime() < sliceEnd && current.isMeasuring() == measured && !current.isFinished());
                if (measured) {
                    current.serverCpu += threads.getCurrentThreadCpuTime() - cpuBefore;
                }
                if (current.isFinished()) {
                    current.finish();
                    current = null;
                }
            } finally {
                BenchMachine.driving = false;
                levelData.setGameTime(realTime);
            }
            throw new GameTestAssertException("benchmark running, " + (queued - queue.size()) + "/" + queued + " passes");
        }

        private void finish() {
            done = true;
            ConfigHolder.ASYNC_RECIPE_SEARCHING.set(asyncBefore);
            MBD2.LOGGER.info("[MBD2-BENCH] config: recipes={} machines={} measured ticks={} warmup={}",
                    RECIPES, MACHINES, TICKS, WARMUP);
            for (var result : results) {
                MBD2.LOGGER.info(result.line());
            }
        }
    }

    /** One scenario run once, sync or async; measured or a throwaway JIT warm-up. */
    private static final class Pass {
        private final GameTestHelper h;
        private final Scenario scenario;
        private final boolean async;
        @Nullable
        private final List<Result> results;
        private final List<MBDMachine> machines = new ArrayList<>();
        private final List<BlockPos> positions = new ArrayList<>();
        private final long[] samples = new long[TICKS];
        private long simTime;
        private int tick;
        private long wallTotal;
        private long serverCpu;
        private long workerCpuStart;
        private int searchesAtStart;

        Pass(GameTestHelper h, Scenario scenario, boolean async, @Nullable List<Result> results) {
            this.h = h;
            this.scenario = scenario;
            this.async = async;
            this.results = results;
            this.simTime = h.getLevel().getGameTime();
            ConfigHolder.ASYNC_RECIPE_SEARCHING.set(async);
            for (int i = 0; i < MACHINES; i++) {
                var pos = new BlockPos(1 + i % 9, 1, 1 + i / 9);
                positions.add(pos);
                var machine = MBDTestHelper.placeMachine(h, MACHINE, pos);
                MBDTestHelper.insertEnergy(h, machine, 50_000_000);
                scenario.setup().apply(machine, i);
                machines.add(machine);
            }
        }

        boolean isFinished() {
            return tick >= WARMUP + TICKS;
        }

        boolean isMeasuring() {
            return tick >= WARMUP;
        }

        void simulateTick(ServerLevelData levelData) {
            levelData.setGameTime(++simTime);
            if (scenario.perTick() != null) {
                scenario.perTick().accept(machines, tick);
            }
            boolean measuring = isMeasuring();
            if (tick == WARMUP) {
                workerCpuStart = workerCpuNanos();
                searchesAtStart = searches();
            }
            long start = System.nanoTime();
            for (var machine : machines) {
                machine.serverTick();
            }
            long elapsed = System.nanoTime() - start;
            if (measuring) {
                samples[tick - WARMUP] = elapsed;
                wallTotal += elapsed;
            }
            if (async) {
                // A real tick lasts 50 ms, time a background search gets to finish in. Ticking back to back would
                // starve it and make async look free, so wait for it — outside the timed part.
                awaitSearches(machines);
            }
            tick++;
        }

        void finish() {
            long workerCpu = workerCpuNanos() - workerCpuStart;
            int finished = 0;
            for (var machine : machines) {
                for (int slot = IN_SLOTS; slot < IN_SLOTS + OUT_SLOTS; slot++) {
                    finished += MBDTestHelper.readItem(h, machine, slot).getCount();
                }
            }
            for (var pos : positions) {
                h.setBlock(pos, Blocks.AIR);
            }
            int searches = searches() - searchesAtStart;
            if (results == null) return;
            var sorted = samples.clone();
            Arrays.sort(sorted);
            results.add(new Result(scenario.name(), async, wallTotal / 1e6 / TICKS,
                    sorted[TICKS / 2] / 1e6, sorted[(int) (TICKS * 0.95)] / 1e6, sorted[TICKS - 1] / 1e6,
                    (serverCpu + workerCpu) / 1e6 / TICKS, finished, searches));
        }

        /** Full searches so far, over every logic of every machine; the build before the index counted none. */
        private int searches() {
            int total = 0;
            for (var machine : machines) {
                for (var logic : machine.getRecipeLogics()) {
                    total += logic.getSearchCount();
                }
            }
            return total;
        }
    }

    // endregion

    // region helpers

    /** CPU time of the threads recipe work runs on besides the server thread: async search, parallel streams. */
    private static long workerCpuNanos() {
        var threads = ManagementFactory.getThreadMXBean();
        long total = 0;
        for (var info : threads.getThreadInfo(threads.getAllThreadIds())) {
            if (info == null) continue;
            var name = info.getThreadName();
            if (name.startsWith("Worker-Main") || name.startsWith("ForkJoinPool.commonPool") || name.equals("Searching recipes")) {
                long cpu = threads.getThreadCpuTime(info.getThreadId());
                if (cpu > 0) total += cpu;
            }
        }
        return total;
    }

    @Nullable
    private static Field searchFutureField;
    private static boolean searchFutureLooked;

    /** Wait for every machine's in-flight async recipe search, if the recipe logic exposes one. */
    private static void awaitSearches(List<MBDMachine> machines) {
        if (!searchFutureLooked) {
            searchFutureLooked = true;
            try {
                searchFutureField = RecipeLogic.class.getDeclaredField("completableFuture");
                searchFutureField.setAccessible(true);
            } catch (ReflectiveOperationException e) {
                MBD2.LOGGER.warn("[MBD2-BENCH] RecipeLogic has no completableFuture field; async passes are not settled");
            }
        }
        if (searchFutureField == null) return;
        for (var machine : machines) {
            try {
                if (searchFutureField.get(machine.getRecipeLogic()) instanceof CompletableFuture<?> future) {
                    future.get(5, TimeUnit.SECONDS);
                }
            } catch (Exception ignored) {
                // cancelled, failed or too slow: the logic deals with that itself on its next poll
            }
        }
    }

    private static void insert(GameTestHelper h, MBDMachine machine, int slot, Item item, int count) {
        MBDTestHelper.insertItem(h, machine, slot, new ItemStack(item, count));
    }

    /** Up to nine pool items, no two of which are the two ingredients of one recipe. */
    private static List<Item> halfMatchingItems() {
        var chosen = new ArrayList<Item>();
        for (var recipe : BENCH_RECIPES) {
            if (chosen.size() >= IN_SLOTS) break;
            var candidate = recipe.a();
            if (chosen.contains(candidate)) continue;
            var withCandidate = new HashSet<>(chosen);
            withCandidate.add(candidate);
            boolean completesARecipe = BENCH_RECIPES.stream()
                    .anyMatch(r -> withCandidate.contains(r.a()) && withCandidate.contains(r.b()));
            if (!completesARecipe) chosen.add(candidate);
        }
        return chosen;
    }

    // endregion
}
