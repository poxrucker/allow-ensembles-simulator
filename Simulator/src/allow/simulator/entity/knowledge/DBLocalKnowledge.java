package allow.simulator.entity.knowledge;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import allow.simulator.entity.Entity;
import allow.simulator.entity.knowledge.DBConnector.DBType;
import allow.simulator.mobility.data.TType;

public class DBLocalKnowledge implements DBKnowledgeModel {
	
	private static final String MY_SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS %1$s "
			+ "(entryNo INTEGER UNSIGNED AUTO_INCREMENT PRIMARY KEY, nodeId INT, prevNodeId INT, "
			+ "ttime DOUBLE, prevttime DOUBLE, weather TINYINT UNSIGNED, weekday TINYINT UNSIGNED, "
			+ "timeOfDay TINYINT UNSIGNED, modality TINYINT UNSIGNED, fillLevel FLOAT, startTime INT UNSIGNED, "
			+ "endTime INT UNSIGNED, INDEX(nodeId, modality, timeOfDay, weekday, prevNodeId, prevttime));%2$s";
	
	private static final String POSTGRE_SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS %1$s "
			+ " (entryNo SERIAL PRIMARY KEY, nodeId INTEGER, prevNodeId INTEGER, "
			+ "ttime REAL, prevttime REAL, weather SMALLINT, weekday SMALLINT, "
			+ "timeOfDay SMALLINT, modality SMALLINT, fillLevel REAL, startTime INTEGER, "
			+ "endTime INTEGER); CREATE INDEX on %2$s "
			+ "(nodeId, modality, timeOfDay, weekday, prevNodeId, prevttime)";
	
	private static final String MY_SQL_SHOW_TABLES = "SHOW TABLES LIKE '%1$s'";
	
	private static final String POSTGRE_SQL_SHOW_TABLES = "SELECT * FROM pg_catalog.pg_tables where "
			+ "tablename like '%1s'";
	
	private static final String SQL_INSERT_VALUES = "INSERT INTO %1$s "
			+ " (nodeId, prevNodeId, ttime, prevttime, weather, weekday, timeOfDay, "
			+ "modality, fillLevel, startTime, endTime) VALUES ";
	
	private DBType type;
	private String sqlCreateTables;
	private String sqlShowTables;
	private String sqlInsertValues;
	
	// Dictionary holding tables which have been 
	private static ConcurrentHashMap<String, Boolean> aIdTableExists = new ConcurrentHashMap<String, Boolean>();
	
	public DBLocalKnowledge(DBType type) {
		this.type = type;

		switch (type) {
		case MYSQL:
			sqlCreateTables = MY_SQL_CREATE_TABLE;
			sqlShowTables = MY_SQL_SHOW_TABLES;
			sqlInsertValues = SQL_INSERT_VALUES;
			break;
			
		case POSTGRE:
			sqlCreateTables = POSTGRE_SQL_CREATE_TABLE;
			sqlShowTables = POSTGRE_SQL_SHOW_TABLES;
			sqlInsertValues = SQL_INSERT_VALUES;
			break;
			
		default:
			throw new IllegalArgumentException("Error: Unknown DB type " + type);
		}
	}
	
	@Override
	public boolean addEntry(Entity agent, List<TravelExperience> prior, List<TravelExperience> it, String tablePrefix) {
		String agentId = String.valueOf(agent.getId());
		
		if (it.size() == 0) {
			return false;
		}
		// check if table for agent already exists (hopefully saves database
		// overhead)
		boolean tableExists = aIdTableExists.get(agentId) == null ? false : true;

		// track error state to avoid having to nest too many try catch
		// statements
		boolean error = false;

		// connection and statement for database query
		Statement stmt = null;
		Connection con = null;
		String stmtString = "";
		String tableName = tablePrefix + "_tbl_" + agentId;
		
		try {

			// get connection
			con = DSFactory.getConnection();
			stmt = con.createStatement();

			// create a new table for an agent representing his EvoKnowledge if it doesnt exist already
			if (!tableExists) {
				try {
					stmt.execute(String.format(sqlCreateTables, tableName, ((type == DBType.MYSQL) ? "" : tableName)));
				
				} catch (SQLException e) {
					e.printStackTrace();
					stmt.close();
					con.close();
					return false;
				}
				aIdTableExists.put(agentId, true);
			}

			// parse the itinerary and add a line for each entry
			stmtString = String.format(sqlInsertValues, tableName);

			boolean firstSeg = true;
			long prevNodeId = 0;
			double prevDuration = 0;

			for (TravelExperience ex : it) {
				long nodeId = ex.getSegmentId();
				long start = ex.getStartingTime();
				long end = ex.getEndTime();
				double duration = ex.getTravelTime();

				stmtString = stmtString.concat(firstSeg ? "" : ",");
				stmtString = stmtString.concat("('" + nodeId + "',");
				stmtString = stmtString.concat("'" + prevNodeId + "',");
				stmtString = stmtString.concat(duration + ",");
				stmtString = stmtString.concat(prevDuration + ",");
				stmtString = stmtString.concat(ex.getWeather().getEncoding()
						+ ",");
				stmtString = stmtString.concat(ex.getWeekday() + ",");
				stmtString = stmtString.concat(EvoEncoding.getTimeOfDay(ex
						.getTStart().getHour()) + ",");
				stmtString = stmtString.concat(TType.getEncoding(ex
						.getTransportationMean()) + ",");
				stmtString = stmtString.concat(ex.getPublicTransportationFillingLevel()
						+ ","); // Density = Number of other entities on
								// segment.
				stmtString = stmtString.concat(String.valueOf(start / 1000)
						+ ",");
				stmtString = stmtString
						.concat(String.valueOf(end / 1000) + ")");

				firstSeg = false;
				prevNodeId = nodeId;
				prevDuration = duration;
			}
			stmtString = stmtString.concat(";");

			// System.out.println(stmtString);
			stmt.execute(stmtString);

		} catch (SQLException e) {
			System.out.println(stmtString);
			// e.printStackTrace();
			error = true;
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				if (con != null)
					con.close();

			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		}
		return !error;
	}

