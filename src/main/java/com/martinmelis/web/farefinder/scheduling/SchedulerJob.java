package com.martinmelis.web.farefinder.scheduling;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.martinmelis.web.farefinder.databaseHandler.DatabaseHandler;
import com.martinmelis.web.farefinder.farescraper.FareScraper;

@DisallowConcurrentExecution
public class SchedulerJob implements Job {
	
		private ArrayList <String> origins;	

	  public SchedulerJob() throws SQLException, ClassNotFoundException 
	  {
		  	  super();
			//-----Defining the List of originating countries-----
		  	  
			  origins = new ArrayList <String> ();
			  
			    
			  	origins.add("AT");
			  	origins.add("SK");
			  	origins.add("CZ");
			  	origins.add("HU");
			  	origins.add("PL");
			  	origins.add("DE");
			  	origins.add("NO");
			  	origins.add("SE");
			  	origins.add("DK");
			  	origins.add("UA");
			  	origins.add("IT");
			  	origins.add("PT");
			  	origins.add("ES");
			  	origins.add("FR");
			  	origins.add("UK");
			  	origins.add("NL");
			  	origins.add("BE");
			  	origins.add("CH");
			  	origins.add("LU");
			  	origins.add("GR");
			  	origins.add("MT");
	            
		}
	  
	  
	public void execute (JobExecutionContext context)
	throws JobExecutionException {
		 
	       
	    FareScraper fareScraper = null;
	    String fares = "";
	    
	    DatabaseHandler databaseHandler = new DatabaseHandler();
		databaseHandler.connectDatabase();
	    
		try {
			fareScraper = new FareScraper();
			fares = fareScraper.getFaresString(origins,databaseHandler);
	        	        
	        File faresFile = new File("fares.html");
	        FileWriter faresWriter = new FileWriter(faresFile, false); // true to append	                             
	        faresWriter.write(fares);
	        faresWriter.close();
	        databaseHandler.disconnectDatabse();
	        
		} catch (Exception e) {
			databaseHandler.disconnectDatabse();
			e.printStackTrace();
		}
		
		System.out.println("Fares updated!");
	}
	
	
}
