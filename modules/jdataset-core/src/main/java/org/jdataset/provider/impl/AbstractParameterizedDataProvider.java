package org.jdataset.provider.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdataset.AbstractDataset;
import org.jdataset.DatasetEnvironment;
import org.jdataset.ParameterParser;
import org.jdataset.params.Parameter;
import org.jdataset.params.ParameterResolver;
import org.jdataset.params.RegexParameterParser;
import org.jdataset.provider.IParameterizedDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends the {@link AbstractDataset} to implement the
 * {@link IParameterizedDataProvider} methods. This class adds handling for parameter
 * resolvers, holding a fixed parameter map, extracting parameters from text and
 * resolving parameters.
 * <p>
 * By default, when the dataset is asked to resolve a comma prefixed parameter,
 * it should look in the parameters map first to see if it exists there.
 * 
 * @author Andy Gibson
 * 
 * @param <T>
 */
public abstract class AbstractParameterizedDataProvider<T> extends
		AbstractDataProvider<T> implements IParameterizedDataProvider<T> {

	private static final long serialVersionUID = 1L;
	private static Logger log = LoggerFactory
			.getLogger(AbstractParameterizedDataProvider.class);

	private ParameterParser parameterParser = new RegexParameterParser();	
	private Map<String, Object> parameters = new HashMap<String, Object>();
	private List<ParameterResolver> parameterResolvers = new ArrayList<ParameterResolver>();

		
	public void addParameter(String name, Object value) {
		parameters.put(name, value);
	}

	public void addParameterResolver(ParameterResolver resolver) {
		parameterResolvers.add(resolver);
	}

	public Object resolveParameter(String name) {

		if (name == null) {
			return null;
		}
				
		Parameter param = new Parameter(name);
		
		//try and resolve through the global parameter resolvers
		if (DatasetEnvironment.getInstance().resolveParameter(this, param)) {
			return param.getValue();
		}

		for (ParameterResolver resolver : parameterResolvers) {

			if (resolver.acceptParameter(name)) {
				if (resolver.resolveParameter(this, param)) {
					log.debug("Resolved using resolver : '{}'", resolver);
					log.debug("Resolved value as : '{}'", param.getValue());
					return param.getValue();
				}
			}
		}
		return null;
	}

	public ParameterParser getParameterParser() {
		return parameterParser;
	}

	public void setParameterParser(ParameterParser parameterParser) {
		this.parameterParser = parameterParser;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}
}