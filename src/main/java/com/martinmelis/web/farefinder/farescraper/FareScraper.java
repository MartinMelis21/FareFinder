package com.martinmelis.web.farefinder.farescraper;

import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import java.io.*;



public class FareScraper {
	
	//-----global variables-----
	
	BufferedReader in;
	private final String USER_AGENT = "Mozilla/5.0";
	private HashMap <Integer,String> locationDictionary;
	StringBuffer finalResponse;
	
	//-----inicialization-----
	
	public FareScraper() throws IOException {
		locationDictionary = new HashMap<Integer,String>();	
		finalResponse = new StringBuffer("Fares:\n");
	}	
	
	//-----Distance calculation-----
	//TODO needs to be changed for MySql connector and dynamic calculation
	
	public int getDistance (String iataCodeString) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException
	{
		//TODO needs to be attached to Database
		
		
		String distanceUrl = "http://www.gcmap.com/dist?P=" + iataCodeString + "&DU=km&DM=&SG=&SU=mph";
		
		URL distanceObj = new URL(distanceUrl);
		HttpURLConnection distanceCon = (HttpURLConnection) distanceObj.openConnection();

		// optional default is GET
		distanceCon.setRequestMethod("GET");
		distanceCon.setRequestProperty("User-Agent", USER_AGENT);
		distanceCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		distanceCon.setRequestProperty("Accept", "application/xml");

		BufferedReader in = new BufferedReader(new InputStreamReader(distanceCon.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		int distance = 1;
		String previousLine = "";
		while ((inputLine = in.readLine()) != null) {
			if (previousLine.contains("Total:") && inputLine.contains("<td class=\"d\">"))
			{
				String pattern = "<td class=\"d\">(.*) km</td>";
			    // Create a Pattern object
			    Pattern r = Pattern.compile(pattern);

			    // Now create matcher object.
			    Matcher m = r.matcher(inputLine);
			      if (m.find( )) {
			          distance = Integer.parseInt(m.group(1).replaceAll(",", ""));
			         
			      }
			}
				previousLine = inputLine;
		}
		in.close();	
	return distance;
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
		
		return response.toString();
	}

	public String getFares(ArrayList <String> countryList) throws Exception {
		
		String fetchedFares;
		
		for (String origin: countryList)
		{
			fetchedFares = fetchFares(origin);
			
		
		 try {	
	         DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	         DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	         Document doc = dBuilder.parse(new InputSource(new StringReader(fetchedFares)));
	         doc.getDocumentElement().normalize();
	        
	         
	         //first I create HashMap dictionary for locations
	         
	         NodeList locationList = doc.getElementsByTagName("PlaceDto");
	         NodeList quoteList = doc.getElementsByTagName("QuoteDto");

		     	for (int temp = 0; temp < locationList.getLength(); temp++) {

		     		Node nNode = locationList.item(temp);
		     				
		     		if (nNode.getNodeType() == Node.ELEMENT_NODE) {

		     			Element eElement = (Element) nNode;
		     			Node node = null;
		     			Integer placeID = null;
		     			String iataCode = "";
		     			String name = "";
		     			String countryName = "";
		     			
		     			//System.out.println(eElement.getElementsByTagName("CountryName").item(0));
		     			
		     			if ((node = eElement.getElementsByTagName("PlaceId").item(0))!=null)
		     				placeID = Integer.parseInt(node.getTextContent());
		     			if ((node = eElement.getElementsByTagName("IataCode").item(0))!=null)
		     				iataCode = node.getTextContent();
		     			if ((node = eElement.getElementsByTagName("Name").item(0))!=null)
		     				name = node.getTextContent();
		     			if ((node = eElement.getElementsByTagName("CountryName").item(0))!=null)
		     				countryName = node.getTextContent();
		     			
		     			if (placeID != null)
		     			{
		     				locationDictionary.put(placeID, name+"|"+countryName+"|"+iataCode);
		     			}
		     			
		     			//TODO add to MySQL Database
		     		}
		     	}
	         
	         
	         
	         
	         //second I collect quotes
	         
				

	     	for (int temp = 0; temp < quoteList.getLength(); temp++) {

	     		Node nNode = quoteList.item(temp);
	     				
	     		if (nNode.getNodeType() == Node.ELEMENT_NODE) {

	     			Element eElement = (Element) nNode;
	     			  			
	     			String originOutbound = locationDictionary.get(Integer.parseInt(((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("OriginId").item(0).getTextContent()));
	     			String destinationOutbound = locationDictionary.get(Integer.parseInt(((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("DestinationId").item(0).getTextContent()));;
	     			String originInbound = locationDictionary.get(Integer.parseInt(((Element) eElement.getElementsByTagName("InboundLeg").item(0)).getElementsByTagName("OriginId").item(0).getTextContent()));;
	     			String destinationInbound = locationDictionary.get(Integer.parseInt(((Element) eElement.getElementsByTagName("InboundLeg").item(0)).getElementsByTagName("DestinationId").item(0).getTextContent()));;
	     			String iataCodeString = originOutbound.split("\\|")[2] + "-" + destinationOutbound.split("\\|")[2]+ "-" + originInbound.split("\\|")[2]+ "-" + destinationInbound.split("\\|")[2];
	     			int price = Integer.parseInt(eElement.getElementsByTagName("MinPrice").item(0).getTextContent());
	     			double dealRatio = price/(double)getDistance(iataCodeString);
	     			
	     			if (dealRatio>=0.015)
	     				continue;
	     			
	     			finalResponse.append(("Outbound Leg\n\tFrom : " + originOutbound + " to " + destinationOutbound) + "\n");
	     			finalResponse.append(("\tDate : " + ((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("DepartureDate").item(0).getTextContent()) + "\n");
	     			finalResponse.append(("Inbound Leg\n\tFrom : " + originInbound + " to " + destinationInbound) + "\n");
	     			finalResponse.append(("\tDate : " + ((Element) eElement.getElementsByTagName("InboundLeg").item(0)).getElementsByTagName("DepartureDate").item(0).getTextContent()) + "\n");
	     			finalResponse.append(("Price : " + price) + "\n");
	     			finalResponse.append(("DealRatio : " + dealRatio) + "\n"+ "\n");
	     			
	     			System.out.println("Outbound Leg\n\tFrom : " + originOutbound + " to " + destinationOutbound);
	     			System.out.println("\tDate : " + ((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("DepartureDate").item(0).getTextContent());
	     			System.out.println("Inbound Leg\n\tFrom : " + originInbound + " to " + destinationInbound);
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
	
	
}