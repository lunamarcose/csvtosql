    package config;

import dto.DTOConfig;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;
 
public class GetPropertyValues {
    
    String result = "";
    InputStream inputStream;
    StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();     

    public DTOConfig getPropValues() throws IOException {
        DTOConfig dto = new DTOConfig();
        try {
            Properties prop = new Properties();
            String propFileName = "config.properties";

            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                    prop.load(inputStream);
            } else {
                    throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            String[] parametros = {"host","port","db_name","username","csv_location_old","intermediate_table","columns","columns_required","include_columns","separator_char",
                "quotes_char","encoding","tolerance_percentage","aux_table","mail_addresses","notification_flag"};
            String[] palabrasNoValidas = {"alter","drop","create","insert","delete"};
            for (int i = 0; i < parametros.length; i++) {
                String valor = prop.getProperty(parametros[i]);
                if(valor == ""){
                    dto.setIsValid(false);
                    dto.setIsValidMotive("Hay valores incompletos en el archivo de configuración.");
                    return dto;
                } else {
                    for (int j = 0; j < palabrasNoValidas.length; j++) {
                        String palabraNoValida = palabrasNoValidas[j];
                        if(valor.toLowerCase().contains(palabraNoValida)){
                            dto.setIsValid(false);
                            dto.setIsValidMotive("El valor " + valor + " para el campo " + parametros[i] + " contiene texto no válido.");
                            return dto;
                        }
                    }
                    if(!parametros[i].equals("quotes_char")){
                        String[] delimitadores = {"\"","'"};
                        for (int j = 0; j < delimitadores.length; j++) {
                            if(valor.toLowerCase().contains(delimitadores[j])){
                                dto.setIsValid(false);
                                dto.setIsValidMotive("El valor " + valor + " para el campo " + parametros[i] + " contiene texto no válido.");
                                return dto;
                            }
                        }
                    }
                    if(parametros[i].equals("tolerance_percentage")){
                        Double porcentajeConfigurado = Double.parseDouble(valor);
                        Double porcentajeAceptado = 15.0;
                        if(porcentajeConfigurado >= porcentajeAceptado){
                            dto.setIsValid(false);
                            dto.setIsValidMotive("El porcentaje de tolerancia configurado es muy alto. No se admite un valor mayor a: " + porcentajeAceptado);
                            return dto;
                        }
                    }
                    
                }
            }
        } catch (IOException e) {
            throw(e);
        } catch (NullPointerException e){
            throw(e);
        }
        finally {
            inputStream.close();
        }
        return dto;
    }
    
    public String getPropValue(String property) throws IOException{        
        String property_value = "";
            try {
                Properties prop = new Properties();
                String propFileName = "config.properties";
                inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
                if (inputStream != null) {
                        prop.load(inputStream);
                } else {
                        throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
                }       
                String cfg_file_loc = prop.getProperty("cfg_path");   
                this.encryptor.setPassword("PASS");   
                Properties propEnc = new EncryptableProperties(this.encryptor);  
                propEnc.load(new FileInputStream(cfg_file_loc));
                property_value = propEnc.getProperty(property);
            } catch (Exception e){
                System.out.println("Exception: " + e);
            } finally {
                inputStream.close();
            }
        return property_value;
    }
}