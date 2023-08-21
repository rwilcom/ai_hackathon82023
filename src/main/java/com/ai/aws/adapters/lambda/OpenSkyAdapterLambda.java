package com.ai.aws.adapters.lambda;

import static com.ai.adapters.OpenSkyAdapter.writeNormalizedLocationToJsonFile;
import com.ai.dto.NormalizedLocation;
import com.ai.opensky.OpenSky;
import com.ai.opensky.OpenSkyRawAirTraffic;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OpenSkyAdapterLambda implements RequestHandler<S3Event, String> {
    
    private static final String TABLE_NAME = "OpenSkyData";
    
    private final AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
    private final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

    @Override
    public String handleRequest(S3Event s3event, Context context) {
            
        LambdaLogger logger = context.getLogger();
        try {    
            // Get the S3 object details from the event
            String bucketName = s3event.getRecords().get(0).getS3().getBucket().getName();
            String key = s3event.getRecords().get(0).getS3().getObject().getKey();

            // Retrieve the S3 object
            S3Object s3Object = s3Client.getObject(bucketName, key);
            S3ObjectInputStream s3InputStream = s3Object.getObjectContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(s3InputStream));

            StringBuilder fullFileAsString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                fullFileAsString.append(line);
            }
            reader.close();
            s3InputStream.close();
            
            //pull air traffic chunks from raw OpenSKy outputs
            ArrayList<OpenSkyRawAirTraffic> osraList = 
                    OpenSky.readAirTrafficRawOutputToObjects( fullFileAsString.toString() );

            logger.log("total flights to normalize: "+osraList.size());
            
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

                logger.log("normalized location:"+location.toString());
                
                /*
                DescribeTableResult tableDescription =
                    dynamoDB.describeTable(TABLE_NAME);
                logger.log("AWS Table Description: "+tableDescription.getTable().getTableStatus()+": "+tableDescription.getTable().getTableName()+" "
                        + "\t ReadCapacityUnits: "+tableDescription.getTable().getProvisionedThroughput().getReadCapacityUnits()+
                        " \t WriteCapacityUnits: "+tableDescription.getTable().getProvisionedThroughput().getWriteCapacityUnits() );                
                */
                
                // Create a DynamoDB item
               
                AttributeValue avId = new AttributeValue();
                avId.setS(""+location.uuid);

                AttributeValue avCallSignTranspndr = new AttributeValue();
                avCallSignTranspndr.setS(""+location.id);
                
                AttributeValue avType = new AttributeValue();
                avType.setS(""+location.type);
                
                AttributeValue avDateTime = new AttributeValue();
                avDateTime.setS(""+location.locationDateTime);

                AttributeValue avLon = new AttributeValue();
                avLon.setS(""+location.longitude);
                
                AttributeValue avLat = new AttributeValue();
                avLat.setS(""+location.latitude);
                
                AttributeValue avAltM = new AttributeValue();
                avAltM.setS(""+location.altitudeMeters);

                AttributeValue avHeadingDecDegN0 = new AttributeValue();
                avHeadingDecDegN0.setS(""+location.headingDecDegFromNorth0);
 
                AttributeValue avPropertyBag = new AttributeValue();
                avPropertyBag.setM(location.properties);
                
                Map<String, AttributeValue> item = new HashMap<>();
                item.put("Id", avId );   
                item.put("type", avType);
                item.put("callsignTnspr", avCallSignTranspndr );                               
                item.put("dateTime", avDateTime);
                item.put("lon", avLon);                                
                item.put("lat", avLat);                
                item.put("altM", avAltM);                
                item.put("headingDecDegN0", avHeadingDecDegN0);  
                item.put("properties", avPropertyBag);
        
                /*
                log AttributeValue object
                for( String itemKey : item.keySet()){
                    AttributeValue value = item.get(itemKey);                    
                    logger.log("dynamo attribtute map="+itemKey+":"+value.toString());
                }*/
                
                // Write the item to the DynamoDB table
                PutItemRequest putItemRequest = new PutItemRequest()
                        .withTableName(TABLE_NAME)
                        .withItem(item);
                
                logger.log("dynamo put item request:"+putItemRequest.toString());                
                
                dynamoDB.putItem(putItemRequest); 
            }
            return "Success";
        } catch (SdkClientException | IOException e) {
           logger.log("GENERAL ERRROR converting OpenSky:"+e.getMessage());
            throw new RuntimeException(e);
        }
    }


}