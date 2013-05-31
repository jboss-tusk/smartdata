package org.jboss.tusk.smartdata.domain.cable;

import java.io.Serializable;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.ProvidedId;
import org.jboss.logging.Logger;
import org.jboss.tusk.smartdata.data.CachedItem;
import org.jboss.tusk.smartdata.data.ParserTuple;

/**
 * Container for set top box log messages to be stored in the data grid.
 * 
 * @author justin
 *
 */
@Indexed @ProvidedId
public class STBLog extends CachedItem implements Serializable {
	
	private final static Logger LOG = Logger.getLogger(STBLog.class);

	@Field protected String key;

	@Field protected final long homeId;
	@Field protected final String accountNumber;
	@Field protected final String mbIpAddress;
	@Field protected final String mbMacAddress;
	@Field protected final String cblmIpAddress;
	@Field protected final String cblmMacAddress;
	@Field protected final String smartCard;
	@Field protected final long timestamp;
	@Field protected final int insertFail;
	@Field protected final int semanticError;
	@Field protected final int tcpInErrs;
	@Field protected final int docsisUncorrectedFecErrors;
	@Field protected final int docsisCorrectedFecErrors;
	
	public STBLog() {
		this(0, null, null, null, null, null, null, 0, 0, 0, 0, 0, 0);
	}
	
	public STBLog(String json) {
		this();
		
		System.out.println("NOT IMPLEMENTED YET; RETURNING DUMMY OBJECT");
		
	}
	
	public STBLog(long homeId, String accountNumber, String mbIpAddress, String mbMacAddress, String cblmIpAddress, String cblmMacAddress,
			String smartCard, long timestamp, int insertFail, int semanticError, int tcpInErrs, int docsisUncorrectedFecErrors,
			int docsisCorrectedFecErrors) {
		this.homeId = homeId;
		this.accountNumber = accountNumber;
		this.mbIpAddress = mbIpAddress;
		this.mbMacAddress = mbMacAddress;
		this.cblmIpAddress = cblmIpAddress;
		this.cblmMacAddress = cblmMacAddress;
		this.smartCard = smartCard;
		this.timestamp = timestamp;
		this.insertFail = insertFail;
		this.semanticError = semanticError;
		this.tcpInErrs = tcpInErrs;
		this.docsisUncorrectedFecErrors = docsisUncorrectedFecErrors;
		this.docsisCorrectedFecErrors = docsisCorrectedFecErrors;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("{")
			.append("\"key\":").append(this.key == null ? "" : this.key).append(",")
			.append("\"ts\":").append(this.timestamp).append(",")
			.append("\"home\":").append(this.homeId).append(",")
			.append("\"account\":\"").append(this.accountNumber).append("\",")
			.append("\"card\":\"").append(this.smartCard).append("\",")
			.append("\"mb\":[")
				.append("\"").append(this.mbIpAddress).append("\",")
				.append("\"").append(this.mbMacAddress).append("\"")
			.append("],")
			.append("\"cbl\":[")
				.append("\"").append(this.cblmIpAddress).append("\",")
				.append("\"").append(this.cblmMacAddress).append("\"")
			.append("],")
			.append("\"err\":{")
				.append("\"insert\":").append(this.insertFail).append(",")
				.append("\"semantic\":").append(this.semanticError).append(",")
				.append("\"tcp\":").append(this.tcpInErrs).append(",")
				.append("\"ufec\":").append(this.docsisUncorrectedFecErrors).append(",")
				.append("\"cfec\":").append(this.docsisCorrectedFecErrors)
			.append("}")
			.append("}");

		return builder.toString();
	}
	
