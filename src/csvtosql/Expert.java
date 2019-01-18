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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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
        String fecha_actual_str = new SimpleDateFormat("yyyy-MM-dd").format(fecha_actual);
        Path target = Paths.get(csv_location_old + "/" + fecha_actual_str + ".csv");
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
        boolean estadoCSV = true;
        String line;
            while((line=br.readLine())!=null){
                String l = line.trim();
                System.out.println(l);
                String[] columnas = l.split(","); // separamos las lineas en las comas
                int cantidad_columnas = columnas.length;
                System.out.println(cantidad_columnas);
                if (cantidad_columnas == 23){ // Deben ser 23 campos
                    for(int i=0; i < cantidad_columnas; i++){
                        String valor = columnas[i].trim(); // Sin el espacio luego de la coma
                        if(i == 0 || i == 1 || i == 2 || i == 3){
                            if(valor.equals("")){
                                System.out.println("Faltan valores requeridos");
                                return false;
                            }
                        }
                    }
                } else {
                    System.out.println("No tiene la cantidad de filas necesarias");
                    return false;
                }
            }
            br.close();
        return estadoCSV;
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
    
    public int verificarDatosAnteriores(PostgresHelper cliente) throws SQLException{
        // verificar si la tabla intermedia
        // es vacía, debe devolver true. Si contiene datos, es porque hubo error al procesar
        // devuelve false
        
        //armo la query para eliminar los registros copiados correctamente a la tabla de usuarios y los que no se copiaron pero corresponden a usuarios sin cuenta en AD
        String query1 = "DELETE FROM tabla_intermedia WHERE flag_registro_copiado = 'true' OR flag_cuenta_ad = 'false'";
        //Ejecuto la query
        cliente.execUpdate(query1);
        
        //armo la query para determinar si se borraron todos los registro de la tabla intermedia, lo cual significaria que la copia fue exitosa
        String query2 = "SELECT COUNT(pk_id_user) FROM tabla_intermedia";
        ResultSet rs = cliente.execQuery(query2);
        
        int resultado = 0;
        
        while(rs.next()) {
            resultado = rs.getInt(1);
        }
        return resultado;
       
    }
}