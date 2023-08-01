package com.ai.aws.adapters.lambda;

import static com.ai.adapters.OpenSkyAdapter.writeNormalizedLocationToJsonFile;
import com.ai.dto.NormalizedLocation;
import com.ai.opensky.OpenSky;
import com.ai.opensky.OpenSkyRawAirTraffic;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OpenSkyAdapterLambda implements RequestHandler<S3Event, String> {
    
    private final AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
    private final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

    @Override
    public String handleRequest(S3Event s3event, Context context) {
        try {
            
             LambdaLogger logger = context.getLogger();
            
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
            
            
            ArrayList<OpenSkyRawAirTraffic> osraList = 
                    OpenSky.readAirTrafficRawOutputToObjects( fullFileAsString.toString() );

            System.out.print("total flights to normalize: "+osraList.size()+"\n");
            
            for( OpenSkyRawAirTraffic osra: osraList ){
                NormalizedLocation location = new NormalizedLocation();
                location.locationDateTime = osra.timeOfPositionDateTime;
                location.uid = osra.callsign +"-"+osra.icao24TransponderAddr;
                location.longitude = osra.longitude;
                location.latitude = osra.latitude;
                location.altitudeMeters = osra.altitudeMeters;
                location.velocityMetersPerSec = osra.velocityMetersPerSec;
                location.headingDecDegFromNorth0 = osra.headingDecDegFromNorth0;

                if( osra.onGround ){
                    location.altitudeMeters = 0.0;
                    location.velocityMetersPerSec = 0.0;
                }

                System.out.print(location.toString()+"\n");
                
                // Create a DynamoDB item
                Map<String, AttributeValue> item = new HashMap<>();
                item.put("index", new AttributeValue().withS( ""+location.uid+location.locationDateTime.getTime()));
                item.put("dateTime", new AttributeValue().withS( ""+location.locationDateTime));
                item.put("id", new AttributeValue().withS( location.uid));
                item.put("long", new AttributeValue().withS(""+location.longitude));
                item.put("lat", new AttributeValue().withS(""+location.latitude));
                item.put("altitudeMeters", new AttributeValue().withS(""+location.altitudeMeters));
                item.put("headingDecDegFromNorth0", new AttributeValue().withS(""+location.headingDecDegFromNorth0));
        
                // Write the item to the DynamoDB table
                PutItemRequest putItemRequest = new PutItemRequest()
                        .withTableName("openskydb")
                        .withItem(item);
                dynamoDB.putItem(putItemRequest); 
            }
            return "Success";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}