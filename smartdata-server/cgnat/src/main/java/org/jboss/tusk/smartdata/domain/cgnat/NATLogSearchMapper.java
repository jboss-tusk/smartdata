package org.jboss.tusk.smartdata.domain.cgnat;

import java.util.ArrayList;
import java.util.List;

import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;

import org.jboss.tusk.smartdata.ispn.InfinispanService;
import org.jboss.tusk.smartdata.ispn.Operator;
import org.jboss.tusk.smartdata.ispn.SearchCriterion;

public class NATLogSearchMapper implements Mapper<String, NATLog, String, NATLog> {
	
	private static final long serialVersionUID = 1465081292848331070L;

	private String criteria = null;
	private List<SearchCriterion> criteriaList = null;
	private boolean isAndQuery = true;
	
	public NATLogSearchMapper(String criteria, boolean isAndQuery) {
		this.criteria = criteria;
		this.criteriaList = InfinispanService.parseCriteria(criteria);
		this.isAndQuery = isAndQuery;
	}
	
//	private List<SearchCriterion> parseCriteria(String criteria) {
//		String[] conditions = null;
//		
//		if (criteria.indexOf("|") != -1) {
//			conditions = criteria.split("\\|");
//		} else {
//			conditions = criteria.split(",");
//		}
//		
//		List<SearchCriterion> criteriaList = new ArrayList<SearchCriterion>(conditions.length);
//		for (String condition : conditions) {
//			condition = condition.trim();
//			String field = condition.substring(0, condition.indexOf("="));
//			String value = condition.substring(condition.indexOf("=") + 1);
//			criteriaList.add(new SearchCriterion(field, value));
//		}
//		
//		return criteriaList;
//	}

	/**
	 * Emits the value if it matches the search criteria. Whether or not it must match ALL
	 * criteria or only some of the criteria is determined by the isAndQuery field.
	 * @param key
	 * @param value
	 * @param coll
	 */
	@Override
	public void map(String key, NATLog value, Collector<String, NATLog> coll) {
//		System.out.println("Mapping " + value + " with criteria " + this.criteriaList);
		
		//no criteria, so emit the value
		if (this.criteriaList == null) {
			coll.emit(key, value);
			return;
		}
		
		//go through criteriaList and only emit the key, value if it matches the criteria
		boolean atLeastOneMatch = false;
		for (SearchCriterion criterion : this.criteriaList) {
			Object itemVal = null;
			
			if (criterion.getField().equals("origIP")) {
				itemVal = value.getOrigIP();
			} else if (criterion.getField().equals("origPort")) {
				itemVal = value.getOrigPort();
			} else if (criterion.getField().equals("destIP")) {
				itemVal = value.getDestIP();
			} else if (criterion.getField().equals("destPort")) {
				itemVal = value.getDestPort();
			} else if (criterion.getField().equals("transIP")) {
				itemVal = value.getTransIP();
			} else if (criterion.getField().equals("transPort")) {
				itemVal = value.getTransPort();
			} else if (criterion.getField().equals("orig")) {
				itemVal = value.getOrig();
			} else if (criterion.getField().equals("type")) {
				itemVal = value.getType();
			} else if (criterion.getField().equals("app")) {
				itemVal = value.getApp();
			}
			
			//if itemVal is null, don't emit and return now as this is an invalid state
			if (itemVal == null) {
				System.err.println("Field in SearchCriterion '" + criterion + "' not recognized; " +
						"not emitting value.");
				return;
//			} else if (!itemVal.equals(criterion.getValue())) { //this way forces an exact match
			} else if (!itemVal.toString().startsWith(criterion.getValue())) { //this way treats the criterion as "val*" (adds wildcard to end)
				if (this.isAndQuery) {
					//the value doesn't match this criterion but it must match ALL criteria, so return now
					return;
				} else {
					//record the match and go on
					atLeastOneMatch = true;
				}
			}
		}
		
		//emit the value because one of the following is true:
		//	there are no criteria
		//	it's an AND and this value matched all criteria
		//	it's an OR and it matched at least one criterion
		if (this.isAndQuery || atLeastOneMatch) {
//			System.out.println("  Match!!");
			coll.emit(key, value);
		}
	}
}
