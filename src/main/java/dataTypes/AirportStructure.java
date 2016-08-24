package dataTypes;

public class AirportStructure {

	private String airportName = 	null;
	private String cityName = 		null;
	private String country = 		null;
	private String iataFaa = 		null;
	private String icao = 		null;
	private Double latitude = 		null;
	private Double longtitude = 	null;
	private Double altitude = 		null;
	private Integer skyScannerID =	null;
	
	public String getIataFaa() {
		return iataFaa;
	}

	public void setIataFaa(String iataFaa) {
		this.iataFaa = iataFaa;
	}
	
	public String getIcao() {
		return icao;
	}

	public void setIcao(String icao) {
		this.icao = icao;
	}
	
	public String getAirportName() {
		return airportName;
	}

	public void setAirportName(String airportName) {
		this.airportName = airportName;
	}

	public String getCityName() {
		return cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongtitude() {
		return longtitude;
	}
	
	public void setAltitude(Double altitude) {
		this.altitude = altitude;
	}

	public Double getAltitude() {
		return altitude;
	}

	public void setLongtitude(Double longtitude) {
		this.longtitude = longtitude;
	}

	public Integer getSkyScannerID() {
		return skyScannerID;
	}

	public void setSkyScannerID(Integer skyScannerID) {
		this.skyScannerID = skyScannerID;
	}

	public AirportStructure(String airportName, String cityName, String country, Double latitude, Double longtitude,
			Integer skyScannerID, String iataFaa, Double altitude, String icao) {
		super();
		this.airportName = airportName;
		this.cityName = cityName;
		this.country = country;
		this.icao = icao;
		this.latitude = latitude;
		this.longtitude = longtitude;
		this.skyScannerID = skyScannerID;
		this.iataFaa = iataFaa;
		this.altitude = altitude;
	}

}
