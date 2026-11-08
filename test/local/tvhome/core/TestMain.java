package local.tvhome.core;

public final class TestMain {
    public static void main(String[] args) {
        run("ConfigTest", ConfigTest::run);
        run("InputPortsTest", InputPortsTest::run);
        run("HomeModelTest", HomeModelTest::run);
        run("CrashGuardTest", CrashGuardTest::run);
        run("WakePolicyTest", WakePolicyTest::run);
        run("HomeGateTest", HomeGateTest::run);
        run("UsageLogTest", UsageLogTest::run);
        run("BackgroundsTest", BackgroundsTest::run);
        System.out.println(T.checks + " checks, " + T.failures + " failed");
        System.exit(T.failures == 0 ? 0 : 1);
    }

    private static void run(String name, Runnable r) {
        try {
            r.run();
        } catch (Throwable e) {
            T.failures++;
            System.out.println("FAIL " + name + " threw " + e);
        }
    }
}
