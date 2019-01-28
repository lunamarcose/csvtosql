package csvtosql;

import config.GetPropertyValues;
import db.PostgresHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Expert {
    
    GetPropertyValues properties = new GetPropertyValues();
    
    public boolean cargarCSV(PostgresHelper cliente) throws IOException, SQLException{
        
        // Obtengo los valores del archivo de propiedades
        String tabla = properties.getPropValue("intermediate_table");
        String columnas = properties.getPropValue("columns");
        String tabla_columnas = tabla + "(" +columnas + ")";
        String csv_location = properties.getPropValue("csv_location");
        String csv_name = properties.getPropValue("csv_name");
        String csv_path = csv_location + "/" + csv_name;
        
        // Armo la query
        String query = "COPY " + tabla_columnas + " FROM " + "'" + csv_path + "'" + "DELIMITER ',' QUOTE '\'\'' ESCAPE '\\' CSV;";
        // Ejecuto la query
        cliente.execUpdate(query);
        return true;
    }
    
    public boolean moverCSV() throws IOException{
        
        // Obtengo los valores del archivo de propiedades
        String csv_location = properties.getPropValue("csv_location");
        String csv_name = properties.getPropValue("csv_name");
        String csv_path = csv_location + "/" + csv_name;
        String csv_location_old = properties.getPropValue("csv_location_old");
        
        // Muevo .csv a otro directorio
        File file = new File(csv_path);
        Path source = Paths.get(csv_path);
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
        String csv_name = properties.getPropValue("csv_name");
        String csv_path = csv_location + "/" + csv_name;
        String encoding = properties.getPropValue("encoding");
        String[] nombres_columnas = properties.getPropValue("columns").split(",");
        String[] nombres_columnas_req = properties.getPropValue("columns_required").split(",");
        
        // Cargo el csv, y verifico fila por fila: 1) cantidad de campos 2) 
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csv_path), encoding));
        boolean estadoCSV = true;
        String line;
        int lineNumber = 0;
            while((line=br.readLine())!=null){
                lineNumber++;
                String l = line.trim();
                l = l.replace("\\,","-escapedValue-");
                String[] columnas = l.split(",", -1); // separamos las lineas en las comas
                int cantidad_columnas = columnas.length;
                if (cantidad_columnas == nombres_columnas.length){ // Deben ser la cantidad de campos definida en el archivo de config
                    HashMap<String,String> hashColumns = new HashMap<>();
                    ArrayList<String> col_req_noVal = new ArrayList();
                    for(int i=0; i < nombres_columnas.length; i++){
                        for(int j=0; j < nombres_columnas_req.length;j++){
                            if(nombres_columnas[i].equals(nombres_columnas_req[j])){
                                if(!columnas[i].equals("")){
                                    hashColumns.put(nombres_columnas_req[j],columnas[i]);
                                } else {
                                    col_req_noVal.add(nombres_columnas_req[j]);
                                }
                            }
                        }
                    }
                    if(col_req_noVal.size() > 0){
                        estadoCSV = false;
                        for(int i = 0; i < col_req_noVal.size(); i++){
                            System.out.println("El campo requerido " + col_req_noVal.get(i) + " no posee valor en la fila número " + lineNumber);
                        }
                    }
                } else {
                    System.out.println("No tiene la cantidad de filas necesaria " + cantidad_columnas);
                    estadoCSV = false;
                }
            }
            br.close();
        return estadoCSV;
    }
    
    public boolean modificarCSV() throws IOException{
        boolean operationStatus;
        String locationCSV = properties.getPropValue("csv_location");
        String csvName = properties.getPropValue("csv_name");
        String filePath = locationCSV + "/" + csvName;
        String separator_char = properties.getPropValue("separator_char").toString();
        
        String oldString = "\\,";
        String newString = separator_char;
        
        File fileToBeModified = new File(filePath); 
        String oldContent = "";
        BufferedReader reader = null;
        FileWriter writer = null;
         
        try {
            reader = new BufferedReader(new FileReader(fileToBeModified));
            //Reading all the lines of input text file into oldContent
            String line = reader.readLine();
            while (line != null) 
            {
                oldContent = oldContent + line + System.lineSeparator();
                line = reader.readLine();
            }
            //Replacing oldString with newString in the oldContent
            String newContent = oldContent.replace(oldString, newString);
            //Rewriting the input text file with newContent
            writer = new FileWriter(fileToBeModified);
            writer.write(newContent);
            operationStatus = true;
        } catch (IOException e){
            operationStatus = false;
            e.printStackTrace();
        } finally {
            try {
                //Closing the resources
                reader.close();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return operationStatus;
    }
    
    public boolean verificarExistenciaCSV() throws FileNotFoundException, IOException{
        
        // Obtengo los valores del archivo de propiedades
        String csv_location = properties.getPropValue("csv_location");
        String csv_name = properties.getPropValue("csv_name");
        String csv_fullPath = csv_location + "/" + csv_name;
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(csv_fullPath));
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