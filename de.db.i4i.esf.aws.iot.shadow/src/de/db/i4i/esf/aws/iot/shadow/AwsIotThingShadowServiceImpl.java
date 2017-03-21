package de.db.i4i.esf.aws.iot.shadow;

import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.Key;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.ssl.SslManagerServiceOptions;
import org.eclipse.kura.ssl.SslServiceListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.iot.client.AWSIotConnectionStatus;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotTimeoutException;

public class AwsIotThingShadowServiceImpl implements AwsIotThingShadowService, ConfigurableComponent, SslServiceListener {

	private static final Logger logger = LoggerFactory.getLogger(AwsIotThingShadowServiceImpl.class);
	
	private AWSIotMqttClient client;
	private final List<AwsIotListeningDevice> devices;
	AwsIotThingShadowServiceOptions options;
	private SslManagerService sslManagerService;
	private CryptoService cryptoService;
	
	public AwsIotThingShadowServiceImpl() {
		devices = new CopyOnWriteArrayList<AwsIotListeningDevice>();
	}
	
	public void setSslManagerService(SslManagerService sslManagerService) {
        this.sslManagerService = sslManagerService;
    }

    public void unsetSslManagerService(SslManagerService sslManagerService) {
        this.sslManagerService = null;
    }
    
    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void unsetCryptoService(CryptoService cryptoService) {
        this.cryptoService = null;
    }
	
	protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
		logger.info("activate {}...", properties.get(ConfigurationService.KURA_SERVICE_PID));
		try {
			this.options = new AwsIotThingShadowServiceOptions(properties);
			KeyStorePasswordPair keyStorePasswordPair = this.getKeyStorePasswordPair();
			this.setupClient(keyStorePasswordPair);
		} catch (Exception e) {
			logger.error("Could not activate service - {}", e.getMessage());
			throw new ComponentException(e);
		}
	}
	
	protected void deactivate(ComponentContext componentContext) {
		logger.info("deactivate {}...", componentContext.getProperties().get(ConfigurationService.KURA_SERVICE_PID));
		for (AwsIotListeningDevice device : this.devices) {
			try {
				logger.info("Detaching device {}",device.getThingName());
				this.devices.remove(device);
				this.client.detach(device);
			} catch (AWSIotException e) {}
		}
		logger.info("Disconnecting");
		try {
			this.client.disconnect(this.options.getConnectionTimeout(), false);
		} catch (Exception e) {}
		this.client = null;
	}
	
	public void updated(Map<String, Object> properties) {
		logger.info("updated {}...: {}", properties.get(ConfigurationService.KURA_SERVICE_PID), properties);
		this.options = new AwsIotThingShadowServiceOptions(properties);
		try {
			restartClient();
		} catch (KuraException e) {
			logger.error("Could not restart client");
			throw new ComponentException(e);
		}
	}
	
	@Override
	public void attach(AwsIotListeningDevice device) throws AWSIotException {
		try {
			if (this.client != null) {
				this.client.attach(device);
				this.devices.add(device);
			}
		} catch (Exception e) {
			logger.error("Could not attach device {}", device.getThingName());
			throw new AWSIotException(e);
		}
		if (this.client.getConnectionStatus() == AWSIotConnectionStatus.DISCONNECTED) {
			try {
				logger.info("Device {} attached, trying to connect", device.getThingName());
				this.client.connect(this.options.getConnectionTimeout(), false);
			} catch (AWSIotTimeoutException e) {}
			catch (KuraException e) {
				throw new AWSIotException(e);
			}
		}
	}

	@Override
	public void detach(AwsIotListeningDevice device) throws AWSIotException {
		try {
			if (this.client != null) {
				this.client.detach(device);
				this.devices.remove(device);
			}
		} catch (Exception e) {
			logger.error("Could not detach device {}", device.getThingName());
			throw new AWSIotException(e);
		}
		if (this.devices.isEmpty()) {
			try {
				logger.info("Device {} detached, no more devices attached, trying to disconnect", device.getThingName());
				this.client.disconnect(this.options.getConnectionTimeout(), false);
			} catch (AWSIotTimeoutException e) {}
			catch (KuraException e) {
				throw new AWSIotException(e);
			}
		}
	}

	@Override
	public void onConfigurationUpdated() {
		logger.info("Notified SSLManagerService configuration updated");
		try {
			restartClient();
		} catch (KuraException e) {
			logger.warn("Could not restart client - {}", e.getMessage());
		}
	}
	
	private void restartClient() throws KuraException {
		logger.info("Restarting client");
		for (AwsIotListeningDevice device : this.devices) {
			logger.info("Detaching device {}", device.getThingName());
			try {
				this.client.detach(device);
			} catch (AWSIotException e) {
				logger.warn("Could not detach device {}", device.getThingName());
			}
		}
		logger.info("Disconnecting");
		if (this.client.getConnectionStatus() != AWSIotConnectionStatus.DISCONNECTED) {
			try {
				this.client.disconnect(this.options.getConnectionTimeout(), true);
			} catch (Exception e) {
				logger.warn("Disconnect failed");
			}
		}
		try {
			KeyStorePasswordPair keyStorePasswordPair = this.getKeyStorePasswordPair();
			this.setupClient(keyStorePasswordPair);
		} catch (Exception e) {
			logger.error("Could not update service - {}", e.getMessage());
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		for (AwsIotListeningDevice device : this.devices) {
			logger.info("Re-attaching device {}", device.getThingName());
			try {
				this.client.attach(device);
			} catch (AWSIotException e) {
				logger.warn("Could not attach device {}", device.getThingName());
				device.onReAttachFailed();
			}
		}
	}

	private KeyStorePasswordPair getKeyStorePasswordPair() throws KuraException {
		KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(null);
		} catch (Exception e) {
			logger.error("Could not create keystore instance - {}", e.getMessage());
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Could not create keystore instance - " + e);
		}
		String keyPassword = "";
		try {
			SslManagerServiceOptions sslOptions = this.sslManagerService.getConfigurationOptions();
			KeyStore kuraKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			FileInputStream is = new FileInputStream(sslOptions.getSslKeyStore());
			char[] kuraKeyStorePassword = this.cryptoService.getKeyStorePassword(sslOptions.getSslKeyStore());
			logger.info("Loading keystore {}", sslOptions.getSslKeyStore());
			kuraKeyStore.load(is, kuraKeyStorePassword);
			Key key = kuraKeyStore.getKey(this.options.getKuraKeystoreAlias(), kuraKeyStorePassword);
			Certificate[] chain = kuraKeyStore.getCertificateChain(this.options.getKuraKeystoreAlias());
			keyPassword = new BigInteger(128, new SecureRandom()).toString(32);
			keyStore.setKeyEntry("alias", key, keyPassword.toCharArray(), chain);
		} catch (Exception e) {
			logger.warn("Could not get keystore/password - {}", e.getMessage());
		}
		return new KeyStorePasswordPair(keyStore, keyPassword);
	}
	
	private void setupClient(KeyStorePasswordPair keyStorePasswordPair) throws KuraException {
		try {
			this.client = new AWSIotMqttClient(this.options.getClientEndpoint(),
											   this.options.getClientId(),
											   keyStorePasswordPair.keyStore,
											   keyStorePasswordPair.keyPassword);
			this.client.setBaseRetryDelay(this.options.getBaseRetryDelay());
			this.client.setConnectionTimeout(this.options.getConnectionTimeout());
			this.client.setKeepAliveInterval(this.options.getKeepAliveInterval());
			this.client.setMaxConnectionRetries(this.options.getMaxConnectionRetries());
			this.client.setMaxOfflineQueueSize(this.options.getMaxOfflineQueueSize());
			this.client.setMaxRetryDelay(this.options.getMaxRetryDelay());
			this.client.setNumOfClientThreads(this.options.getNumOfClientThreads());
			this.client.setServerAckTimeout(this.options.getServerAckTimeout());
			if (!this.options.getWillMessageTopic().isEmpty()) {
				this.client.setWillMessage(new AWSIotMessage(this.options.getWillMessageTopic(),
						this.options.getWillMessageQos(), this.options.getWillMessagePayload()));
			}
		} catch (Exception e) {
			logger.error("Could not setup client - {}", e.getMessage());
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public static class KeyStorePasswordPair {
        public KeyStore keyStore;
        public String keyPassword;

        public KeyStorePasswordPair(KeyStore keyStore, String keyPassword) {
            this.keyStore = keyStore;
            this.keyPassword = keyPassword;
        }
    }
}
