package com.martinmelis.web.farefinder.servlet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;

import com.martinmelis.web.farefinder.publisher.Publisher;
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

}
