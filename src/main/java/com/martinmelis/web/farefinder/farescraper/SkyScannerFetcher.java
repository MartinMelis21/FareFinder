package com.martinmelis.web.farefinder.farescraper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.martinmelis.web.farefinder.databaseHandler.DatabaseHandler;
import com.martinmelis.web.farefinder.databaseHandler.DatabaseQueries;

import dataTypes.RoundTripFare;

public class SkyScannerFetcher extends FareFetcher {
	
	DatabaseHandler databaseHandler;
	
	private ArrayList <String> accountedFares;
	private final static String SkyScannerAPIKey = "er894518990376063234868271076630";
	

	public SkyScannerFetcher(DatabaseHandler databaseHandler) {
		this.databaseHandler = databaseHandler;
		accountedFares = new ArrayList <String> ();
	}
	
	public ArrayList <RoundTripFare> getFareList (String origin)
	{
		ArrayList<RoundTripFare> fares = new ArrayList<RoundTripFare>();
		try 
			{
				createMapping();
				String fetchedFares = fetchFaresSS(origin);	
			
         DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
         DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
         Document doc = dBuilder.parse(new InputSource(new StringReader(fetchedFares)));
         doc.getDocumentElement().normalize();
         
         
         NodeList locationList = doc.getElementsByTagName("PlaceDto");
         NodeList quoteList = doc.getElementsByTagName("QuoteDto");
         Integer originID;
      	 Integer destinationID;

         
     	for (int temp = 0; temp < quoteList.getLength(); temp++) 
     	{
     		Node nNode = quoteList.item(temp);
     				
     		if (nNode.getNodeType() == Node.ELEMENT_NODE) 
     		{
     			Element eElement = (Element) nNode;     			
     			int price = Integer.parseInt(eElement.getElementsByTagName("MinPrice").item(0).getTextContent());
     			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
     			String outboundDateString = ((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("DepartureDate").item(0).getTextContent();
     			String inboundDateString = ((Element) eElement.getElementsByTagName("InboundLeg").item(0)).getElementsByTagName("DepartureDate").item(0).getTextContent();
     			
     			Date outboundDate = 	df.parse(outboundDateString);
     			Date inboundDate = 		df.parse(inboundDateString);
     			 
     			
     			int originSSID =		Integer.parseInt(((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("OriginId").item(0).getTextContent());
     			int destinationSSID = 	Integer.parseInt(((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("DestinationId").item(0).getTextContent());
     			
     			
     			if ((originID = databaseHandler.getCachingLists().getSkyScannerIDMapping().get(originSSID)) == null)
     				// I update SSID to database
     			{
     				originID = databaseHandler.updateSSID(locationList, originSSID);
     				databaseHandler.getCachingLists().getSkyScannerIDMapping().put(originSSID, originID);
     			}
     			if ((destinationID = databaseHandler.getCachingLists().getSkyScannerIDMapping().get(destinationSSID)) == null)
     			{
     				// I update SSID to databse
     				destinationID = databaseHandler.updateSSID(locationList, destinationSSID);  
     				databaseHandler.getCachingLists().getSkyScannerIDMapping().put(destinationSSID, destinationID);
     			}
     			
     			RoundTripFare fare = 	databaseHandler.getRoundTripFare (originID,destinationID,price,outboundDate,inboundDate);
     			fare.setBookingURL("http://www.skyscanner.com");
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
			 		System.out.println("Skyscanner fetcher failed");
					e.printStackTrace();
				}			
		return fares;
		
	}
	
	public void createMapping() throws SQLException, XPathExpressionException, IOException, ParserConfigurationException, SAXException
	{ 	
		PreparedStatement ps = null;
		ResultSet resultSet = null;
					
		
		//first I collect mapping between SSID and Our Airport ID
		if (databaseHandler.getCachingLists().getSkyScannerIDMapping().isEmpty())
		{			
			ps = databaseHandler.getDatabaseConnection().prepareStatement(DatabaseQueries.getSSIDMapping);
			resultSet = ps.executeQuery();
			
			Integer AirportID = null;
			Integer SSID = null;
			
			if (resultSet.next())
			{
				AirportID =					resultSet.getInt(1); 
     			SSID =						resultSet.getInt(2);
				if (SSID != 0)
				{
					databaseHandler.getCachingLists().getSkyScannerIDMapping().put(SSID, AirportID);
				}
			}
			
		}
	}
	
	public String fetchFaresSS(String origin) throws Exception {	
		//-------Skyscanner API URL------
		String skyScannerUrl = "http://partners.api.skyscanner.net/apiservices/browsequotes/v1.0/SK/EUR/en-US/"+origin+"/anywhere/anytime/anytime?apiKey=prtl6749387986743898559646983194";
		String response = getRequest (skyScannerUrl);
		return response.toString();
	}
 	
	public String getRequest (String ssURL) throws IOException
	{
		URL skyScannerObj = new URL(ssURL);
		HttpURLConnection skyScannerCon = (HttpURLConnection) skyScannerObj.openConnection();
		skyScannerCon.setRequestMethod("GET");
		skyScannerCon.setRequestProperty("User-Agent", "Mozilla/5.0");
		skyScannerCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		skyScannerCon.setRequestProperty("Accept", "application/xml");
		skyScannerCon.setReadTimeout(0);
		skyScannerCon.setConnectTimeout(0);
		skyScannerCon.connect();
		
		//reading the response
		BufferedReader in = new BufferedReader(new InputStreamReader(skyScannerCon.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
				
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		skyScannerCon.disconnect();
		
		if (response.toString().equals(""))
			System.out.print("");
		
		return response.toString();
	}

	public Integer getLivePrice(RoundTripFare fare) throws Exception {
 		
 		String fareString = fare.getOrigin().getAirportID() + fare.getDestination().getAirportID() + fare.getOutboundLeg().toString() + fare.getInboundLeg().toString();
 		
 		if (!accountedFares.contains(fareString))
 		{
 			accountedFares.add(fareString);
 		}
 		else
 		{
 			System.out.print("");
 		}
		
 		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		//setting up parameters
 		Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("apiKey", SkyScannerAPIKey);
        params.put("country", "DE");
        params.put("currency", "EUR");
        params.put("locale", "en-US");
        params.put("originplace", fare.getOrigin().getIataFaa());
        params.put("destinationplace", fare.getDestination().getIataFaa());
        params.put("outbounddate", dateFormat.format(fare.getOutboundLeg()));
        params.put("inbounddate", dateFormat.format(fare.getInboundLeg()));
        params.put("locationschema", "iata");
        params.put("cabinclass", "Economy");
        params.put("adults", "1");
        params.put("children", "0");
        params.put("infants", "0");
        params.put("groupPricing", "true");
        
        StringBuilder postData = new StringBuilder();
        
        for (Map.Entry<String,String> param : params.entrySet()) {
            if (postData.length() != 0) postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }
        
        byte[] postDataBytes = postData.toString().getBytes("UTF-8");
        String sessionKey = skyScannerGetSessionKey ("http://partners.api.skyscanner.net/apiservices/pricing/v1.0?", postDataBytes);
 		
      //wait untill Skyscanner provides URL for session        
        Node nNode = null;
        String status = "UpdatesPending";
        Document doc = null;
        String itineraries = null;
        Thread.sleep(1000);
        int counter = 0;
        
      //TODO-------Polling via HTTP get------
        
        while (status.equals("UpdatesPending"))
        {
        counter++;
        itineraries = getRequest (sessionKey +"?apiKey=" + SkyScannerAPIKey);
        
       // if (!reply.equals(""))
        //{
        //	skipStatus = false;
        //	itineraries = reply;
        //}
        //else
        //{
        //	skipStatus = true;
        	if (itineraries.equals(""))
        		return null;
        //}
        
        //We parse the price of the cheapest and compare if it is what we expect
        
        	
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            StringReader sr = new StringReader(itineraries);
            InputSource is = new InputSource(sr);
            try{
            doc = dBuilder.parse(is);
            }
            catch(Exception e)
            {
            	System.out.print("");
            }
            
            status = doc.getElementsByTagName("Status").item(0).getTextContent();
            
            if (status.equals("UpdatesPending"))
            	System.out.print("");
        }
            
            
            NodeList itinerariesList = doc.getElementsByTagName("ItineraryApiDto");
            
            //in case provided session key doesnt work
            if (itinerariesList == null)
            	return null;
            
            //I take the first one since the list is ordered
        	nNode = itinerariesList.item(0);
        	        
    	if (nNode == null)
        	return null;
    	
    	Double itineraryPrice = null;		
     	String bookingURL = null;
     	
    	
    	//NUll pointer exception
        		if (nNode.getNodeType() == Node.ELEMENT_NODE) 
        		{
        			Element eElement = (Element) nNode;
        			itineraryPrice =	Double.parseDouble(((Element) eElement.getElementsByTagName("PricingOptionApiDto").item(0)).getElementsByTagName("Price").item(0).getTextContent());
         			bookingURL = 	((Element) eElement.getElementsByTagName("PricingOptionApiDto").item(0)).getElementsByTagName("DeeplinkUrl").item(0).getTextContent();	
        		}
        
				if (itineraryPrice != null && (itineraryPrice < fare.getPrice() || itineraryPrice < (fare.getPrice()*1.1)))    
				{
					fare.setBookingURL(bookingURL);
					return itineraryPrice.intValue();
				}
				else
					return null;
	}
	
	public String skyScannerGetSessionKey (String ssURL, byte[] parameters) throws IOException
 	{
 		URL url = new URL(ssURL);
 		HttpURLConnection skyScannerCon = (HttpURLConnection)url.openConnection();
 		skyScannerCon.setRequestMethod("POST");
        skyScannerCon.setRequestProperty("User-Agent", "Mozilla/5.0");
		skyScannerCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		skyScannerCon.setRequestProperty("Accept", "application/xml");		
		skyScannerCon.setRequestProperty("Content-Length", String.valueOf(parameters.length));
		skyScannerCon.setDoOutput(true);
		skyScannerCon.getOutputStream().write(parameters);
		skyScannerCon.connect();
		String sessionString = skyScannerCon.getHeaderField("Location");

		skyScannerCon.disconnect();
		return sessionString;
 	}
}
