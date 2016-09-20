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

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.martinmelis.web.farefinder.farescraper.FareScraper;


public class SchedulerJob implements org.quartz.StatefulJob {
	
		private ArrayList <String> origins;
		private Connection conn;
	

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
			  	origins.add("UK");
			  	origins.add("FR");
			  	
			  		
			//-----------------connect to Database-----------------
			    
			    
				Context initialContext=null;
			    String dataResourceName = "jdbc/MySQLDS";
				DataSource dataSource = null;
				Context environmentContext = null;
				try {
					initialContext = new InitialContext();
					environmentContext = (Context) initialContext.lookup("java:comp/env");
					dataSource = (DataSource) environmentContext.lookup(dataResourceName);
					conn = dataSource.getConnection();
				} catch (Exception e) {
					e.printStackTrace();
				}	    
			
		     //-------------------------------------------------
	            
		}
	  
	  
	
	

	public void execute (JobExecutionContext context)
	throws JobExecutionException {
		 
	      
	      
	    FareScraper fareScraper = null;
	    BufferedWriter dataOut;
	    String fares = "";
	    
		try {
			fareScraper = new FareScraper();
			fares = fareScraper.getFaresString(origins,conn);
	        	        
	        File faresFile = new File("fares.txt");
	        FileWriter faresWriter = new FileWriter(faresFile, false); // true to append	                             
	        faresWriter.write(fares);
	        faresWriter.close();
	        conn.close();
	        
	        
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Fares updated!");
		
	}
	
	
}
