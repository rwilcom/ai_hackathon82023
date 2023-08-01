package com.ai.aws.adapters.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;


public class OpenSkyAdapterLambda implements RequestHandler<String, Integer>{

  @Override
  public Integer handleRequest(String rawFile, Context context){
    LambdaLogger logger = context.getLogger();
    logger.log("RawFile found: " + rawFile);
    return 0;
  }


}