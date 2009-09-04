package org.jdataset.util;

import java.util.Iterator;

import org.jdataset.IObjectDataset;

/**
 * Iterator implementation for the iterator that is returned from the 
 * @author Andy Gibson
 *
 * @param <T> Type of object this iterator returns
 */
public class DatasetIterator<T> implements Iterator<T> {

	private static final long serialVersionUID = 1L;
	private final IObjectDataset<T> dataset;
	private int index;

	public DatasetIterator(IObjectDataset<T> dataset) {
		super();
		if (dataset == null) {
			throw new IllegalArgumentException(
					"Cannot pass null to the dataset iterator");
		}
		this.dataset = dataset;
		dataset.first();
		index = 0;

	}

	public boolean hasNext() {
		if (index < dataset.getResultList().size()) {
			return true;
		}

		return dataset.isNextAvailable();
	}

	public T next() {
		if (index == dataset.getResultList().size()) {
			dataset.next();
			index = 0;
		}
		if (index < dataset.getResultList().size()) {
			return dataset.getResultList().get(index++);
		}

		throw new IllegalStateException("Unable to return item");
	}

	public void remove() {
		throw new UnsupportedOperationException(
				"Dataset iterators do not support element removal");

	}

}
