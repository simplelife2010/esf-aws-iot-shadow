package de.db.i4i.esf.aws.iot.shadow;

import com.amazonaws.services.iot.client.AWSIotDevice;

public abstract class AwsIotListeningDevice extends AWSIotDevice implements AwsIotThingShadowServiceListener {

	public AwsIotListeningDevice(String thingName) {
		super(thingName);
	}

	@Override
	public void onReAttachFailed() {}

}
