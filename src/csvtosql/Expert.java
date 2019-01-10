package csvtosql;

import config.GetPropertyValues;
import db.PostgresHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Date;

public class Expert {
    
    GetPropertyValues properties = new GetPropertyValues();
    
    public boolean cargarCSV(PostgresHelper cliente) throws IOException, SQLException{
        
        // Obtengo los valores del archivo de propiedades
        String tabla = properties.getPropValue("intermediate_table");
        String columnas = properties.getPropValue("columns");
        String tabla_columnas = tabla + "(" +columnas + ")";
        String csv_location = properties.getPropValue("csv_location");
        
        // Armo la query
        String query = "COPY " + tabla_columnas + " FROM " + "'" + csv_location + "'" + " WITH (Format csv);";
        // Ejecuto la query
        cliente.execUpdate(query);
        return true;
    }
    
    public boolean moverCSV() throws IOException{
        
        // Obtengo los valores del archivo de propiedades
        String csv_location = properties.getPropValue("csv_location");
        String csv_location_old = properties.getPropValue("csv_location_old");
        
        // Muevo .csv a otro directorio
        File file = new File(csv_location);
        Path source = Paths.get(csv_location);
        Date fecha_actual= new Date();
        String fecha_str = fecha_actual.toString();
        Path target = Paths.get(csv_location_old + "/" + fecha_str + ".csv");
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e){
            e.printStackTrace();
        }
        return true;
    }
}
