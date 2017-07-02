package com.martinmelis.web.farefinder.farescraper;

import java.util.HashMap;

import dataTypes.AirportStructure;
import dataTypes.DealPost;
import dataTypes.RoundTripFare;

public class CachingModule {

	private HashMap <Integer,AirportStructure> locationDictionary;
	private HashMap <String,Integer> iataFaaMapping;
	private HashMap <Integer,Integer> skyScannerIDMapping;
	private HashMap <String,RoundTripFare> faresCachedList;
	private HashMap <String,DealPost> dealPosts;
	
	public HashMap<String, Integer> getIataFaaMapping() {
		return iataFaaMapping;
	}

	public void setIataFaaMapping(HashMap<String, Integer> iataFaaMapping) {
		this.iataFaaMapping = iataFaaMapping;
	}

	public CachingModule(){
		locationDictionary = new HashMap <Integer,AirportStructure> ();
		iataFaaMapping = new HashMap <String,Integer> ();
		skyScannerIDMapping = new HashMap <Integer,Integer> ();
		faresCachedList = new HashMap <String,RoundTripFare> (100000);
		dealPosts = new HashMap <String,DealPost> (100);
	}

	public HashMap<Integer, Integer> getSkyScannerIDMapping() {
		return skyScannerIDMapping;
	}
	
	public HashMap <String,RoundTripFare> getCachedFares() {
		return faresCachedList;
	}
	
	public void cacheFare(RoundTripFare fare) {
		this.faresCachedList.put(new String (fare.getOrigin().toString() + fare.getDestination().toString()),fare);
	}

	public void setSkyScannerIDMapping(HashMap<Integer, Integer> skyScannerIDMapping) {
		this.skyScannerIDMapping = skyScannerIDMapping;
	}

	public HashMap<Integer, AirportStructure> getLocationDictionary() {
		return locationDictionary;
	}

	public void setLocationDictionary(HashMap<Integer, AirportStructure> locationDictionary) {
		this.locationDictionary = locationDictionary;
	}
	
}
