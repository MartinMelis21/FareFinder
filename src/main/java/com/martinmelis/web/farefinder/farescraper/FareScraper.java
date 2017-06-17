package com.martinmelis.web.farefinder.farescraper;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.martinmelis.web.farefinder.databaseHandler.DatabaseHandler;
import com.martinmelis.web.farefinder.modules.MailSender;
import com.martinmelis.web.farefinder.publisher.Publisher;

import dataTypes.AirportStructure;
import dataTypes.InterregionalFare;
import dataTypes.RoundTripFare;

import java.io.*;



public class FareScraper {
	
	//-----global variables-----
	public final static long MILLIS_PER_DAY = 24 * 60 * 60 * 1000L;
	public final static String SkyScannerAPIKey = "er894518990376063234868271076630";
	
	BufferedReader in;
	private final String USER_AGENT = "Mozilla/5.0";
	private ArrayList <FareFetcher> fareFetcherList;
	
	StringBuffer finalResponse;

	
	private Connection conn = null;
	private MailSender mailSender = null;
	private Publisher portalPublisher = null;

	//-----inicialization-----
	
	public FareScraper() throws IOException {
			DateTimeZone zone = DateTimeZone.forID("Europe/Bratislava");
			DateTime dt = new DateTime(zone);
			DateTimeFormatter fmt = DateTimeFormat.forPattern("HH:mm dd.MM.yyyy");
			mailSender = new MailSender ();
			portalPublisher = new Publisher();
			
			
 		System.out.println("Last fares update:\t" + fmt.print(dt));	
		finalResponse = new StringBuffer("Last fares update:\t" +fmt.print(dt));
		finalResponse.append("\n\nFares:\n");
	}	
		
	public AirportStructure accountNewAirport (String iataCode) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException
	{
		//TODO needs to be attached to Database
		
		
		String sourceUrl = "http://www.gcmap.com/airport/" + iataCode;
		
		URL distanceObj = new URL(sourceUrl);
		HttpURLConnection distanceCon = (HttpURLConnection) distanceObj.openConnection();

		// optional default is GET
		distanceCon.setRequestMethod("GET");
		distanceCon.setRequestProperty("User-Agent", USER_AGENT);
		distanceCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		distanceCon.setRequestProperty("Accept", "application/xml");
		distanceCon.connect();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(distanceCon.getInputStream(), StandardCharsets.ISO_8859_1));
		String inputLine;
		
		Double latitude = null;
		Double longtitude = null;
		Double altitude = null;
		
		String name = null;
		String country = null;
		String city = null;
		String ICAO = null;
		
		Integer zone = null;

		while ((inputLine = in.readLine()) != null) {
				String patternName = "<tr valign=top><td>Name:</td><td colspan=2 class=\"fn org\">(.*?)</td></tr>";
				String patternCity = "<span class=\"locality\">(.*?)</span>";
				String patternCountry = "<span class=\"country-name\">(.*?)</span>";
				String patternICAO = "<tr valign=top><td nowrap>ICAO:</td><td colspan=2 nowrap><a href=\"/mapui\\?P=(.*?)\"";
				String patternLatitude = "class=\"latitude\" title=\"(.*?)\"";
				String patternLongtitude = "class=\"longitude\" title=\"(.*?)\"";
				String patternAltitude = "Elevation:</td><td colspan=2 nowrap>(.*?) ft";
			    // Create a Pattern object
				Pattern r = null;
				Matcher m = null;
				
			    r = Pattern.compile(patternName);
			    m = r.matcher(inputLine);
			    //TODO need to fix UTF
			      if (m.find())
			          name = m.group(1);
			      
			    r = Pattern.compile(patternCity);
				m = r.matcher(inputLine);
				  if (m.find( ))
				      city = m.group(1);
				      
				r = Pattern.compile(patternCountry);
				m = r.matcher(inputLine);
				  if (m.find( ))
				      country = m.group(1);
				  
				r = Pattern.compile(patternICAO);
				m = r.matcher(inputLine);
				  if (m.find())
				      ICAO = m.group(1);
				      
				r = Pattern.compile(patternLatitude);
				m = r.matcher(inputLine);
				  if (m.find( ))
				      latitude = Double.parseDouble(m.group(1));
					      
				r = Pattern.compile(patternLongtitude);
				m = r.matcher(inputLine);
				  if (m.find( ))
				      longtitude = Double.parseDouble(m.group(1));
				  
				r = Pattern.compile(patternAltitude);
				m = r.matcher(inputLine);
				  if (m.find( ))
				      altitude = Double.parseDouble(m.group(1));
		}
		in.close();	
		distanceCon.disconnect();
		
