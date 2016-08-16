package com.martinmelis.web.farefinder.servlet;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.martinmelis.web.farefinder.dao.*;
import com.martinmelis.web.farefinder.farescraper.FareScraper;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

/**
 * Servlet implementation class FareFinder
 */

@WebServlet("/FareFinder")
public class FareFinder extends HttpServlet{
	  
	
	  private static final long serialVersionUID = 1L;
	  int count;
	  private FileDao dao;
	  private FareScraper fareScraper;
	  private ArrayList <String> origins;
	  
	  DataSource dataSource = null;
	  static int lport;
	  static String rhost;
	  static int rport;
	  
	  public FareFinder() throws SQLException, ClassNotFoundException {
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
			  	
			  		
			  	/*
			  	
			  	//-----connect to Database-----
			  	try
			  	{

		            System.out
		                    .println("-----------------------------------------------------");
		            System.out
		                    .println("init method has been called and servlet is initialized");
		
		            		            
		            Context initContext = new InitialContext();
		            Context envContext = (Context) initContext.lookup("java:/comp/env");
		            dataSource = (DataSource) envContext.lookup("jdbc/farefinder");
		            
		            System.out.println("Using JDNI lookup got the DataSource : "+ dataSource);
		
		            System.out
		                    .println("-----------------------------------------------------");
			  	}

			        catch( Exception exe )
			        {
			            exe.printStackTrace();
			        }
	            */
	            
			}
	  
	  
	  
	  public static void sslTunnel (){
	        String user = "57ab68bb2d527108510001d0";
	        String password = "mWecpW5nfj";
	        String host = "farefinder-martinmelis.rhcloud.com";
	        int port=22;
	        try
	            {
	            JSch jsch = new JSch();
	            Session session = jsch.getSession(user, host, port);
	            jsch.addIdentity("/home/user/.ssh/id_rsa");
	            lport = 4321;
	            rhost = "localhost";
	            rport = 46036;
	            session.setPassword(password);
	            session.setConfig("StrictHostKeyChecking", "no");
	            System.out.println("Establishing Connection...");
	            session.connect();
	            int assinged_port=session.setPortForwardingL(lport, rhost, rport);
	            System.out.println("localhost:"+assinged_port+" -> "+rhost+":"+rport);
	            }
	        catch(Exception e){System.err.print(e);}
	    }
	  
	  
	  @Override
	  protected void doGet(HttpServletRequest request,
	     HttpServletResponse response) throws ServletException, IOException {
	    // Set a cookie for the user, so that the counter does not increate
	    HttpSession session = request.getSession(true);
	    response.setContentType("text/plain");
	    PrintWriter out = response.getWriter();
	    
		
	  	//-----connect to Database-----
	    
	    
		Context initialContext=null;
		try {
			initialContext = new InitialContext();
		} catch (NamingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		Context environmentContext = null;
		try {
			environmentContext = (Context) initialContext.lookup("java:comp/env");
		} catch (NamingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		String dataResourceName = "jdbc/farefinder";
		DataSource dataSource = null;
		try {
			dataSource = (DataSource) environmentContext.lookup(dataResourceName);
		} catch (NamingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		StringBuilder msg = new StringBuilder();
		
		 //STEP 4: Execute a query
	      System.out.println("Creating statement...");
	      Statement stmt = null;
		try {
			stmt = conn.createStatement();
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

	      String sql = "SELECT VERSION() as version";
	      ResultSet rs = null;
		try {
			rs = stmt.executeQuery(sql);
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	      //STEP 5: Extract data from result set
	      try {
			while(rs.next()){
			     //Retrieve by column name
			     System.out.println(rs.getInt("version"));
			  }
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	      try {
			rs.close();
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	      
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    /*
	    
        	 System.out.println("-----Connecting to database-----");
             Connection con = null;
             String driver = "com.mysql.jdbc.Driver";
             String url = "jdbc:mysql://127.0.0.1:46036/";
             String db = "farefinder";
             String dbUser = "adminhbdsaei";
             String dbPasswd = "hhcXbX8hrHLP";
             try {
				Class.forName(driver);
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
             try {
				con = DriverManager.getConnection(url+db, dbUser, dbPasswd);
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	    
        //---------------------------
        */
	      
	      
	    fareScraper = new FareScraper();
	    String fares = "";
	    try {	    	
			fares = fareScraper.getFares(origins);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    if (session.isNew()) {
	      count++;
	    }
	    out.println(fares);
	    
	  }
	  
	  	
	  	
	  @Override
	  public void init() throws ServletException {
	    dao = new FileDao();
	    try {
	      count = dao.getCount();
	    } catch (Exception e) {
	      getServletContext().log("An exception occurred in FileCounter", e);
	      throw new ServletException("An exception occurred in FileCounter"
	          + e.getMessage());
	    }
	  }
	  
	  public void destroy() {
	    super.destroy();
	    try {
	      dao.save(count);
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	  }
}