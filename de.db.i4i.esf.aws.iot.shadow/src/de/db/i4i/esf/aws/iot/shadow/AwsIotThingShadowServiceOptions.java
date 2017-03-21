package de.db.i4i.esf.aws.iot.shadow;

import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

import com.amazonaws.services.iot.client.AWSIotQos;

public class AwsIotThingShadowServiceOptions {
	
	private static final String CLIENT_ENDPOINT_PROP_NAME = "client.endpoint";
	private static final String CLIENT_ID_PROP_NAME = "client.id";
	private static final String BASE_RETRY_DELAY_PROP_NAME = "base.retry.delay";
	private static final String CONNECTION_TIMEOUT_PROP_NAME = "connection.timeout";
	private static final String KEEP_ALIVE_INTERVAL_PROP_NAME = "keep.alive.interval";
	private static final String MAX_CONNECTION_RETRIES_PROP_NAME = "max.connection.retries";
	private static final String MAX_OFFLINE_QUEUE_SIZE_PROP_NAME = "max.offline.queue.size";
	private static final String MAX_RETRY_DELAY_PROP_NAME = "max.retry.delay";
	private static final String NUM_OF_CLIENT_THREADS_PROP_NAME = "num.of.client.threads";
	private static final String SERVER_ACK_TIMEOUT_PROP_NAME = "server.ack.timeout";
	private static final String WILL_MESSAGE_PAYLOAD_PROP_NAME = "will.message.payload";
	private static final String WILL_MESSAGE_TOPIC_PROP_NAME = "will.message.topic";
	private static final String WILL_MESSAGE_QOS_PROP_NAME = "will.message.qos";
	private static final String KURA_KEYSTORE_ALIAS_PROP_NAME = "kura.keystore.alias";

	private Map<String, Object> properties;
	
	public AwsIotThingShadowServiceOptions(Map<String, Object> properties) {
		this.properties = properties;
	}

	public String getClientEndpoint() throws KuraException {
		return (String) getProperty(CLIENT_ENDPOINT_PROP_NAME);
	}
	
	public String getClientId() throws KuraException {
		return (String) getProperty(CLIENT_ID_PROP_NAME);
	}
	
	public Integer getBaseRetryDelay() throws KuraException {
		return (Integer) getProperty(BASE_RETRY_DELAY_PROP_NAME);
	}
	
	public Integer getConnectionTimeout() throws KuraException {
		return (Integer) getProperty(CONNECTION_TIMEOUT_PROP_NAME);
	}
	
	public Integer getKeepAliveInterval() throws KuraException {
		return (Integer) getProperty(KEEP_ALIVE_INTERVAL_PROP_NAME);
	}
	
	public Integer getMaxConnectionRetries() throws KuraException {
		return (Integer) getProperty(MAX_CONNECTION_RETRIES_PROP_NAME);
	}
	
	public Integer getMaxOfflineQueueSize() throws KuraException {
		return (Integer) getProperty(MAX_OFFLINE_QUEUE_SIZE_PROP_NAME);
	}
	public Integer getMaxRetryDelay() throws KuraException {
		return (Integer) getProperty(MAX_RETRY_DELAY_PROP_NAME);
	}
	
	public Integer getNumOfClientThreads() throws KuraException {;
		return (Integer) getProperty(NUM_OF_CLIENT_THREADS_PROP_NAME);
	}
	
	public Integer getServerAckTimeout() throws KuraException {
		return (Integer) getProperty(SERVER_ACK_TIMEOUT_PROP_NAME);
	}
	
	public String getWillMessagePayload() throws KuraException {
		try {
			return (String) getProperty(WILL_MESSAGE_PAYLOAD_PROP_NAME);
		} catch (Exception e) {
			return "";
		}
	}
	
	public String getWillMessageTopic() throws KuraException {
		try {
			return (String) getProperty(WILL_MESSAGE_TOPIC_PROP_NAME);
		} catch (Exception e) {
			return "";
		}
	}
	
	public AWSIotQos getWillMessageQos() throws KuraException {
		try {
			return AWSIotQos.valueOf((String) getProperty(WILL_MESSAGE_QOS_PROP_NAME));
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, e);
		}
	}
	
	public String getKuraKeystoreAlias() throws KuraException {
		return (String) getProperty(KURA_KEYSTORE_ALIAS_PROP_NAME);
	}
	
	private Object getProperty(String propertyName) throws KuraException {
		if (this.properties.containsKey(propertyName)) {
			return this.properties.get(propertyName);
		} else {
			throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_UNDEFINED, propertyName);
		}
	}
}
