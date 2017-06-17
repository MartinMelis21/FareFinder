package com.martinmelis.web.farefinder.databaseHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.martinmelis.web.farefinder.farescraper.CachingModule;

import dataTypes.AirportStructure;
import dataTypes.InterregionalFare;
import dataTypes.RoundTripFare;

public class DatabaseHandler {

	private Connection databaseConnection;
	private CachingModule cachingModule;
	
	public DatabaseHandler(){
		this.cachingModule = new CachingModule();
	}
	
	public Connection getDatabaseConnection() {
		return databaseConnection;
	}
	//-----------------connect to Database-----------------
    public void connectDatabase ()
    {
		Context initialContext=null;
	    String dataResourceName = "jdbc/MySQLDS";
		DataSource dataSource = null;
		Context environmentContext = null;
		try {
			initialContext = new InitialContext();
			environmentContext = (Context) initialContext.lookup("java:comp/env");
			dataSource = (DataSource) environmentContext.lookup(dataResourceName);
			databaseConnection = dataSource.getConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}	    
    }
    //----------------disconnect from Database-----------------
    public void disconnectDatabse()
	{
		if (databaseConnection !=null)
			try {
				databaseConnection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

    //----------------database handlers----------------------
    
	public RoundTripFare getRoundTripFare (Integer originID, Integer destinationID,Integer price, Date outbound, Date inbound) throws SQLException, XPathExpressionException, IOException, ParserConfigurationException, SAXException
	{
	
	RoundTripFare fare;
	AirportStructure origin;
	AirportStructure destination;
	PreparedStatement ps = null;
	ResultSet resultSet = null;
	
			
		ps = databaseConnection.prepareStatement(DatabaseQueries.getFareSQL);
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
		Integer portalPostID=				-1;
		String portalPostStatus = 			null;
		Integer lastAccountedPrice=			null;
		
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
			
			lastAccountedPrice =			resultSet.getInt(19); 
			numberOfPricesRoundTrip =		resultSet.getInt(20);
			averageAccountedPrice =			resultSet.getDouble(21); 
			
			originSSID =					resultSet.getInt(22);
			destinationSSID =				resultSet.getInt(23);     			
			lastFareNotification =			resultSet.getDate(24);
			portalPostID=					resultSet.getInt(25); ;
			portalPostStatus = 				resultSet.getString(26);
			
			resultSet.close();
			
			//In this case I dont do any additional SQL calls     			
			if (cachingModule.getLocationDictionary().containsKey(originID))
				origin = cachingModule.getLocationDictionary().get(originID);
			else
			{
				origin = new AirportStructure(originAirportName,originCityName,originCountry,originLatitude,originLongtitude,originSSID,originIataFaa, originAltitude, originIcao,originZone,originID);
	 			cachingModule.getLocationDictionary().put(originID, origin);
			}
			
			if (cachingModule.getLocationDictionary().containsKey(destinationID))
				destination = cachingModule.getLocationDictionary().get(destinationID);
			else
			{
				destination = new AirportStructure(destinationAirportName,destinationCityName,destinationCountry,destinationLatitude,destinationLongtitude,destinationSSID,destinationIataFaa, destinationAltitude, destinationIcao,destinationZone,destinationID);
	 			cachingModule.getLocationDictionary().put(destinationID, destination);
			}
			
			fare = new RoundTripFare (origin, destination, price, outbound, inbound, averageAccountedPrice,numberOfPricesRoundTrip,lastFareNotification,portalPostID,portalPostStatus); 
			fare.setLastAccountedPrice (lastAccountedPrice);     			
		}
		// In case Fare is not accounted, either we know airports, but dont know fare, or we dont know one airport, or we dont know any of airports
		else
		{
			// In this case I need to do additional SQL calls
			origin = getAirportInfo (originID); 				
			destination = getAirportInfo (destinationID);		
			
			fare = new RoundTripFare (origin, destination, price, outbound, inbound, averageAccountedPrice,0,null,-1,null); 
			fare.setLastAccountedPrice (price); 
			fare.setIsNew();			
			insertDatabaseFare(fare);
			
		}			
		return fare;
}	
	
	public CachingModule getCachingLists ()
	{
		return this.cachingModule;
	}
	
	public void updateDatabaseFare(RoundTripFare fare) throws SQLException
	{
			PreparedStatement ps = null;
			Integer numberOfAccountedPricesRoundTrip = fare.getNumberOfAccountedPricesRoundTrip();
			double averagePriceRoundTrip = fare.getBaseFare();
								
			if (numberOfAccountedPricesRoundTrip == 50000)
			numberOfAccountedPricesRoundTrip = 30000;
			
			
		double newAveragePriceRoundTrip = ((numberOfAccountedPricesRoundTrip*averagePriceRoundTrip)+(double)fare.getPrice())/(double)(numberOfAccountedPricesRoundTrip+1);
		numberOfAccountedPricesRoundTrip++;
		ps = databaseConnection.prepareStatement(DatabaseQueries.updateRoutePrice);
		int lastAccountedPriceRoundTrip = fare.getPrice();
		ps.setInt(1, numberOfAccountedPricesRoundTrip);
		ps.setInt(2, lastAccountedPriceRoundTrip);
		ps.setDouble(3, newAveragePriceRoundTrip);
		ps.setString(4, fare.getOrigin().getIataFaa().toUpperCase());
		ps.setString(5, fare.getDestination().getIataFaa().toUpperCase());
		ps.setDate(6, new java.sql.Date(fare.getOutboundLeg().getTime()));
		ps.setDate(7, new java.sql.Date(fare.getInboundLeg().getTime()));
		
		ps.executeUpdate();
	}
	
	public void updateInterregionalFares(HashMap <String, InterregionalFare> interregionalFares) throws SQLException
	{
		PreparedStatement ps = null;								
		ps = databaseConnection.prepareStatement(DatabaseQueries.updateRegionalRoutePrice);
		
		for (InterregionalFare irFare : interregionalFares.values())
		{
			if (irFare.getDatabaseUpdateRequirement() == true)
			{
				
			ps.setInt(1, irFare.getNumberOfAccountedPrices());
			ps.setInt(2, irFare.getLastAccountedPrice());
			ps.setDouble(3, irFare.getAveragePrice());
			ps.setInt(4, irFare.getDepartureRegion());
			ps.setInt(5, irFare.getArrivalRegion());
			
			ps.executeUpdate();
			
			irFare.doesNotrequireDatabaseUpdate();
			
			}
		}
	}
	
	public HashMap <String, InterregionalFare> getInterregionalFares () throws SQLException{
		
		HashMap <String, InterregionalFare> interregionalFares = new HashMap <String, InterregionalFare> ();
		final PreparedStatement ps = databaseConnection.prepareStatement(DatabaseQueries.getInterregionalFares);
		final ResultSet resultSet = ps.executeQuery();
		
		Integer departureRegion;
		Integer arrivalRegion;
		Double averagePrice;
		int numberOfAccountedPricesRoundTrip;
		int lastAccountedPriceRoundTrip;
		java.sql.Date lastAccountedPriceTimeRoundTrip;
		
		
		while (resultSet.next()) {  		     	
			departureRegion = 						resultSet.getInt(1);
			arrivalRegion = 						resultSet.getInt(2);
			averagePrice = 							resultSet.getDouble(3);
			numberOfAccountedPricesRoundTrip = 		resultSet.getInt(4);
			lastAccountedPriceRoundTrip = 			resultSet.getInt(5);
			lastAccountedPriceTimeRoundTrip =		resultSet.getDate(6);   
			
			resultSet.close();
			
			InterregionalFare irFare = new InterregionalFare (departureRegion, arrivalRegion,averagePrice,numberOfAccountedPricesRoundTrip,lastAccountedPriceRoundTrip);
			irFare.setLastAccountedPriceTime(lastAccountedPriceTimeRoundTrip);
			interregionalFares.put(new String (departureRegion.toString() + arrivalRegion.toString()), irFare);
		}
		
		
		return interregionalFares;
	}
	
	public void updateFarePublication (RoundTripFare fare) throws SQLException {
		
	//this need to be done as separate SQL call, because when prices are updated, we still dont know if we publish
	
	PreparedStatement ps = null;
	ps = databaseConnection.prepareStatement(DatabaseQueries.updateFarePublication);
	ps.setInt(1, fare.getPortalPostID());
	ps.setString(2, fare.getPortalPostStatus());
	ps.setInt(3, fare.getOrigin().getAirportID());
	ps.setInt(4, fare.getDestination().getAirportID());
	
	ps.executeUpdate();
	}
	
	public void insertDatabaseFare(RoundTripFare fare) throws SQLException
	{
		PreparedStatement ps = null;
		
			ps = databaseConnection.prepareStatement(DatabaseQueries.insertRoutePrice);
			ps.setString(1, fare.getOrigin().getIataFaa().toUpperCase());
			ps.setString(2, fare.getDestination().getIataFaa().toUpperCase());
			ps.setInt(3, fare.getPrice());
			ps.setDouble(4, fare.getPrice());
			ps.setDate(5, new java.sql.Date(fare.getOutboundLeg().getTime()));
			ps.setDate(6, new java.sql.Date(fare.getInboundLeg().getTime()));
			
			ps.executeUpdate();
	}
		
	public AirportStructure getAirportInfo (Integer airportID) throws SQLException, XPathExpressionException, IOException, ParserConfigurationException, SAXException
	{ 	
		if (cachingModule.getLocationDictionary().containsKey(airportID))
		{
			return cachingModule.getLocationDictionary().get(airportID);
	
		} 			
			
			
			final PreparedStatement ps = databaseConnection.prepareStatement(DatabaseQueries.getAirportOnIDSQL);
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
			
			//TODO maybe airport infor WS needs to be called here ?
			
			AirportStructure airportStructure = new AirportStructure(airportName,cityName,country,latitude,longtitude,SSID,iataFaa, altitude, icao,zone,id);
			cachingModule.getLocationDictionary().put(airportID, airportStructure);
			
			return airportStructure;
	}

	public int updateSSID (NodeList locationList, int ssid) throws XPathExpressionException, IOException, ParserConfigurationException, SAXException, SQLException
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
 				PreparedStatement ps = databaseConnection.prepareStatement(DatabaseQueries.updateSSIDSQL);
 				ps.setInt(1, ssid);
 				ps.setString(2, iataCode.toUpperCase());
 				if (ps.executeUpdate()==0)
 				{
 					AirportStructure newAirport = accountNewAirport(iataCode.toUpperCase());
					try {
						airportID = insertAirport(newAirport);
					} catch (SQLException e) {
						e.printStackTrace();
					}
 				}
 				else
 				{
 					ps = databaseConnection.prepareStatement(DatabaseQueries.getAirportOnSSIDSQL);
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
	
	public AirportStructure accountNewAirport (String iataCode) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException
	{
		//TODO needs to be attached to Database
		
		
		String sourceUrl = "http://www.gcmap.com/airport/" + iataCode;
		
		URL distanceObj = new URL(sourceUrl);
		HttpURLConnection distanceCon = (HttpURLConnection) distanceObj.openConnection();

		// optional default is GET
		distanceCon.setRequestMethod("GET");
		distanceCon.setRequestProperty("User-Agent", "Mozilla/5.0");
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
	{
		System.out.println("Error by fetching information about airport " + iataCode);
		return null;
	}
	
	
	}
	
	public int insertAirport (AirportStructure airport) throws SQLException
	{			
			
			Integer airportID = null;
			
			PreparedStatement ps = databaseConnection.prepareStatement(DatabaseQueries.addAirportSQL);
			ps.setString(1, airport.getIataFaa());
			ps.setString(2, airport.getAirportName());
			ps.setString(3, airport.getCityName());
			ps.setString(4, airport.getCountry());
			ps.setDouble(5, airport.getLatitude());
			ps.setDouble(6, airport.getLongtitude());
			try{
			ps.setDouble(7, airport.getAltitude());
			}catch( Exception e)
			{
				System.out.println("\n\nError inserting airport - airport.getIataFaa() ... no altitude\n\n");
				e.printStackTrace();
				ps.setNull(7, java.sql.Types.NULL);
			}
			ps.setString(8, airport.getIcao());
			
			ps.executeUpdate();
			
			ps = databaseConnection.prepareStatement(DatabaseQueries.getAirportOnIataFaaSQL);
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

	public ArrayList <String> getOriginCountryAirports (String countryCode) throws SQLException
	{
		ArrayList <String> airportCodesList = new ArrayList <String>();
		
		final PreparedStatement ps = databaseConnection.prepareStatement(DatabaseQueries.getOriginAirports);
		ps.setString(1, countryCode.toUpperCase());
		final ResultSet resultSet = ps.executeQuery();
		
		while (resultSet.next()) {  		     	
			airportCodesList.add(resultSet.getString(1).toUpperCase());
		}
	
		if (airportCodesList.isEmpty())
			return null;
		else
			return airportCodesList;
		
	}
}
