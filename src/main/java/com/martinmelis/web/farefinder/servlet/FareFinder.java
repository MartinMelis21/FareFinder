package com.martinmelis.web.farefinder.servlet;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
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
import com.martinmelis.web.farefinder.publisher.Publisher;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import net.bican.wordpress.exceptions.FileUploadException;
import net.bican.wordpress.exceptions.InsufficientRightsException;
import net.bican.wordpress.exceptions.InvalidArgumentsException;
import net.bican.wordpress.exceptions.ObjectNotFoundException;
import redstone.xmlrpc.XmlRpcFault;

/**
 * Servlet implementation class FareFinder
 */

@WebServlet("/FareFinder")
public class FareFinder extends HttpServlet{
	  
	
	  private static final long serialVersionUID = 1L;
	  int count;
	  private FileDao dao;
	  
	  DataSource dataSource = null;
	  static int lport;
	  static String rhost;
	  static int rport;
	  
	  
	  
	  @Override
	  protected void doGet(HttpServletRequest request,
	     HttpServletResponse response) throws ServletException, IOException {
	    // Set a cookie for the user, so that the counter does not increate
	    response.setContentType("text/plain");
	    PrintWriter out = response.getWriter();
	    String fares = "";
	      
	    
	    try(BufferedReader br = new BufferedReader(new FileReader("fares.txt"))) {
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();

	        while (line != null) {
	            sb.append(line);
	            sb.append(System.lineSeparator());
	            line = br.readLine();
	        }
	       fares = sb.toString();
	    }    
	    out.println(fares);
	  }
	  
	  	
	  	
	  @Override
	  public void init() throws ServletException {
	    dao = new FileDao();
	    try {
	    	//test
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