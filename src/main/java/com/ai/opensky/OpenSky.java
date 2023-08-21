package com.ai.opensky;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.ClientConfig;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.client.ClientProperties;

/**
 *
 * Raw airplane traffic feed via REST API: 
 *     https://opensky-network.org/apidoc/
 *     https://opensky-network.org/apidoc/rest.html
 *     https://opensky-network.org/api/states/all  (the feed!)
 * 
 * 
 * @author rwilcom
 */
public class OpenSky {
    
    private static final String OPEN_SKY_API_GETALL = "https://opensky-network.org/api/states/all";
    
    private static final int CYCLE_TEST_COUNT = 10;
    private static final int CYCLE_TEST_PAUSE_MIN = 5;
    
    
    
    //private static final String OUTPUT_DIR = "./data";
    private static final String OUTPUT_DIR = "/home/ec2-user/OpenSky/Data"; //path to work on amazon ec2
    private static final String OUTPUT_FILE_PREFIX = "osFlightData";
    
    private static HostnameVerifier hostnameVerifier;
    private static SSLContext sslContext;    
    private static Client wsClient;
    
    static{        
        trustAllHosts();
        wsClient = hostIgnoringClient(); //create();
    }
    
    /**
     *  
     * 
     * 
     */
    static private void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException
            {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }            
        } };
        
        // Install the all-trusting trust manager
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
                       

        hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify( String s, SSLSession sslSession ) {
                    return true;
                }
            };        
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);        
    }
 
    
    /**
     * 
     * 
     * 
     * @return 
     */
    static private Client hostIgnoringClient() {
        try { 
            ClientConfig config = new ClientConfig();
            config = config.property(ClientProperties.CONNECT_TIMEOUT, 30000);
            config = config.property(ClientProperties.READ_TIMEOUT, 30000);

            //Client client = JerseyClientBuilder.createClient(config);
            Client client = JerseyClientBuilder.newBuilder()
                    .hostnameVerifier(hostnameVerifier)
                    .sslContext(sslContext)
                    .withConfig(config)
                    .build();

            return client;
        }
        catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }    
    
    /**
     * 
    * Read directly from the API
     * 
     * 
     * @return 
     */
    static public String readAirTrafficApiToRawOutput(){
        WebTarget wt = wsClient.target(OPEN_SKY_API_GETALL);
        Response resp = wt.request(MediaType.APPLICATION_JSON).get();
        final int status = resp.getStatus();
        if( status>300) {
            //unexpected error
        }else{
            String jsonBody = resp.readEntity(String.class);
            return jsonBody;
        }
        return null;
    }
    
    /**
     * Read from a raw file (API raw output format)
     * 
     * 
     * @param fullFileLocationAndNameAndExt
     * @return 
     */
    static public String readAirTrafficRawFileToRawOutput( String fullFileLocationAndNameAndExt) {
       
        try {
            return Files.readString( Path.of(fullFileLocationAndNameAndExt) );
        } catch (IOException e) {        
            System.out.println(e.toString());
        } 
        return null;       
       
    }
    
    
    /**
     * 
     * @param rawApiOutput
     * call with either:
     *          readAirTrafficApiToRawOutput (direct call to API)
     *          readAirTrafficRawFileToRawOutput (from a raw file)
     * 
    * 
     * @return 
     */
    static public JsonObject readAirTrafficRawOutputToJson(String rawApiOutput){

       if( rawApiOutput!=null) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonOpenSkyObj = jsonParser.parse(rawApiOutput).getAsJsonObject();
            return jsonOpenSkyObj;
        }
        
        return null;
        
    }
    
    /**
     * 
     * 
     * 
     * @param fileLocation
     * @param baseFileName 
     */    
    static public void readAirTrafficApiToFileRaw(String fileLocation, String baseFileName){

        try{
            
            JsonObject jsonObj = readAirTrafficRawOutputToJson(readAirTrafficApiToRawOutput());
            
            if( jsonObj==null){
                System.out.println("No JSON to write?");
                return;
            }
            
            FileWriter fileWriter = new FileWriter(fileLocation+"/"+baseFileName+"_"+System.currentTimeMillis()+".raw");                      
            fileWriter.write(jsonObj.toString());
            fileWriter.close();
        
        } catch (IOException e) {        
            System.out.println(e.toString());
        }  
        
        /*
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        gson.toJson(new User(1, null, "Unknown"), new FileWriter(filePath));
        JsonWriter jsonWriter = gson.newJsonWriter(fileWriter);
        jsonWriter.
        */
        
        
    }
    
    /**
     * 
     * 
     * 
     * @param fileLocation
     * @param baseFileName 
     */    
    static public void readAirTrafficApiToFileJson(String fileLocation, String baseFileName){

        ArrayList<OpenSkyRawAirTraffic> openSkyRawList = readAirTrafficApiToObjects();
        
        if( openSkyRawList!=null) {
            try{               
                FileWriter fileWriter = new FileWriter(fileLocation+"/"+baseFileName+"_"+System.currentTimeMillis()+".json"); 
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(openSkyRawList, fileWriter);
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
     * @return 
     */    
    static private ArrayList<OpenSkyRawAirTraffic> readAirTrafficApiToObjects(  JsonObject jsonOpenSkyObj ){
        
        ArrayList<OpenSkyRawAirTraffic> openSkyRawList = 
            new ArrayList<>();

        if( jsonOpenSkyObj!=null) {

            Long timeAsLong = jsonOpenSkyObj.get("time").getAsLong();

            if( jsonOpenSkyObj.get("states").isJsonNull()){
                return openSkyRawList; // empty
            }
            
            JsonArray jsonOpenSkyStatesListArr = jsonOpenSkyObj.getAsJsonArray("states");
            for( JsonElement openSkyState : jsonOpenSkyStatesListArr){
                /*                    
                0 	icao24 	string 	Unique ICAO 24-bit address of the transponder in hex string representation.
                1 	callsign 	string 	Callsign of the vehicle (8 chars). Can be null if no callsign has been received.
                2 	origin_country 	string 	Country name inferred from the ICAO 24-bit address.
                3 	time_position 	float 	Unix timestamp (seconds) for the last position update. Can be null if no position report was received by OpenSky within the past 15s.
                4 	time_velocity 	float 	Unix timestamp (seconds) for the last velocity update. Can be null if no velocity report was received by OpenSky within the past 15s.
                5 	longitude 	float 	WGS-84 longitude in decimal degrees. Can be null.
                6 	latitude 	float 	WGS-84 latitude in decimal degrees. Can be null.
                7 	altitude 	float 	Barometric or geometric altitude in meters. Can be null.
                8 	on_ground 	boolean 	Boolean value which indicates if the position was retrieved from a surface position report.
                9 	velocity 	float 	Velocity over ground in m/s. Can be null.
                10 	heading 	float 	Heading in decimal degrees clockwise from north (i.e. north=0°). Can be null.
                11 	vertical_rate 	float 	Vertical rate in m/s. A positive value indicates that the airplane is climbing, a negative value indicates that it descends. Can be null.
                12 	sensors 	int[]   (will be null for 'all' call)
                */
                
                OpenSkyRawAirTraffic openSkyRaw = new OpenSkyRawAirTraffic(); 
                openSkyRaw.lastReadDateTime = new Date(timeAsLong*1000);  

                JsonArray jsonOpenSkyStateArr = openSkyState.getAsJsonArray();

                if( !jsonOpenSkyStateArr.get(0).isJsonNull())                    
                    openSkyRaw.icao24TransponderAddr = jsonOpenSkyStateArr.get(0).getAsString().trim();

                if( !jsonOpenSkyStateArr.get(1).isJsonNull())                    
                    openSkyRaw.callsign = jsonOpenSkyStateArr.get(1).getAsString().trim();

                if( !jsonOpenSkyStateArr.get(2).isJsonNull())                    
                    openSkyRaw.originCountry = jsonOpenSkyStateArr.get(2).getAsString().trim();

                if( !jsonOpenSkyStateArr.get(3).isJsonNull()){
                    Long timeOfPosition = jsonOpenSkyStateArr.get(3).getAsLong();
                    openSkyRaw.timeOfPositionDateTime = new Date(timeOfPosition*1000);
                }

                if( !jsonOpenSkyStateArr.get(4).isJsonNull()){
                   Long timeOfVelocity = jsonOpenSkyStateArr.get(4).getAsLong();
                   openSkyRaw.timeOfVelocityDateTime = new Date(timeOfVelocity*1000);
                }

                if( !jsonOpenSkyStateArr.get(5).isJsonNull())                    
                    openSkyRaw.longitude = jsonOpenSkyStateArr.get(5).getAsDouble();

                if( !jsonOpenSkyStateArr.get(6).isJsonNull())                    
                    openSkyRaw.latitude = jsonOpenSkyStateArr.get(6).getAsDouble();

                if( !jsonOpenSkyStateArr.get(7).isJsonNull())                    
                    openSkyRaw.altitudeMeters = jsonOpenSkyStateArr.get(7).getAsDouble();

                if( !jsonOpenSkyStateArr.get(8).isJsonNull())                    
                    openSkyRaw.onGround = jsonOpenSkyStateArr.get(8).getAsBoolean();

                if( !jsonOpenSkyStateArr.get(9).isJsonNull())                    
                    openSkyRaw.velocityMetersPerSec = jsonOpenSkyStateArr.get(9).getAsDouble();

                if( !jsonOpenSkyStateArr.get(10).isJsonNull())                    
                    openSkyRaw.headingDecDegFromNorth0 = jsonOpenSkyStateArr.get(10).getAsDouble();

                if( !jsonOpenSkyStateArr.get(11).isJsonNull())                    
                    openSkyRaw.verticalRateMetersPerSec = jsonOpenSkyStateArr.get(11).getAsDouble();

                //if we have no callsign then its not useful
                if( openSkyRaw.callsign!=null && !openSkyRaw.callsign.isEmpty()){
                    openSkyRawList.add(openSkyRaw);
                }
            }
        }             
        return openSkyRawList;
    }    
    
          
    /**
     * 
     * @return 
     */    
    static public ArrayList<OpenSkyRawAirTraffic> readAirTrafficApiToObjects(){

        return readAirTrafficApiToObjects( 
                readAirTrafficRawOutputToJson(
                        readAirTrafficApiToRawOutput()));        
        
    }

              
    /**
     * 
     * @param fullFileLocationAndNameAndExt
     * @return 
     */    
    static public ArrayList<OpenSkyRawAirTraffic> readAirTrafficRawFileToObjects(  String fullFileLocationAndNameAndExt ) {
 
        return readAirTrafficApiToObjects( 
                readAirTrafficRawOutputToJson(
                        readAirTrafficRawFileToRawOutput( fullFileLocationAndNameAndExt)));                
    }

    /**
     * 
     * @param rawFileOutput
     * @return 
     */    
    static public ArrayList<OpenSkyRawAirTraffic> readAirTrafficRawOutputToObjects( String rawFileOutput ){
        
        JsonObject jsonOpenSkyObj = readAirTrafficRawOutputToJson(rawFileOutput);
        return readAirTrafficApiToObjects( jsonOpenSkyObj );
    } 
    
    
     public static void main(String[] args) throws Exception {

        //start test code        
        
        for( int cycle=0; cycle<CYCLE_TEST_COUNT; cycle++){

            /*
            ArrayList<OpenSkyRawAirTraffic> osraList;
            //read directly from API (live)
            osraList = OpenSky.readAirTrafficApiToObjects();   
            //read directly from a raw file (API format)
            //osraList = OpenSky.readAirTrafficRawFileToObjects(OUTPUT_DIR+"/"+OUTPUT_FILE_PREFIX+"_1690830830104.raw");

            for( OpenSkyRawAirTraffic osra: osraList ){
                System.out.print(osra.toString()+"\n");

                if( osra.onGround ){
                    System.out.print("...."+osra.callsign +" is GROUNDED!!\n");
                }

                //applying a geohash algorithm
                //if( osra.latitude!=null && osra.longitude!=null){
                //    System.out.print(".... GEOHASH:"+com.github.davidmoten.geo.GeoHash.encodeHash(osra.latitude, osra.longitude) +"\n");                
                //}

                //test byte streaming
                //byte[] asBytes = osra.getAsBytes();
                //System.out.print("....as bytes L="+asBytes.length+"\n");            
                //OpenSkyRawAirTraffic osraRehydraded = OpenSkyRawAirTraffic.getFromBytes(asBytes);
                //System.out.print("....rehydrated:"+osraRehydraded.callsign+"\n");                            
            }

            System.out.print("TOTAL FLIGHTS: "+osraList.size()+"\n");
            */

            //com.github.davidmoten.geo.Coverage coverage =
            //    com.github.davidmoten.geo.GeoHash.coverBoundingBox(-0.489,51.28,0.236,51.686);
            //System.out.print(".... GEOHASH FROM BBOX:"+coverage.toString());
            
            //write to JSON flat file
            //System.out.print("Writing to JSON file...\n"); 
            //new File(OUTPUT_DIR).mkdirs();
            //readAirTrafficApiToFileJson(OUTPUT_DIR, OUTPUT_FILE_PREFIX) ;

            
            //write to raw flat file
            System.out.print("Writing to raw file...\n"); 
            new File(OUTPUT_DIR).mkdirs();
            readAirTrafficApiToFileRaw(OUTPUT_DIR, OUTPUT_FILE_PREFIX) ;
            
            if( CYCLE_TEST_PAUSE_MIN>0 ) System.out.print("PAUSING BEFORE PULLING NEXT FLIGHT DATA...\n");
            
            Thread.sleep(CYCLE_TEST_PAUSE_MIN*60*1000);
        }
        
         //end test code
     } 
     
    
}
