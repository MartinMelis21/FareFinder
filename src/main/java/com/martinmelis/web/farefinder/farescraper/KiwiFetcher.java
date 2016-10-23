package com.martinmelis.web.farefinder.farescraper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONObject;
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

import dataTypes.AirportStructure;
import dataTypes.RoundTripFare;

public class KiwiFetcher extends FareFetcher {
	private DatabaseHandler databaseHandler;
	
	public KiwiFetcher(DatabaseHandler databaseHandler) {
		this.databaseHandler=databaseHandler;
	}

	public ArrayList <RoundTripFare> getFareList (String origin)
	{
		ArrayList<RoundTripFare> fares = new ArrayList<RoundTripFare>();
		String fetchURL = "https://api.skypicker.com/flights?flyFrom="+origin+"&typeFlight=return&xml=1&oneforcity=1&limit=200";
		
		 try {
		 creatMapping ();
		 String fetchedFares = getRequest(fetchURL);			
         DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
         DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
         Document doc = dBuilder.parse(new InputSource(new StringReader(fetchedFares)));
         doc.getDocumentElement().normalize();
         
         
         NodeList quoteList = doc.getElementsByTagName("data");
         Integer originID = null;
         Integer destinationID = null;
         
     	for (int temp = 0; temp < quoteList.getLength(); temp++) 
     	{
     		Node nNode = quoteList.item(temp);
     				
     		if (nNode.getNodeType() == Node.ELEMENT_NODE) 
     		{
     			Element eElement = (Element) nNode;
     			//TODO Null pointer exception
     			int price = Integer.parseInt(eElement.getElementsByTagName("price").item(0).getTextContent());
     			String originIata = eElement.getElementsByTagName("flyFrom").item(0).getTextContent();
     			String destinationIata = eElement.getElementsByTagName("flyTo").item(0).getTextContent();
     			String bookingToken = eElement.getElementsByTagName("booking_token").item(0).getTextContent();
     			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");     			
     			
     			String outboundDateString = ((Element) eElement.getElementsByTagName("route").item(0)).getElementsByTagName("aTime").item(0).getTextContent();
     			String inboundDateString =  ((Element) eElement.getElementsByTagName("route").item(1)).getElementsByTagName("aTime").item(0).getTextContent();
     			
     			Date outboundDate = 	df.parse(outboundDateString);
     			Date inboundDate = 		df.parse(inboundDateString);
     			
     			
     			if ((originID = databaseHandler.getCachingLists().getIataFaaMapping().get(originIata)) == null)
     				// I update SSID to database
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
     			fare.setBookingURL("https://www.kiwi.com/us/booking?token=" + bookingToken);
     			fares.add(fare);     			
     			
     			System.out.println("Outbound Leg\n\tFrom : " + fare.getOrigin().getCityName()  + " to " + fare.getDestination().getCityName() );
     			System.out.println("\tDate : " + fare.getOutboundLeg().toString());
     			System.out.println("Inbound Leg\n\tFrom : " + fare.getOrigin().getCityName()  + " to " + fare.getDestination().getCityName() );
     			System.out.println("\tDate : " + fare.getInboundLeg().toString());
     			System.out.println("Price : " + fare.getPrice());
     			System.out.println("Average price on this route : " + fare.getBaseFare());
     			System.out.println("Sale : " + fare.getSaleRatio());
     			System.out.println("DealRatio : " + fare.getDealRatio());
     			System.out.println("Booking URL : " + fare.getBookingURL());
     			System.out.println ();
     		}
         }
      } catch (Exception e) {
         e.printStackTrace();
      }			
		return fares;
		
	}
	
 	public String getRequest(String url) throws Exception {
 			
 			//-------Skyscanner API URL------
 			String kiwiUrl = url;
 			URL kiwiObj = new URL(kiwiUrl);
 			HttpURLConnection kiwiCon = (HttpURLConnection) kiwiObj.openConnection();
 			kiwiCon.setRequestMethod("GET");
 			kiwiCon.setRequestProperty("User-Agent", "Mozilla/5.0");
 			kiwiCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
 			kiwiCon.setRequestProperty("Accept", "application/xml");
 			kiwiCon.connect();
 			
 			
 			//reading the response
 			BufferedReader in = new BufferedReader(new InputStreamReader(kiwiCon.getInputStream()));
 			String inputLine;
 			StringBuffer response = new StringBuffer();

 			while ((inputLine = in.readLine()) != null) {
 				response.append(inputLine);
 			}
 			in.close();
 			
 			kiwiCon.disconnect();
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

	public Integer getLivePrice (RoundTripFare fare) throws Exception
	{
		//hardcoded index of booking URL start
		String bookingToken = fare.getBookingURL().substring(38);
		String checkURL = "https://booking-api.skypicker.com/api/v0.1/check_flights?v=2&booking_token=" + bookingToken + "&bnum=0&pnum=1&currency=\"EUR\"";
		String response = getRequest(checkURL);	
		
		//total
    	JSONObject obj = new JSONObject(response);
    	Integer livePrice = (int) obj.getDouble("total");
		
    	if (livePrice != null && (livePrice < fare.getPrice() || livePrice < (fare.getPrice()*1.1)))    
		{  		
    		return livePrice.intValue();
    	}
		
		return null;
		
	}
	
	
}
