package allow.simulator.entity.knowledge;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import allow.simulator.entity.Entity;
import allow.simulator.entity.Entity.Type;
import allow.simulator.entity.knowledge.DBConnector.DBType;

public class DBExpertKnowledge implements DBKnowledgeModel {
	// Dictionary holding tables which have been
	private static ConcurrentHashMap<String, Boolean> aIdTableExists = new ConcurrentHashMap<String, Boolean>();

	private static final String MY_SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS %1$s "
			+ "(entryNo INTEGER UNSIGNED AUTO_INCREMENT PRIMARY KEY, nodeId INT, prevNodeId INT, "
			+ "ttime DOUBLE, prevttime DOUBLE, weather TINYINT UNSIGNED, weekday TINYINT UNSIGNED, "
			+ "timeOfDay TINYINT UNSIGNED, modality TINYINT UNSIGNED, density FLOAT, startTime INT UNSIGNED, "
			+ "endTime INT UNSIGNED, mean FLOAT, std FLOAT, INDEX(nodeId, modality, timeOfDay, weekday, prevNodeId, prevttime));%2$s";

	private static final String POSTGRE_SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS %1$s "
			+ " (entryNo SERIAL PRIMARY KEY, nodeId INTEGER, prevNodeId INTEGER, "
			+ "ttime REAL, prevttime REAL, weather SMALLINT, weekday SMALLINT, "
			+ "timeOfDay SMALLINT, modality SMALLINT, density REAL, startTime INTEGER, "
			+ "endTime INTEGER, mean REAL, std REAL); CREATE INDEX on %2$s "
			+ "(nodeId, modality, timeOfDay, weekday, prevNodeId, prevttime)";

	private static final String MY_SQL_SHOW_TABLES = "SHOW TABLES LIKE '%1$s'";

	private static final String POSTGRE_SQL_SHOW_TABLES = "SELECT * FROM pg_catalog.pg_tables where "
			+ "tablename like '%1s'";

	private static final String SQL_INSERT_VALUES = "INSERT INTO %1$s "
			+ " (nodeId, prevNodeId, ttime, prevttime, weather, weekday, timeOfDay, "
			+ "modality, density, startTime, endTime, mean, std) VALUES ";

	private DBType type;
	private String sqlCreateTables;
	private String sqlShowTables;
	private String sqlInsertValues;

	public DBExpertKnowledge(DBType type) {
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
	public boolean addEntry(Entity agent, List<TravelExperience> proir, List<TravelExperience> posterior, String tablePrefix) {

		return false;
	}

	@Override
	public List<TravelExperience> getPredictedItinerary(Entity agent, List<TravelExperience> it, String tablePrefix) {
		Collection<Entity> persons = agent.getContext().getWorld().getEntitiesOfType(Type.PERSON);
		return null;
	}

	@Override
	public void clean(Entity entity, String tablePrefix) {
		
	}

}
