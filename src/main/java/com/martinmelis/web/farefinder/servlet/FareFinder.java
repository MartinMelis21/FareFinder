package com.martinmelis.web.farefinder.servlet;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.martinmelis.web.farefinder.dao.*;
import com.martinmelis.web.farefinder.farescraper.FareScraper;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
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
	  private Connection databaseConnection = null;
	  
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
			  	Class.forName("com.mysql.jdbc.Driver");
			  	
			  	String host = "57ab68bb2d527108510001d1-martinmelis.rhcloud.com";
			  	String port = "46036";
			  	String name = "FareFinder";
	            String databaseURL = "jdbc:mysql://" + host + ":" + port + "/" + name;
	            Properties info = new Properties();
	            info.put("user", "adminhbdsaei");
	            info.put("password", "hhcXbX8hrHLP");
	 
	            databaseConnection = DriverManager.getConnection(databaseURL, info);
	            
	            System.out.println("Connected to the database " + name);
	            
	            if (databaseConnection != null) 
		            {
		                System.out.println("Connected to the database " + name);
		            }
	            */
	            
			}
	  
	  @Override
	  protected void doGet(HttpServletRequest request,
	     HttpServletResponse response) throws ServletException, IOException {
	    // Set a cookie for the user, so that the counter does not increate
	    HttpSession session = request.getSession(true);
	    response.setContentType("text/plain");
	    PrintWriter out = response.getWriter();
	    
		
	  	//-----connect to Database-----
	    

        Properties props = new Properties();
        FileInputStream fis = null;
        MysqlDataSource ds = null;

        ds = new MysqlConnectionPoolDataSource();
        ds.setURL("jdbc:mysql://57ab68bb2d527108510001d1-martinmelis.rhcloud.com:46036/farefinder");
        ds.setUser("adminhbdsaei");
        ds.setPassword("hhcXbX8hrHLP");
        
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        
        try {

            con = (Connection) ds.getConnection();
            pst = (PreparedStatement) con.prepareStatement("SELECT VERSION()");
            rs = pst.executeQuery();

            if (rs.next()) {

                String version = rs.getString(1);
                System.out.println(version);
            }

        } catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {

            if (rs != null) {
                try {
					rs.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }

            if (pst != null) {
                try {
					pst.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }

            if (con != null) {
                try {
					con.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        }
	    
	    /*
	  	try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	  	
	  	String host = "57ab68bb2d527108510001d1-martinmelis.rhcloud.com";
	  	String port = "46036";
	  	String name = "farefinder";
        String databaseURL = "jdbc:mysql://" + host + ":" + port + "/" + name;
        Properties info = new Properties();
        info.put("user", "adminhbdsaei");
        info.put("password", "hhcXbX8hrHLP");

        try {
			databaseConnection = DriverManager.getConnection(databaseURL, info);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        
        if (databaseConnection != null) 
            {
                System.out.println("Connected to the database " + name);
            }
        
	    
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
	    
	    */
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