	public static STBLog fromJSONOptimized(StringBuffer buf) throws Exception {
		int i = -1;
		int end = 0;
 
		//get the 'key' field; but just ignore it because we'll assign our own key
		//TODO should we append our own key to the existing key to not lose it?
		ParserTuple tuple = getJSONField(buf, "key", false, i);
		i = tuple.getIndex();

		//get the 'ts' field
		tuple = getJSONField(buf, "ts", false, i);
		long timestamp = new Long(tuple.getValue());
		i = tuple.getIndex();
//		System.out.println("timestamp=" + timestamp);

		//get the 'home' field
		tuple = getJSONField(buf, "home", false, i);
		long homeId = new Long(tuple.getValue());
		i = tuple.getIndex();
//		System.out.println("homeId=" + homeId);

		//get the 'account' field
		tuple = getJSONField(buf, "account", true, i);
		String accountNumber = tuple.getValue();
		i = tuple.getIndex();
//		System.out.println("accountNumber=" + accountNumber);

		//get the 'card' field
		tuple = getJSONField(buf, "card", true, i);
		String smartCard = tuple.getValue();
		i = tuple.getIndex();
//		System.out.println("smartCard=" + smartCard);
		
		//skip until the "mb:", then the whitespace, then the [, then the whitespace again
		do {
			i++;
		} while (keepGoing(buf, i, "mb")); //get past the mb:
		i = ignoreWhitespace(buf, i+1); //ignore whitespace to get to the opening [ for the list
		i = ignoreWhitespace(buf, i+1); //ignore the whitespace, starting at one char to the right (ie skipping over [)
		//get the next two double-quote-enclosed values, which are separated by ,
		end = ignoreUntilChar(buf, '\"', i+1); //get the next two double-quote-enclosed values, starting one to the right to ignore the opening "
		String mbIpAddress = buf.substring(i+1, end);
		i = end + 1;
//		System.out.println("mbIpAddress=" + mbIpAddress);

		//get the next two double-quote-enclosed values, which are separated by ,
		i = ignoreWhitespace(buf, i+1); //ignore whitespace to get to the next "
		end = ignoreUntilChar(buf, '\"', i+1); //get the next two double-quote-enclosed values, starting one to the right to ignore , that separates the vals of the list
		String mbMacAddress = buf.substring(i+1, end);
		i = end + 1;
//		System.out.println("mbMacAddress=" + mbMacAddress);

		//skip until the "cbl:", then the whitespace, then the [, then the whitespace again
		//get the next two double-quote-enclosed values, which are separated by ,
		do {
			i++;
		} while (keepGoing(buf, i, "cbl")); //get past the cbl:
		i = ignoreWhitespace(buf, i+1); //ignore whitespace to get to the opening [ for the list
		i = ignoreWhitespace(buf, i+1); //ignore the whitespace, starting at one char to the right (ie skipping over [)
		//get the next two double-quote-enclosed values, which are separated by ,
		end = ignoreUntilChar(buf, '\"', i+1); //get the next two double-quote-enclosed values, starting one to the right to ignore the opening "
		String cblmIpAddress = buf.substring(i+1, end);
//		System.out.println("cblmIpAddress=" + cblmIpAddress);

		//get the next two double-quote-enclosed values, which are separated by ,
		i = end + 1;
		i = ignoreWhitespace(buf, i+1); //ignore whitespace to get to the next "
		end = ignoreUntilChar(buf, '\"', i+1); //get the next two double-quote-enclosed values, starting one to the right to ignore , that separates the vals of the list
		String cblmMacAddress = buf.substring(i+1, end);
//		System.out.println("cblmMacAddress=" + cblmMacAddress);
		
		//skip until the "err:", then the whitespace, then the [, then the whitespace again
		//get the next five double-quote-enclosed values, which are separated by ,
		do {
			i++;
		} while (keepGoing(buf, i, "err")); //get past the err:
		i = ignoreWhitespace(buf, i+1); //ignore whitespace to get to the opening { for the map
		i = ignoreWhitespace(buf, i+1); //ignore the whitespace, starting at one char to the right (ie skipping over {)

		//get insertFail field
		tuple = getJSONField(buf, "insert", false, i);
		int insertFail = new Integer(tuple.getValue());
		i = tuple.getIndex();
//		System.out.println("insertFail=" + insertFail);

		//get semanticError field
		tuple = getJSONField(buf, "semantic", false, i);
		int semanticError = new Integer(tuple.getValue());
		i = tuple.getIndex();
//		System.out.println("semanticError=" + semanticError);

		//get tcpInErrs field
		tuple = getJSONField(buf, "tcp", false, i);
		int tcpInErrs = new Integer(tuple.getValue());
		i = tuple.getIndex();
//		System.out.println("tcpInErrs=" + tcpInErrs);

		//get docsisUncorrectedFecErrors field
		tuple = getJSONField(buf, "ufec", false, i);
		int docsisUncorrectedFecErrors = new Integer(tuple.getValue());
		i = tuple.getIndex();
//		System.out.println("docsisUncorrectedFecErrors=" + docsisUncorrectedFecErrors);

		//get docsisCorrectedFecErrors field
		tuple = getJSONField(buf, "cfec", false, true, i);
		int docsisCorrectedFecErrors = new Integer(tuple.getValue());
		i = tuple.getIndex();
//		System.out.println("docsisCorrectedFecErrors=" + docsisCorrectedFecErrors);
		
		STBLog stbLog = new STBLog(homeId, accountNumber, mbIpAddress, mbMacAddress, 
				cblmIpAddress, cblmMacAddress, smartCard, timestamp, 
				insertFail, semanticError, tcpInErrs, docsisUncorrectedFecErrors, docsisCorrectedFecErrors);
		
		stbLog.setKey(makeKey());
		
		return stbLog;
	}
	
