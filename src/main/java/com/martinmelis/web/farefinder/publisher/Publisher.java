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
	    config.setBasicPassword("Patrolman486@");
	    config.setBasicUserName("martinmelis");
	    config.setEnabledForExtensions(true);
		      try {
				config.setServerURL(new URL("http://www.errorflights.com/xmlrpc.php"));
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
	      params.addElement("Patrolman486@");
	     
	}
	
	public Integer publishFareToPortal(RoundTripFare fare) throws Exception {
	    
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
	    		mediaID = 4910;
	      
	      //---------------------------------------------------------------------
	      
	      
	      //------------Setting up post content---------------  
	      
	      
	      Hashtable post = new Hashtable();
	      post.put("post_title", fare.getOrigin().getCityName() + " to " + fare.getDestination().getCityName());
	      post.put("post_content","Price: "+ fare.getPrice() + "\nSale: " + fare.getSaleRatio() + "\nEUR/Km: " + fare.getDealRatio() + "\n<a href=\""+ fare.getBookingURL() + "\">URL</a>");
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
	      customFields.put("value", fare.getOrigin().getCityName() + " (" + fare.getOrigin().getCountry() + ")" );
	      customFieldsList.add(customFields);
	      
	      customFields = new Hashtable();
	      customFields.put("key", "destination");
	      customFields.put("value", fare.getDestination().getCityName() + " (" + fare.getDestination().getCountry() + ")");
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
    		  fare.setPortalPostID(Integer.parseInt(postId));
    		  
    return 	 Integer.parseInt(postId);   
	  
}
	
	public void updateFareOnPortal(RoundTripFare fare, String newStatus) throws Exception {
	
		//for the existing custom fields we get their IDs
		System.out.println(fare.getPortalPostID());
		HashMap<String,Integer> CFMap = getCustomFieldIDs (fare.getPortalPostID());
		
		initializeConnection();
		
		//first we need to get id of post relevant for current fare		
		params.addElement(fare.getPortalPostID());
		Hashtable taxonomies = new Hashtable();
		//now we update the specific fare on portal
		
		Hashtable post = new Hashtable();
		if (newStatus.equals("updated"))
			post.put("post_title", "[UPDATED] " + fare.getOrigin().getCityName() + " to " + fare.getDestination().getCityName());
		if (newStatus.equals("expired"))
			post.put("post_title", "[EXPIRED] " + fare.getOrigin().getCityName() + " to " + fare.getDestination().getCityName());
		if (newStatus.equals("new"))
			post.put("post_title",fare.getOrigin().getCityName() + " to " + fare.getDestination().getCityName());	
		if (newStatus.equals("updated") || newStatus.equals("actianalyzeResidualFaresve"))
			post.put("post_content","Price: "+ fare.getPrice() + "\nSale: " + fare.getSaleRatio() + "\nEUR/Km: " + fare.getDealRatio()  + "\n<a href=\""+ fare.getBookingURL() + "\">URL</a>");
		
		
			// we set new value for custom field ID
		if (!newStatus.equals ("expired"))	
				{
					 //custom fields....	      
				      List<Hashtable> customFieldsList = new ArrayList<Hashtable>();
				      
				      Hashtable customFields = new Hashtable();
				      if (CFMap.containsKey("origin"))
				    	  customFields.put("id", CFMap.get("origin"));
				      customFields.put("key", "origin");
				      customFields.put("value", fare.getOrigin().getCityName() + " (" + fare.getOrigin().getCountry() + ")" );
				      customFieldsList.add(customFields);
				      
				      customFields = new Hashtable();
				      if (CFMap.containsKey("destination"))
				    	  customFields.put("id", CFMap.get("destination"));
				      customFields.put("key", "destination");
				      customFields.put("value", fare.getDestination().getCityName() + " (" + fare.getDestination().getCountry() + ")");
				      customFieldsList.add(customFields);
				      
				      //----Date format definition------
				      SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy (E)");
				      
				      customFields = new Hashtable();
				      if (CFMap.containsKey("outboundDate"))
				    	  customFields.put("id", CFMap.get("outboundDate"));
				      customFields.put("key", "outboundDate");
				      customFields.put("value", dateFormat.format(fare.getOutboundLeg()));
				      customFieldsList.add(customFields);
				      
				      customFields = new Hashtable();
				      if (CFMap.containsKey("inboundDate"))
				    	  customFields.put("id", CFMap.get("inboundDate"));
				      customFields.put("key", "inboundDate");
				      customFields.put("value", dateFormat.format(fare.getInboundLeg()));
				      customFieldsList.add(customFields);
				      
				      customFields = new Hashtable();
				      if (CFMap.containsKey("price"))
				    	  customFields.put("id", CFMap.get("price"));
				      customFields.put("key", "price");
				      customFields.put("value", fare.getPrice());
				      customFieldsList.add(customFields);
				      
				      customFields = new Hashtable();
				      if (CFMap.containsKey("sale"))
				    	  customFields.put("id", CFMap.get("sale"));
				      customFields.put("key", "sale");
				      customFields.put("value", fare.getSaleRatio());
				      customFieldsList.add(customFields);
				      
				      customFields = new Hashtable();
				      if (CFMap.containsKey("bookingURL"))
				    	  customFields.put("id", CFMap.get("bookingURL"));
				      customFields.put("key", "bookingURL");
				      customFields.put("value", fare.getBookingURL());
				      customFieldsList.add(customFields);
				      
				      post.put("custom_fields", customFieldsList);
	}
	      
	      
	      List<String> categories = new ArrayList<String>();
	      if (newStatus.equals("active") || newStatus.equals("updated"))
	    	  categories.add("TopDeals"); 
	      if (newStatus.equals("expired"))
	    	  categories.add("ExpiredDeals"); 
	      
	      taxonomies.put("category", categories);
	      post.put("terms_names", taxonomies);
	      
	      params.addElement(post);
	          
	          
		client.execute("wp.editPost", params);
	}
	
	public void deleteFareOnPortal(RoundTripFare fare) throws Exception {
		deletePostOnPortal(fare.getPortalPostID());  	      
	}
	
	public void deletePostOnPortal(Integer postID) throws Exception {
		
		Vector paramsDelete = new Vector();
		paramsDelete.addElement(new Integer(0));
		paramsDelete.addElement(postID);
		paramsDelete.addElement("martinmelis");
		paramsDelete.addElement("Patrolman486@");
		client.execute("metaWeblog.deletePost", paramsDelete);
  	      
	}
	
	public HashMap <String,Integer> getCustomFieldIDs(int postID) throws XmlRpcException{
		
		HashMap <String,Integer> resultMap = new HashMap<String,Integer> ();
		
		initializeConnection();		 
		
		Vector paramsGet = new Vector();
		paramsGet.addElement(new Integer (0));
		paramsGet.addElement("martinmelis");
		paramsGet.addElement("Patrolman486@");
		paramsGet.addElement(postID);
		
		Object b = (Object)client.execute("wp.getPost", paramsGet);
		Object customFields [] = ((Object [])((HashMap) b).get("custom_fields"));
		
		int i = 0;
		
		for (i=0;i<customFields.length;i++)
		{
			String customFieldNameKey = (String)(((HashMap)customFields[i]).get("key"));
			Integer customFieldID =Integer.parseInt((String)(((HashMap)customFields[i]).get("id")));
			resultMap.put(customFieldNameKey, customFieldID);
		}
			
		return resultMap;
		
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
			deletePostOnPortal(postID);
		
	}
	
}
