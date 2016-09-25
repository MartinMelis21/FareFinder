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
import java.util.List;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.martinmelis.web.farefinder.modules.MailSender;

import dataTypes.AirportStructure;
import dataTypes.RoundTripFare;

import java.io.*;



public class FareScraper {
	
	//-----global variables-----
	public final static long MILLIS_PER_DAY = 24 * 60 * 60 * 1000L;
	
	BufferedReader in;
	private final String USER_AGENT = "Mozilla/5.0";
	private HashMap <Integer,AirportStructure> locationDictionary;
	private HashMap <Integer,Integer> skyScannerIDMapping;
	private HashMap <String,Integer> kiwiMapping;
	
	StringBuffer finalResponse;
	final String getAirportOnIDSQL = 		"SELECT a.airportName,a.airportCity,a.airportCountry,a.latitude,a.longtitude,a.iataFaa,a.altitude,a.icao,z.id, a.airportID, a.SkyScannerID from Airports a left join Countries c on a.airportCountry=c.countryName left join Zones z on c.zone=z.id WHERE airportID = ?";
	final String getAirportOnSSIDSQL = 		"SELECT a.airportName,a.airportCity,a.airportCountry,a.latitude,a.longtitude,a.iataFaa,a.altitude,a.icao,z.id, a.airportID, a.SkyScannerID from Airports a left join Countries c on a.airportCountry=c.countryName left join Zones z on c.zone=z.id WHERE SkyScannerID = ?";
	final String getAirportOnIataFaaSQL = 	"SELECT a.airportName,a.airportCity,a.airportCountry,a.latitude,a.longtitude,a.iataFaa,a.altitude,a.icao,z.id, a.airportID, a.SkyScannerID from Airports a left join Countries c on a.airportCountry=c.countryName left join Zones z on c.zone=z.id WHERE a.iataFaa = ?";
	final String getFareSQL = 				"SELECT o.airportName,o.airportCity,o.airportCountry,o.latitude,o.longtitude,o.iataFaa,o.altitude,o.icao,oc.zone,d.airportName,d.airportCity,d.airportCountry,d.latitude,d.longtitude,d.iataFaa,d.altitude,d.icao,dc.zone,f.lastAccountedPriceRoundTrip, f.numberOfAccountedPricesRoundTrip, f.averagePriceRoundTrip, o.SkyScannerID, d.SkyScannerID,  if(f.lastFareNotification = '0000-00-00', null, f.lastFareNotification) as lastFareNotification from Fares f, Airports o, Airports d, Countries oc, Countries dc where f.origin = ? and f.destination = ? and f.origin=o.airportID and f.destination=d.airportID and oc.countryName=o.airportCountry and dc.countryName=d.airportCountry";
	final String addAirportSQL = 			"INSERT INTO Airports (iataFaa, airportName, airportCity, airportCountry, latitude, longtitude, altitude, icao) values (?, ?, ?, ?, ?, ?, ?, ?)";
	final String updateSSIDSQL = 			"UPDATE Airports SET SkyScannerID = ? WHERE iataFaa = ?";
	final String checkFareExistance = 		"SELECT numberOfAccountedPricesRoundTrip, averagePriceRoundTrip FROM Fares WHERE origin = (SELECT airportID FROM Airports WHERE iataFaa = ?) AND destination = (SELECT airportID FROM Airports WHERE iataFaa = ?)";
	final String updateRoutePrice = 		"UPDATE Fares SET numberOfAccountedPricesRoundTrip = ?,lastAccountedPriceRoundTrip = ?,lastAccountedPriceTimeRoundTrip = NOW(),averagePriceRoundTrip = ?,isPublished = ? WHERE origin=(SELECT airportID FROM Airports WHERE iataFaa = ?) AND destination = (SELECT airportID FROM Airports WHERE iataFaa = ?)";
	final String insertRoutePrice = 		"INSERT INTO Fares (origin,destination,lastAccountedPriceTimeRoundTrip,lastAccountedPriceRoundTrip,numberOfAccountedPricesRoundTrip,averagePriceRoundTrip,isPublished)VALUES ((SELECT airportID FROM Airports WHERE iataFaa = ?),(SELECT airportID FROM Airports WHERE iataFaa = ?),NOW(),?,1,?,?);";
	final String getSSIDMapping =			"SELECT airportID,SkyScannerID FROM Airports";
	final String getIataMapping =			"SELECT airportID,iataFaa FROM Airports";
	final String updateFarePublishDate =	"UPDATE Fares SET lastFareNotification = NOW() WHERE origin = ? and destination = ? ";
	
