package de.db.i4i.esf.aws.iot.shadow;

import com.amazonaws.services.iot.client.AWSIotException;

public interface AwsIotThingShadowService {

	public void attach(AwsIotListeningDevice device) throws AWSIotException;
	
	public void detach(AwsIotListeningDevice device) throws AWSIotException;
	
}
