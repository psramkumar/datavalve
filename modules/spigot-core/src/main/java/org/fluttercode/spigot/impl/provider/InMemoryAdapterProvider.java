/*
* Copyright 2010, Andrew M Gibson
*
* www.andygibson.net
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

package org.fluttercode.spigot.impl.provider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.fluttercode.spigot.DataProvider;
import org.fluttercode.spigot.DefaultPaginator;
import org.fluttercode.spigot.Paginator;
import org.fluttercode.spigot.provider.InMemoryDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a {@link DataProvider} that acts as an adapter onto another
 * data provider and makes it an in memory data provider. It does this by
 * pre-fetching the data from the underlying data provider and holding it
 * locally in memory. There are a couple of reasons to use this :
 * <ul>
 * <li>Caching data from a slow data store in memory for reuse</li>
 * <li>Allowing multiple independent data sources to work from a single in
 * memory store by keeping separate lists of the data in the adapter. Note that
 * while the lists are independent, the actual objects in the list are shared.</li>
 * </ul>
 * To use, just create an instance of this data provider and pass it the
 * {@link DataProvider} to source the data from. This provider can also use the
 * in memory sorting facilities using comparators.
 * 
 * @author Andy Gibson
 * 
 * @param <T>
 *            The type of object this provider will return.
 */
public class InMemoryAdapterProvider<T> extends InMemoryDataProvider<T> implements Serializable{

	private static final long serialVersionUID = 1L;

	private static Logger log = LoggerFactory
			.getLogger(InMemoryAdapterProvider.class);

	private DataProvider<T> provider;
	private List<T> backingData;

	public InMemoryAdapterProvider() {
		super();
	}

	public InMemoryAdapterProvider(DataProvider<T> provider) {
		setProvider(provider);
	}

	@Override
	protected List<T> fetchBackingData() {
		if (backingData == null) {
			generateBackingData();
		}
		invalidateData();
		return backingData;
	}

	private void generateBackingData() {

		if (provider == null) {
			throw new NullPointerException("Provider is null");
		}

		log.debug("Fetching backing data from provider");
		log.debug("provider = " + provider);
		Paginator p = new DefaultPaginator();
		p.setFirstResult(0);
		p.setMaxRows(null);
		List<T> results = provider.fetchResults(p);
		List<T> temp = new ArrayList<T>(results.size());
		for (T item : results) {
			temp.add(item);
		}
		backingData = temp;

	}

	public void setProvider(DataProvider<T> provider) {
		if (this.provider != provider) {
			this.provider = provider;
			//to be helpful, if we are sourcing from an {@link InMemoryDataProvider} 
			//then try and automatically assign the order key information
			if (provider instanceof InMemoryDataProvider<?>) {
				InMemoryDataProvider<T> imProvider = (InMemoryDataProvider<T>) provider;
				getOrderKeyMap().clear();
				getOrderKeyMap().putAll(imProvider.getOrderKeyMap());
			}
			invalidateData();
		}
	}

}
