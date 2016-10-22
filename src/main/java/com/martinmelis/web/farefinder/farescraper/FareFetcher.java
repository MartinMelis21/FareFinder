package com.martinmelis.web.farefinder.farescraper;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.martinmelis.web.farefinder.databaseHandler.DatabaseHandler;

import dataTypes.RoundTripFare;

public abstract class FareFetcher {

	DatabaseHandler databaseHandler;
	
	public void fareFetcher (DatabaseHandler databaseHandler)
	{
		this.databaseHandler=databaseHandler;
	}
	
	public abstract  ArrayList <RoundTripFare> getFareList (String origin);
	public  abstract Integer getLivePrice (RoundTripFare fare) throws Exception;
	
}
