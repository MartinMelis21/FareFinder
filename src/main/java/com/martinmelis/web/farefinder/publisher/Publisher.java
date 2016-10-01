package com.martinmelis.web.farefinder.publisher;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import dataTypes.AirportStructure;
import dataTypes.RoundTripFare;

public class Publisher {
	
	XmlRpcClientConfigImpl config;
	XmlRpcClient client;
	Vector params;
	
	private void initializeConnection()
	{
		config = new XmlRpcClientConfigImpl();
	      config.setBasicPassword("P(qo#zKmm6hfXAq*X8");
	      config.setBasicUserName("martinmelis");
	      config.setEnabledForExtensions(true);
	      config.setEnabledForExceptions(true);
	      try {
			config.setServerURL(new URL("http://errorflights-martinmelis.rhcloud.com/xmlrpc.php"));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	      client = new XmlRpcClient();
	      client.setConfig(config);
	      
	      //---------setup parameters---------------
	      
	      params = new Vector();
	      params.addElement(new Integer(0));
	      params.addElement("martinmelis");
	      params.addElement("P(qo#zKmm6hfXAq*X8");
	}
	
	public void publishFareToPortal(RoundTripFare fare) throws Exception {
	    
		initializeConnection ();
	     /*	
		AirportStructure origin = new AirportStructure (null, null, null, null, null, null, null, null, null, null, null);
		AirportStructure destination = new AirportStructure (null, null, null, null, null, null, null, null, null, null, null);
		origin.setCityName("Vienna");
		destination.setCityName("Kutaisi");
		
		origin.setCountry("Austria");
		destination.setCountry("Georgia");
		
		origin.setLatitude(1.0);
		origin.setLongtitude(1.0);
		
		destination.setLatitude(1.0);
		destination.setLongtitude(1.0);
		
		RoundTripFare fare = new RoundTripFare(origin, destination, 0, null, null, 0, 0, null);
		fare.setBookingURL("booking URL");
		
		fare.setOrigin(origin);
		fare.setDestination(destination);
		fare.setPrice(0);
		fare.setSaleRatio(0);
		fare.setDealRatio(0);
		*/
		
	      //-------------------Automatic thumbnail insertion----------------------
	      
	      
	      Object a[] = (Object[])client.execute("wp.getMediaLibrary", params);
	    	Integer mediaItemIndex = 0;
	    	Integer mediaID = null;
	    	Boolean cityFound = false;
	    	Boolean countryFound = false;
	    	Boolean regionFound = false;
	    	
	    	//-----------First we check whether we have picture uploaded with the name of a city---------
	    	
	    	for (mediaItemIndex=0;mediaItemIndex<a.length;mediaItemIndex++)
	    	{
	    		String city = '/' + fare.getDestination().getCityName().replaceAll("'", "").replaceAll(" ", "_").toLowerCase() + ".jpg";   	 
	    		String link = ((String)((HashMap)a[mediaItemIndex]).get("link"));
	    			    				
	    		if(link.toLowerCase().contains(city))
	    		{
	    			cityFound = true;
	    			String wpMediaID = (String) ((HashMap)a[mediaItemIndex]).get("attachment_id");
	    			mediaID = Integer.parseInt(wpMediaID);
	    			break;
	    		}
	    	}
	    	
	    	//---------If picture is not accounted for this city we check for the country---------
	    	
	    	if (cityFound==false)
	    	{
	    		for (mediaItemIndex=0;mediaItemIndex<a.length;mediaItemIndex++)
	        	{
	    			

		    		String country = '/' + fare.getDestination().getCountry().replaceAll("'", "").replaceAll(" ", "_").toLowerCase() + ".jpg";       		
		    		String link = ((String)((HashMap)a[mediaItemIndex]).get("link"));
		    		
	        		if(link.toLowerCase().contains(country.toLowerCase()))
	        		{
	        			countryFound = true;
	        			String wpMediaID = (String) ((HashMap)a[mediaItemIndex]).get("attachment_id");
		    			mediaID = Integer.parseInt(wpMediaID);
		    			break;
	        		}
	        	}
	    	}
	    	
	    	//TODO
	    	/*
	    	
	    	//---------If there is no picture for city or country we use the region----------
	    	
	    	if (cityFound == false && countryFound==false)
	    	{
	    		for (mediaItemIndex=0;mediaItemIndex<a.length;mediaItemIndex++)
	        	{
	        		String region = '/' + fare.getDestination().getRegion().replaceAll("'", "").replaceAll(" ", "_").toLowerCase() + ".jpg";       		
		    		String link = ((String)((HashMap)a[mediaItemIndex]).get("link"));
		    		
	        		if(link.toLowerCase().contains(region.toLowerCase()))
	        		{
	        			countryFound = true;
	        			String wpMediaID = (String) ((HashMap)a[mediaItemIndex]).get("attachment_id");
		    			mediaID = Integer.parseInt(wpMediaID);
		    			break;
	        		}
	        	}
	    	}
	    	*/
	    	
	    	//If nothing is present we use the generic one
	    	if (cityFound == false && countryFound == false && regionFound == false)
	    		mediaID = 4960;
	      
	      //---------------------------------------------------------------------
	      
	      
	      //------------Setting up post content---------------  
	      
	      
	      Hashtable post = new Hashtable();
	      post.put("post_title", fare.getOrigin().getCityName() + " to " + fare.getDestination().getCityName());
	      post.put("post_content","Price: "+ fare.getPrice() + "\nSale: " + fare.getSaleRatio() + "\nEUR/Km: " + fare.getDealRatio() + "\nURL: " + fare.getBookingURL());
		  post.put("post_status", "publish");
	      post.put("post_thumbnail", mediaID.toString());
	      post.put("comment_status", "open");
	      post.put("ping_status", "open");
	      
	      Hashtable taxonomies = new Hashtable();
	      
	      List<String> categories = new ArrayList<String>();
	      
	      /*
	      Set<ItemTheme> themes = item.getItemThemes();
	      for (Iterator iterator = themes.iterator(); iterator.hasNext();) {
			ItemTheme itemTheme = (ItemTheme) iterator.next();
			Theme theme = itemTheme.getTheme();
			categories.add(theme.getTitle());
	      }
	      */
	      
	      categories.add("TopDeals");
	      
	      //custom taxonomies...
	      List<String> tags = new ArrayList<String>();
	      List<String> persons = new ArrayList<String>();
	      List<String> places = new ArrayList<String>();
	      List<String> events = new ArrayList<String>();
	      List<String> organizations = new ArrayList<String>();
	      List<String> source = new ArrayList<String>();		      
	      //..add keywords to your taxonomies...
	      
	      /*
	      for (Iterator iterator = themes.iterator(); iterator.hasNext();) {
			String theme = (String) iterator.next();
			categories.add(theme);
	      }
	      */
	      	      
	      taxonomies.put("category", categories);
	      //taxonomies.put("post_tag", tags);
	      //taxonomies.put("person", persons);
	      //taxonomies.put("place", places);
	      //taxonomies.put("event", events);
	      //taxonomies.put("organization", organizations);
	      
	      
	      	
	    //custom fields....	      
	      List<Hashtable> customFieldsList = new ArrayList<Hashtable>();
	      
	      Hashtable customFields = new Hashtable();
    	  customFields.put("key", "origin");
	      customFields.put("value", fare.getOrigin().getCityName());
	      customFieldsList.add(customFields);
	      
	      customFields = new Hashtable();
	      customFields.put("key", "destination");
	      customFields.put("value", fare.getDestination().getCityName());
	      customFieldsList.add(customFields);
	      
	      //----Date format definition------
	      SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy (E)");
	      
	      customFields = new Hashtable();
	      customFields.put("key", "outboundDate");
	      customFields.put("value", dateFormat.format(fare.getOutboundLeg()));
	      customFieldsList.add(customFields);
	      
	      customFields = new Hashtable();
	      customFields.put("key", "inboundDate");
	      customFields.put("value", dateFormat.format(fare.getInboundLeg()));
	      customFieldsList.add(customFields);
	      
	      customFields = new Hashtable();
	      customFields.put("key", "price");
	      customFields.put("value", fare.getPrice());
	      customFieldsList.add(customFields);
	      
	      customFields = new Hashtable();
	      customFields.put("key", "sale");
	      customFields.put("value", fare.getSaleRatio());
	      customFieldsList.add(customFields);
	      
	      customFields = new Hashtable();
	      customFields.put("key", "bookingURL");
	      customFields.put("value", fare.getBookingURL());
	      customFieldsList.add(customFields);
	      
	      post.put("custom_fields", customFieldsList);
	      
	      
	      post.put("terms_names", taxonomies);	
	          params.addElement(post);
    		  //log.debug("params:" + params);
    		  String postId = (String) client.execute("wp.newPost", params);
    	     
	  
}
	
	public void updateFareOnPortal(RoundTripFare fare) throws Exception {
	
		initializeConnection();
		Hashtable post = new Hashtable();
		post.put("post_title", fare.getOrigin().getCityName() + " to " + fare.getDestination().getCityName());
	    post.put("post_content","Price: "+ fare.getPrice() + "\nSale: " + fare.getSaleRatio() + "\nEUR/Km: " + fare.getDealRatio() + "\nURL: " + fare.getBookingURL());
		
	}
	
	public void outDateFareOnPortal(RoundTripFare fare) throws Exception {
		
		initializeConnection();
		Hashtable post = new Hashtable();
		post.put("post_title", "[SOLD-OUT]" + fare.getOrigin().getCityName() + " to " + fare.getDestination().getCityName());
		params.addElement(fare.getPortalPostID());
  		params.addElement(post);
  		client.execute("wp.editPost", params);
  	    
	}
	
	//TODO change from int ID to fare.get
	public void deleteFareOnPortal(int portalPostID) throws Exception {
		
		Vector paramsDelete = new Vector();
		paramsDelete.addElement(new Integer(0));
		paramsDelete.addElement(portalPostID);
		paramsDelete.addElement("martinmelis");
		paramsDelete.addElement("P(qo#zKmm6hfXAq*X8");
		client.execute("metaWeblog.deletePost", paramsDelete);
  	      
	}
	
	public void deleteAllFaresOnPortal () throws NumberFormatException, Exception
	{
		initializeConnection();
		Object a[] = (Object[])client.execute("metaWeblog.getRecentPosts", params);
		 
		int postIndex = 0;
		ArrayList<Integer> postIDs = new ArrayList<Integer> ();
		
		for (postIndex=0;postIndex<a.length;postIndex++)
    	{
			String postID = ((String)((HashMap) a[postIndex]).get("postid"));
			postIDs.add(Integer.parseInt(postID));
    	}
			
		for (int postID: postIDs)
			deleteFareOnPortal(postID);
		
	}
	
}
