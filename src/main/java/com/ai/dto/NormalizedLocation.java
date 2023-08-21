package com.ai.dto;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * 
 */
public class NormalizedLocation  {

    public NormalizedLocation(){}

    public Date locationDateTime;
    public String uuid;
    public String id; 
    public String type;
    public Double longitude;
    public Double latitude;
    public Double altitudeMeters;
    public Double velocityMetersPerSec;
    public Double headingDecDegFromNorth0;
    public Map properties = new HashMap<String, String>();

    @Override
    public String toString(){
        return "locationDateTime:"+locationDateTime+
               ";uuid:"+id+
               ";id:"+id+
               ";type:"+type+
               ";longitude:"+longitude+
               ";latitude:"+latitude+
               ";altitudeMeters:"+altitudeMeters+
               ";velocityMetersPerSec:"+velocityMetersPerSec+
               ";headingDecDegFromNorth0:"+headingDecDegFromNorth0+
               ";properties:"+properties;
    }   
    
}
