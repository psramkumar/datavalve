package org.jdataset.wicket;

import java.util.Iterator;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.jdataset.IPaginator;
import org.jdataset.SimplePaginator;
import org.jdataset.provider.IDataProvider;

public class PaginatedDataProvider<T> extends SortableDataProvider<T> {

	private static final long serialVersionUID = 1L;
	
	private IDataProvider<T> provider;
	private Integer size;

	public Iterator<? extends T> iterator(int first, int count) {
		IPaginator paginator = new SimplePaginator();
		paginator.setFirstResult(first);
		paginator.setMaxRows(count);
		return provider.fetchResults(paginator).iterator();
	}

	public IModel<T> model(T object) {
		return new CompoundPropertyModel<T>(object);
	}

	public int size() {
		if (size == null) {
			size = provider.fetchResultCount();
		}
		return size;
	}

}
