package org.jboss.tusk.smartdata.data.impl;

import java.util.ArrayList;
import java.util.List;

import javax.jms.BytesMessage;
import javax.jms.JMSException;

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
public class SampleHelper implements CachedItemHelper {

	private final static Logger LOG = Logger.getLogger(SampleHelper.class);
	
	private char SEPARATOR = '\n';
	
	@Override
	public CachedItem[] parseFromString(String val) {
		CachedItem[] arr = new Sample[1];
		
		arr[0] = sample();
		
		return arr;
	}

	@Override
	public CachedItem[] parseFromBytes(BytesMessage val) throws JMSException {
		List<Sample> list = new ArrayList<Sample>();

		StringBuffer buf = new StringBuffer();
		
		for (int i = 0; i < val.getBodyLength(); i++) {
			char c = (char) val.readByte();
			if (c == SEPARATOR) {
				list.add(parseOneSample(buf));
				buf = new StringBuffer();
			} else {
				buf.append(c);
			}
		}
		
		if (buf.length() > 0) {
			LOG.info("We have one last item to parse: " + buf.toString());
			list.add(parseOneSample(buf));
		}

//		LOG.info("We parsed out " + list.size() + " items.");

		return list.toArray(new Sample[1]);
	}

	private Sample parseOneSample(StringBuffer buf) {
		return new Sample(buf.toString());
	}
	
	@Override
	public Class getSearchClass() {
		return Sample.class;
	}
	
	@Override
	public CachedItem sample() {
		return new Sample().sample();
	}
	
	@Override
	public CachedItem fromString(String str) {
		return new Sample(str);
	}
	
	@Override
	public char getBatchSeparator() {
		return SEPARATOR;
	}

}
