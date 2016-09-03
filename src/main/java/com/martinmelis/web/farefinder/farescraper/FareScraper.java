package com.martinmelis.web.farefinder.farescraper;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
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

import java.io.*;



public class FareScraper {
	
	//-----global variables-----
	
	BufferedReader in;
	private final String USER_AGENT = "Mozilla/5.0";
	private HashMap <Integer,AirportStructure> locationDictionary;
	StringBuffer finalResponse;
	final String getAirportSQL = "SELECT a.airportName,a.airportCity,a.airportCountry,a.latitude,a.longtitude,a.iataFaa,a.altitude,a.icao,z.id from Airports a left join Countries c on a.airportCountry=c.countryName left join Zones z on c.zone=z.id WHERE SkyScannerID = ?";
	final String addAirportSQL = "INSERT INTO Airports (iataFaa, airportName, airportCity, airportCountry, latitude, longtitude, altitude, icao) values (?, ?, ?, ?, ?, ?, ?, ?)";
	final String updateSSIDSQL = "UPDATE Airports SET SkyScannerID = ? WHERE iataFaa = ?";
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
	
	//-----Distance calculation-----
	//TODO needs to be changed for MySql connector and dynamic calculation
	
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
		return new AirportStructure(name, city, country, latitude, longtitude, null, iataCode, altitude, ICAO, zone);
	else
		return null;
	}

	
	//-----Fetching the Fares Data and Quote Metadata-----
	
 	public String fetchFares(String origin) throws Exception {
		
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

	public String getFares(ArrayList <String> countryList, Connection conn) throws Exception {
		
		String fetchedFares;
		this.conn=conn;
		
		for (String origin: countryList)
		{
			fetchedFares = fetchFares(origin);
			
		
		 try {	
	         DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	         DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	         Document doc = dBuilder.parse(new InputSource(new StringReader(fetchedFares)));
	         doc.getDocumentElement().normalize();
	        
	         
	         
	         NodeList locationList = doc.getElementsByTagName("PlaceDto");
	         NodeList quoteList = doc.getElementsByTagName("QuoteDto");

	         
	     	for (int temp = 0; temp < quoteList.getLength(); temp++) {

	     		Node nNode = quoteList.item(temp);
	     				
	     		if (nNode.getNodeType() == Node.ELEMENT_NODE) {

	     			Element eElement = (Element) nNode;
	     			  			
	     			//TODO I need to take values from HashList, if there is no value, I take from database
	    	     			
	     			AirportStructure originOutbound 		= getAirportInfoSkyScanner(Integer.parseInt(((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("OriginId").item(0).getTextContent()), locationList);
	     			AirportStructure destinationOutbound 	= getAirportInfoSkyScanner(Integer.parseInt(((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("DestinationId").item(0).getTextContent()), locationList);
	     			AirportStructure originInbound 			= getAirportInfoSkyScanner(Integer.parseInt(((Element) eElement.getElementsByTagName("InboundLeg").item(0)).getElementsByTagName("OriginId").item(0).getTextContent()), locationList);
	     			AirportStructure destinationInbound 	= getAirportInfoSkyScanner(Integer.parseInt(((Element) eElement.getElementsByTagName("InboundLeg").item(0)).getElementsByTagName("DestinationId").item(0).getTextContent()), locationList);
	     			
	     			int price = Integer.parseInt(eElement.getElementsByTagName("MinPrice").item(0).getTextContent());
	     			  			
	     			double latOrigin = originOutbound.getLatitude();
	     			double longOrigin = originOutbound.getLongtitude();
	     			double latDestination = destinationOutbound.getLatitude();
	     			double longDestination = destinationOutbound.getLongtitude();
	     			
	     			int zoneOrigin = originOutbound.getZone();
	     			int zoneDestination = destinationOutbound.getZone();
	     			double distance = getDistance(latOrigin,longOrigin,latDestination,longDestination);
	     			
	     			double dealRatio = price/(2*distance);
	     			
	     			//if i deal with intercontinental
	     			if (!((zoneOrigin != zoneDestination && dealRatio<=0.015) || (zoneOrigin == zoneDestination && dealRatio<=0.008)))
	     				continue;
	     			String fare = "";
	     			
	     			fare += (("Outbound Leg\n\tFrom : " + originOutbound.getCityName() + " to " + destinationOutbound.getCityName() ) + "\n");
	     			fare += (("\tDate : " + ((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("DepartureDate").item(0).getTextContent()) + "\n");
	     			fare += (("Inbound Leg\n\tFrom : " + originInbound.getCityName()  + " to " + destinationInbound.getCityName() ) + "\n");
	     			fare += (("\tDate : " + ((Element) eElement.getElementsByTagName("InboundLeg").item(0)).getElementsByTagName("DepartureDate").item(0).getTextContent()) + "\n");
	     			fare += (("Price : " + price) + "\n");
	     			fare += (("DealRatio : " + dealRatio) + "\n"+ "\n");
	     			
	     			
	     			finalResponse.append(fare);
	     			
	     			//if (zoneOrigin != zoneDestination && dealRatio<=0.030)
	     			//	mailSender.sendMail("martin.melis21@gmail.com", fare);
	     			
	     			System.out.println("Outbound Leg\n\tFrom : " + originOutbound.getCityName()  + " to " + destinationOutbound.getCityName() );
	     			System.out.println("\tDate : " + ((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("DepartureDate").item(0).getTextContent());
	     			System.out.println("Inbound Leg\n\tFrom : " + originInbound.getCityName()  + " to " + destinationInbound.getCityName() );
	     			System.out.println("\tDate : " + ((Element) eElement.getElementsByTagName("InboundLeg").item(0)).getElementsByTagName("DepartureDate").item(0).getTextContent());
	     			System.out.println("Price : " + price);
	     			System.out.println("DealRatio : " + dealRatio);

	     		}
	     		
	     		System.out.println();
	     		
	         }
	      } catch (Exception e) {
	         e.printStackTrace();
	      }
		}
		
		return finalResponse.toString();
	}

	public double getDistance (double lat1, double lon1, double lat2, double lon2) {
		double theta = lon1 - lon2;
		double dist = Math.sin((lat1 * Math.PI / 180.0)) * Math.sin((lat2 * Math.PI / 180.0)) + Math.cos((lat1 * Math.PI / 180.0)) * Math.cos((lat2 * Math.PI / 180.0)) * Math.cos((theta * Math.PI / 180.0));
		dist = Math.acos(dist);
		dist = dist * 180 / Math.PI;
		dist = dist * 60 * 1.1515;
			dist = dist * 1.609344;

		return (dist);
	}
	
	public void insertAirport (AirportStructure airport, Connection conn) throws SQLException
	{			
				
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
	}
	
	
	public void updateSSID (NodeList locationList, int ssid, Connection conn) throws SQLException, XPathExpressionException, IOException, ParserConfigurationException, SAXException
	{
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
     					insertAirport(accountNewAirport(iataCode.toUpperCase()), conn);
     				
     			}
     			
     		}
			 			
	}
	}
		
	
		public AirportStructure getAirportInfoSkyScanner (Integer SSID, NodeList locationList) throws SQLException, XPathExpressionException, IOException, ParserConfigurationException, SAXException
		{ 	
			if (locationDictionary.containsKey(SSID))
			{
				return locationDictionary.get(SSID);

			} 			
 			
 			
 			final PreparedStatement ps = conn.prepareStatement(getAirportSQL);
 			ps.setInt(1, SSID);
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
     			
     			resultSet.close();
 			}
 			else
 			{
 				//in case update failed, i insert new airport entry to the table
 				updateSSID(locationList, SSID, conn);
 				resultSet.close();
 				AirportStructure airportStructure = getAirportInfoSkyScanner (SSID, locationList);
 	 			return airportStructure;
 			}
 			
 			AirportStructure airportStructure = new AirportStructure(airportName,cityName,country,latitude,longtitude,SSID,iataFaa, altitude, icao,zone);
 			locationDictionary.put(SSID, airportStructure);
 			
 			return airportStructure;
		}
}