	public static STBLog fromXMLOptimized(StringBuffer buf) throws Exception {
		//TODO implement
		return (STBLog)new STBLog().sample();
	}

	@Override
	public CachedItem sample() {
		STBLog stbLog = new STBLog(makeRandomLong(), makeRandomString(), makeRandomIP(), 
				makeRandomMAC(), makeRandomIP(), makeRandomMAC(), makeRandomString(), 
				makeRandomLong(), makeRandomInt(10), makeRandomInt(10), makeRandomInt(10),
				makeRandomInt(10), makeRandomInt(10));
		stbLog.setKey(makeKey());
		
		return stbLog;
	}

	
	@Override
	public Object getValForField(String fieldName) {
		Object itemVal = null;
		
		if ("homeId".equals(fieldName)) {
			itemVal = this.homeId;
		} else if ("accountNumber".equals(fieldName)) {
			itemVal = this.accountNumber;
		} else if ("mbIpAddress".equals(fieldName)) {
			itemVal = this.mbIpAddress;
		} else if ("mbMacAddress".equals(fieldName)) {
			itemVal = this.mbMacAddress;
		} else if ("cblmIpAddress".equals(fieldName)) {
			itemVal = this.cblmIpAddress;
		} else if ("cblmMacAddress".equals(fieldName)) {
			itemVal = this.cblmMacAddress;
		} else if ("smartCard".equals(fieldName)) {
			itemVal = this.smartCard;
		} else if ("timestamp".equals(fieldName)) {
			itemVal = this.timestamp;
		} else if ("insertFail".equals(fieldName)) {
			itemVal = this.insertFail;
		} else if ("semanticError".equals(fieldName)) {
			itemVal = this.semanticError;
		} else if ("tcpInErrs".equals(fieldName)) {
			itemVal = this.tcpInErrs;
		} else if ("docsisUncorrectedFecErrors".equals(fieldName)) {
			itemVal = this.docsisUncorrectedFecErrors;
		} else if ("docsisCorrectedFecErrors".equals(fieldName)) {
			itemVal = this.docsisCorrectedFecErrors;
		}
		
		return itemVal;
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public void setKey(String key) {
		this.key = key;
	}

	public long getHomeId() {
		return homeId;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public String getMbIpAddress() {
		return mbIpAddress;
	}

	public String getMbMacAddress() {
		return mbMacAddress;
	}

	public String getCblmIpAddress() {
		return cblmIpAddress;
	}

	public String getCblmMacAddress() {
		return cblmMacAddress;
	}

	public String getSmartCard() {
		return smartCard;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public int getInsertFail() {
		return insertFail;
	}

	public int getSemanticError() {
		return semanticError;
	}

	public int getTcpInErrs() {
		return tcpInErrs;
	}

	public int getDocsisUncorrectedFecErrors() {
		return docsisUncorrectedFecErrors;
	}

	public int getDocsisCorrectedFecErrors() {
		return docsisCorrectedFecErrors;
	}


}
