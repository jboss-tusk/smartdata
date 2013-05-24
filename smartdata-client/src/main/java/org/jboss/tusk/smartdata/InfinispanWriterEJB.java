package org.jboss.tusk.smartdata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.tusk.smartdata.data.CachedItem;
import org.jboss.tusk.smartdata.data.CachedItemHelper;
import org.jboss.tusk.smartdata.data.CachedItemHelperFactory;
import org.jboss.tusk.smartdata.ejb.RemoteIngester;

public class InfinispanWriterEJB {
	
	private static int BUFFER_SIZE = -1;
	private static int BATCH_SIZE = -1;
	private static int NUM_INGESTERS = -1;
	private static List<RemoteIngester> ingesters = null;
	private static CachedItem[] messagesBuffer = null;
	private static int bufferCt = 0;
	private static Properties config = new Properties();
	
	//security credentials
	public static final String USERNAME = "jboss";
	public static final String PASSWORD = "password";

	private static int totalCt = 0;

	private static CachedItemHelper helper = CachedItemHelperFactory.getInstance();
	
	public static int init(String propsFile) {
		System.out.println("In init");

		//get all system properties in one place
		config.putAll(System.getProperties());

		propsFile = !StringUtils.isEmpty(propsFile) ? 
				propsFile : 
					"smartdata-client.properties";
		System.out.println("Using properties file named " + propsFile + ".");
		
		try {
			config.load(ClassLoader.getSystemClassLoader().getResourceAsStream(propsFile));
		} catch (Exception ex) {
			System.err.println("Could not load the " + propsFile + " file.");
		}

		BUFFER_SIZE = Integer.valueOf(config.getProperty("buffer.size", "3000"));
		BATCH_SIZE = Integer.valueOf(config.getProperty("batch.size", "150"));
		NUM_INGESTERS = BUFFER_SIZE / BATCH_SIZE;
		ingesters = new ArrayList<RemoteIngester>(NUM_INGESTERS);
		messagesBuffer = new CachedItem[BUFFER_SIZE];

		System.out.println("BUFFER_SIZE=" + BUFFER_SIZE);
		System.out.println("BATCH_SIZE=" + BATCH_SIZE);
		System.out.println("NUM_INGESTERS=" + NUM_INGESTERS);
		
		String ingesterServer = config.getProperty("ingester.server", "localhost:4447");

		//get the remote ingester server hostname and port
		if (StringUtils.isEmpty(ingesterServer)) {
			System.err.println("Could not create any initial context. Check the 'ingester.server' " +
					"property and make sure that the corresponding server is running.");
		}

		//get the remote EJBs from the ingester server
		for (int i = 0; i < NUM_INGESTERS; i++) {
			RemoteIngester ingester = getIngester(ingesterServer);
			if (ingester != null) {
				System.out.println("Adding ingester " + i + ", which uses server " + ingesterServer);
				ingesters.add(ingester);
			} else {
				System.err.println("Could not get ingester from server " + ingesterServer);
			}
		}

		return 1;
	}