	if (name == null && city != null)
		name=city;
	
	if (name != null && latitude != null && longtitude != null)
		return new AirportStructure(name, city, country, latitude, longtitude, null, iataCode, altitude, ICAO, zone,null);
	else
		return null;
	}
	
	public void accountRegionalFares (ArrayList<RoundTripFare> allFares, HashMap <String, InterregionalFare> interregionalFares, DatabaseHandler databaseHandler) throws SQLException
	{
		
	for (RoundTripFare roundTripFare : allFares)
		{
		//may get nullpointer for example if newly accounted airport is not classified
			Integer originRegion = roundTripFare.getOrigin().getZone();
			Integer destinationRegion = roundTripFare.getDestination().getZone();
			
			//String concat of Zone codes used for hashing...can be problem if zone codes change
			String key = originRegion.toString() + destinationRegion.toString();
			InterregionalFare interregionalFare;
			
			if ((interregionalFare = interregionalFares.get(key)) != null )
			{
				int numberOfAccountedPrices = interregionalFare.getNumberOfAccountedPrices();
				Double averagePrice = interregionalFare.getAveragePrice();
				Double newAveragePrice = (numberOfAccountedPrices*averagePrice+roundTripFare.getPrice())/(numberOfAccountedPrices+1);
				
				if (averagePrice !=newAveragePrice)
				{
				interregionalFare.addNewAccountedPrice();
				interregionalFare.setAveragePrice (newAveragePrice);
				interregionalFare.setLastAccountedPrice(roundTripFare.getPrice());
				interregionalFare.requiresDatabaseUpdate();
				}
			}
			else
			{
				
				int numberOfAccountedPrices = 0;
				Double averagePrice = 0.0;
				Double newAveragePrice = new Double (roundTripFare.getPrice());
				
				interregionalFare = new InterregionalFare (originRegion, destinationRegion,newAveragePrice, 1, roundTripFare.getPrice());
				interregionalFare.requiresDatabaseUpdate();				
				interregionalFares.put(key, interregionalFare);
			}
			
		}
	
	databaseHandler.updateInterregionalFares (interregionalFares);
		
	}
	
	public ArrayList<RoundTripFare> filterFares(ArrayList<RoundTripFare> fares)
	{
		ArrayList<RoundTripFare> filteredFares = new ArrayList<RoundTripFare> ();
		ArrayList<Integer> skipIndexes = new ArrayList<Integer>();
		int outterIndex, innerIndex;
		RoundTripFare outterPivotFare = null;
		RoundTripFare innerPivotFare = null;
		RoundTripFare cheapestFare = null;
		
		for (outterIndex = 0;outterIndex < (fares.size());outterIndex++)
		{
			if (skipIndexes.contains(outterIndex))
				continue;
			
			outterPivotFare = fares.get(outterIndex);
			
			cheapestFare = outterPivotFare;
			for (innerIndex = (outterIndex+1);innerIndex < fares.size();innerIndex++)
			{
				if (skipIndexes.contains(innerIndex))
					continue;
				
				innerPivotFare = fares.get(innerIndex);
							
				if (innerPivotFare.getOrigin().equals(outterPivotFare.getOrigin()) && innerPivotFare.getDestination().equals(outterPivotFare.getDestination()))
				{
					skipIndexes.add(innerIndex);
					if (innerPivotFare.getPrice() < cheapestFare.getPrice())
					{
						cheapestFare = 	innerPivotFare;
					}
				}
				
				
			}
			filteredFares.add(cheapestFare);
		}
		
		return filteredFares;
	}

	public String getFaresString (ArrayList <String> countryList, DatabaseHandler databaseHandler) throws Exception
	{
		ArrayList<RoundTripFare> results = getFares(countryList, databaseHandler);
		
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy");
		Calendar cal = Calendar.getInstance();		
		String faresDescription = "Last Fares update \t\t" + dateFormat.format(cal.getTime()) +"\n";
		
		
		ListIterator <RoundTripFare> li = results.listIterator(results.size());
		// Iterate in reverse.
		RoundTripFare fare = null;
		
		while(li.hasPrevious()) {
		  fare = li.previous();
 			faresDescription += (("Outbound Leg\n\tFrom : " + fare.getOrigin().getCityName() + " to " + fare.getDestination().getCityName() ) + "\n");
 			faresDescription += ("\tDate : " + fare.getOutboundLeg().toString() + "\n");
 			faresDescription += (("Inbound Leg\n\tFrom : " + fare.getDestination().getCityName()  + " to " + fare.getOrigin().getCityName() ) + "\n");
 			faresDescription += ("\tDate : " + fare.getInboundLeg().toString() + "\n");
 			faresDescription += (("Price : " + fare.getPrice()) + "\n");
 			faresDescription += ("Average price on this route : " + fare.getBaseFare() + "\n");
 			faresDescription += (("Sale : " + fare.getSaleRatio()) + "\n");
 			faresDescription += (("DealRatio : " + fare.getDealRatio()) + "\n"+ "\n");
 			faresDescription += (("Booking URL : " + fare.getBookingURL()) + "\n"+ "\n");
 			finalResponse.append(faresDescription);
		}
		
	return 	faresDescription;
	}
	//-----Fetching the Fares Data and Quote Metadata-----

	public static ArrayList<RoundTripFare> sortFares(ArrayList<RoundTripFare> fares) {
		
		ArrayList<RoundTripFare> sortedFares = fares;
		
		Collections.sort(sortedFares, 
		    new Comparator<RoundTripFare>() {
		        @Override
		        public int compare(RoundTripFare e1, RoundTripFare e2) {
		            return ((Double)e2.getSaleRatio()).compareTo(((Double)e1.getSaleRatio()));
		        }
		    }
		);
		
		return sortedFares;
		}
	
	public ArrayList<RoundTripFare> getFares(ArrayList <String> countryList, DatabaseHandler databaseHandler) throws Exception {
		
		
		//-----------------TODO new architecture-------------------
		fareFetcherList = new ArrayList <FareFetcher> ();
		FareFetcher skyScannerFetcher = new SkyScannerFetcher (databaseHandler);
		FareFetcher kiwiFetcher = new KiwiFetcher (databaseHandler);
		FareFetcher kayakFetcher = new KayakFetcher (databaseHandler);
		
		fareFetcherList.add(skyScannerFetcher);
		fareFetcherList.add(kiwiFetcher);
		fareFetcherList.add(kayakFetcher);
		
		//--------------------------------------------------------
		
		ArrayList<RoundTripFare> filteredFares = new ArrayList<RoundTripFare> ();
		ArrayList<RoundTripFare> resultFares = new ArrayList<RoundTripFare> ();
		ArrayList<RoundTripFare> allFares= new ArrayList <RoundTripFare> ();
		HashMap <String, InterregionalFare> interregionalFares = databaseHandler.getInterregionalFares ();
				
		for (String origin: countryList)
		{
			ArrayList<RoundTripFare> fares= new ArrayList <RoundTripFare> ();
			
			for (FareFetcher fareFetcher : fareFetcherList)
			{
				fares.addAll(fareFetcher.getFareList(origin));
				allFares.addAll(fares);
			}
	        
						
			//-----------------------------------------------------
		filteredFares = filterFares(fares);
			
			//TODO I also need to set all fares, which are not in current Deals list to be set to isPublished = 0
			
		
		
		for (int temp = 0; temp < filteredFares.size(); temp++) 
 		{
			RoundTripFare fare = filteredFares.get(temp);
			

			//I check if fare is interesting
				if (!fare.isInteresting()){
				//If it is not interesting anymore we check if it is published
					if (fare.getPortalPostID() != null &&  fare.getPortalPostID() != -1){
						//If it is not interesting and published we set fare to Expired and published to false
						fare.expireFarePublication(portalPublisher, databaseHandler);
					}
						//If it is not interesting and not published we simply skip
				}
				else
				{
					resultFares.add(fare);
					//we check for live price and if we were unable to get one, we skip
					//TODO maybe if it is published we should expire
					if (fare.fetchLivePrice(fareFetcherList,fare)==null)
						continue;
					
					
				//If it is interesting we check if it is published
					if (fare.getPortalPostID()!= -1){
						//If it is interesting and published we check if the price change is significant
						Double priceChange = (fare.getLastAccountedPrice()-fare.getPrice())/Double.parseDouble(fare.getLastAccountedPrice().toString());
						
						//TODO also if lastaccountedpricetime is old enough
						
						if (priceChange >= 1.2)
						{
								//If price changed more than 20percent we get the live price
								
								if (fare.isInteresting())
								{
									//If live price is still interesting i set fare to updated with new live price
									fare.notifyAboutFare(mailSender);
									fare.updateFarePublication(portalPublisher, databaseHandler);
								}
								else{
									//If live price is not interesting anymore I set fare to Expired and isPublished to false
									fare.expireFarePublication(portalPublisher, databaseHandler);
								}
						}
								//If price changed less than 20percent we leave as as is and dont check for live price
						
					}
					else{
						//If it is interesting but not published we get the live price, check again if it is interesting and
						
						if (fare.isInteresting())
						{
							//If live price is interesting i set fare to New with new live price
							fare.notifyAboutFare(mailSender);
							fare.publishFare (portalPublisher, databaseHandler);
						}
							//If live price is not interesting nor published, we skip
					}
				}
			
			

 		}
		
		
		
		
		
		
		
		
		
		
//	     	for (int temp = 0; temp < filteredFares.size(); temp++) 
//	     		{
//	     		
//	     			RoundTripFare fare = filteredFares.get(temp);
//	     			
//	     			
//	     			//--------------for FareFinder output------------------
//	     			if (fare.isInteresting()){	
//	     				resultFares.add(fare);
//     				}	    			
//	    			//-----------------------------------------------------
//	    			
//	    			
//	    			
//	     			
//	     			//check if fare is published	     			
//	     			//fare is published
//	     			if (fare.getPortalPostStatus() !=null)
//	     				{
//		     				
//		     				// fare is published and interesting...to save on calls we only call those we expect to be good
//		     				if (fare.getOrigin().getZone()/10 != fare.getDestination().getZone()/10  && fare.getPrice() <= 300 && fare.getSaleRatio() >= 30 )
//		     				{		  
//		     					Integer livePrice = fetchLivePrice (fareFetcherList,fare);  
//		     					
//		     					// We were unable to register updated fare price or the live gathered price is not interesting
//		    	     			if (livePrice != null && livePrice < 300 && ((fare.getBaseFare()* 0.7)/livePrice >= 1 ))
//		    	     			{
//		    	     			
//		    	     			///-------this means we have live price and live price is considered a deal----
//		    	     			
//		     					//If is currently published - Active/[Updated]
//			     					if (fare.getPortalPostStatus().equals("active") || fare.getPortalPostStatus().equals ("updated"))
//			     					{
//			     						if (fare.getLastAccountedPrice() != livePrice)
//			     						{
//				     						//If price has changes dramatically - set published - Updated
//				     						try{
//				     							portalPublisher.updateFareOnPortal(fare,"updated");
//				     							fare.setPortalPostStatus("updated");
//				     							databaseHandler.updateFarePublication (fare);
//				     						}
//				     						catch (Exception e)
//				     						{}		     					
//			     						}
//			     					}
//			     					//If is currently published - [Expired]
//			     					else
//			     					{
//			     							//we create new post
//			     							try
//			    	     					{
//			     								portalPublisher.updateFareOnPortal(fare,"active");
//				     							fare.setPortalPostStatus("active");
//				     							databaseHandler.updateFarePublication (fare);		    		     					
//			    		     					
//			    	     					}
//			    	     					catch (Exception e)
//			    	     					{}			     						
//			     					}
//			     					// new fare is posted on portal
//			     					
//			     					if (fare.getIsNew())
//			     						databaseHandler.insertDatabaseFare (fare);
//					     			else
//					     				databaseHandler.updateDatabaseFare(fare);
//		    	     			}
//		    	     			else
//		    	     			{
//		    	     				// fare is updated but not posted to portal
//		    	     				
//			     					if (fare.getIsNew())
//			     						databaseHandler.insertDatabaseFare (fare);
//					     			else
//					     				databaseHandler.updateDatabaseFare(fare);
//		    	     			}
//		     				}	
//		     				//fare is not interesting but is published ... we set to expired
//		     				else
//		     				{	
//		     					
//		     					try
//		     					{
//		     						portalPublisher.updateFareOnPortal(fare,"expired");
//	     							fare.setPortalPostStatus("expired");
//	     							databaseHandler.updateFarePublication (fare);
//		     					}
//		     					catch (Exception e)
//		     					{}
//		     						     						
//	     						if (fare.getIsNew())
//	     							databaseHandler.insertDatabaseFare (fare);
//				     			else
//				     				databaseHandler.updateDatabaseFare(fare);
//		     				}
//	     				}
//	     			//fare is not published
//	     			else
//	     			{
//    	     			
//	     				//is interesting
//	     				if (fare.getOrigin().getZone()/10 != fare.getDestination().getZone()/10  && fare.getPrice() <= 300 && fare.getSaleRatio() >= 30)
//	     				{
//	     					
//	     					Integer livePrice = fetchLivePrice (fareFetcherList,fare);
//	    	     			
//	    	     			// We were unable to register updated fare price
//	    	     			if (livePrice != null && livePrice < 300 && ((fare.getBaseFare()* 0.7)/livePrice >= 1 ))
//	    	     			{
//		     					try
//		     					{
//		     						fare.setPortalPostStatus("active");
//		     						portalPublisher.publishFareToPortal(fare);
//		     						databaseHandler.updateFarePublication (fare);
//		     					}
//		     					catch (Exception e)
//		     					{}
//	    	     			   					
//
//		     					//--------------------notification----------------
//			     				
//			     				
//			     				//I check lastFareNotification
//			     				Date lastAccountedDate = fare.getLastFareNotification();
//			     				DateTime lastFareNotification = new DateTime (lastAccountedDate);
//			     				//If it is more than a day or is better than 20% of previously announced
//			     				DateTime lastWeek = DateTime.now().minusDays(7);
//			     				
//			     				//TODO publishing live needs to be redone
//				     			//portalPublisher.publishFareToPortal(fare);
//			     				
//			     				if (lastAccountedDate == null || (lastFareNotification.getMillis() < lastWeek.getMillis()))
//			     				{	//TODO or 20% better
//			     					mailSender.sendMail("martin.melis21@gmail.com", fare);//TODO send to all mail reciepts
//			     					
//			     				}
//	    	     			}
//			     			//--------------------------------------
//	    	     			
//	    	     			if (fare.getIsNew())
//	    	     				databaseHandler.insertDatabaseFare (fare);
//			     			else
//			     				databaseHandler.updateDatabaseFare(fare);
//	     				}	
//	     				else
//	     				{
//		     				if (fare.getIsNew())
//		     					databaseHandler.insertDatabaseFare (fare);
//			     			else
//			     				databaseHandler.updateDatabaseFare(fare);
//	     				}
//	     			}	
//	     			
//	     		}
	         }		
		accountRegionalFares(allFares,interregionalFares,databaseHandler);
		
		//we need to check all published fares, that were not accounted in resultFares
		//TODO analyzeResidualFares(resultFares);
		return sortFares(resultFares);
	}			
		/*
	public void analyzeResidualFares (ArrayList<RoundTripFare> filteredFares, DatabaseHandler databaseHandler) throws Exception 
	{ 				
			ArrayList <RoundTripFare> residualFares = new ArrayList <RoundTripFare> ();
			final PreparedStatement ps = conn.prepareStatement(getResidualFares);
			final ResultSet resultSet = ps.executeQuery();
			
			//database columns
			Integer price = 					null;
			Integer portalPostID =				null;
			String portalPostStatus =			null;
			Date outbound = 					null;
			Date inbound =						null;
			AirportStructure origin =			null;
			AirportStructure destination =		null;
			
			Integer originAirportID = 			null;
			String originAirportName = 			null;
 			String originCityName = 			null;
 			String originCountry = 				null;
 			Double originLatitude = 			null;
 			Double originLongtitude = 			null;
 			Double originAltitude = 			null;
 			String originIcao=					null;
 			String originIataFaa=				null;
 			Integer originZone = 				null;
 			Integer originSSID = 				null;
 			
 			Integer destinationAirportID = 		null;
 			String destinationAirportName = 	null;
 			String destinationCityName = 		null;
 			String destinationCountry = 		null;
 			Double destinationLatitude = 		null;
 			Double destinationLongtitude = 		null;
 			Double destinationAltitude = 		null;
 			String destinationIcao=				null;
 			String destinationIataFaa=			null;
 			Integer destinationZone = 			null;
 			Integer destinationSSID = 			null;
 			Integer destinationId =				null;
 			
 			Double baseFare =					null;
 			Integer numberOfAccountedFares =	null;		
 			Date lastFareNotification =			null;
			
			while (resultSet.next())
			{	 
				originAirportID = 						resultSet.getInt(1);			
				destinationAirportID = 					resultSet.getInt(2);
				price = 								resultSet.getInt(3);
				
				outbound = 								resultSet.getDate(4);
				inbound =								resultSet.getDate(5);
				
				portalPostID =							resultSet.getInt(6);
				portalPostStatus =						resultSet.getString(7);
				
				originAirportName = 					resultSet.getString(8);
				originCityName = 						resultSet.getString(9);
				originCountry = 						resultSet.getString(10);
				originLatitude = 						resultSet.getDouble(11);
				originLongtitude = 						resultSet.getDouble(12);
				originIataFaa=							resultSet.getString(13);
				originAltitude = 						resultSet.getDouble(14);
				originIcao=								resultSet.getString(15);
				originZone = 							resultSet.getInt(16);
				originSSID = 							resultSet.getInt(17);
				
				destinationAirportName = 				resultSet.getString(18);
				destinationCityName = 					resultSet.getString(19);
				destinationCountry = 					resultSet.getString(20);
				destinationLatitude = 					resultSet.getDouble(21);
				destinationLongtitude = 				resultSet.getDouble(22);
				destinationIataFaa=						resultSet.getString(23);
				destinationAltitude = 					resultSet.getDouble(24);
				destinationIcao=						resultSet.getString(25);
				destinationZone = 						resultSet.getInt(26);
				destinationSSID = 						resultSet.getInt(27);				
			
				baseFare =								resultSet.getDouble(28);
				numberOfAccountedFares =				resultSet.getInt(29);
				lastFareNotification =					resultSet.getDate(30);
				
				
				if (locationDictionary.containsKey(originAirportID))
				{
					origin =  locationDictionary.get(originAirportID);

				} 
				else
				{
					origin = new AirportStructure(originAirportName,originCityName,originCountry,originLatitude,originLongtitude,originSSID,originIataFaa, originAltitude, originIcao,originZone,originAirportID);
					locationDictionary.put(origin.getAirportID(), origin);
				}
				
				if (locationDictionary.containsKey(destinationAirportID))
				{
					destination =  locationDictionary.get(destinationAirportID);

				} 
				else
				{
		 			destination = new AirportStructure(destinationAirportName,destinationCityName,destinationCountry,destinationLatitude,destinationLongtitude,destinationSSID,destinationIataFaa, destinationAltitude, destinationIcao,destinationZone,destinationAirportID);
		 			locationDictionary.put(destination.getAirportID(), destination);
				}
				
				RoundTripFare residualFare = new RoundTripFare (origin, destination, price, outbound, inbound, baseFare, numberOfAccountedFares, lastFareNotification,portalPostID,portalPostStatus);		
				residualFares.add(residualFare);
				
				}
				resultSet.close();
				
				for (RoundTripFare fare : residualFares)
				{
				
					for (RoundTripFare filteredFare : filteredFares)
					{
						SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
						
						if (filteredFare.getOrigin().getAirportID() == fare.getOrigin().getAirportID() && filteredFare.getDestination().getAirportID() == fare.getDestination().getAirportID() && filteredFare.getPrice() == fare.getPrice() && fmt.format(fare.getOutboundLeg()).equals(fmt.format(filteredFare.getOutboundLeg())) && fmt.format(fare.getInboundLeg()).equals(fmt.format(filteredFare.getInboundLeg()))  )
						{
							continue;
						}
	
					
					Integer livePrice = fetchLivePrice (fareFetcherList,fare);
					
					if (livePrice == null || livePrice > 300 || fare.getSaleRatio() < 30)
	 				{	
						try
	 					{
	 						fare.setPortalPostStatus("expired");
	     					portalPublisher.updateFareOnPortal(fare, "expired");     					
	     					databaseHandler.updateFarePublication (fare);
	 					}
	 					catch (Exception e)
	 					{}
	 				}
					else
					{
						if(fare.getPortalPostStatus().equals("active"))
						{
							if (livePrice != fare.getPrice())
							{
								try
			 					{
			 						fare.setPortalPostStatus("updated");
			     					portalPublisher.updateFareOnPortal(fare, "updated");     					
			     					databaseHandler.updateFarePublication (fare);
			 					}
			 					catch (Exception e)
			 					{}
							}
						}	
					}
					databaseHandler.updateDatabaseFare(fare);
					}
				}
			
	}
		*/
}