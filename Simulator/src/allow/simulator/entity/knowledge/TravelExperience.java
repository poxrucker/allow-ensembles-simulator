package allow.simulator.entity.knowledge;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import allow.simulator.mobility.data.TType;
import allow.simulator.util.Coordinate;
import allow.simulator.world.StreetSegment;
import allow.simulator.world.Weather;

public final class TravelExperience extends Experience {
	// Segment the statistics were collected on.
	private StreetSegment segment;
	
	// Travel time in seconds.
	private double travelTime;
	
	// Costs to travel the segment.
	private double costs;
	
	// Transportation used to travel the segment.
	private TType meansOfTransportation;
	
	// Timestamp in ms the entity began to move along the segment.
	private long timeStart;
	private LocalDateTime tStart;
	
	// Timestamp in ms the entity finished to move along the segment.
	private long timeEnd;
	
	// Number of people on the segment.
	private int nPeopleOnSegment;
	
	// If public transportation was used, number of people who were on
	// the same vehicle.
	private double publicTransportationFillingLevel;
	
	// Id of trip of public transportation.
	private String publicTransportationTripId;
	
	// Weather when segment was finished.
	private Weather.State weather;
	
	public TravelExperience(StreetSegment segment,
			double travelTime,
			double costs,
			TType meansOfTransportation,
			long timeStart,
			long timeEnd,
			int nPeopleOnSegment,
			double publicTranbsportationFillingLevel,
			String publicTransportationTripId,
			Weather.State weather) {
		super(Experience.Type.TRAVEL);
		this.segment = segment;
		this.travelTime = travelTime;
		this.costs = costs;
		this.meansOfTransportation = meansOfTransportation;
		this.timeStart = timeStart;
		tStart = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeStart), ZoneId.of("Europe/Rome"));
		this.timeEnd = timeEnd;
		this.nPeopleOnSegment = nPeopleOnSegment;
		this.publicTransportationFillingLevel = publicTranbsportationFillingLevel;
		this.publicTransportationTripId = publicTransportationTripId;
		this.weather = weather;
	}
	
	public TravelExperience(double travelTime,
			double costs,
			TType meansOfTransportation,
			long timeStart,
			long timeEnd,
			int nPeopleOnSegment,
			double publicTranbsportationFillingLevel,
			String publicTransportationTripId,
			Weather.State weather) {
		super(Experience.Type.TRAVEL);
		this.travelTime = travelTime;
		this.costs = costs;
		this.meansOfTransportation = meansOfTransportation;
		this.timeStart = timeStart;
		tStart = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeStart), ZoneId.of("Europe/Rome"));
		this.timeEnd = timeEnd;
		this.nPeopleOnSegment = nPeopleOnSegment;
		this.publicTransportationFillingLevel = publicTranbsportationFillingLevel;
		this.publicTransportationTripId = publicTransportationTripId;
		this.weather = weather;
	}
	
	private TravelExperience() {
		super(Experience.Type.TRAVEL);
	}
	
	public long getSegmentId() {
		return segment.getId();
	}
	
	public boolean isTransient() {
		return (segment == null);
	}
	
	public double getSegmentLength() {
		return segment.getLength();
	}
	
	public Coordinate getStartPosition() {
		return segment.getStartingPoint();
	}
	
	public Coordinate getEndPosition() {
		return segment.getEndPoint();
	}
	
	public double getTravelTime() {
		return travelTime;
	}
	
	public void setTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}
	
	public double getCosts() {
		return costs;
	}
	
	public TType getTransportationMean() {
		return meansOfTransportation;
	}
	
	public long getStartingTime() {
		return timeStart;
	}
	
	public LocalDateTime getTStart() {
		return tStart;
	}
	
	public void setStartingTime(long startingTime) {
		timeStart = startingTime;
		tStart = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeStart), ZoneId.of("Europe/Rome"));
	}
	
	public int getWeekday() {
		return tStart.getDayOfWeek().getValue();
	}
	
	public long getEndTime() {
		return timeEnd;
	}

	public void setEndTime(long endTime) {
		timeEnd = endTime;
	}
	
	public int getNumberOfPeopleOnSegment() {
		return nPeopleOnSegment;
	}
	
	public double getPublicTransportationFillingLevel() {
		return publicTransportationFillingLevel;
	}
	
	public void setPublicTransportationFillingLevel(double fillLevel) {
		publicTransportationFillingLevel = fillLevel;
	}
	
	public String getPublicTransportationTripId() {
		return publicTransportationTripId;
	}
	
	public Weather.State getWeather() {
		return weather;
	}
	
	public TravelExperience clone() {
		TravelExperience ret = new TravelExperience();
		ret.segment = segment;
		ret.travelTime = travelTime;
		ret.costs = costs;
		ret.meansOfTransportation = meansOfTransportation;
		ret.timeStart = timeStart;
		ret.tStart = tStart.minusSeconds(0);
		ret.timeEnd = timeEnd;
		ret.nPeopleOnSegment = nPeopleOnSegment;
		ret.publicTransportationFillingLevel = publicTransportationFillingLevel;
		ret.publicTransportationTripId = publicTransportationTripId;
		ret.weather = weather;
		return ret;
	}
}
