package com.ai.adapters;

import com.ai.dto.NormalizedLocation;
import com.ai.opensky.OpenSky;
import com.ai.opensky.OpenSkyRawAirTraffic;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;


/**
 *
 *
 * @author rwilcom
 */
public class OpenSkyAdapter {
    
    // private static final String OUTPUT_DIR = "/projects/temp/locs";   //Path to work on a local machine
    private static final String OUTPUT_DIR = "/home/ec2-user/OpenSky/Data"; //path to work on amazon ec2
    private static final String OUTPUT_FILE_PREFIX = "loc";
    
  /**
     * 
     * 
     * 
     * @param location
     * @param fileLocation
     * @param baseFileName 
     */    
    static public void writeNormalizedLocationToJsonFile(NormalizedLocation location, String fileLocation, String baseFileName){
        
        if( location!=null) {
            try{               
                String pathAndFileName = fileLocation+"/"+baseFileName+location.uuid+"_"+location.id+"_"+System.currentTimeMillis()+".json";
                System.out.println("Writing file: "+pathAndFileName);
                FileWriter fileWriter = new FileWriter(pathAndFileName); 
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(location, fileWriter);
                fileWriter.close();                              
            } catch (IOException e) {        
                 System.out.println(e.toString());
            } 
        }
    }    
    
    /**
     * 
     * 
     * 
     * 
     * 
     * @param args
     * @throws Exception 
     */
     public static void main(String[] args) throws Exception {

        //read directly from API (live)
        ArrayList<OpenSkyRawAirTraffic> osraList = OpenSky.readAirTrafficApiToObjects();   
        
        //read directly from a raw file (API format)
        //ArrayList<OpenSkyRawAirTraffic> osraList = OpenSky.readAirTrafficRawFileToObjects(args[0]);

        System.out.print("TOTAL FLIGHTS TO NORMALIZE: "+osraList.size()+"\n");
        
        new File(OUTPUT_DIR).mkdirs();
        
        int count = 0;
        int throttleOutput = 10;        
        for( OpenSkyRawAirTraffic osra: osraList ){
            NormalizedLocation location = new NormalizedLocation();
            
            location.uuid = ""+java.util.UUID.randomUUID();
            location.id = osra.icao24TransponderAddr;
            location.locationDateTime = osra.timeOfPositionDateTime;
            location.id = osra.callsign +"-"+osra.icao24TransponderAddr;
            location.type = "unknown";
            location.longitude = osra.longitude;
            location.latitude = osra.latitude;
            location.altitudeMeters = osra.altitudeMeters;
            location.velocityMetersPerSec = osra.velocityMetersPerSec;
            location.headingDecDegFromNorth0 = osra.headingDecDegFromNorth0;
            location.properties.put("dob", "unknown");
            location.properties.put("callsign", osra.callsign);
            
            if( osra.onGround ){
                location.altitudeMeters = 0.0;
                location.velocityMetersPerSec = 0.0;
            }
            
            //System.out.print(location.toString()+"\n");
                        
            if( count<throttleOutput ){                
                writeNormalizedLocationToJsonFile(location, OUTPUT_DIR, OUTPUT_FILE_PREFIX) ;
            }
            
            count++;
        }
     } 
     
    
}
