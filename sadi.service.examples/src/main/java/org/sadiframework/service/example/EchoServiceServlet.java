package org.sadiframework.service.example;

import org.sadiframework.service.simple.SimpleSynchronousServiceServlet;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.ResourceUtils;

@SuppressWarnings("serial")
public class EchoServiceServlet extends SimpleSynchronousServiceServlet
{	
	public void processInput(Resource input, Resource output)
	{
		output.getModel().add(ResourceUtils.reachableClosure(input));
	}
}
