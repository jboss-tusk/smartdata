package org.jboss.tusk.smartdata.domain.cgnat;

import java.util.ArrayList;
import java.util.List;

import javax.jms.BytesMessage;
import javax.jms.JMSException;

import org.jboss.logging.Logger;
import org.jboss.tusk.smartdata.data.CachedItem;
import org.jboss.tusk.smartdata.data.CachedItemHelper;

public class NATLogHelper implements CachedItemHelper {

	private final static Logger LOG = Logger.getLogger(NATLogHelper.class);

	private static final String PIPE_SEPARATED_JSON = "pipeSeparatedJSON";
	private static final String NEWLINE_SEPARATED_JSON = "newlineSeparatedJSON";
	private static final String PIPE_SEPARATED_SYSLOG = "pipeSeparatedSyslog";
	private static final String NEWLINE_SEPARATED_SYSLOG = "newlineSeparatedSyslog";
	private static String PARSE_TYPE = System.getProperty("parsetype", NEWLINE_SEPARATED_SYSLOG);
	
	//figure out which separator to use for the batch
	char SEPARATOR = (PARSE_TYPE.equalsIgnoreCase(NEWLINE_SEPARATED_SYSLOG) || 
			PARSE_TYPE.equalsIgnoreCase(NEWLINE_SEPARATED_JSON)) ? '\n' : '|';
	
	@Override
	public CachedItem[] parseFromString(String val) {
//		LOG.info("In parseFromString for: " + val);
		NATLog[] natLogs = null;
		
		//first have to parse it out into individual NATLog items
		if (PARSE_TYPE.equalsIgnoreCase(NEWLINE_SEPARATED_SYSLOG)) {
			natLogs = NATLog.parseAsNewlineSeparatedSyslog(val);
		} else if (PARSE_TYPE.equalsIgnoreCase(PIPE_SEPARATED_SYSLOG)) {
			natLogs = NATLog.parseAsPipeSeparatedSyslog(val);
		} else if (PARSE_TYPE.equalsIgnoreCase(NEWLINE_SEPARATED_JSON)) {
			natLogs = NATLog.parseAsNewlineSeparatedJSON(val);
		} else if (PARSE_TYPE.equalsIgnoreCase(PIPE_SEPARATED_JSON)) {
			natLogs = NATLog.parseAsPipeSeparatedJSON(val);
		} else {
			LOG.error("Invalid parsetype (" + PARSE_TYPE + "). Cannot parse message, so I'm ignoring it.");
		}
		
		return natLogs;
	}

	@Override
	public CachedItem[] parseFromBytes(BytesMessage val) throws JMSException {
		List<NATLog> natLogsList = new ArrayList<NATLog>();

		StringBuffer buf = new StringBuffer();
		
		for (int i = 0; i < val.getBodyLength(); i++) {
			char c = (char) val.readByte();
			if (c == SEPARATOR) {
				natLogsList.add(parseOneNATLog(buf));
				buf = new StringBuffer();
			} else {
				buf.append(c);
			}
		}
		
		if (buf.length() > 0) {
			LOG.info("We have one last item to parse: " + buf.toString());
			natLogsList.add(parseOneNATLog(buf));
		}

//		LOG.info("We parsed out " + natLogsList.size() + " items.");

		return natLogsList.toArray(new NATLog[1]);
	}

	private NATLog parseOneNATLog(StringBuffer buf) {
		try {
//			LOG.info("  Got one: " + buf);
//			natLogsList.add(NATLog.fromSyslogOptimized(buf));

			if (PARSE_TYPE.equalsIgnoreCase(NEWLINE_SEPARATED_SYSLOG) || PARSE_TYPE.equalsIgnoreCase(PIPE_SEPARATED_SYSLOG)) {
//				LOG.info("  Parsing as SYSLOG: " + buf.toString());
				//parse from the syslog string
				return NATLog.fromSyslogOptimized(buf);
			} else if (PARSE_TYPE.equalsIgnoreCase(NEWLINE_SEPARATED_JSON) || PARSE_TYPE.equalsIgnoreCase(PIPE_SEPARATED_JSON)) {
//				LOG.info("  Parsing as JSON: " + buf.toString());
				//use the constructor, which accepts a JSON argument
				return new NATLog(buf.toString());
			} else {
				LOG.error("Invalid parsetype (" + PARSE_TYPE + "). Cannot parse message, so I'm ignoring it.");
			}
			
		} catch (Exception ex) {
//			System.out.println("Caught " + ex.getClass().getName() + " (" + ex.getMessage() + 
//			") parsing syslog into NATLog object: '" + buf.toString() + "'");
		}
		
		return null;
	}
	
	@Override
	public Class getSearchClass() {
		return NATLog.class;
	}

	@Override
	public CachedItem sample() {
		return new NATLog().sample();
	}
	
	@Override
	public CachedItem fromString(String str) {
		return new NATLog(str);
	}
	
	@Override
	public char getBatchSeparator() {
		return SEPARATOR;
	}

}
