package org.sadiframework.client;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import org.sadiframework.SADIException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * A class that aggregates results from several Registry objects.
 * This would be so much easier in Python...
 */
public class MultiRegistry implements Registry
{
	public static final Logger log = Logger.getLogger(MultiRegistry.class);
	
	List<Registry> registries;
	
	/**
	 * Constructs a new Registry that will aggregate results from the
	 * specified list of component registries.
	 * @param registries the component registries
	 */
	public MultiRegistry(List<Registry> registries)
	{
		this.registries = registries;
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.registry.Registry#getService(java.lang.String)
     */
	@Override
	public Service getService(String serviceURI)
	{
		for (Registry registry: registries) {
			try {
				Service service = registry.getService(serviceURI);
				if (service != null)
					return service;
			} catch (Exception e) {
				log.error(String.format("error contacting registry %s", registry), e);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Registry#getAllServices()
	 */
	@Override
	public Collection<Service> getAllServices() {
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.getAllServices();
			}	
		});
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Registry#getServiceStatus(java.lang.String)
	 */
	@Override
	public ServiceStatus getServiceStatus(String serviceURI) throws SADIException {
		for (Registry registry: registries) {
			try {
				ServiceStatus status = registry.getServiceStatus(serviceURI);
				if (status != null)
					return status;
			} catch (Exception e) {
				log.error(String.format("error contacting registry %s", registry), e);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Registry#findServicesByAttachedProperty(com.hp.hpl.jena.rdf.model.Property)
	 */
	@Override
	public Collection<Service> findServicesByAttachedProperty(final Property property) throws SADIException
	{
		log.debug("¡¡¡¡===INIT MultiRegistry SADI.CLIENT - findServicesByAttachedProperty()= "+property);
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.findServicesByAttachedProperty(property);
			}
		});
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Registry#findServicesByInputClass(com.hp.hpl.jena.rdf.model.Resource)
	 */
	@Override
	public Collection<Service> findServicesByInputClass(final Resource clazz) throws SADIException
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.findServicesByInputClass(clazz);
			}
		});
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Registry#findServicesByConnectedClass(com.hp.hpl.jena.rdf.model.Resource)
	 */
	@Override
	public Collection<Service> findServicesByConnectedClass(final Resource clazz) throws SADIException
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.findServicesByConnectedClass(clazz);
			}
		});
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.registry.Registry#findServices(com.hp.hpl.jena.rdf.model.Resource, java.lang.String)
     */
	/** @deprecated */
	public Collection<Service> findServices(final Resource subject, final String predicate)
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.findServices(subject, predicate);
			}	
		});
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.registry.Registry#findServicesByPredicate(java.lang.String)
	 */
	/** @deprecated */
	public Collection<Service> findServicesByPredicate(final String predicate)
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.findServicesByPredicate(predicate);
			}	
		});
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.registry.Registry#findPredicatesBySubject(com.hp.hpl.jena.rdf.model.Resource)
     */
	/** @deprecated */
	public Collection<String> findPredicatesBySubject(final Resource subject)
	{
		return (Collection<String>) accumulate(new Accumulator<String>() {
			public Collection<String> get(Registry registry) throws Exception {
				return registry.findPredicatesBySubject(subject);
			}	
		});
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.registry.Registry#discoverServices(com.hp.hpl.jena.rdf.model.Resource)
     */
	public Collection<Service> discoverServices(final Resource subject)
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.discoverServices(subject);
			}	
		});
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.registry.Registry#discoverServices(com.hp.hpl.jena.rdf.model.Model)
     */
	public Collection<ServiceInputPair> discoverServices(final Model model)
	{
		return accumulate(new Accumulator<ServiceInputPair>() {
			public Collection<? extends ServiceInputPair> get(Registry registry) throws Exception {
				return registry.discoverServices(model);
			}	
		});
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Registry#findServices(ca.wilkinsonlab.sadi.client.RegistrySearchCriteria)
	 */
	@Override
	public Collection<? extends Service> findServices(final RegistrySearchCriteria criteria) throws SADIException
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.findServices(criteria);
			}
		});
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Registry#findAttachedProperties(ca.wilkinsonlab.sadi.client.RegistrySearchCriteria)
	 */
	@Override
	public Collection<Property> findAttachedProperties(final RegistrySearchCriteria criteria) throws SADIException
	{
		return accumulate(new Accumulator<Property>() {
			public Collection<Property> get(Registry registry) throws Exception {
				return registry.findAttachedProperties(criteria);
			}
		});
	}
	
//	@Override
//	public Collection<Service> findServicesByInputClass(final String inputClassURI)
//	{
//		return accumulate(new Accumulator<Service>() {
//			public Collection<? extends Service> get(Registry registry) throws Exception {
//				Collection<Service> services = new ArrayList<Service>();
//				for (Service service: registry.getAllServices()) {
//					if (ObjectUtils.equals(inputClassURI, service.getInputClass()))
//						services.add(service);
//				}
//				return services;
//			}
//		});
//	}
	
	private <T> Collection<T> accumulate(Accumulator<T> accum)
	{
		Collection<T> results = new HashSet<T>();
		for (Registry registry: registries) {
			try {
				results.addAll(accum.get(registry));
			} catch (Exception e) {
				log.error(String.format("error contacting registry %s", registry), e);
			}
		}
		return results;
	}

	private interface Accumulator<T>
	{	
		public Collection<? extends T> get(Registry registry) throws Exception;
	}
}
