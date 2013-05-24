package org.jboss.tusk.smartdata.data.impl;

import java.io.Serializable;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.ProvidedId;

import org.jboss.tusk.smartdata.data.CachedItem;

/**
 * Container for set top box log messages to be stored in the data grid.
 * 
 * TODO add the fields that need to be stored in the data grid, marking the
 * ones that needs to be indexed as well.
 * 
 * @author justin
 *
 */
@Indexed @ProvidedId
public class Sample extends CachedItem implements Serializable {

	@Field protected String key;

	@Field protected final String field1;
	@Field protected final int field2;
	
	public Sample() {
		this(null, 0);
	}
	
	public Sample(String json) {
		this();
		System.out.println("NOT IMPLEMENTED YET; RETURNING DUMMY OBJECT");
	}
	
	public Sample(String field1, int field2) {
		this.field1 = field1;
		this.field2 = field2;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("{")
			.append("\"key\":").append(this.key == null ? "" : this.key).append(",")
			.append("\"field1\":\"").append(this.field1).append("\",")
			.append("\"field2\":").append(this.field2).append(",")
			.append("}");

		return builder.toString();
	}
	
	/**
	 * This should be a single-pass JSON parser that is as efficient as possible, at the
	 * expense of needing to be very tightly coupled to a particular input data format.
	 * @param buf
	 * @return
	 * @throws Exception
	 */
	public static Sample fromJSONOptimized(StringBuffer buf) throws Exception {
		//TODO implement
		Sample sample = (Sample)new Sample().sample();
		
		sample.setKey(makeKey());
		
		return sample;
	}

	/**
	 * This should be a single-pass XML parser that is as efficient as possible, at the
	 * expense of needing to be very tightly coupled to a particular input data format.
	 * @param buf
	 * @return
	 * @throws Exception
	 */
	public static Sample fromXMLOptimized(StringBuffer buf) throws Exception {
		//TODO implement
		return fromJSONOptimized(buf);
	}

	@Override
	public CachedItem sample() {
		Sample sample = new Sample(makeRandomString(), makeRandomInt(100));
		
		sample.setKey(makeKey());
		
		return sample;
	}

	
	@Override
	public Object getValForField(String fieldName) {
		Object itemVal = null;
		
		if ("field1".equals(fieldName)) {
			itemVal = this.field1;
		} else if ("field2".equals(fieldName)) {
			itemVal = this.field2;
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

	public String getField1() {
		return field1;
	}

	public int getField2() {
		return field2;
	}
}
