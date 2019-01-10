package db;

import java.io.IOException;
import java.sql.SQLException;

public interface IndireccionPersistencia {
    
    public static PostgresHelper conectar() throws SQLException, IOException{
         PostgresHelper client = new PostgresHelper(
                        "host", 
                        "db_name",
                        "username",
                        "password");
        try {
                if (client.connect()) {
                        return client;
                }

        } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
        }
        return null;
    }
    
    public static boolean desconectar(PostgresHelper cliente) throws SQLException{
        return cliente.disconnect();
    }
}
