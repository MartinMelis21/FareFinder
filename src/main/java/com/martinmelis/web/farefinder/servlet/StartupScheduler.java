package com.martinmelis.web.farefinder.servlet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

import com.martinmelis.web.farefinder.publisher.Publisher;
import com.martinmelis.web.farefinder.scheduling.HelloJob;
import com.martinmelis.web.farefinder.scheduling.SchedulerJob;

import net.bican.wordpress.exceptions.InsufficientRightsException;
import net.bican.wordpress.exceptions.InvalidArgumentsException;
import net.bican.wordpress.exceptions.ObjectNotFoundException;
import redstone.xmlrpc.XmlRpcFault;

/**
 * Servlet implementation class StartupScheduler
 */
@WebServlet("/StartupScheduler")
public class StartupScheduler extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public StartupScheduler() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
	
	 public void init() {
		 /*
		    JobDetail job = new JobDetail();
	    	job.setName("farescraping");
	    	job.setJobClass(SchedulerJob.class);

	    	//configure the scheduler time
	    	SimpleTrigger trigger = new SimpleTrigger();
	    	trigger.setName("farescraping");
	    	trigger.setStartTime(new Date(System.currentTimeMillis()));
	    	trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
	    	trigger.setRepeatInterval(300000);

	    	//schedule it
	    	Scheduler scheduler = null;
			try {
				scheduler = new StdSchedulerFactory().getScheduler();
			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	try {
				scheduler.start();
			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	try {
				scheduler.scheduleJob(job, trigger);
			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  }
		 */		 
		 
			// Quartz 1.6.3
			// JobDetail job = new JobDetail();
			// job.setName("dummyJobName");
			// job.setJobClass(HelloJob.class);

			JobDetail job = JobBuilder.newJob((Class<? extends Job>) SchedulerJob.class)
				.withIdentity("dummyJobName", "group1").build();

	                //Quartz 1.6.3
			// SimpleTrigger trigger = new SimpleTrigger();
			// trigger.setStartTime(new Date(System.currentTimeMillis() + 1000));
			// trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
			// trigger.setRepeatInterval(30000);

			// Trigger the job to run on the next round minute
			Trigger trigger = TriggerBuilder
				.newTrigger()
				.withIdentity("dummyTriggerName", "group1")
				.withSchedule(
					SimpleScheduleBuilder.simpleSchedule()
					.withIntervalInSeconds(1)
						.repeatForever())
				.build();

			// schedule it
			Scheduler scheduler = null;
			try {
				scheduler = new StdSchedulerFactory().getScheduler();
			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				scheduler.start();
			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				scheduler.scheduleJob(job, trigger);
			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 		 
		 
		 
		 
		 
				 
		 /*
		// Grab the Scheduler instance from the Factory
		  Scheduler scheduler = null;
		try {
			scheduler = StdSchedulerFactory.getDefaultScheduler();
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  // and start it off
		  try {
			scheduler.start();
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 		 
		 // define the job and tie it to our MyJob class
		  JobDetail job = newJob(SchedulerJob.class)
		      .withIdentity("StartupScheduler", "group1")
		      .build();

		  // Trigger the job to run now, and then repeat every 40 seconds
		  Trigger trigger = newTrigger()
		      .withIdentity("Trigger1", "group1")
		      .startNow()
		      .withSchedule(simpleSchedule()
		              .withIntervalInSeconds(10)
		              .repeatForever())
		      .build();

		  // Tell quartz to schedule the job using our trigger
		  try {
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
		 */
		 
		 
		 /*
		 System.out.println("Initializing..");

	        // First we must get a reference to a scheduler
	        SchedulerFactory sf = new StdSchedulerFactory();
	        Scheduler sched = null;
			try {
				sched = sf.getScheduler();
			} catch (SchedulerException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

	        System.out.println("Initialization Complete..");

	        System.out.println("Not Scheduling any Jobs - relying on XML definitions..");

	        // start the schedule
	        try {
				sched.start();
			} catch (SchedulerException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

	        System.out.println("Scheduler Started..");

	        // wait 5 second to give our jobs a chance to run
	        try {
	            Thread.sleep(5L * 1000L);
	        } catch (Exception e) {
	        }
	       
	        System.out.println("Shutting Down..");
	        // shut down the scheduler
	        try {
				sched.shutdown(true);
			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	        SchedulerMetaData metaData = null;
			try {
				metaData = sched.getMetaData();
			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        System.out.println("Executed " + metaData.getNumberOfJobsExecuted() + " jobs.");
		  */
	 }	  
}
