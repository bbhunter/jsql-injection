package com.jsql.model.injection.strategy.blind;

import com.jsql.model.InjectionModel;
import com.jsql.model.exception.StoppedByUserSlidingException;
import com.jsql.util.LogLevelUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Time attack using parallel threads.
 * Waiting time in seconds, response time exceeded means query is false.
 * Noting that sleep() functions will add up for each line from request.
 * A sleep time of 5 will be executed only if the SELECT returns exactly one line.
 */
public class InjectionTime extends AbstractInjectionMonobit<CallableTime> {

    /**
     * Log4j logger sent to view.
     */
    private static final Logger LOGGER = LogManager.getRootLogger();

    /**
     *  Time based works by default, many tests will change it to false if it isn't confirmed.
     */
    private boolean isTimeInjectable = true;

    /**
     * Create time attack initialization.
     * If every false requests are under 5 seconds and every true are below 5 seconds,
     * then time attack is confirmed.
     */
    public InjectionTime(InjectionModel injectionModel, BinaryMode binaryMode) {
        super(injectionModel, binaryMode);
        
        // No blind
        if (this.falsy.isEmpty() || this.injectionModel.isStoppedByUser()) {
            return;
        }

        // Concurrent calls to the FALSE statements,
        // it will use inject() from the model
        ExecutorService taskExecutor = this.injectionModel.getMediatorUtils().getThreadUtil().getExecutor("CallableGetTimeTagFalse");
        Collection<CallableTime> callablesFalseTest = new ArrayList<>();
        for (String falseTest: this.falsy) {
            callablesFalseTest.add(new CallableTime(
                falseTest,
                injectionModel,
                this,
                    binaryMode,
                "time#falsy"
            ));
        }
        
        // If one FALSE query makes less than X seconds,
        // then the test is a failure => exit
        // Allow the user to stop the loop
        try {
            List<Future<CallableTime>> futuresFalseTest = taskExecutor.invokeAll(callablesFalseTest);
            this.injectionModel.getMediatorUtils().getThreadUtil().shutdown(taskExecutor);
            for (Future<CallableTime> futureFalseTest: futuresFalseTest) {
                if (this.injectionModel.isStoppedByUser()) {
                    return;
                }
                if (futureFalseTest.get().isTrue()) {
                    this.isTimeInjectable = false;
                    return;
                }
            }
        } catch (ExecutionException e) {
            LOGGER.log(LogLevelUtil.CONSOLE_JAVA, e, e);
        } catch (InterruptedException e) {
            LOGGER.log(LogLevelUtil.IGNORE, e, e);
            Thread.currentThread().interrupt();
        }
        
        this.checkTrueTests(binaryMode);
    }

    private void checkTrueTests(BinaryMode binaryMode) {
        // Concurrent calls to the TRUE statements,
        // it will use inject() from the model
        ExecutorService taskExecutor = this.injectionModel.getMediatorUtils().getThreadUtil().getExecutor("CallableGetTimeTagTrue");
        Collection<CallableTime> callablesTrueTest = new ArrayList<>();
        for (String trueTest: this.truthy) {
            callablesTrueTest.add(new CallableTime(
                trueTest,
                this.injectionModel,
                this,
                    binaryMode,
                "time#truthy"
            ));
        }

        // If one TRUE query makes more than X seconds,
        // then the test is a failure => exit.
        // Allow the user to stop the loop
        try {
            List<Future<CallableTime>> futuresTrueTest = taskExecutor.invokeAll(callablesTrueTest);
            this.injectionModel.getMediatorUtils().getThreadUtil().shutdown(taskExecutor);
            for (Future<CallableTime> futureTrueTest: futuresTrueTest) {
                if (this.injectionModel.isStoppedByUser()) {
                    return;
                }
                if (!futureTrueTest.get().isTrue()) {
                    this.isTimeInjectable = false;
                    return;
                }
            }
        } catch (ExecutionException e) {
            LOGGER.log(LogLevelUtil.CONSOLE_JAVA, e, e);
        } catch (InterruptedException e) {
            LOGGER.log(LogLevelUtil.IGNORE, e, e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public CallableTime getCallableBitTest(String sqlQuery, int indexCharacter, int bit) {
        return new CallableTime(
            sqlQuery,
            indexCharacter,
            bit,
            this.injectionModel,
            this,
            this.binaryMode,
            "bit#" + indexCharacter + "~" + bit
        );
    }

    @Override
    public boolean isInjectable() throws StoppedByUserSlidingException {
        if (this.injectionModel.isStoppedByUser()) {
            throw new StoppedByUserSlidingException();
        }
        var timeTest = new CallableTime(
            this.injectionModel.getMediatorVendor().getVendor().instance().sqlTestBinaryInit(),
            this.injectionModel,
            this,
            this.binaryMode,
            "time#confirm"
        );
        try {
            timeTest.call();
        } catch (Exception e) {
            LOGGER.log(LogLevelUtil.CONSOLE_JAVA, e, e);
        }
        return this.isTimeInjectable && timeTest.isTrue();
    }

    public int getSleepTime() {
        return this.injectionModel.getMediatorUtils().getPreferencesUtil().isLimitingSleepTimeStrategy()
            ? this.injectionModel.getMediatorUtils().getPreferencesUtil().countSleepTimeStrategy()
            : 5;
    }

    @Override
    public String getInfoMessage() {
        return "- Strategy Time: query True when delays for "+ this.getSleepTime() +"s\n\n";
    }
}
