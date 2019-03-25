package db;

import config.GetPropertyValues;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PostgresHelper {

	private Connection conn;
	private String host;
	private String dbName;
	private String user;
	private String pass;
	
	// No se utiliza el constructor por defecto
	protected PostgresHelper() {}
	
	public PostgresHelper(String host, String dbName, String user, String pass) throws IOException {
                GetPropertyValues properties = new GetPropertyValues();
		this.host = properties.getPropValue(host);       
		this.dbName = properties.getPropValue(dbName);
		this.user = properties.getPropValue(user);
		this.pass = properties.getPropValue(pass);
	}
	
	public boolean connect() throws SQLException, ClassNotFoundException{
		if (host.isEmpty() || dbName.isEmpty() || user.isEmpty() || pass.isEmpty()) {
			throw new SQLException("Credenciales de conexión incompletas");
		}
		
		Class.forName("org.postgresql.Driver");
		this.conn = DriverManager.getConnection(
				this.host + this.dbName,
				this.user, this.pass);
		return true;
	}
        
        public void execUpdate(String query) throws SQLException {
            this.conn.createStatement().executeUpdate(query);
        }
        
        public ResultSet execQuery(String query) throws SQLException {
	    return this.conn.createStatement().executeQuery(query);
	}
        
        public boolean disconnect() throws SQLException{
            try {
                this.conn.close();
            } catch (SQLException e){
                e.printStackTrace();
            }
            return true;
        }
}
