package org.jboss.tusk.smartdata.domain.cable;

import java.util.ArrayList;
import java.util.List;

import javax.jms.BytesMessage;
import javax.jms.JMSException;

import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;
import org.jboss.tusk.smartdata.data.CachedItem;
import org.jboss.tusk.smartdata.data.CachedItemHelper;

/**
 * 
 * This class helps with parsing data to create STBLog objects. It also
 * helps with identifying the class to use for the search.
 * 
 * @author justin
 *
 */
public class STBLogHelper implements CachedItemHelper {

	private final static Logger LOG = Logger.getLogger(STBLogHelper.class);

	private static final String PIPE_SEPARATED_JSON = "pipeSeparatedJSON";
	private static final String NEWLINE_SEPARATED_JSON = "newlineSeparatedJSON";
	private static final String PIPE_SEPARATED_XML = "pipeSeparatedXML";
	private static final String NEWLINE_SEPARATED_XML = "newlineSeparatedXML";
	private static String PARSE_TYPE = System.getProperty("parsetype", NEWLINE_SEPARATED_JSON);
	
	//figure out which separator to use for the batch
	private char SEPARATOR = (PARSE_TYPE.equalsIgnoreCase(NEWLINE_SEPARATED_JSON) || 
			PARSE_TYPE.equalsIgnoreCase(NEWLINE_SEPARATED_XML)) ? '\n' : '|';
	
	@Override
	public CachedItem[] parseFromString(String val) {
		// TODO it's OK to ignore this for now
		
		LOG.warn("Not implemented yet.");
		
		return null;
	}

	@Override
	public CachedItem[] parseFromBytes(BytesMessage val) throws JMSException {

		//TODO - remove after testing
//		LOG.info("Saving off copy of message");
//		StringBuffer tempBuf = new StringBuffer();
//		for (int i = 0; i < val.getBodyLength(); i++) {
//			char c = (char) val.readByte();
//			tempBuf.append(c);
//		}
//		String str = tempBuf.toString();
////		str = str.replaceAll("}{", "}\n{");
//		try {
//			FileUtils.writeStringToFile(new java.io.File("/tmp/out" + System.currentTimeMillis() + ".json"), str);
//		} catch (Exception ex) {
//			LOG.error("Caught " + ex.getClass().getName() + " writing file: " + ex.getMessage());
//		}
//		LOG.info("Done saving off copy of message");
//		if (1 == 1) return null;
		//TODO - remove after testing
		
		
		List<STBLog> stbLogsList = new ArrayList<STBLog>();

		StringBuffer buf = new StringBuffer();
		
		for (int i = 0; i < val.getBodyLength(); i++) {
			char c = (char) val.readByte();
			if (c == SEPARATOR) {
				stbLogsList.add(parseOneSTBLog(buf));
				buf = new StringBuffer();
			} else {
				buf.append(c);
			}
		}
		
		if (buf.length() > 0) {
			LOG.info("We have one last item to parse: " + buf.toString());
			stbLogsList.add(parseOneSTBLog(buf));
		}

//		LOG.info("We parsed out " + stbLogsList.size() + " items.");

		return stbLogsList.toArray(new STBLog[1]);
	}

	private STBLog parseOneSTBLog(StringBuffer buf) {
		try {
//			LOG.info("  Got one: " + buf);

			if (PARSE_TYPE.equalsIgnoreCase(NEWLINE_SEPARATED_XML) || PARSE_TYPE.equalsIgnoreCase(PIPE_SEPARATED_XML)) {
//				LOG.info("  Parsing as XML: " + buf.toString());
				//parse from the syslog string
				return STBLog.fromXMLOptimized(buf);
			} else if (PARSE_TYPE.equalsIgnoreCase(NEWLINE_SEPARATED_JSON) || PARSE_TYPE.equalsIgnoreCase(PIPE_SEPARATED_JSON)) {
//				LOG.info("  Parsing as JSON: " + buf.toString());
				//use the constructor, which accepts a JSON argument
//				return new STBLog(buf.toString());
				return STBLog.fromJSONOptimized(buf);
			} else {
				LOG.error("Invalid parsetype (" + PARSE_TYPE + "). Cannot parse message, so I'm ignoring it.");
			}
			
		} catch (Exception ex) {
//			System.out.println("Caught " + ex.getClass().getName() + " (" + ex.getMessage() + 
//			") parsing into STBLog object: '" + buf.toString() + "'");
		}
		
		return null;
	}
	
	@Override
	public Class getSearchClass() {
		return STBLog.class;
	}
	
	@Override
	public CachedItem sample() {
		return new STBLog().sample();
	}
	
	@Override
	public CachedItem fromString(String str) {
		return new STBLog(str);
	}
	
	@Override
	public char getBatchSeparator() {
		return SEPARATOR;
	}

}
