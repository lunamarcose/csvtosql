package csvtosql;

import config.GetPropertyValues;
import db.PostgresHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Expert {
    
    GetPropertyValues properties = new GetPropertyValues();
    
    public boolean cargarCSV(PostgresHelper cliente, CSV csv) throws IOException, SQLException, InterruptedException{
        
        // Obtengo los valores del archivo de propiedades
        String tabla = properties.getPropValue("intermediate_table");
        String tabla_aux = properties.getPropValue("aux_table");
        String columnas = properties.getPropValue("columns");
        String tabla_columnas = tabla + "(" +columnas + ")";
        String location = csv.getFullPath();
        boolean procesoCompletado = false;
        
        // Se debe reiniciar la clave incremental antes de copiar el csv
        //String query = "ALTER SEQUENCE idmhr.intermedia_pk_id_user_seq RESTART WITH 1";
        //cliente.execUpdate(query);
        // Query para ejecutar el copy
        String query = "COPY " + tabla_columnas + " FROM " + "'" + location + "'" + "DELIMITER ',' QUOTE '\'\'' ESCAPE '\\' CSV;";
        // Ejecuto la query
        cliente.execUpdate(query);
        // Verifico si se terminaron de cargar los datos mediante el comando COPY
        for (int i = 1; i < 11; i++) { // Durante 10 segundos, cada un segundo
            String query2 = "SELECT COUNT(pk_id_user) FROM " + tabla;
            ResultSet rs = cliente.execQuery(query2);
            if(rs.next()){
                rs.getInt(1);
                if(rs.getInt(1) == csv.getCantidadRegistros()){ // Se insertaron todos los registros
                    procesoCompletado = true;
                    break;
                }
            }
            Thread.sleep(1000);
        }
        if(procesoCompletado){
            query = "INSERT INTO " + tabla_aux + "(porcentaje,cantidad_registros,fecha_procesamiento) VALUES (" + csv.getPorcentaje() + "," + csv.getCantidadRegistros() + ",CURRENT_TIMESTAMP(2));";
            cliente.execUpdate(query);
            return true;
        } else {
            return false;
        }
    }
    
    public boolean moverCSV(CSV csv) throws IOException{
        
        // Obtengo los valores del archivo de propiedades
        String csv_path = csv.getFullPath();
        String csv_location_old = properties.getPropValue("csv_location_old");
        
        // Muevo .csv a otro directorio
        File file = new File(csv_path);
        Path source = Paths.get(csv_path);
        Date fecha_actual= new Date();
        String fecha_actual_str = new SimpleDateFormat("yyyy-MM-dd").format(fecha_actual);
        Path target = Paths.get(csv_location_old + "/" + fecha_actual_str + ".csv.old");
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e){
            e.printStackTrace();
        }
        return true;
    }
    
    public CSV verificarCSV(PostgresHelper cliente, CSV csv) throws FileNotFoundException, IOException, SQLException{       
        
        String csv_path = csv.getFullPath();
        String encoding = properties.getPropValue("encoding");
        String[] nombres_columnas = properties.getPropValue("columns").split(",");
        String[] nombres_columnas_req = properties.getPropValue("columns_required").split(",");
        
        // Siguiente paso, verificar el contenido del csv (cantidad de campos y obligatoriedad de los mismos)
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csv_path), encoding));
        String line;
        int lineNumber = 0;
        while((line=br.readLine())!=null){
            lineNumber++;
            String l = line.trim();
            ArrayList<String> columnas = cantidadColumnas(l);
            int cantidad_columnas = columnas.size();
            if (cantidad_columnas == nombres_columnas.length){ // Deben ser la cantidad de campos definida en el archivo de config
                HashMap<String,String> hashColumns = new HashMap<>();
                ArrayList<String> col_req_noVal = new ArrayList();
                for(int i=0; i < nombres_columnas.length; i++){
                    for(int j=0; j < nombres_columnas_req.length;j++){
                        if(nombres_columnas[i].equals(nombres_columnas_req[j])){
                            if(!columnas.get(i).equals("")){
                                hashColumns.put(nombres_columnas_req[j],columnas.get(i));
                            } else {
                                col_req_noVal.add(nombres_columnas_req[j]);
                            }
                        }
                    }
                }
                if(col_req_noVal.size() > 0){
                    csv.setIsValid(false);
                    for(int i = 0; i < col_req_noVal.size(); i++){
                        System.out.println("El campo requerido " + col_req_noVal.get(i) + " no posee valor en la fila número " + lineNumber);
                    }
                    return csv;
                }
            } else {
                System.out.println("No tiene la cantidad de filas necesaria " + cantidad_columnas);
                csv.setIsValid(false);
                return csv;
            }
        }
        csv.setCantidadRegistros(lineNumber);
        br.close();
        
        // Verificación de cantidad de registros, en comparación con los registros anteriores en la tabla
        // Verificar si el porcentaje de tolerancia de bajas se cumple
        // Obtengo de la tabla auxiliar la cantidad de registros cargados en la última ejecución
        int cantidadRegistrosTabla = 0;
        String query_latestRecords = "SELECT cantidad_registros FROM auxiliar ORDER BY fecha_procesamiento DESC LIMIT 1;";
        ResultSet rs = cliente.execQuery(query_latestRecords);
        if(rs.next()){
            cantidadRegistrosTabla = rs.getInt(1);
        }
        // Obtengo del .csv la cantidad de registros a cargar
        int cantidadRegistrosCSV = csv.getCantidadRegistros();
        // Máxima tolerancia
        double tolerancia = Double.parseDouble(properties.getPropValue("tolerance_percentage"));
        // Realizo el cálculo del porcentaje
        int diferenciaRegistros = cantidadRegistrosTabla - cantidadRegistrosCSV;
        if(diferenciaRegistros > 0){
            double porcentaje = diferenciaRegistros * 100 / cantidadRegistrosTabla;
            csv.setPorcentaje(porcentaje);
            // Si es mayor al porcentaje de tolerancia no se debe procesar
            if(porcentaje > tolerancia){
                csv.setIsValid(false);
                return csv;
            }
        }
        // Si el .csv pasó todas las validaciones
        csv.setIsValid(true);
        return csv;
    }
    
    public ArrayList cantidadColumnas(String cadena) throws IOException{
        
        char caracter_delimitador = properties.getPropValue("quotes_char").charAt(0);
        char caracter_separador = properties.getPropValue("separator_char").charAt(0);
        boolean flag_conteo = true;
        String palabra = "";
        ArrayList<String> valores = new ArrayList();
        int cantidad_columnas = 0;
        
        for (int i = 0; i < cadena.length(); i++){
            char c = cadena.charAt(i);
            if(c == caracter_delimitador){
                if((palabra.length() > 0) && ((i == cadena.length()-1) || (""+c+cadena.charAt(i+1)).equals("\',"))){ // Fin de palabra
                    valores.add(palabra);
                    palabra = "";
                }
                if(i == 0){ // Es el primer caracter de la cadena
                    flag_conteo = !flag_conteo;
                } else if(i == cadena.length()-1) { // Es el ultimo caracter de la cadena
                        flag_conteo = !flag_conteo;
                    } else if((""+c+cadena.charAt(i+1)).equals("\',")){ // Uno de los posibles caracteres intermedios
                            flag_conteo = !flag_conteo;
                        } else if((""+cadena.charAt(i-1)+c).equals(",\'")){ // Otro de los posibles caracteres intermedios
                                flag_conteo = !flag_conteo;
                            }
            } else {
                if(!flag_conteo){
                    palabra = palabra+c;
                }
            }
            if((c == caracter_separador) && flag_conteo){
                cantidad_columnas++;
                if(i == 0){ // El primer item venia vacio
                    valores.add("");
                } else if(((""+cadena.charAt(i-1)).equals(",")) || (i == cadena.length()-1)){ // Cadenas intermedias vacías
                    valores.add("");
                }
            }
        }
        return valores;
    }
    
    public CSV verificarExistenciaCSV() throws FileNotFoundException, IOException{
        
        // Obtengo los valores del archivo de propiedades
        String csv_location = properties.getPropValue("csv_location");
        
        int cantidadCSV = 0;
        String nombreCSV = "";
        
        File folder = new File(csv_location);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
              String fileName = listOfFiles[i].getName();
              if(fileName.endsWith(".csv")){
                  cantidadCSV++;
                  nombreCSV = fileName;
              }
            }
        }
        
        if(cantidadCSV > 1){
            System.out.println("ERROR: Existen más de 1 archivo CSV en el directorio");
            return null;
        } else if (cantidadCSV == 1){
            String fullPath = csv_location + "/" + nombreCSV;
            CSV csv = new CSV(fullPath);
            return csv;
        } else {
            System.out.println("No existen CSV a procesar en el directorio.");
            return null;
        }
    }
    
    public boolean verificarEstadoConfig() throws IOException{
        return properties.getPropValues();
    }
}