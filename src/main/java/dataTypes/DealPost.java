package dataTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class DealPost {

	private ArrayList <RoundTripFare> aggregatedFares;
	private String destinationCountry;

	public DealPost(String destinationCountry) {
		super();
		aggregatedFares = new ArrayList <RoundTripFare> ();
		this.destinationCountry = destinationCountry;
	}
	
	public void addFare (RoundTripFare fare) {
		this.aggregatedFares.add(fare);
	}
	
	public String getDestinationCountry (){
		
		return this.destinationCountry;
	}
	
	
	public String getOriginZonesString (){
		String originZones = "";
		ArrayList <String> zones = new ArrayList <String> ();
		String zoneString;
		
		for (RoundTripFare fare :this.aggregatedFares)
		{
			zoneString = fare.getOrigin().getZoneString();
			if (!zones.contains(zoneString))
				zones.add(zoneString);
		}
		
		//for each of the fares we identify the zone of the fare and add to a unique list
		
		int iterationNumber = 0;
		for (String zone :zones)
		{
			if (iterationNumber == 0)
			{
				originZones = originZones.concat(zone);
			}
			else
			{
				if (iterationNumber != (zones.size()-2))
				{
					originZones = originZones.concat(", " + zone);
				}
				else
				{
					originZones = originZones.concat(" and " + zone);
				}
			}
			
			iterationNumber++;
		}
		
		
		return "From" + originZones +"";
	}
	
	public Integer getLowestPrice (){
		
		//for each of the  fares I find the minimum price
		Integer minimumPrice = null;
		
		for (RoundTripFare fare :aggregatedFares)
		{
			if (minimumPrice==null || fare.getPrice() <minimumPrice)
				minimumPrice = fare.getPrice();
		}
	
		return minimumPrice;
	}
	
	public String getPostTextContent (){
		
		//for each of the  fares I find the minimum price
				String contentText = "";
				
				HashMap <String,ArrayList<RoundTripFare>> sortedFares = new HashMap <String,ArrayList<RoundTripFare>> ();
				
				for (RoundTripFare fare :aggregatedFares)
				{
				ArrayList<RoundTripFare> faresForCity = null;
				
				if ((faresForCity = sortedFares.get(fare.getDestination().getCityName())) != null)
				{
					faresForCity.add(fare);
				}
				else
				{
					faresForCity = new ArrayList<RoundTripFare>();
					faresForCity.add(fare);
					sortedFares.put(fare.getDestination().getCityName(), faresForCity);
				}
				}
								
				
				for (ArrayList<RoundTripFare> faresForCountry : sortedFares.values()){
					
				Collections.sort(faresForCountry, new Comparator<RoundTripFare>() {
			        @Override public int compare(RoundTripFare fare1, RoundTripFare fare2) {
			            return fare1.getPrice()- fare2.getPrice(); // Ascending
			        }
				});
			    }
				
				for (String destinationCity :sortedFares.keySet()){
					
					ArrayList<RoundTripFare> aggregatedFares = sortedFares.get(destinationCity);
					contentText = contentText.concat("Destination:\t<b>"+destinationCity+"\n</b>");
					for (RoundTripFare fare :aggregatedFares)
					{
						String origin = fare.getOrigin().getCityName();
						String destination =fare.getDestination().getCityName();
						String outboundDateString = fare.getOutboundLeg().toString();
						String inboundDateString = fare.getInboundLeg().toString();
						Integer price = fare.getPrice();
						String bookingURL = fare.getBookingURL();
						contentText = contentText.concat("<a href =\""+ bookingURL +"\">\tfrom"+origin+"\t on\t"+outboundDateString+"-"+inboundDateString+"\tPrice:"+price+"\n</a>");
					}
				}
				return contentText;
	}
	
}
