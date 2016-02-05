package allow.simulator.mobility.data;

/**
 * Different means of transportation supported in the simulation.
 * 
 * @author Andreas Poxrucker (DFKI)
 *
 */
public enum TType {
	
	/**
	 * Walking.
	 */
	WALK,
	
	/**
	 * Driving.
	 */
	CAR,
	
	/**
	 * Going by bus.
	 */
	BUS,
	
	/**
	 * Going by train.
	 */
	RAIL,
	
	/**
	 * Going by cable car.
	 */
	CABLE_CAR,
	
	/**
	 * Going by bike.
	 */
	BICYCLE,
	
	/**
	 * Using general public transport.
	 */
	TRANSIT,
	
	/**
	 * Using a FlexiBus.
	 */
	FLEXIBUS;
	
	public static byte getEncoding(TType type) {
		
		switch (type) {
		case WALK:
			return 0;
		
		case CAR:
			return 1;
		
		case BUS:
			return 2;
		
		case RAIL:
			return 3;
		
		case CABLE_CAR:
			return 4;
		
		case BICYCLE:
			return 5;
		
		case TRANSIT:
			return 6;
		
		case FLEXIBUS:
			return 7;
			
		default:
			throw new IllegalArgumentException("Error: Unknown transporation type " + type);
		}
	}
}
