package org.jboss.tusk.smartdata.data;

public class ParserTuple {

	String value;
	int index;
	public ParserTuple(String value, int index) {
		this.value = value;
		this.index = index;
	}
	public String getValue() {
		return this.value;
	}
	public int getIndex() {
		return this.index;
	}
	
}
