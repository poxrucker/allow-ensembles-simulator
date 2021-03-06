package allow.simulator.flow.activity.person;

import java.util.List;

import allow.simulator.entity.Person;
import allow.simulator.entity.knowledge.Experience;
import allow.simulator.entity.knowledge.TravelExperience;
import allow.simulator.entity.relation.Relation;
import allow.simulator.flow.activity.Activity;
import allow.simulator.flow.activity.MovementActivity;
import allow.simulator.mobility.data.TType;
import allow.simulator.util.Coordinate;
import allow.simulator.util.Geometry;
import allow.simulator.world.StreetSegment;

/**
 * Represents a cycling Activity.
 * 
 * @author Andreas Poxrucker (DFKI)
 *
 */
public final class Cycle extends MovementActivity {
	
	/**
	 * Creates new instance of the cycling Activity.
	 * 
	 * @param person The person moving.
	 * @param path The path to cycle.
	 */
	public Cycle(Person entity, List<StreetSegment> path) {
		super(Activity.Type.CYCLE, entity, path);
	}
	
	@Override
	public double execute(double deltaT) {
		
		if (isFinished()) 
			return 0.0;
		
		// Note tStart.
		if (tStart == -1) {
			tStart = entity.getContext().getTime().getTimestamp();
		}

		// Register for knowledge exchange.
		entity.getRelations().addToUpdate(Relation.Type.DISTANCE);
		double rem = travel(deltaT);
		entity.setPosition(getCurrentPosition());

		if (isFinished()) {
			
			for (Experience ex : experiences) {
				entity.getKnowledge().collect(ex);
			}
		}
		return rem;
	}
	
	/**
	 * 
	 * 
	 * @param travelTime Time interval for travelling.
	 * @return Time used to travel which may be less than travelTime,
	 * if journey finishes before travelTime is over.
	 */
	private double travel(double travelTime) {
		double deltaT = 0.0;
		
		while (deltaT < travelTime && !isFinished()) {
			// Get current state.
			StreetSegment s = getCurrentSegment();
			double v = s.getCyclingSpeed();
			Coordinate p = getCurrentPosition();
			
			// Compute distance to next segment (i.e. end of current segment).
			double distToNextSeg = Geometry.haversine(p, s.getEndPoint());
			
			// Compute distance to travel within deltaT seconds.
			double distToTravel = (travelTime - deltaT) * v;
					
			if (distToTravel >= distToNextSeg) {
				// If distance to travel is bigger than distance to next segment,
				// a new log entry needs to be created.
				double tNextSegment = distToNextSeg / v;
				
				double sumTravelTime = segmentTravelTime + tNextSegment;
				tEnd = tStart + (long) sumTravelTime;
				Experience newEx = new TravelExperience(s,
						sumTravelTime,
						s.getLength() * 0.000005,
						TType.BICYCLE, 
						tStart,
						tEnd,
						s.getNumberOfVehicles(),
						0,
						null,
						entity.getContext().getWeather().getCurrentState());
				experiences.add(newEx);
				segmentTravelTime = 0.0;
				distOnSeg = 0.0;
				tStart = tEnd;
				deltaT += tNextSegment;
				distanceIndex++;
				
			} else {
				// If distance to next segment is bigger than distance to travel,
				// update time on segment, travelled distance, and reset deltaT.
				segmentTravelTime += (travelTime - deltaT);
				distOnSeg += distToTravel;
				deltaT += (travelTime - deltaT);
			}
			if (experiences.size() == path.size())
				setFinished();
		}
		return deltaT;
	}
	
	public String toString() {
		return "Cycle " + entity;
	}
}
