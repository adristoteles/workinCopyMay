package org.sadiframework.utils;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sadiframework.utils.ResourceFactory;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;

public class ResourceFactoryTest
{
	@Test
	public void testCreateInstance()
	{
		String typeURI = "http://example.com/type";
		String id = "11235";
		Model model = ModelFactory.createDefaultModel();
		Resource type = model.createResource(typeURI);
		Resource r = ResourceFactory.createInstance(model, type, id);
		assertTrue("new resource is missing rdf:type", r.hasProperty(RDF.type, type));
		assertTrue("new resource is missing dc:identifier", r.hasProperty(DC.identifier, id));
	}
}
