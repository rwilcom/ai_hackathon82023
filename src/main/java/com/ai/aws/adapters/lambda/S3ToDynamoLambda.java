package com.ai.aws.adapters.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/*
 * EXAMPLE FILE OF LAMBDA FUNCTION TO MOVE FROM S3 to DYNAMO 
 * 
 */ 
public class S3ToDynamoLambda implements RequestHandler<S3Event, String> {

    private final AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
    private final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

    @Override
    public String handleRequest(S3Event s3event, Context context) {
        try {
            // Get the S3 object details from the event
            String bucketName = s3event.getRecords().get(0).getS3().getBucket().getName();
            String key = s3event.getRecords().get(0).getS3().getObject().getKey();

            // Retrieve the S3 object
            S3Object s3Object = s3Client.getObject(bucketName, key);
            S3ObjectInputStream s3InputStream = s3Object.getObjectContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(s3InputStream));

            // Read and parse the file (adjust this logic to suit your file's format)
            String line;
            while ((line = reader.readLine()) != null) {
                // Example: split a CSV line by commas
                String[] parts = line.split(",");
                String id = parts[0];
                String value = parts[1];

                // Create a DynamoDB item
                Map<String, AttributeValue> item = new HashMap<>();
                item.put("id", new AttributeValue().withS(id));
                item.put("value", new AttributeValue().withS(value));

                // Write the item to the DynamoDB table
                PutItemRequest putItemRequest = new PutItemRequest()
                        .withTableName("Your-Table-Name")
                        .withItem(item);
                dynamoDB.putItem(putItemRequest);
            }

            // Close the streams
            reader.close();
            s3InputStream.close();

            return "Success";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}