package csvtosql;

import config.GetPropertyValues;
import db.PostgresHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
    
    public boolean verificarCSV() throws FileNotFoundException, IOException{
        
        // Obtengo los valores del archivo de propiedades
        String csv_location = properties.getPropValue("csv_location");
        
        BufferedReader br = new BufferedReader(new FileReader(csv_location));
        String line;
            while((line=br.readLine())!=null){
                String pattern = "^(\\d+),(\\w+),((\\w+)=(\\w+)),((\\w+)=(\\w+)+?"; //Regexp a aplicar
                if(!line.matches(pattern)){
                    return false;
                }
            }
        return true;
    }
    
    public boolean verificarExistenciaCSV() throws FileNotFoundException, IOException{
        
        // Obtengo los valores del archivo de propiedades
        String csv_location = properties.getPropValue("csv_location");
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(csv_location));
            return true;
        } catch (FileNotFoundException e){
            return false;
        }
    }
    
    public boolean verificarDatosAnteriores(){
        // Falta agregar la lógica para conectarse a la db, verificar si la tabla intermedia
        // es vacía, debe devolver true. Si contiene datos, es porque hubo error al procesar
        // devuelve false
        return true;
    }
}