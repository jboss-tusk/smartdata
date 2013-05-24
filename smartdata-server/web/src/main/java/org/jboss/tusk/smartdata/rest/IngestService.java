/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the 
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.tusk.smartdata.rest;

import org.jboss.tusk.smartdata.data.CachedItem;
import org.jboss.tusk.smartdata.data.CachedItemHelper;
import org.jboss.tusk.smartdata.data.CachedItemHelperFactory;
import org.jboss.tusk.smartdata.ejb.IngesterHelper;

/**
 * A CDI service which is used to ingest data
 * 
 */
public class IngestService {

	private CachedItemHelper cachedItemHelper = CachedItemHelperFactory.getInstance();

	int ingest(String val) {
		return ingestBatch(val);
	}

	int ingestBatch(String batch) {
		CachedItem[] cachedItems = cachedItemHelper.parseFromString(batch);
		IngesterHelper ingesterHelper = new IngesterHelper();
		int num = ingesterHelper.ingestBatch(cachedItems);
		
		return num;
	}

	int ingestRandom() {
		return ingestRandom(1);
	}

	int ingestRandom(int num) {
		//TODO might want to put a cap on how big num can be to prevent overdoing it
		
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < num; i++) {
			if (buf.length() > 0) {
				buf.append(cachedItemHelper.getBatchSeparator());
			}
			buf.append(cachedItemHelper.sample().toString());
		}
		return ingest(buf.toString());
	}

}
