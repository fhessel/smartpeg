package de.tudarmstadt.smartpeg.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * The ScheduledExecutor is a ServletContextListener that executes Runnables periodically while the server is up.
 * 
 * The Runnables can be configured in the contextInitialized method.
 * 
 * @author frank
 *
 */
public class ScheduledExecutor implements ServletContextListener {

    private static Logger logger = Logger.getLogger(MachineLearningTask.class.getName());
    
    /** The executor - volatile at it may be accessed asynchronously */
	private volatile ScheduledExecutorService executor;
	
	@Override
	public void contextInitialized(ServletContextEvent ctxEvent) {
		executor = Executors.newScheduledThreadPool(2);
		
		// Add runnables here.
	    executor.scheduleAtFixedRate(new MachineLearningTask(), 0, 2, TimeUnit.MINUTES);
	    
	    logger.info("Scheduler has been started");
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent ctxEvent) {
		final ScheduledExecutorService executor = this.executor;

	    if (executor != null)
	    {
	        executor.shutdown();
	        this.executor = null;
	    }

	    logger.info("Scheduler has been stopped");
	}
	
}