	private Connection conn = null;
	private MailSender mailSender = null;

	//-----inicialization-----
	
	public FareScraper() throws IOException {
			locationDictionary = new HashMap<Integer,AirportStructure>();
			DateTimeZone zone = DateTimeZone.forID("Europe/Bratislava");
			DateTime dt = new DateTime(zone);
			DateTimeFormatter fmt = DateTimeFormat.forPattern("HH:mm dd.MM.yyyy");
			mailSender = new MailSender ();

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
	
	if (name != null && latitude != null && longtitude != null)
		return new AirportStructure(name, city, country, latitude, longtitude, null, iataCode, altitude, ICAO, zone,null);
	else
		return null;
	}
	
	public ArrayList<RoundTripFare> filterFares(ArrayList<RoundTripFare> fares)
	{
		ArrayList<RoundTripFare> filteredFares = new ArrayList<RoundTripFare> ();
		ArrayList<Integer> skipIndexes = new ArrayList<Integer>();
		int outterIndex, innerIndex;
		RoundTripFare outterPivotFare = null;
		RoundTripFare innerPivotFare = null;
		RoundTripFare cheapestFare = null;
		
		for (outterIndex = 0;outterIndex < (fares.size()-1);outterIndex++)
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

	public String getFaresString (ArrayList <String> countryList, Connection conn) throws Exception
	{
		ArrayList<RoundTripFare> results = getFares(countryList,conn);
		
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy");
		Calendar cal = Calendar.getInstance();		
		String faresDescription = "Last Fares update \t\t" + dateFormat.format(cal.getTime()) +"\n";
		
		for (RoundTripFare fare : results)
		{
 			
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
	
	public ArrayList<RoundTripFare> getFares(ArrayList <String> countryList, Connection conn) throws Exception {
		
		this.conn=conn;
		ArrayList<RoundTripFare> filteredFares = new ArrayList<RoundTripFare> ();
		ArrayList<RoundTripFare> resultFares = new ArrayList<RoundTripFare> ();
		
		for (String origin: countryList)
		{
			ArrayList<RoundTripFare> 	faresSS =		 	getFareListSS (origin);
			ArrayList<RoundTripFare> 	faresKiwi = 		getFareListKiwi (origin);
	        
			
			ArrayList<RoundTripFare> 	fares= new ArrayList <RoundTripFare> ();
			fares.addAll(faresSS);
			fares.addAll(faresKiwi);
						
			//-----------------------------------------------------
			filteredFares = filterFares(fares);
						
	     	for (int temp = 0; temp < filteredFares.size(); temp++) 
	     		{
	     		
	     			RoundTripFare fare = filteredFares.get(temp);
	     			
	     			//TODO I also need to set all fares, which are not in current Deals list to be set to isPublished = 0
	     			
	     			   		
	     			if (fare.getOrigin().getZone() == fare.getDestination().getZone()  || fare.getPrice() > 300 || fare.getSaleRatio()<30)
	     			{
	     				if (fare.getIsNew())
		     				insertDatabaseFare (fare,false);
		     			else
		     				updateDatabaseFare(fare,false);
	     			}
	     			else
	     			{
	     				if (fare.getIsNew())
		     				insertDatabaseFare (fare,true);
		     			else
		     				updateDatabaseFare(fare,true);
	     				
	     				resultFares.add(fare);
	     				
	     				
	     				//---------------notification------------
	     				
	     				
	     				//I check lastFareNotification
	     				Date lastAccountedDate = fare.getLastFareNotification();
	     				DateTime lastFareNotification = new DateTime (lastAccountedDate);
	     				//If it is more than a day or is better than 20% of previously announced
	     				DateTime yesterday = DateTime.now().minusDays(1);
	     				
	     				if (lastAccountedDate == null || (lastFareNotification.getMillis() < yesterday.getMillis()))
	     				{	//TODO or 20% better
	     					mailSender.sendMail("martin.melis21@gmail.com", fare);//TODO send to all mail reciepts
	     					
	     					//update notification time
	     					updateFarePublishDate(fare);
	     				}
		     			
		     			
		     			//--------------------------------------
		     			
	     			}
	     			
	     		}
	         }		
		
		//TODO check validity of all fares, but primarily those that are marked as published but are not in result fares
		
		return sortFares(resultFares);
	}
	
	public ArrayList <RoundTripFare> getFareListSS (String origin) throws Exception
	{
		ArrayList<RoundTripFare> fares = new ArrayList<RoundTripFare>();
		createSkyScannerMapping();
		String fetchedFares = fetchFaresSS(origin);		
	
	 try {	
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
     			//TODO problem with time conversion from format
     			DateFormat df = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss", Locale.ENGLISH);
     			String outboundDateString = ((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("DepartureDate").item(0).getTextContent().replaceAll("T"," ");
     			String inboundDateString = ((Element) eElement.getElementsByTagName("InboundLeg").item(0)).getElementsByTagName("DepartureDate").item(0).getTextContent().replaceAll("T"," ");
     			
     			Date outboundDate = 	df.parse(outboundDateString);
     			Date inboundDate = 		df.parse(inboundDateString);
     			
     			
     			int originSSID =		Integer.parseInt(((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("OriginId").item(0).getTextContent());
     			int destinationSSID = 	Integer.parseInt(((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("DestinationId").item(0).getTextContent());
     			//TODO need to get SkyScanner booking URL
     			
     			
     			if ((originID = skyScannerIDMapping.get(originSSID)) == null)
     				// I update SSID to database
     				originID = updateSSID(locationList, originSSID, conn);
     			if ((destinationID = skyScannerIDMapping.get(destinationSSID)) == null)
     				// I update SSID to databse
     				destinationID = updateSSID(locationList, destinationSSID, conn);     			
     			
     			RoundTripFare fare = 	getRoundTripFare (originID,destinationID,price,outboundDate,inboundDate);
     			fare.setBookingURL("SkyScanner");
     			fares.add(fare);
     			

     			System.out.println("Outbound Leg\n\tFrom : " + fare.getOrigin().getCityName()  + " to " + fare.getDestination().getCityName() );
     			System.out.println("\tDate : " + fare.getOutboundLeg().toString());
     			System.out.println("Inbound Leg\n\tFrom : " + fare.getDestination().getCityName()  + " to " + fare.getOrigin().getCityName() );
     			System.out.println("\tDate : " + fare.getInboundLeg().toString());
     			System.out.println("Price : " + fare.getPrice());
     			System.out.println("Average price on this route : " + fare.getBaseFare());
     			System.out.println("Sale : " + fare.getSaleRatio());
     			System.out.println("DealRatio : " + fare.getDealRatio());
     			System.out.println ();
     		}
         }
      } catch (Exception e) {
         e.printStackTrace();
      }			
		return fares;
		
	}
		
	public ArrayList <RoundTripFare> getFareListKiwi (String origin) throws Exception
	{
		ArrayList<RoundTripFare> fares = new ArrayList<RoundTripFare>();
		creatKiwiMapping ();
		String fetchedFares = fetchFaresKiwi(origin);		
	
	 try {	
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
     			int price = Integer.parseInt(eElement.getElementsByTagName("price").item(0).getTextContent());
     			String originIata = eElement.getElementsByTagName("flyFrom").item(0).getTextContent();
     			String destinationIata = eElement.getElementsByTagName("flyTo").item(0).getTextContent();
     			String bookingToken = eElement.getElementsByTagName("booking_token").item(0).getTextContent();
     			//TODO problem with time conversion from format
     			DateFormat df = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss", Locale.ENGLISH);     			
     			Date outboundDate = 	df.parse(((Element) eElement.getElementsByTagName("route").item(0)).getElementsByTagName("aTimeUTC").item(0).getTextContent());
     			Date inboundDate = 		df.parse(((Element) eElement.getElementsByTagName("route").item(1)).getElementsByTagName("aTimeUTC").item(0).getTextContent());
     			
     			if ((originID = kiwiMapping.get(originIata)) == null)
     				// I update SSID to database
     			{
     				AirportStructure newAirport = accountNewAirport(originIata.toUpperCase());
     				originID = insertAirport(newAirport, conn);
     			}
     			if ((destinationID = kiwiMapping.get(destinationIata)) == null)
     				// I update SSID to databse
     			{
     				AirportStructure newAirport = accountNewAirport(destinationIata.toUpperCase());
     				destinationID = insertAirport(newAirport, conn);
     			}
     			
     			RoundTripFare fare = getRoundTripFare (originID, destinationID, price,outboundDate,inboundDate);
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
     			System.out.println ();
     		}
         }
      } catch (Exception e) {
         e.printStackTrace();
      }			
		return fares;
		
	}
	
 	public String fetchFaresSS(String origin) throws Exception {
		
		//-------Skyscanner API URL------
		String skyScannerUrl = "http://partners.api.skyscanner.net/apiservices/browsequotes/v1.0/SK/EUR/en-US/"+origin+"/anywhere/anytime/anytime?apiKey=prtl6749387986743898559646983194";
		URL skyScannerObj = new URL(skyScannerUrl);
		HttpURLConnection skyScannerCon = (HttpURLConnection) skyScannerObj.openConnection();
		skyScannerCon.setRequestMethod("GET");
		skyScannerCon.setRequestProperty("User-Agent", USER_AGENT);
		skyScannerCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		skyScannerCon.setRequestProperty("Accept", "application/xml");
		
		
		//reading the response
		BufferedReader in = new BufferedReader(new InputStreamReader(skyScannerCon.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		skyScannerCon.disconnect();
		return response.toString();
	}

 	public String fetchFaresKiwi(String origin) throws Exception {
 			
 			//-------Skyscanner API URL------
 			String kiwiUrl = "https://api.skypicker.com/flights?flyFrom="+origin+"&typeFlight=return&xml=1";
 			URL kiwiObj = new URL(kiwiUrl);
 			HttpURLConnection kiwiCon = (HttpURLConnection) kiwiObj.openConnection();
 			kiwiCon.setRequestMethod("GET");
 			kiwiCon.setRequestProperty("User-Agent", USER_AGENT);
 			kiwiCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
 			kiwiCon.setRequestProperty("Accept", "application/xml");
 			
 			
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
 			
	public int insertAirport (AirportStructure airport, Connection conn) throws SQLException
	{			
			
			Integer airportID = null;
			
			PreparedStatement ps = conn.prepareStatement(addAirportSQL);
			ps.setString(1, airport.getIataFaa());
			ps.setString(2, airport.getAirportName());
			ps.setString(3, airport.getCityName());
			ps.setString(4, airport.getCountry());
			ps.setDouble(5, airport.getLatitude());
			ps.setDouble(6, airport.getLongtitude());
			ps.setDouble(7, airport.getAltitude());
			ps.setString(8, airport.getIcao());
			
			ps.executeUpdate();
			
			ps = conn.prepareStatement(getAirportOnIataFaaSQL);
			ps.setString(1, airport.getIataFaa());
			ResultSet resultSet = ps.executeQuery();     		 			
	 			
			if (resultSet.next()) 	
	 			{
	 				airportID = resultSet.getInt(10); 
	     			resultSet.close();
	 			}
			
			airport.setAirportID(airportID);
			
			return airportID;
	}
		
	public int updateSSID (NodeList locationList, int ssid, Connection conn) throws XPathExpressionException, IOException, ParserConfigurationException, SAXException, SQLException
		{
			
			Integer airportID = null;
			ResultSet resultSet;
			
		for (int temp = 0; temp < locationList.getLength(); temp++) {

     		Node nNode = locationList.item(temp);
     				
     		if (nNode.getNodeType() == Node.ELEMENT_NODE) {

     			Element eElement = (Element) nNode;
     			Node node = null;
     			Integer placeID = null;
     			String iataCode = "";
     			
     			
     			
     			if ((node = eElement.getElementsByTagName("PlaceId").item(0))!=null)
     				placeID = Integer.parseInt(node.getTextContent());
     			if ((node = eElement.getElementsByTagName("IataCode").item(0))!=null)
     				iataCode = node.getTextContent().toUpperCase();
     			
     			if (placeID.equals(ssid))
     			{
     				PreparedStatement ps = conn.prepareStatement(updateSSIDSQL);
     				ps.setInt(1, ssid);
     				ps.setString(2, iataCode.toUpperCase());
     				if (ps.executeUpdate()==0)
     				{
     					AirportStructure newAirport = accountNewAirport(iataCode.toUpperCase());
						try {
							airportID = insertAirport(newAirport, conn);
						} catch (SQLException e) {
							e.printStackTrace();
							System.out.println(newAirport.getIataFaa());
							System.out.println(newAirport.getAirportName());
							System.out.println(newAirport.getCityName());
							System.out.println(newAirport.getCountry());
							System.out.println(newAirport.getLatitude());
							System.out.println(newAirport.getLongtitude());
							System.out.println(newAirport.getAltitude());
							System.out.println(newAirport.getIcao());
						}
     				}
     				else
     				{
     					ps = conn.prepareStatement(getAirportOnSSIDSQL);
         				ps.setInt(1, ssid);
     					resultSet = ps.executeQuery();     		 			
     		 			if (resultSet.next()) 	
     		 			{
     		 				airportID = resultSet.getInt(10); 
     		     			resultSet.close();
     		 			}
     				}
     				
     			}
     			
     		}
			 			
	}
		
		return airportID;
	}
			
	public void createSkyScannerMapping() throws SQLException, XPathExpressionException, IOException, ParserConfigurationException, SAXException
		{ 	
			PreparedStatement ps = null;
			ResultSet resultSet = null;
						
			
			//first I collect mapping between SSID and Our Airport ID
			if (skyScannerIDMapping == null)
			{
				skyScannerIDMapping = new HashMap <Integer, Integer> ();
				
				
				ps = conn.prepareStatement(getSSIDMapping);
				resultSet = ps.executeQuery();
				
				Integer AirportID = null;
				Integer SSID = null;
				
				while (resultSet.next())
				{
					AirportID =					resultSet.getInt(1); 
	     			SSID =						resultSet.getInt(2);
					if (SSID != 0)
					{
						skyScannerIDMapping.put(SSID, AirportID);
					}
				}
				
			}
		}
		
	public void creatKiwiMapping () throws SQLException, XPathExpressionException, IOException, ParserConfigurationException, SAXException
		{ 	
			PreparedStatement ps = null;
			ResultSet resultSet = null;
						
			
			//first I collect mapping between iataFaa and Our Airport ID
			if (kiwiMapping == null)
			{
				kiwiMapping = new HashMap <String, Integer> ();
				
				
				ps = conn.prepareStatement(getIataMapping);
				resultSet = ps.executeQuery();
				
				Integer AirportID = null;
				String iataFaa = null;
				
				while (resultSet.next())
				{
					AirportID =					resultSet.getInt(1); 
					iataFaa =					resultSet.getString(2);
					if (iataFaa != "" && iataFaa != null)
					{
						kiwiMapping.put(iataFaa, AirportID);
					}
				}
				
			}
		}
		
	public RoundTripFare getRoundTripFare (Integer originID, Integer destinationID,Integer price, Date outbound, Date inbound) throws SQLException, XPathExpressionException, IOException, ParserConfigurationException, SAXException
			{
			
			RoundTripFare fare;
			AirportStructure origin;
			AirportStructure destination;
			PreparedStatement ps = null;
			ResultSet resultSet = null;
			
					
 			ps = conn.prepareStatement(getFareSQL);
 			ps.setInt(1, originID);
 			ps.setInt(2, destinationID);
 			resultSet = ps.executeQuery();
 			
 			//database columns
 			String originAirportName = 			null;
 			String originCityName = 			null;
 			String originCountry = 				null;
 			Double originLatitude = 			null;
 			Double originLongtitude = 			null;
 			Double originAltitude = 			null;
 			String originIcao=					null;
 			String originIataFaa=				null;
 			Integer originZone = 				null;
 			String destinationAirportName = 	null;
 			String destinationCityName = 		null;
 			String destinationCountry = 		null;
 			Double destinationLatitude = 		null;
 			Double destinationLongtitude = 		null;
 			Double destinationAltitude = 		null;
 			String destinationIcao=				null;
 			String destinationIataFaa=			null;
 			Integer destinationZone = 			null;
 			
 			Integer numberOfPricesRoundTrip = 	null;
 			Double averageAccountedPrice = 		(double)price;
 			
 			Integer originSSID =			 	null;
 			Integer destinationSSID =		 	null;
 			Date lastFareNotification=			null;
 			
 			// In case we know such a fare and thus know all airports
 			if (resultSet.next()) {  		     	
     			originAirportName = 			resultSet.getString(1);
     			originCityName = 				resultSet.getString(2);
     			originCountry = 				resultSet.getString(3);
     			originLatitude = 				resultSet.getDouble(4);
     			originLongtitude = 				resultSet.getDouble(5);
     			originIataFaa =					resultSet.getString(6);  
     			originAltitude=					resultSet.getDouble(7); 
     			originIcao=						resultSet.getString(8); 
     			originZone =					resultSet.getInt(9); 
     			
     			destinationAirportName = 		resultSet.getString(10);
     			destinationCityName = 			resultSet.getString(11);
     			destinationCountry = 			resultSet.getString(12);
     			destinationLatitude = 			resultSet.getDouble(13);
     			destinationLongtitude = 		resultSet.getDouble(14);
     			destinationIataFaa =			resultSet.getString(15);  
     			destinationAltitude=			resultSet.getDouble(16); 
     			destinationIcao=				resultSet.getString(17); 
     			destinationZone =				resultSet.getInt(18); 
     			
     			numberOfPricesRoundTrip =		resultSet.getInt(20);
     			averageAccountedPrice =			resultSet.getDouble(21); 
     			
     			originSSID =					resultSet.getInt(22);
     			destinationSSID =				resultSet.getInt(23);     			
     			lastFareNotification =			resultSet.getDate(24);
     			
     			
     			resultSet.close();
     			
     			//In this case I dont do any additional SQL calls     			
     			if (locationDictionary.containsKey(originID))
     				origin = locationDictionary.get(originID);
     			else
     			{
     				origin = new AirportStructure(originAirportName,originCityName,originCountry,originLatitude,originLongtitude,originSSID,originIataFaa, originAltitude, originIcao,originZone,originID);
     	 			locationDictionary.put(originID, origin);
     			}
     			
     			if (locationDictionary.containsKey(destinationID))
     				destination = locationDictionary.get(destinationID);
     			else
     			{
     				destination = new AirportStructure(destinationAirportName,destinationCityName,destinationCountry,destinationLatitude,destinationLongtitude,destinationSSID,destinationIataFaa, destinationAltitude, destinationIcao,destinationZone,destinationID);
     	 			locationDictionary.put(destinationID, destination);
     			}
     			
     			fare = new RoundTripFare (origin, destination, price, outbound, inbound, averageAccountedPrice,numberOfPricesRoundTrip,lastFareNotification); 
     			     			
 			}
 			// In case Fare is not accounted, either we know airports, but dont know fare, or we dont know one airport, or we dont know any of airports
 			else
 			{
 				// In this case I need to do additional SQL calls
 				origin = getAirportInfo (originID); 				
 				destination = getAirportInfo (destinationID);		
 				
 				fare = new RoundTripFare (origin, destination, price, outbound, inbound, averageAccountedPrice,0,null); 
     			fare.setIsNew();
 			}			
 			return fare;
		}	
		
	public void updateDatabaseFare(RoundTripFare fare, boolean isPublished) throws SQLException
		{
 			PreparedStatement ps = null;
 			Integer numberOfAccountedPricesRoundTrip = fare.getNumberOfAccountedPricesRoundTrip();
 			double averagePriceRoundTrip = fare.getBaseFare();
 								
     		if (numberOfAccountedPricesRoundTrip == 50000)
				numberOfAccountedPricesRoundTrip = 30000;
     		
     		
			double newAveragePriceRoundTrip = ((numberOfAccountedPricesRoundTrip*averagePriceRoundTrip)+(double)fare.getPrice())/(double)(numberOfAccountedPricesRoundTrip+1);
			numberOfAccountedPricesRoundTrip++;
			ps = conn.prepareStatement(updateRoutePrice);
			int lastAccountedPriceRoundTrip = fare.getPrice();
			ps.setInt(1, numberOfAccountedPricesRoundTrip);
			ps.setInt(2, lastAccountedPriceRoundTrip);
			ps.setDouble(3, newAveragePriceRoundTrip);
			ps.setString(5, fare.getOrigin().getIataFaa().toUpperCase());
			ps.setString(6, fare.getDestination().getIataFaa().toUpperCase());
			if(isPublished == false)
				ps.setInt(4,0);
			else
				ps.setInt(4,1);
			
			ps.executeUpdate();
		}
	
	public void updateFarePublishDate (RoundTripFare fare) throws SQLException {
			
		//this need to be done as separate SQL call, because when prices are updated, we still dont know if we publish
		
		PreparedStatement ps = null;
		ps = conn.prepareStatement(updateFarePublishDate);
		ps.setInt(1, fare.getOrigin().getAirportID());
		ps.setInt(2, fare.getDestination().getAirportID());
		
		ps.executeUpdate();
	}
		
	public void insertDatabaseFare(RoundTripFare fare, boolean isPublished) throws SQLException
		{
			PreparedStatement ps = null;
			
				ps = conn.prepareStatement(insertRoutePrice);
				ps.setString(1, fare.getOrigin().getIataFaa().toUpperCase());
				ps.setString(2, fare.getDestination().getIataFaa().toUpperCase());
				ps.setInt(3, fare.getPrice());
				ps.setDouble(4, fare.getPrice());
				
				if(isPublished == false)
					ps.setInt(5,0);
				else
					ps.setInt(5,1);
				
				ps.executeUpdate();
		}
			
	public AirportStructure getAirportInfo (Integer airportID) throws SQLException, XPathExpressionException, IOException, ParserConfigurationException, SAXException
		{ 	
			if (locationDictionary.containsKey(airportID))
			{
				return locationDictionary.get(airportID);

			} 			
 			
 			
 			final PreparedStatement ps = conn.prepareStatement(getAirportOnIDSQL);
 			ps.setInt(1, airportID);
 			final ResultSet resultSet = ps.executeQuery();
 			
 			//database columns
 			String airportName = 	null;
 			String cityName = 		null;
 			String country = 		null;
 			Double latitude = 		null;
 			Double longtitude = 	null;
 			Double altitude = 		null;
 			String icao=			null;
 			String iataFaa=			null;
 			Integer zone = 			null;
 			Integer SSID = 			null;
 			Integer id =			null;
 			
 			if (resultSet.next()) {  		     	
     			airportName = 	resultSet.getString(1);
     			cityName = 		resultSet.getString(2);
     			country = 		resultSet.getString(3);
     			latitude = 		resultSet.getDouble(4);
     			longtitude = 	resultSet.getDouble(5);
     			iataFaa =		resultSet.getString(6);  
     			altitude=		resultSet.getDouble(7); 
     			icao=			resultSet.getString(8); 
     			zone =			resultSet.getInt(9); 
     			id =			resultSet.getInt(10);
     			SSID = 			resultSet.getInt(11);  
     			
     			resultSet.close();
 			}
 			
 			AirportStructure airportStructure = new AirportStructure(airportName,cityName,country,latitude,longtitude,SSID,iataFaa, altitude, icao,zone,id);
 			locationDictionary.put(airportID, airportStructure);
 			
 			return airportStructure;
		}
		
		
}