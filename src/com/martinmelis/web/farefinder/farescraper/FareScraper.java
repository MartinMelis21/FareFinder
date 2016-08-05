package com.martinmelis.web.farefinder.farescraper;

import java.net.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

public class FareScraper {
	
	BufferedReader in;
	private final String USER_AGENT = "Mozilla/5.0";
	private HashMap <Integer,String> locationDictionary;
	
	public FareScraper() throws IOException {
		locationDictionary = new HashMap<Integer,String>();
			
	}	
	
	public int getDistance (String iataCodeString) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException
	{
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
	
	public void getFares() throws Exception {

		String skyScannerUrl = "http://partners.api.skyscanner.net/apiservices/browsequotes/v1.0/SK/EUR/en-US/AT/anywhere/anytime/anytime?apiKey=prtl6749387986743898559646983194";
		
		URL skyScannerObj = new URL(skyScannerUrl);
		HttpURLConnection skyScannerCon = (HttpURLConnection) skyScannerObj.openConnection();

		// optional default is GET
		skyScannerCon.setRequestMethod("GET");

		//add request header
		skyScannerCon.setRequestProperty("User-Agent", USER_AGENT);
		skyScannerCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		skyScannerCon.setRequestProperty("Accept", "application/xml");
		
		int responseCode = skyScannerCon.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + skyScannerUrl);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(skyScannerCon.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		
		
// printing the response to stdout		
		
		
		 try {	
	         DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	         DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	         Document doc = dBuilder.parse(new InputSource(new StringReader(response.toString())));
	         doc.getDocumentElement().normalize();
	        
	         
	         //first I create HashMap dictionary for locations
	         
	         NodeList nList = doc.getElementsByTagName("PlaceDto");
	         

		     	for (int temp = 0; temp < nList.getLength(); temp++) {

		     		Node nNode = nList.item(temp);
		     				
		     		if (nNode.getNodeType() == Node.ELEMENT_NODE) {

		     			Element eElement = (Element) nNode;
		     			Node node = null;
		     			Integer placeID = null;
		     			String iataCode = "";
		     			String name = "";
		     			String countryName = "";
		     			
		     			if ((node = eElement.getElementsByTagName("PlaceId").item(0))!=null)
		     				placeID = Integer.parseInt(node.getTextContent());
		     			if ((node = eElement.getElementsByTagName("IataCode").item(0))!=null)
		     				iataCode = node.getTextContent();
		     			if ((node = eElement.getElementsByTagName("Name").item(0))!=null)
		     				name = node.getTextContent();
		     			if ((node = eElement.getElementsByTagName("CountryName").item(0))!=null)
		     				countryName = node.getTextContent();
		     			
		     			if (placeID != null)
		     				locationDictionary.put(placeID, name+"|"+countryName+"|"+iataCode);
		     		}
		     	}
	         
	         
	         
	         
	         //second I collect quotes
	         
	        System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
				
	     	nList = doc.getElementsByTagName("QuoteDto");
	     			
	     	System.out.println("----------------------------");

	     	for (int temp = 0; temp < nList.getLength(); temp++) {

	     		Node nNode = nList.item(temp);
	     				
	     		if (nNode.getNodeType() == Node.ELEMENT_NODE) {

	     			Element eElement = (Element) nNode;
	     				     			
	     			String originOutbound = locationDictionary.get(Integer.parseInt(((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("OriginId").item(0).getTextContent()));
	     			String destinationOutbound = locationDictionary.get(Integer.parseInt(((Element) eElement.getElementsByTagName("OutboundLeg").item(0)).getElementsByTagName("DestinationId").item(0).getTextContent()));;
	     			String originInbound = locationDictionary.get(Integer.parseInt(((Element) eElement.getElementsByTagName("InboundLeg").item(0)).getElementsByTagName("OriginId").item(0).getTextContent()));;
	     			String destinationInbound = locationDictionary.get(Integer.parseInt(((Element) eElement.getElementsByTagName("InboundLeg").item(0)).getElementsByTagName("DestinationId").item(0).getTextContent()));;
	     			String iataCodeString = originOutbound.split("\\|")[2] + "-" + destinationOutbound.split("\\|")[2]+ "-" + originInbound.split("\\|")[2]+ "-" + destinationInbound.split("\\|")[2];
	     			int price = Integer.parseInt(eElement.getElementsByTagName("MinPrice").item(0).getTextContent());
	     			double dealRatio = price/(double)getDistance(iataCodeString);
	     			
	     			if (dealRatio>=0.025)
	     				continue;
	     			
	     			
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
	
	
}