	@Override
	public List<TravelExperience> getPredictedItinerary(Entity agent, List<TravelExperience> it, String tablePrefix) {
		String agentId = String.valueOf(agent.getId());

		// connection and statement for database query
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		String tableName = tablePrefix + "_tbl_" + agentId;
		
		try {

			// get connection
			con = DSFactory.getConnection();
			stmt = con.createStatement();

			// check if user even has evoknowledge
			rs = stmt.executeQuery(String.format(sqlShowTables, tableName));
			
			if (!rs.next()) {
				rs.close();
				return it;
			}
			String stmt1 = "SELECT %1$s FROM " + tableName
					+ " WHERE nodeId = %2$d";
			String stmt2 = stmt1.concat(" AND modality = %3$d");
			String stmt3 = stmt2.concat(" AND timeOfDay = %4$d");
			String stmt4 = stmt3.concat(" AND weekday = %5$d");
			String stmt5 = stmt4
					.concat(" AND prevNodeId = %6$d AND (prevttime BETWEEN %7$d AND %8$d)");

			String stmtString = null;
			// String stmtPredStr = null;

			boolean firstSeg = true;

			long prevNodeId = 0;
			double prevTTime = -1;

			long segmentTStart = 0;

			for (TravelExperience ex : it) {
				// System.out.println("Before: " + ex.getSegmentId() + " " +
				// ex.getStartingTime() + " " + ex.getEndTime() + " " +
				// ex.getTravelTime());

				if (firstSeg) {
					segmentTStart = ex.getStartingTime() / 1000;
				}

				if (ex.isTransient()) {
					continue;
				}
				double predictedTravelTime = ex.getTravelTime();
				double predictedFillLevel = 0.0;
				
				boolean foundMatch = false;

				long nodeId = ex.getSegmentId();
				byte modality = TType.getEncoding(ex.getTransportationMean()); // future:
																				// might
																				// want
																				// to
																				// handle
																				// no
																				// specification
				byte timeOfDay = EvoEncoding.getTimeOfDay(ex.getTStart().getHour()); // TODO: CALCULATE FROM SEGMENTTSTART
				byte weekDay = (byte) ex.getWeekday(); // TODO: CALCULATE FROM SEGMENTTSTART

				// try the most detailed query first
				if (!firstSeg && prevTTime != -1) {
					// stmtString = String.format(stmt5, "COUNT(*)", nodeId,
					// modality, timeOfDay, weekDay, prevNodeId,
					// Math.round((double)prevTTime * 0.7),
					// Math.round((double)prevTTime * 1.3));
					stmtString = String.format(stmt5, "AVG(ttime), AVG(fillLevel)", nodeId,
							modality, timeOfDay, weekDay, prevNodeId,
							Math.round((double) prevTTime * 0.7),
							Math.round((double) prevTTime * 1.3));
					rs = stmt.executeQuery(stmtString);

					if (rs.next() && (rs.getDouble(1) > 0)) { // case matched,
																// create
																// estimate
																// statement
						// stmtPredStr = String.format(stmt5, "AVG(ttime)",
						// nodeId, modality, timeOfDay, weekDay, prevNodeId,
						// Math.round((double)prevTTime * 0.7),
						// Math.round((double)prevTTime * 1.3));
						predictedTravelTime = rs.getDouble(1);
						predictedFillLevel = rs.getDouble(2);
						foundMatch = true;
					}
					rs.close();
				}

				// Try without previous node
				if (!foundMatch) {
					// stmtString = String.format(stmt4, "COUNT(*)", nodeId,
					// modality, timeOfDay, weekDay);
					stmtString = String.format(stmt4, "AVG(ttime), AVG(fillLevel)", nodeId,
							modality, timeOfDay, weekDay);
					rs = stmt.executeQuery(stmtString);

					if (rs.next() && (rs.getDouble(1) > 0)) { // case matched,
																// estimate
																// ttime
						// stmtPredStr = String.format(stmt4, "AVG(ttime)",
						// nodeId, modality, timeOfDay, weekDay);
						predictedTravelTime = rs.getDouble(1);
						predictedFillLevel = rs.getDouble(2);
						foundMatch = true;
					}
					rs.close();
				}

				// Try without weekday
				if (!foundMatch) {
					// stmtString = String.format(stmt3, "COUNT(*)", nodeId,
					// modality, timeOfDay);
					stmtString = String.format(stmt3, "AVG(ttime), AVG(fillLevel)", nodeId,
							modality, timeOfDay);
					rs = stmt.executeQuery(stmtString);

					if (rs.next() && (rs.getDouble(1) > 0)) { // case matched,
																// estimate
																// ttime
						// stmtPredStr = String.format(stmt3, "AVG(ttime)",
						// nodeId, modality, timeOfDay);
						predictedTravelTime = rs.getDouble(1);
						predictedFillLevel = rs.getDouble(2);
						foundMatch = true;
					}
					rs.close();
				}

				// Try without time of day
				if (!foundMatch) {
					// stmtString = String.format(stmt2, "COUNT(*)", nodeId,
					// modality);
					stmtString = String.format(stmt2, "AVG(ttime), AVG(fillLevel)", nodeId,
							modality);
					rs = stmt.executeQuery(stmtString);

					if (rs.next() && (rs.getDouble(1) > 0)) { // case matched,
																// estimate
																// ttime
						// stmtPredStr = String.format(stmt2, "AVG(ttime)",
						// nodeId, modality);
						predictedTravelTime = rs.getDouble(1);
						predictedFillLevel = rs.getDouble(2);
						foundMatch = true;
					}
					rs.close();
				}

				// try without modality
				if (!foundMatch) {
					// stmtString = String.format(stmt1, "COUNT(*)", nodeId);
					stmtString = String.format(stmt1, "AVG(ttime), AVG(fillLevel)", nodeId);
					rs = stmt.executeQuery(stmtString);

					if (rs.next() && (rs.getDouble(1) > 0)) { // case matched,
																// estimate
																// ttime
						// stmtPredStr = String.format(stmt1, "AVG(ttime)",
						// nodeId);
						predictedTravelTime = rs.getDouble(1);
						predictedFillLevel = rs.getDouble(2);
						foundMatch = true;
					}
					rs.close();
				}
				// Estimate actual ttime
				if (!foundMatch) {
					// No estimate is possible for this segment
					// maybe do something

				} /*
				 * else { rs = stmt.executeQuery(stmtPredStr);
				 * 
				 * if (rs.next()) { predictedTravelTime = rs.getInt(1); } else {
				 * System.err.println(
				 * "Couldnt estimate despite db entries. Should not happen!"); }
				 * rs.close(); }
				 */
				firstSeg = false;
				prevNodeId = nodeId;
				prevTTime = predictedTravelTime;

				// TODO: UPDATE current ex with values estimated here:
				// startTime, endTime etc.
				// might have to add .set Methods to TravelExperience
				ex.setStartingTime(segmentTStart * 1000);
				segmentTStart = segmentTStart
						+ ((int) predictedTravelTime * 1000);
				ex.setEndTime(segmentTStart * 1000);
				ex.setTravelTime(predictedTravelTime);
				ex.setPublicTransportationFillingLevel(predictedFillLevel);
				// System.out.println("After: " + ex.getSegmentId() + " " +
				// ex.getStartingTime() + " " + ex.getEndTime() + " " +
				// ex.getTravelTime());
				// TODO: rough estimate for segmentTStart from distance and
				// whatever if there's no database prediction
			}

		} catch (SQLException e) {
			e.printStackTrace();

		} finally {

			try {
				if (stmt != null)
					stmt.close();
				if (con != null)
					con.close();
				if (rs != null)
					rs.close();

			} catch (SQLException e) {
				e.printStackTrace();
				return null;
			}
		}
		return it;
	}

	@Override
	public void clean(Entity agend, String tablePrefix) {
		
	}
}
