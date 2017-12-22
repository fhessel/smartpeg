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
		
		// The MachineLearningTask creates predictions
	    executor.scheduleAtFixedRate(new MachineLearningTask(), 0, 2, TimeUnit.MINUTES);
	    
	    // The DataExtractionTask creates training samples.
	    // As it is a time consuming task, we schedule it only every hours hours, so three times a day
	    //executor.scheduleAtFixedRate(new DataExtractionTask(), 0, 8, TimeUnit.HOURS);
	    
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
