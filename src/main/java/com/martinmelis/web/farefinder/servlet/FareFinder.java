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
	  private Connection conn;
	  
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
			  	
			  		
			//-----------------connect to Database-----------------
			    
			    
				Context initialContext=null;
			    String dataResourceName = "jdbc/farefinder";
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
	  
	  
	  @Override
	  protected void doGet(HttpServletRequest request,
	     HttpServletResponse response) throws ServletException, IOException {
	    // Set a cookie for the user, so that the counter does not increate
	    HttpSession session = request.getSession(true);
	    response.setContentType("text/plain");
	    PrintWriter out = response.getWriter();   
	      
	      
	    fareScraper = new FareScraper();
	    String fares = "";
	    try {	    	
			fares = fareScraper.getFares(origins,conn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	    out.println(fares);
	    
	  }
	  
	  	
	  	
	  @Override
	  public void init() throws ServletException {
	    dao = new FileDao();
	    try {
	      count = dao.getCount() ;   
	      
	      
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