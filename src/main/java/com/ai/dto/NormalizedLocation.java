package com.ai.dto;

import java.io.IOException;
import java.util.Date;

/**
 *
 * 
 */
public class NormalizedLocation  {

    public NormalizedLocation(){}

    public Date locationDateTime;
    public String uid; 
    public Double longitude;
    public Double latitude;
    public Double altitudeMeters;
    public Double velocityMetersPerSec;
    public Double headingDecDegFromNorth0;

    @Override
    public String toString(){
        return "locationDateTime:"+locationDateTime+
               ";uid:"+uid+
               ";longitude:"+longitude+
               ";latitude:"+latitude+
               ";altitudeMeters:"+altitudeMeters+
               ";velocityMetersPerSec:"+velocityMetersPerSec+
               ";headingDecDegFromNorth0:"+headingDecDegFromNorth0;
    }   
    
}