	private static RemoteIngester getIngester(String ingesterServer) {
		String server = ingesterServer.trim();
		String host = server.substring(0, server.indexOf(":"));
		String port = server.substring(server.indexOf(":") + 1);
		
		Properties props = new Properties();
		props.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
		props.put("remote.connections", "default");
		props.put("remote.connection.default.port", port);
		props.put("remote.connection.default.host", host);
		props.put("remote.connection.default.username", USERNAME);
		props.put("remote.connection.default.password", PASSWORD);
		props.put("remote.connection.default.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
		EJBClientConfiguration clientConfig = new PropertiesBasedEJBClientConfiguration(props);
		ContextSelector<EJBClientContext> selector = new ConfigBasedEJBClientContextSelector(clientConfig);
		final ContextSelector<EJBClientContext> previousSelector = EJBClientContext.setSelector(selector);
		
		final Hashtable jndiProperties = new Hashtable();
		jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
		jndiProperties.put("jboss.naming.client.ejb.context", true);
		
		try {
			InitialContext context = new InitialContext(jndiProperties);
			RemoteIngester ingester = (RemoteIngester) context
					.lookup("ejb:smartdata-ear/smartdata-ejb//IngesterEJB!"
							+ RemoteIngester.class.getName());
			return ingester;
		} catch (NamingException ex) {
			System.err.println("Caught NamingException getting initial context " +
					"for remote ingester server " + server + "; parsed as '" + 
					host + "':'" + port + "'.");
			System.err.println(ex.getMessage());
			ex.printStackTrace();
		}
		
		return null;
	}

	public static int write(String message) {
		if (ingesters.isEmpty()) {
			init(null);
		}

		if (bufferCt < BUFFER_SIZE) {
			// add to buffer
			messagesBuffer[bufferCt] = helper.fromString(message);
			messagesBuffer[bufferCt].setKey(CachedItem.makeKey());
			bufferCt++;
		}

		// now flush and then reset buffer if we've filled it up
		if (bufferCt == BUFFER_SIZE) {
			flushBuffer();
			messagesBuffer = new CachedItem[BUFFER_SIZE];
		}

		// System.out.println("Added '" + message + "' to data grid with key " +
		// uuid + ". Response was: " + response);

		totalCt++;
		
//		if (totalCt % 100000 == 0) {
//			System.out.println("  Wrote " + totalCt
//					+ " messages. Last one was '" + message + "'");
//		}
		
		return 1;
	}

	private static void flushBuffer() {
		System.out.println("Flushing buffer, which has "
				+ messagesBuffer.length + " items.");

		long start = System.currentTimeMillis();

		// this just sends the entire buffer in one batch
		// String response = ingester.ingest(messagesBuffer);

		List<CachedItem[]> batches = new ArrayList<CachedItem[]>(NUM_INGESTERS);
		for (int i = 0; i < NUM_INGESTERS; i++) {
			batches.add(new CachedItem[BATCH_SIZE]);
		}
		
		int[] cts = new int[NUM_INGESTERS];
		for (int i = 0; i < NUM_INGESTERS; i++) {
			cts[i] = 0;
		}
		
		for (int i = 0; i < messagesBuffer.length; i++) {
			int whichBatch = i / BATCH_SIZE;
//			System.out.println("whichBatch=" + whichBatch);
			CachedItem[] currBatch = batches.get(whichBatch);
			cts[whichBatch]++;
			currBatch[i % BATCH_SIZE] = messagesBuffer[i];
		}

		List<IngesterThreadEJB> threads = new ArrayList<IngesterThreadEJB>(NUM_INGESTERS);
		for (int i = 0; i < NUM_INGESTERS; i++) {
			threads.add(new IngesterThreadEJB(ingesters.get(i), batches.get(i)));
		}
		
//		System.out.println("About to start flush threads");
		for (int i = 0; i < NUM_INGESTERS; i++) {
			if (cts[i] > 0) { 
				threads.get(i).start(); 
			}
		}

		for (int i = 0; i < NUM_INGESTERS; i++) {
			if (cts[i] > 0) { 
				try {
					threads.get(i).join();
				} catch (InterruptedException ex) {
					System.err.println("Caught InterruptedException on joining thread " + i + ": " + ex.getMessage());
				} 
			}
		}
			
		//System.out.println("Flush threads finished");

		long total = System.currentTimeMillis() - start;
		System.out.println("Flushing buffer of " + messagesBuffer.length
				+ " messages took " + total + " ms.");

		// System.out.println("Added '" + message + "' to data grid with key " +
		// uuid + ". Response was: " + response);

		bufferCt = 0;
	}

	public static int cleanup() {
		// indexGrid.getCacheManager().stop();

		if (bufferCt > 0) {
			// if we don't have a full buffer, make a smaller one and send it
			if (bufferCt < BUFFER_SIZE) {
				CachedItem[] messagesBuffer2 = new CachedItem[bufferCt];
				for (int i = 0; i < bufferCt; i++) {
					messagesBuffer2[i] = messagesBuffer[i];
				}
				messagesBuffer = messagesBuffer2;
			}
			flushBuffer();
		}

		return 1;
	}

	public static void main(String[] args) {
		System.out.println("In main");
		init(args.length > 1 ? args[1] : null);

		// see how many messages to write
		int num = 1;
		if (args.length > 0) {
			try {
				num = Integer.valueOf(args[0]);
			} catch (NumberFormatException ex) {
				System.out.println("Couldn't convert '" + args[0]
						+ "' to an integer; Using default of " + num);
			}
		}
		System.out.println("Writing " + num + " message(s).");

		// prepare messages to write
		CachedItemHelper helper = CachedItemHelperFactory.getInstance();
		String[] messages = new String[num];
		for (int i = 0; i < num; i++) {
			messages[i] = helper.sample().toString();
		}

		// write messages
		long start = System.currentTimeMillis();
		for (int i = 0; i < num; i++) {
			write(messages[i]);
		}
		long total = System.currentTimeMillis() - start;

		cleanup();

		System.out.println(num + " writes took " + total + " ms. Messages per sec: " + (1.0 * num / total));
		System.out.println("Finished with main");
	}
}

class IngesterThreadEJB extends Thread {
	private RemoteIngester ingester;
	private CachedItem[] batch;

	public IngesterThreadEJB(RemoteIngester ingester, CachedItem[] batch) {
		this.ingester = ingester;
		this.batch = batch;
	}

	@Override
	public void run() {
		long start = System.currentTimeMillis();

		//this way uses the EJB to ingest the batch
		String response = this.ingester.ingest(this.batch);

		long total = System.currentTimeMillis() - start;
		System.out.println("  " + System.currentTimeMillis() + ": EJB call to ingest batch of " + this.batch.length
				+ " messages took " + total + " ms.");
	}

}
