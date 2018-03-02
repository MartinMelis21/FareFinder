package dataTypes;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import org.joda.time.DateTime;

import com.martinmelis.web.farefinder.databaseHandler.DatabaseHandler;
import com.martinmelis.web.farefinder.farescraper.FareFetcher;
import com.martinmelis.web.farefinder.farescraper.KayakFetcher;
import com.martinmelis.web.farefinder.farescraper.KiwiFetcher;
import com.martinmelis.web.farefinder.farescraper.SkyScannerFetcher;
import com.martinmelis.web.farefinder.modules.MailSender;
import com.martinmelis.web.farefinder.publisher.Publisher;

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
	private Integer numberOfAccountedPricesRoundTrip = 0;
	private Integer portalPostID = -1;
	private String portalPostStatus = null;
	private Integer lastAccountedPrice = -1;
	
	
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
	public Boolean isInteresting() {
		
		//get the average price to that region from this region
		
		if (this.getOrigin().getZone()/10 != this.getDestination().getZone()/10  && this.getPrice() <= 300 && this.getSaleRatio() >= 30 && this.getDealRatio() <= 0.04)
			return true;
		else
			return false;
	}
	
	public Integer fetchLivePrice (ArrayList <FareFetcher> fetcherList, RoundTripFare fare) throws Exception
	{
		Integer livePrice = null;
		FareFetcher fetcher = null;
		//---TODO new sources need to be added here---
		
		//---SkyScanner fare---
			if (fare.getBookingURL().equals("http://www.skyscanner.com"))
				{
					for (FareFetcher fareFetcher:fetcherList)
					{
						if (fareFetcher instanceof SkyScannerFetcher)
						{
							fetcher = fareFetcher ;
							break;
						}
					}
				}
		
		//---Kiwi fare---
				if (fare.getBookingURL().startsWith("https://www.kiwi.com"))
					{
						for (FareFetcher fareFetcher:fetcherList)
						{
							if (fareFetcher instanceof KiwiFetcher)
							{
								fetcher = fareFetcher ;
								break;
							}
						}
					}
		
		//---Kayak fare---
				if (fare.getBookingURL().startsWith("https://www.kayak.de"))
					{
						for (FareFetcher fareFetcher:fetcherList)
						{
							if (fareFetcher instanceof KayakFetcher)
							{
								fetcher = fareFetcher ;
								break;
							}
						}
					}		
		
		
		//------return the fetched live price
				
				if (fetcher != null)
				{
					// If the live checked price is not in the limit we skip this fare
					if ((livePrice =fetcher.getLivePrice(fare))!=null)
					{
						this.price = livePrice;
						this.setLastAccountedPrice(livePrice);
						return livePrice;
					}
				}
				return null;
	}
	
	public void publishFare (Publisher portalPublisher, DatabaseHandler databaseHandler) throws Exception {
		try {
			portalPublisher.publishFareToPortal(this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.setPortalPostStatus("new");
		databaseHandler.updateFarePublication (this);
	}
	
	public void expireFarePublication (Publisher portalPublisher, DatabaseHandler databaseHandler) throws Exception {
		try {
			portalPublisher.updateFareOnPortal(this,"expired");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.setPortalPostStatus("expired");
		this.setPortalPostID(-1);
		databaseHandler.updateFarePublication (this);
	}
	
	public void updateFarePublication (Publisher portalPublisher, DatabaseHandler databaseHandler) throws Exception {
		try {
			portalPublisher.updateFareOnPortal(this,"updated");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.setPortalPostStatus("updated");
		databaseHandler.updateFarePublication (this);
	}
	
	public void notifyAboutFare (MailSender mailSender) throws SQLException {
		Date lastAccountedDate = this.getLastFareNotification();
		Date today = new Date();
		today.setHours(0); //same for minutes and seconds
		
		DateTime lastFareNotification = new DateTime (lastAccountedDate);
		//If it is more than a day or is better than 20% of previously announced
		DateTime lastWeek = DateTime.now().minusDays(7);
		
		
		if (lastAccountedDate == null || (lastFareNotification.getMillis() < lastWeek.getMillis()))
		{	//TODO or 20% better
			mailSender.sendMail("martin.melis21@gmail.com", this);//TODO send to all mail reciepts
			this.setLastFareNotification(today);
		}
	}
	
	
}
