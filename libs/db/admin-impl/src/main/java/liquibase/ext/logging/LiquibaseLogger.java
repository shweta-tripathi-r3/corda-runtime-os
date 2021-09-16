package liquibase.ext.logging; // this is *very* important

import liquibase.logging.LogMessageFilter;
import liquibase.logging.core.AbstractLogService;
import liquibase.logging.core.AbstractLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.logging.Level;

/**
 * TODO: need to work out how to resolve this proper
 * https://stackoverflow.com/questions/20880783/how-to-get-liquibase-to-log-using-slf4j
 *
 * https://github.com/mattbertolini/liquibase-slf4j does not have OSGi exports.
 *
 * Liquibase finds this class by itself by doing a custom component scan (sl4fj wasn't generic enough).
 */
//public class LiquibaseLogger extends AbstractLogger {
//    private static final Logger LOGGER = LoggerFactory.getLogger(LiquibaseLogger.class);
//    private String name = "";
//
//    public LiquibaseLogger(LogMessageFilter filter) {
//        super(filter);
//    }
//
//    @Override
//    public void close() throws Exception {
//        super.close();
//    }
//
//    @Override
//    public void log(Level level, String message, Throwable e) {
//        // TODO
//    }
//
//    @Override
//    public void severe(String message) {
//        LOGGER.error("{} {}", name, message);
//    }
//
//    @Override
//    public void severe(String message, Throwable e) {
//        LOGGER.error("{} {}", name, message, e);
//    }
//
//    @Override
//    public void warning(String message) {
//        LOGGER.warn("{} {}", name, message);
//    }
//
//    @Override
//    public void warning(String message, Throwable e) {
//        LOGGER.warn("{} {}", name, message, e);
//    }
//
//    @Override
//    public void info(String message) {
//        LOGGER.info("{} {}", name, message);
//    }
//
//    @Override
//    public void info(String message, Throwable e) {
//        LOGGER.info("{} {}", name, message, e);
//    }
//
//    @Override
//    public void debug(String message) {
//        LOGGER.debug("{} {}", name, message);
//    }
//
//    @Override
//    public void debug(String message, Throwable e) {
//        LOGGER.debug("{} {}", message, e);
//    }
//}