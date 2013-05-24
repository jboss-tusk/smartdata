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
package org.jboss.tusk.smartdata.ui.controller;

import org.jboss.tusk.smartdata.data.CachedItem;
import org.jboss.tusk.smartdata.data.CachedItemHelper;
import org.jboss.tusk.smartdata.data.CachedItemHelperFactory;
import org.jboss.tusk.smartdata.ejb.RemoteIngester;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.util.UUID;

/**
 * A simple managed bean that is used to invoke the IngestEJB.
 *
 * @author jhayes
 */
@Named("ingester")
@SessionScoped
public class Ingester implements Serializable {

	/**
	 * Injected IngesterEJB client
	 */
	@EJB
	private RemoteIngester ingesterEJB;

	private String response;

	public void setNum(int num) {
		String responseAggr = "";
		CachedItemHelper helper = CachedItemHelperFactory.getInstance();
		for (int i = 0; i < num; i++) {
			String key = CachedItem.makeKey();
			CachedItem item = helper.sample();
			responseAggr += ingesterEJB.ingest(key, item) + ", ";
		}
		this.response = responseAggr;
	}

	public String getResponse() {
		return this.response;
	}

}
