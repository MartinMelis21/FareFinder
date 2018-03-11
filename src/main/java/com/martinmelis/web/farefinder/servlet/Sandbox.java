package com.martinmelis.web.farefinder.servlet;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.martinmelis.web.farefinder.dao.*;
import com.martinmelis.web.farefinder.farescraper.FareScraper;
import com.martinmelis.web.farefinder.farescraper.KayakFetcher;
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

@WebServlet("/Sandbox")
public class Sandbox extends HttpServlet{
	  
	
	  private static final long serialVersionUID = 1L;
	 	  
	  
	  @Override
	  protected void doGet(HttpServletRequest request,
	     HttpServletResponse response) throws ServletException, IOException {
		 
		  ClassLoader classLoader = getClass().getClassLoader();
          URL resource = classLoader.getResource("/FareFinder/src/main/resources/drivers/geckodriver");
          File f = new File("Driver");
          if (!f.exists()) {
              f.mkdirs();
          }
          File geckodriver = new File("Driver" + File.separator + "geckodriver");
          if (!geckodriver.exists()) {
        	  geckodriver.createNewFile();
              org.apache.commons.io.FileUtils.copyURLToFile(resource, geckodriver);
          }
          
          /*
          File pathToBinary = new File(browserDriver.getAbsolutePath());
          FirefoxBinary ffBinary = new FirefoxBinary(pathToBinary);
          FirefoxProfile firefoxProfile = new FirefoxProfile();
          FirefoxDriver driver = new FirefoxDriver(ffBinary,firefoxProfile);
          
         System.setProperty("webdriver.firefox.marionette",browserDriver.getAbsolutePath());*/
          
          // register path for windows
          String operatingSystem = System.getProperty("os.name");
          System.out.println (operatingSystem);
          
          /*else
          {
          
          //unix
          //chmod +x geckodriver
          //sudo mv geckodriver /usr/local/bin/
          }*/
          
         File pathToBinary = new File(geckodriver.getAbsolutePath());
         FirefoxBinary ffBinary = new FirefoxBinary(pathToBinary);
         FirefoxProfile firefoxProfile = new FirefoxProfile();
         
         FirefoxOptions option=new FirefoxOptions();
         option.setProfile(firefoxProfile);
         option.setBinary(ffBinary);
         
         //System.setProperty("webdriver.gecko.driver",browserDriver.getAbsolutePath());
  			
         WebDriver driver = new FirefoxDriver (option);
        //FirefoxDriver(option);
  		//comment the above 2 lines and uncomment below 2 lines to use Chrome
  		//System.setProperty("webdriver.chrome.driver","G:\\chromedriver.exe");
  		//WebDriver driver = new ChromeDriver();
      	
          String baseUrl = "http://demo.guru99.com/test/newtours/";
          String expectedTitle = "Welcome: Mercury Tours";
          String actualTitle = "";

          // launch Fire fox and direct it to the Base URL
          driver.get(baseUrl);

          // get the actual value of the title
          actualTitle = driver.getTitle();

          /*
           * compare the actual title of the page with the expected one and print
           * the result as "Passed" or "Failed"
           */
          if (actualTitle.contentEquals(expectedTitle)){
              System.out.println("Test Passed!");
          } else {
              System.out.println("Test Failed");
          }
         
          //close Fire fox
          driver.close();
          
          
		  /*
		  
		  HtmlUnitDriver driver = (org.openqa.selenium.htmlunit.HtmlUnitDriver) new HtmlUnitDriver(BrowserVersion.CHROME,true){
		        @Override
		        protected WebClient newWebClient(BrowserVersion version) {
		            WebClient webClient = super.newWebClient(version);
		            webClient.getOptions().setThrowExceptionOnScriptError(false);
		            webClient.getOptions().setActiveXNative(true);
		            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		            webClient.getOptions().setAppletEnabled(true);
		            return webClient;
		        }
		    };		
		 
		    WebClient webClient = new WebClient(BrowserVersion.FIREFOX_45);
		    webClient.getOptions().setThrowExceptionOnScriptError(false);
		    HtmlPage page = webClient.getPage("https://www.kayak.de/flights/LHR-JFK/2018-05-04/2018-05-08?sort=price_a");
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.waitForBackgroundJavaScript(10000);
		    
		 //WebDriver driver = new FirefoxDriver();	
		//((HtmlUnitDriver) driver).setJavascriptEnabled(true);
		//driver.get("https://www.kayak.de/flights/LHR-JFK/2018-05-04/2018-05-08?sort=price_a");
		//webClient.waitForBackgroundJavaScript(3000);
		
		
		 // wait for jQuery to load
	    ExpectedCondition<Boolean> jQueryLoad = new ExpectedCondition<Boolean>() {
	      @Override
	      public Boolean apply(WebDriver driver) {
	        try {
	          return ((Long)((JavascriptExecutor)driver).executeScript("return jQuery.active") == 0);
	        }
	        catch (Exception e) {
	          // no jQuery present
	          return true;
	        }
	      }
	    };

	    	    
	    // wait for Javascript to load
	    ExpectedCondition<Boolean> jsLoad = new ExpectedCondition<Boolean>() {
	      @Override
	      public Boolean apply(WebDriver driver) {
	        return ((JavascriptExecutor)driver).executeScript("return document.readyState")
	        .toString().equals("complete");
	      }
	    };
	    */
	    
		//WebDriverWait wait = new WebDriverWait(driver, 30);
				
				//wait.until(jQueryLoad);
				//wait.until(jsLoad);
		
				//WebDriverWait wait = new WebDriverWait(driver,600);
		
	/*			wait.until(new ExpectedCondition<Boolean>() {
				    public Boolean apply(WebDriver driver) {
				        System.out.println ("Entering until");
				    	WebElement button = driver.findElement(By.cssSelector("div[class='bar']"));
				    	
				    	//style="transform: translateX(100%);"
				    			
				        String text = button.getAttribute("style");
				        System.out.println(text);
				        if(text.equals("transform: translateX(100%);")) 
				            return true;
				        else
				            return false;
				    }
				});*/
				
				//List<WebElement> deals =  driver.findElements(By.cssSelector("div[class='Common-Booking-MultiBookProvider featured-provider cheapest multi-row Theme-featured-large']"));
			
				//for (WebElement deal:deals)
				//{
				//	System.out.println(deal.getAttribute("innerHTML").toString());
				//}
	    
	    /*
				File fout = new File("C:\\Users\\Martin Melis\\Desktop\\errorflights\\test.txt");
				FileOutputStream fos = new FileOutputStream(fout);
			    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			 
				bw.write(page.asXml());
			 
				bw.close();
				driver.close();
				
				
				*/
	  }
	  
	 
	  	
	  	
	  @Override
	  public void init() throws ServletException {
	    
	  }
}