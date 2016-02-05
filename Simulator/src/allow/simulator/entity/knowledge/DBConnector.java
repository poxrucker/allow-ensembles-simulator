package allow.simulator.entity.knowledge;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;

import allow.simulator.core.EvoKnowledgeConfiguration;
import allow.simulator.core.Simulator;
import allow.simulator.entity.Entity;
import allow.simulator.entity.Entity.Type;

public class DBConnector {
	
	public enum DBType {
		
		MYSQL,
		
		POSTGRE;
	}
	// Dictionary holding tables which have been 
	// private static ConcurrentHashMap<String, Boolean> aIdTableExists = null;
	private static String prefix = null;
	private static EvoKnowledgeConfiguration config;
	private static DBKnowledgeModel model;
	private static DBType dbType;
	
	private static final String KNOWLEDGE_MODEL_NO_KNOWLEDGE = "without";
	private static final String KNOWLEDGE_MODEL_LOCAL = "local";
	private static final String KNOWLEDGE_MODEL_GLOBAL_TEMPORAL = "global (temporally restricted)";
	private static final String KNOWLEDGE_MODEL_EXPERT = "expert";
	
	private static void initMySQL() {
		Connection con = null, con2 = null;
		Statement stmt = null, stmt2 = null;
		ResultSet queries = null;
		
		try {
			// Init driver
			Class.forName("com.mysql.jdbc.Driver");
			
			// Creating database
			con = DriverManager.getConnection(config.getModelPath(), config.getUser(), config.getPassword());
			stmt = con.createStatement();
			stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + config.getModelName());
			queries = stmt.executeQuery("SELECT CONCAT(\"DROP TABLE \", table_name, \";\") "
					+ "FROM information_schema.tables WHERE table_schema = \"" + config.getModelName() + "\" "
					+ "AND table_name LIKE \"" + prefix + "%\";");

			// Deleting previous tables
			con2 = DriverManager.getConnection(config.getModelPath() + config.getModelName(), config.getUser(), config.getPassword());
			stmt2 = con2.createStatement();
			
			while (queries.next()) {
				stmt2.executeUpdate(queries.getString(1));
			}
			stmt2.executeUpdate("DROP TABLE IF EXISTS " + prefix + "_tbl_global;");

		} catch (SQLException e) {
			e.printStackTrace();
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			
		} finally {
			
			try {
				if (stmt != null) stmt.close();
				if (con != null) con.close();
				if (stmt2 != null) stmt2.close();
				if (con2 != null) con2.close();
				if (queries != null) queries.close();
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void initPostgre() {
		Connection con = null;
		Statement stmt = null;
		
		try {
			Class.forName("org.postgresql.Driver");
			
			// Reset tables if they exist.
			con = DriverManager.getConnection(config.getModelPath() + config.getModelName(), config.getUser(), config.getPassword());
			stmt = con.createStatement();
			Collection<Entity> persons = Simulator.Instance().getContext().getWorld().getEntitiesOfType(Type.PERSON);
			System.out.println(persons.size());
			
			for (Entity e : persons) {
				stmt.executeUpdate("DROP TABLE IF EXISTS " + prefix + "_tbl_" + e.getId() + ";");
			}
			stmt.executeUpdate("DROP TABLE IF EXISTS " + prefix + "_tbl_global;");

		} catch (SQLException e) {
			e.printStackTrace();
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			
		} finally {
			
			try {
				if (stmt != null) stmt.close();
				if (con != null) con.close();
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void initDatabase() {
		
		if (config.getModelPath().contains("mysql")) {
			dbType = DBType.MYSQL;
			initMySQL();
			
		} else if (config.getModelPath().contains("postgres")) {
			dbType = DBType.POSTGRE;
			initPostgre();
			
		} else {
			throw new IllegalArgumentException("Error: Unknown database driver.");
		}
		System.out.println("EvoKnowledge database connector initialized.");
	}
	
	public static void init(EvoKnowledgeConfiguration config, String knowledgeModel, String prefix) {
		DBConnector.prefix = prefix;
		DBConnector.config = config;
		DSFactory.init(config);
		
		switch (knowledgeModel) {
			case KNOWLEDGE_MODEL_NO_KNOWLEDGE:
				model = new DBNoKnowledge();
				break;
				
			case KNOWLEDGE_MODEL_LOCAL:
				initDatabase();
				model = new DBLocalKnowledge(dbType);
				break;
				
			case KNOWLEDGE_MODEL_GLOBAL_TEMPORAL:
				initDatabase();
				model = new DBGlobalKnowledge(dbType);
				break;
			
			case KNOWLEDGE_MODEL_EXPERT:
				initDatabase();
				model = new DBExpertKnowledge(dbType);
				break;
				
			default:
				throw new IllegalArgumentException("Error: Knowledge model \"" + knowledgeModel  + "\" unknown.");
		}
		
		//aIdTableExists = new ConcurrentHashMap<String, Boolean>();
	}
	
	public static boolean addEntry(Entity agent, List<TravelExperience> prior, List<TravelExperience> posterior) {
		return model.addEntry(agent, prior, posterior, prefix);
	}
	
	public static List<TravelExperience> getPredictedItinerary(Entity agent, List<TravelExperience> it) {
		return model.getPredictedItinerary(agent, it, prefix);
	}
	
	public static void cleanModel(Entity agent) {
		model.clean(agent, prefix);
	}
	
	/*public static boolean addEntry(String agentId, List<TravelExperience> it) {
		if (it.size() == 0) {
			return false;
		}
		//get datasource
		DataSource ds = DSFactory.getMySQLDataSource(config, dbName);
		
		//check if table for agent already exists (hopefully saves database overhead)
		boolean tableExists = aIdTableExists.get(agentId) == null ? false : true;
		
		//track error state to avoid having to nest too many try catch statements
		boolean error = false;
		
		//connection and statement for database query
		Statement stmt = null;
		Connection con = null;
		String stmtString = "";
		
		try {
			
			//get connection
			con = ds.getConnection();
			stmt = con.createStatement();
			
			//create a new table for an agent representing his EvoKnowledge if it doesnt exist already
			if (!tableExists) {
				try {
					stmt.execute("CREATE TABLE IF NOT EXISTS tbl_" + agentId + 
				     		" (entryNo INTEGER UNSIGNED AUTO_INCREMENT PRIMARY KEY, nodeId INT, prevNodeId INT, " +
					        "ttime DOUBLE, prevttime DOUBLE, weather TINYINT UNSIGNED, weekday TINYINT UNSIGNED, " + 
						    "timeOfDay TINYINT UNSIGNED, modality TINYINT UNSIGNED, density FLOAT, startTime INT UNSIGNED, " + 
						    "endTime INT UNSIGNED, INDEX(nodeId, modality, timeOfDay, weekday, prevNodeId, prevttime));");
				} catch (SQLException e) {
					e.printStackTrace();
					stmt.close();
					con.close();
					return false;
				}
				aIdTableExists.put(agentId, true);
			}
			
			//parse the itinerary and add a line for each entry
			stmtString = "INSERT INTO tbl_" + agentId + " (nodeId, prevNodeId, ttime, prevttime, weather, weekday, timeOfDay, " +
                    "modality, density, startTime, endTime) VALUES ";
			
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
                stmtString = stmtString.concat(ex.getWeather().getEncoding() + ",");
                stmtString = stmtString.concat(ex.getWeekday() + ",");
                stmtString = stmtString.concat(EvoEncoding.getTimeOfDay(ex.getTStart().getHour()) + ",");
                stmtString = stmtString.concat(TType.getEncoding(ex.getTransportationMean()) + ",");
                stmtString = stmtString.concat(ex.getNumberOfPeopleOnSegment() + ","); // Density = Number of other entities on segment.
                stmtString = stmtString.concat(String.valueOf(start / 1000) + ",");
                stmtString = stmtString.concat(String.valueOf(end / 1000) + ")");
                
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
                if(stmt != null) stmt.close();
                if(con != null) con.close();
                
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
		}
		return !error;
	}*/
	
	/*public static List<TravelExperience> getPredictedItinerary(String agentId, List<TravelExperience> it) {
		//System.out.println(agentId);
		//get datasource
		DataSource ds = DSFactory.getMySQLDataSource(config, dbName);
		//connection and statement for database query
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {

			// get connection
			con = ds.getConnection();
			stmt = con.createStatement();
			
			//check if user even has evoknowledge
			rs = stmt.executeQuery("SHOW TABLES LIKE 'tbl_" + agentId + "'");
			if (!rs.next()) {
				// System.out.println("User has no EK at all");
				rs.close();
				return it;
			}
			String stmt1 = "SELECT %1$s FROM tbl_" + agentId + " WHERE nodeId = %2$d";
			String stmt2 = stmt1.concat(" AND modality = %3$d");
			String stmt3 = stmt2.concat(" AND timeOfDay = %4$d");
			String stmt4 = stmt3.concat(" AND weekday = %5$d");
			String stmt5 = stmt4.concat(" AND prevNodeId = %6$d AND (prevttime BETWEEN %7$d AND %8$d)");
					
			String stmtString = null;
			// String stmtPredStr = null;
			
			boolean firstSeg = true;

			long prevNodeId = 0;
			double prevTTime = -1;

			long segmentTStart = 0;
			
			for (TravelExperience ex : it) {
				//System.out.println("Before: " + ex.getSegmentId() + " " + ex.getStartingTime() + " " + ex.getEndTime() + " " + ex.getTravelTime());

				if (firstSeg) {
					segmentTStart = ex.getStartingTime() / 1000;
				}
				
				if (ex.isTransient()) {
					continue;
				}
				double predictedTravelTime = ex.getTravelTime();
				boolean foundMatch = false;
				
				long nodeId = ex.getSegmentId();
				byte modality = TType.getEncoding(ex.getTransportationMean()); //future: might want to handle no specification
				byte timeOfDay = EvoEncoding.getTimeOfDay(ex.getTStart().getHour()); //TODO: CALCULATE FROM SEGMENTTSTART
				byte weekDay = (byte) ex.getWeekday(); //TODO: CALCULATE FROM SEGMENTTSTART
				
				//try the most detailed query first
				if (!firstSeg && prevTTime != -1) {
					// stmtString = String.format(stmt5, "COUNT(*)", nodeId, modality, timeOfDay, weekDay, prevNodeId, Math.round((double)prevTTime * 0.7), Math.round((double)prevTTime * 1.3));
					stmtString = String.format(stmt5, "AVG(ttime)", nodeId, modality, timeOfDay, weekDay, prevNodeId, Math.round((double)prevTTime * 0.7), Math.round((double)prevTTime * 1.3));
					rs = stmt.executeQuery(stmtString);
					
					if (rs.next() && (rs.getDouble(1) > 0)) { //case matched, create estimate statement
						//stmtPredStr = String.format(stmt5, "AVG(ttime)", nodeId, modality, timeOfDay, weekDay, prevNodeId, Math.round((double)prevTTime * 0.7), Math.round((double)prevTTime * 1.3));							
						predictedTravelTime = rs.getDouble(1);
						foundMatch = true;
					}
					rs.close();	
				}
				
				// Try without previous node 
				if (!foundMatch) {
					// stmtString = String.format(stmt4, "COUNT(*)", nodeId, modality, timeOfDay, weekDay);
					stmtString = String.format(stmt4, "AVG(ttime)", nodeId, modality, timeOfDay, weekDay);
					rs = stmt.executeQuery(stmtString);
					
					if (rs.next() && (rs.getDouble(1) > 0)) { //case matched, estimate ttime
						// stmtPredStr = String.format(stmt4, "AVG(ttime)", nodeId, modality, timeOfDay, weekDay);
						predictedTravelTime = rs.getDouble(1);
						foundMatch = true;
					}
					rs.close();
				}
				
				// Try without weekday
				if (!foundMatch) {
					// stmtString = String.format(stmt3, "COUNT(*)", nodeId, modality, timeOfDay);
					stmtString = String.format(stmt3, "AVG(ttime)", nodeId, modality, timeOfDay);
					rs = stmt.executeQuery(stmtString);
					
					if (rs.next() && (rs.getDouble(1) > 0)) { //case matched, estimate ttime
						// stmtPredStr = String.format(stmt3, "AVG(ttime)", nodeId, modality, timeOfDay);
						predictedTravelTime = rs.getDouble(1);
						foundMatch = true;
					}
					rs.close();
				}
				
				// Try without time of day
				if (!foundMatch) {
					// stmtString = String.format(stmt2, "COUNT(*)", nodeId, modality);
					stmtString = String.format(stmt2, "AVG(ttime)", nodeId, modality);
					rs = stmt.executeQuery(stmtString);
					
					if (rs.next() && (rs.getDouble(1) > 0)) { //case matched, estimate ttime
						//stmtPredStr = String.format(stmt2, "AVG(ttime)", nodeId, modality);
						predictedTravelTime = rs.getDouble(1);
						foundMatch = true;
					}
					rs.close();
				}
				
				//try without modality
				if (!foundMatch) {
					// stmtString = String.format(stmt1, "COUNT(*)", nodeId);
					stmtString = String.format(stmt1, "AVG(ttime)", nodeId);
					rs = stmt.executeQuery(stmtString);
					
					if (rs.next() && (rs.getDouble(1) > 0)) { //case matched, estimate ttime
						// stmtPredStr = String.format(stmt1, "AVG(ttime)", nodeId);
						predictedTravelTime = rs.getDouble(1);
						foundMatch = true;
					}
					rs.close();
				}
				// Estimate actual ttime
				if (!foundMatch) {
					// No estimate is possible for this segment
					//maybe do something

				} /*else {
					rs = stmt.executeQuery(stmtPredStr);
					
					if (rs.next()) {
						predictedTravelTime = rs.getInt(1);
					} else {
						System.err.println("Couldnt estimate despite db entries. Should not happen!");
					}
					rs.close();
				}
				firstSeg = false;
				prevNodeId = nodeId;
				prevTTime = predictedTravelTime;
				
				//TODO: UPDATE current ex with values estimated here: startTime, endTime etc.
				//might have to add .set Methods to TravelExperience
				ex.setStartingTime(segmentTStart * 1000);
				segmentTStart = segmentTStart + ((int) predictedTravelTime * 1000); 
				ex.setEndTime(segmentTStart * 1000);
				ex.setTravelTime(predictedTravelTime);
				//System.out.println("After: " + ex.getSegmentId() + " " + ex.getStartingTime() + " " + ex.getEndTime() + " " + ex.getTravelTime());
				//TODO: rough estimate for segmentTStart from distance and whatever if there's no database prediction
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			
		} finally{
			
			try {
				if(stmt != null) stmt.close();
				if(con != null) con.close();
				if(rs != null) rs.close();
				
			} catch (SQLException e) {
				e.printStackTrace();
				return null;
			}
		}
		return it;
	}*/
}