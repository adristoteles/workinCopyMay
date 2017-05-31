package org.sadiframework.utils;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.sadiframework.decompose.RestrictionVisitor;


import org.apache.jena.ontology.ConversionException;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Restriction;

public class RestrictedPropertyCollector implements RestrictionVisitor
{
	private static final Logger log = Logger.getLogger(RestrictedPropertyCollector.class);
	
	private Set<OntProperty> properties;
	
	public RestrictedPropertyCollector()
	{
		properties = new HashSet<OntProperty>();
	}
	
	public Set<OntProperty> getProperties()
	{
		return properties;
	}
	
	/* (non-Javadoc)
	 * @see org.sadiframework.decompose.RestrictionVisitor#visit(com.hp.hpl.jena.ontology.Restriction)
	 */
	public void visit(Restriction restriction)
	{
		try {
			OntProperty p = restriction.getOnProperty();
			if (p != null)
				properties.add(p);
		} catch (ConversionException e) {
			// we should already have warned about this above, but just in case...
			log.warn(String.format("undefined restricted property %s"), e);
		}
	}
}