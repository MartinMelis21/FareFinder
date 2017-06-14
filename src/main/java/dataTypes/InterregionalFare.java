package dataTypes;

import java.sql.Date;

public class InterregionalFare {

	private Boolean requiresDatabaseUpdate;
	private int departureRegion;
	private int arrivalRegion;
	private Double averagePrice;
	private int numberOfAccountedPrices;
	private int lastAccountedPrice;
	private Date lastAccountedPriceDate;
	
	public InterregionalFare(Integer departureRegion2, Integer arrivalRegion2, Double averagePrice2, int numberOfAccountedPricesRoundTrip, int lastAccountedPriceRoundTrip) {
		
		this.departureRegion = departureRegion2;
		this.arrivalRegion = arrivalRegion2;
		this.averagePrice = averagePrice2;
		this.numberOfAccountedPrices = numberOfAccountedPricesRoundTrip;
		this.lastAccountedPrice = lastAccountedPriceRoundTrip;
		this.requiresDatabaseUpdate = false;
		
	}
	
	
	public void setLastAccountedPriceTime (Date lastAccountedPriceTimeRoundTrip)
	{

		this.lastAccountedPriceDate = lastAccountedPriceTimeRoundTrip;
	}
	
	public Date getLastAccountedPriceTime ()
	{
		return this.lastAccountedPriceDate;
	}

	public void setLastAccountedPrice (int price)
	{
		this.lastAccountedPrice = price;
	}
	
	public int getLastAccountedPrice ()
	{
		return this.lastAccountedPrice;
	}
	
	public int getNumberOfAccountedPrices ()
	{
		return this.numberOfAccountedPrices;
	}
	
	public void requiresDatabaseUpdate ()
	{
		this.requiresDatabaseUpdate = true;
	}
	
	public void doesNotrequireDatabaseUpdate ()
	{
		this.requiresDatabaseUpdate = false;
	}
	
	public Boolean getDatabaseUpdateRequirement ()
	{
		return this.requiresDatabaseUpdate;
	}
	
	
	public void addNewAccountedPrice ()
	{
		if (this.numberOfAccountedPrices < 10000)
			this.numberOfAccountedPrices++;
		else
			this.numberOfAccountedPrices = 1;
	}
	
	public void setAveragePrice (Double newAveragePrice)
	{
		this.averagePrice = newAveragePrice;
	}
	
	public Double getAveragePrice ()
	{
		return this.averagePrice;
	}
	
	public int getDepartureRegion ()
	{
		return this.departureRegion;
	}
	
	public int getArrivalRegion ()
	{
		return this.arrivalRegion;
	}
	
}
