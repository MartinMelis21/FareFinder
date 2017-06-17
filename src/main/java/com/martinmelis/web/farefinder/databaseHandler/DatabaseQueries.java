package com.martinmelis.web.farefinder.databaseHandler;

public class DatabaseQueries {
	public final static String getAirportOnIDSQL = 			"SELECT a.airportName,a.airportCity,a.airportCountry,a.latitude,a.longtitude,a.iataFaa,a.altitude,a.icao,z.id, a.airportID, a.SkyScannerID from Airports a left join Countries c on a.airportCountry=c.countryName left join Zones z on c.zone=z.id WHERE airportID = ?";
	public final static String getAirportOnSSIDSQL = 		"SELECT a.airportName,a.airportCity,a.airportCountry,a.latitude,a.longtitude,a.iataFaa,a.altitude,a.icao,z.id, a.airportID, a.SkyScannerID from Airports a left join Countries c on a.airportCountry=c.countryName left join Zones z on c.zone=z.id WHERE SkyScannerID = ?";
	public final static String getAirportOnIataFaaSQL = 	"SELECT a.airportName,a.airportCity,a.airportCountry,a.latitude,a.longtitude,a.iataFaa,a.altitude,a.icao,z.id, a.airportID, a.SkyScannerID from Airports a left join Countries c on a.airportCountry=c.countryName left join Zones z on c.zone=z.id WHERE a.iataFaa = ?";
	public final static String getFareSQL = 				"SELECT o.airportName,o.airportCity,o.airportCountry,o.latitude,o.longtitude,o.iataFaa,o.altitude,o.icao,oc.zone,d.airportName,d.airportCity,d.airportCountry,d.latitude,d.longtitude,d.iataFaa,d.altitude,d.icao,dc.zone,f.lastAccountedPriceRoundTrip, f.numberOfAccountedPricesRoundTrip, f.averagePriceRoundTrip, o.SkyScannerID, d.SkyScannerID,  if(f.lastFareNotification = '0000-00-00', null, f.lastFareNotification), f.portalPostID, f.portalPostStatus as lastFareNotification from Fares f, Airports o, Airports d, Countries oc, Countries dc where f.origin = ? and f.destination = ? and f.origin=o.airportID and f.destination=d.airportID and oc.countryName=o.airportCountry and dc.countryName=d.airportCountry";
	public final static String addAirportSQL = 				"INSERT INTO Airports (iataFaa, airportName, airportCity, airportCountry, latitude, longtitude, altitude, icao) values (?, ?, ?, ?, ?, ?, ?, ?)";
	public final static String updateSSIDSQL = 				"UPDATE Airports SET SkyScannerID = ? WHERE iataFaa = ?";
	public final static String checkFareExistance = 		"SELECT numberOfAccountedPricesRoundTrip, averagePriceRoundTrip FROM Fares WHERE origin = (SELECT airportID FROM Airports WHERE iataFaa = ?) AND destination = (SELECT airportID FROM Airports WHERE iataFaa = ?)";
	public final static String updateRoutePrice = 			"UPDATE Fares SET numberOfAccountedPricesRoundTrip = ?,lastAccountedPriceRoundTrip = ?,lastAccountedPriceTimeRoundTrip = NOW(),averagePriceRoundTrip = ?,outboundDate = ?,inboundDate = ? WHERE origin=(SELECT airportID FROM Airports WHERE iataFaa = ?) AND destination = (SELECT airportID FROM Airports WHERE iataFaa = ?)";
	public final static String insertRoutePrice = 			"INSERT INTO Fares (origin,destination,lastAccountedPriceTimeRoundTrip,lastAccountedPriceRoundTrip,numberOfAccountedPricesRoundTrip,averagePriceRoundTrip,outboundDate,inboundDate,portalPostID)VALUES ((SELECT airportID FROM Airports WHERE iataFaa = ?),(SELECT airportID FROM Airports WHERE iataFaa = ?),NOW(),?,1,?,?,?,?);";
	public final static String getSSIDMapping =				"SELECT airportID,SkyScannerID FROM Airports";
	public final static String getIataMapping =				"SELECT airportID,iataFaa FROM Airports";
	public final static String updateFarePublication =		"UPDATE Fares SET lastFareNotification = NOW(),portalPostID = ?,portalPostStatus = ? WHERE origin = ? and destination = ? ";
	public final static String getResidualFares =			"SELECT f.origin, f.destination, f.lastAccountedPriceRoundTrip, f.outboundDate,f.inboundDate, f.portalPostID, f.portalPostStatus, o.airportName, o.airportCity,o.airportCountry,o.latitude,o.longtitude,o.iataFaa,o.altitude,o.icao,oc.id,o.SkyScannerID, d.airportName, d.airportCity,d.airportCountry,d.latitude,d.longtitude,d.iataFaa,d.altitude,d.icao,dc.id,d.SkyScannerID, f.averagePriceRoundTrip, f.numberOfAccountedPricesRoundTrip, if(f.lastFareNotification = '0000-00-00', null, f.lastFareNotification) FROM Fares f, Airports o, Airports d, Countries oc, Countries dc WHERE o.airportCountry=oc.countryName and d.airportCountry=dc.countryName and f.origin=o.airportID and f.destination = d.airportID and portalPostID IS NOT NULL AND portalPostStatus <> 'expired'";	
	public final static String getOriginAirports =			"select distinct (o.iataFaa) from Fares f, Airports o, Countries oc where f.origin=o.airportID and oc.countryName=o.airportCountry and oc.countryCode=?";
	public final static String updateRegionalRoutePrice = 	"UPDATE InterregionalPriceAverages SET numberOfAccountedPricesRoundTrip = ?,lastAccountedPriceRoundTrip = ?,lastAccountedPriceTimeRoundTrip = NOW(),averagePrice = ? WHERE departureRegion = ? AND arrivalRegion = ?";
	public final static String insertRegionalRoutePrice = 	"INSERT INTO InterregionalPriceAverages (departureRegion,arrivalRegion,averagePrice,numberOfAccountedPricesRoundTrip,lastAccountedPriceRoundTrip,lastAccountedPriceTimeRoundTrip)VALUES (?,?,?,?,?,NOW());";
	public final static String getInterregionalFares =		"SELECT * FROM InterregionalPriceAverages";
}
