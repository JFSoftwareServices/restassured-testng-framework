package com.jide.framework.listeners;

import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestListener implements TestNG's ITestListener to log test lifecycle events.
 *
 * In parallel runs, multiple threads write to the console simultaneously.
 * Including the thread name in every log line makes it possible to trace
 * which thread ran which test and in what order, which is essential for
 * debugging parallel-specific failures.
 *
 * The listener is registered in testng-parallel.xml via:
 *   <listeners>
 *     <listener class-name="com.jide.framework.listeners.TestListener"/>
 *   </listeners>
 */
public class TestListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        System.out.printf("[%s][START ] %s.%s%n",
            threadName(),
            result.getTestClass().getName(),
            result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.printf("[%s][PASS  ] %s.%s (%.0fms)%n",
            threadName(),
            result.getTestClass().getName(),
            result.getMethod().getMethodName(),
            (double)(result.getEndMillis() - result.getStartMillis()));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        System.out.printf("[%s][FAIL  ] %s.%s — %s%n",
            threadName(),
            result.getTestClass().getName(),
            result.getMethod().getMethodName(),
            result.getThrowable().getMessage());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.printf("[%s][SKIP  ] %s.%s%n",
            threadName(),
            result.getTestClass().getName(),
            result.getMethod().getMethodName());
    }

    private static String threadName() {
        return Thread.currentThread().getName();
    }
}