package com.martinmelis.web.farefinder.farescraper;

import java.util.HashMap;

import dataTypes.AirportStructure;

public class CachingModule {

	private HashMap <Integer,AirportStructure> locationDictionary;
	
	public CachingModule(){
		locationDictionary = new HashMap <Integer,AirportStructure> ();
	}

	public HashMap<Integer, AirportStructure> getLocationDictionary() {
		return locationDictionary;
	}

	public void setLocationDictionary(HashMap<Integer, AirportStructure> locationDictionary) {
		this.locationDictionary = locationDictionary;
	}
	
}
