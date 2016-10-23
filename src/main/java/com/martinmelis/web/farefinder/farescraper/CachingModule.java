package com.martinmelis.web.farefinder.farescraper;

import java.util.HashMap;

import dataTypes.AirportStructure;

public class CachingModule {

	private HashMap <Integer,AirportStructure> locationDictionary;
	private HashMap <String,Integer> iataFaaMapping;
	private HashMap <Integer,Integer> skyScannerIDMapping;
	
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
	}

	public HashMap<Integer, Integer> getSkyScannerIDMapping() {
		return skyScannerIDMapping;
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