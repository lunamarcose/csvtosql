package csvtosql;

import java.sql.SQLException;
import java.io.*;

public class Main {

   public static void main(String[] args) throws SQLException, IOException {
       
       // Controlador
       Controller controlador = new Controller();
       controlador.cargarCSV();
       
        //client.execQuery("COPY tabla_intermedia(nombre,apellido,edad)  FROM '/home/marcos/Escritorio/test.csv' WITH (Format csv);");
        
        //IndireccionPersistencia
        //GetPropertyValues properties = new GetPropertyValues();
        //properties.getPropValues();

        //client.execQuery("COPY tabla_intermedia(nombre,apellido,edad)  FROM '/home/marcos/Escritorio/test.csv' WITH (Format csv);");
    }
    
}
