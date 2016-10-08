package dataTypes;

import java.util.Date;

public class RoundTripFare {

	private AirportStructure origin = null;
	private AirportStructure destination = null;
	private Date outboundLeg = null;
	private Date inboundLeg = null;
	private Date lastFareNotification = null;
	private int price;
	private double baseFare;
	private double dealRatio;
	private double saleRatio;
	private boolean isNew;
	private String bookingURL = null;
	private Integer numberOfAccountedPricesRoundTrip;
	private Integer portalPostID = null;
	private String portalPostStatus = null;
	private Integer lastAccountedPrice = null;
	
	
	public RoundTripFare(AirportStructure origin, AirportStructure destination, int price, Date outbound, Date inbound, double baseFare, int numberOfAccountedFares, Date lastFareNotification,Integer portalPostID,String portalPostStatus) {
		super();
		this.origin = origin;
		this.destination = destination;
		this.outboundLeg = outbound;
		this.inboundLeg = inbound;
		this.price = price;
		//TODO What if by mistake I dont have Airport GPS
		this.dealRatio = price/getDistance();
		this.baseFare = baseFare;
		this.saleRatio = (1-(price/baseFare))*100;
		this.isNew = false;
		this.numberOfAccountedPricesRoundTrip = numberOfAccountedFares;
		this.lastFareNotification = lastFareNotification;
		this.portalPostID = portalPostID;
		this.portalPostStatus = portalPostStatus;
	}
		
	private double getDistance () {
		double theta = origin.getLongtitude() - destination.getLongtitude();
		double dist = Math.sin((origin.getLatitude() * Math.PI / 180.0)) * Math.sin((destination.getLatitude() * Math.PI / 180.0)) + Math.cos((origin.getLatitude() * Math.PI / 180.0)) * Math.cos((destination.getLatitude() * Math.PI / 180.0)) * Math.cos((theta * Math.PI / 180.0));
		dist = Math.acos(dist);
		dist = dist * 180 / Math.PI;
		dist = dist * 60 * 1.1515;
			dist = dist * 1.609344;

		return (dist);
	}
	
	
	
	public Integer getLastAccountedPrice() {
		return lastAccountedPrice;
	}

	public void setLastAccountedPrice(Integer lastAccountedPrice) {
		this.lastAccountedPrice = lastAccountedPrice;
	}

	public String getPortalPostStatus() {
		return portalPostStatus;
	}

	public void setPortalPostStatus(String portalPostStatus) {
		this.portalPostStatus = portalPostStatus;
	}

	public Integer getPortalPostID() {
		return portalPostID;
	}

	public void setPortalPostID(Integer portalPostID) {
		
		try{
		//TODO I need to insert new portalPostID to database
			
		this.portalPostID = portalPostID;
		}
		catch(Exception e){
			
		}
	}

	public String getBookingURL() {
		return bookingURL;
	}

	public void setBookingURL(String bookingURL) {
		this.bookingURL = bookingURL;
	}

	public Date getLastFareNotification() {
		return lastFareNotification;
	}

	public void setLastFareNotification(Date lastFareNotification) {
		this.lastFareNotification = lastFareNotification;
	}

	public Integer getNumberOfAccountedPricesRoundTrip() {
		return numberOfAccountedPricesRoundTrip;
	}

	public void setNumberOfAccountedPricesRoundTrip(Integer numberOfAccountedPricesRoundTrip) {
		this.numberOfAccountedPricesRoundTrip = numberOfAccountedPricesRoundTrip;
	}

	public void setIsNew() {
		this.isNew = true;
	}
	
	public boolean getIsNew() {
		return this.isNew;
	}
	
	public double getSaleRatio() {
		return saleRatio;
	}

	public void setSaleRatio(double saleRatio) {
		this.saleRatio = saleRatio;
	}

	public double getBaseFare() {
		return baseFare;
	}

	public void setBaseFare(double baseFare) {
		this.baseFare = baseFare;
	}

	public AirportStructure getOrigin() {
		return origin;
	}
	public void setOrigin(AirportStructure origin) {
		this.origin = origin;
	}
	public AirportStructure getDestination() {
		return destination;
	}
	public void setDestination(AirportStructure destination) {
		this.destination = destination;
	}
	public Date getOutboundLeg() {
		return outboundLeg;
	}
	public void setOutboundLeg(Date outboundLeg) {
		this.outboundLeg = outboundLeg;
	}
	public Date getInboundLeg() {
		return inboundLeg;
	}
	public void setInboundLeg(Date inboundLeg) {
		this.inboundLeg = inboundLeg;
	}
	public int getPrice() {
		return price;
	}
	public void setPrice(int price) {
		this.price = price;
	}
	public double getDealRatio() {
		return dealRatio;
	}
	public void setDealRatio(double dealRatio) {
		this.dealRatio = dealRatio;
	}
	
	
	
}
