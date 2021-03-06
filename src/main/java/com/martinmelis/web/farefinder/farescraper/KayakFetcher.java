package com.martinmelis.web.farefinder.farescraper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.martinmelis.web.farefinder.databaseHandler.DatabaseHandler;
import com.martinmelis.web.farefinder.databaseHandler.DatabaseQueries;

import dataTypes.AirportStructure;
import dataTypes.RoundTripFare;

public class KayakFetcher extends FareFetcher {

	private DatabaseHandler databaseHandler;
	
	public KayakFetcher(DatabaseHandler databaseHandler) {
		this.databaseHandler=databaseHandler;
	}

	@Override
	public ArrayList<RoundTripFare> getFareList(String origin) {
		
		ArrayList <String> origins = null;
		
		try {
			origins = this.databaseHandler.getOriginCountryAirports(origin);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		ArrayList<RoundTripFare> fares = new ArrayList<RoundTripFare>();
		Integer originID = null;
		Integer destinationID = null;
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd");
		
		try {
	    	creatMapping ();
		
		for (String originIata : origins)
		{
			String response=null;
			try {
				response = getRequest("https://www.kayak.de/h/explore/api?airport=" + originIata + "&budget=300");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//total
	    	JSONObject obj = new JSONObject(response);
	    	JSONArray destinations =  (JSONArray) obj.getJSONArray("destinations");    	
	    	for (int i=0;i<destinations.length();i++)
	    	{
	 
	    		JSONObject destination = (JSONObject) destinations.get(i);
	    		Date outboundDate = df.parse((String) destination.get("depart"));
	    		Date inboundDate = df.parse((String) destination.get("return"));
	    		Integer price = (Integer) ((JSONObject) destination.get("flightInfo")).get("price");
	    		String destinationIata = (String) ((JSONObject) destination.get("airport")).get("shortName");
	    		
	    		
	    		if ((originID = databaseHandler.getCachingLists().getIataFaaMapping().get(originIata)) == null)
     			{
     				AirportStructure newAirport = databaseHandler.accountNewAirport(originIata.toUpperCase());
     				
     				if (newAirport == null)
     					continue;
     				
     				originID = databaseHandler.insertAirport(newAirport);
     				databaseHandler.getCachingLists().getIataFaaMapping().put(originIata, originID);
     			}
     			if ((destinationID = databaseHandler.getCachingLists().getIataFaaMapping().get(destinationIata)) == null)
     				// I update SSID to databse
     			{
     				AirportStructure newAirport = databaseHandler.accountNewAirport(destinationIata.toUpperCase());
     				
     				if (newAirport == null)
     					continue;
     				
     				destinationID = databaseHandler.insertAirport(newAirport);
     				databaseHandler.getCachingLists().getIataFaaMapping().put(destinationIata, destinationID);
     			}
     			
     			RoundTripFare fare = databaseHandler.getRoundTripFare (originID, destinationID, price,outboundDate,inboundDate);
     			
     			fare.setBookingURL("https://www.kayak.de/flights/" + originIata.toUpperCase()+"-"+destinationIata.toUpperCase()+"/" + outputFormatter.format(outboundDate) +"/" +outputFormatter.format(inboundDate) +"/");
     			
     			fares.add(fare);     			
     			
     			System.out.println("Outbound Leg\n\tFrom : " + fare.getOrigin().getCityName()  + " to " + fare.getDestination().getCityName() );
     			System.out.println("\tDate : " + fare.getOutboundLeg().toString());
     			System.out.println("Inbound Leg\n\tFrom : " + fare.getDestination().getCityName()  + " to " + fare.getOrigin().getCityName() );
     			System.out.println("\tDate : " + fare.getInboundLeg().toString());
     			System.out.println("Price : " + fare.getPrice());
     			System.out.println("Average price on this route : " + fare.getBaseFare());
     			System.out.println("Sale : " + fare.getSaleRatio());
     			System.out.println("DealRatio : " + fare.getDealRatio());
     			System.out.println("Booking URL : " + fare.getBookingURL());
     			System.out.println ();	    		
	    	}
	    	
		}
		
		}
		catch (Exception e)
		{   
			e.printStackTrace();
		}
		
		return fares;
	}

	@Override
	public Integer getLivePrice(RoundTripFare fare) throws Exception {
		// TODO temporary hack
		//visit https://www.kayak.de/flights/LHR-JFK/2018-05-04/2018-05-08?sort=price_a
		//wait for full load
		//parse Common-Booking-MultiBookProvider featured-provider cheapest multi-row Theme-featured-large
		
		//WebDriver driver = new FirefoxDriver();
		//driver.get("https://www.kayak.de/flights/" + LHR-JFK +"/" + 2018-05-04 + "/" + 2018-05-08 + "?sort=price_a");
		//List <WebElement> results= driver.findElements(By.className("Common-Booking-MultiBookProvider featured-provider cheapest multi-row Theme-featured-large"));
       
		
		
		return fare.getPrice();
	}
	
	public String getRequest (String ssURL) throws IOException
	{
		URL kayakObj = new URL(ssURL);
		HttpsURLConnection kayakCon = (HttpsURLConnection) kayakObj.openConnection();
		kayakCon.setRequestMethod("GET");
		kayakCon.setRequestProperty("User-Agent", "Mozilla/5.0");
		kayakCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		kayakCon.setRequestProperty("Accept", "application/xml");
		kayakCon.setReadTimeout(0);
		kayakCon.setConnectTimeout(0);
		kayakCon.connect();
		
		//reading the response
		BufferedReader in = new BufferedReader(new InputStreamReader(kayakCon.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
				
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		kayakCon.disconnect();
		
		if (response.toString().equals(""))
			System.out.print("");
		
		return response.toString();
	}
	
	public void creatMapping () throws SQLException, XPathExpressionException, IOException, ParserConfigurationException, SAXException
	{ 	
		PreparedStatement ps = null;
		ResultSet resultSet = null;
					
		
		if (databaseHandler.getCachingLists().getIataFaaMapping().isEmpty())
		{
			ps = databaseHandler.getDatabaseConnection().prepareStatement(DatabaseQueries.getIataMapping);
			resultSet = ps.executeQuery();
			
			Integer AirportID = null;
			String iataFaa = null;
			
			while (resultSet.next())
			{
				AirportID =					resultSet.getInt(1); 
				iataFaa =					resultSet.getString(2);
				if (iataFaa != "" && iataFaa != null)
				{
					databaseHandler.getCachingLists().getIataFaaMapping().put(iataFaa, AirportID);
				}
			}
		}
			
	}

}
