package com.example.locker.it;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;
import java.util.logging.Logger;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public final class TestRunnerMain {

    private static final Logger LOG = Logger.getLogger(TestRunnerMain.class.getName());

    public static void main(String[] args) {
        LOG.info("Starting integration tests...");

        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(EndToEndIT.class))
                .build();

        Launcher launcher = LauncherFactory.create();
        launcher.execute(request, listener);

        TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out, true));

        long failures = summary.getTotalFailureCount();
        if (failures > 0) {
            summary.printFailuresTo(new PrintWriter(System.err, true));
            LOG.severe("FAILED: " + failures + " test(s) failed.");
            System.exit(1);
        }

        LOG.info("ALL TESTS PASSED (" + summary.getTestsSucceededCount() + " tests)");
        System.exit(0);
    }
}

