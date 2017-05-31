package com.example;

import org.apache.log4j.Logger;
import org.sadiframework.service.annotations.Name;
import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.Description;
import org.sadiframework.service.annotations.InputClass;
import org.sadiframework.service.annotations.OutputClass;
import org.sadiframework.service.simple.SimpleSynchronousServiceServlet;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
//import com.hp.hpl.jena.rdf.model.Statement;
//import com.hp.hpl.jena.rdf.model.StmtIterator;

@Name("hello")
@ContactEmail("adril9@hotmail.com")
@Description("MOdify the diagnose code")
@InputClass("http://sadiframework.org/examples/hello.owl#NamedIndividual")
@OutputClass("http://sadiframework.org/examples/hello.owl#GreetedIndividual")
public class hello extends SimpleSynchronousServiceServlet
{
	private static final Logger log = Logger.getLogger(hello.class);
	private static final long serialVersionUID = 1L;

	@Override
	public void processInput(Resource input, Resource output)
	{
		/* your code goes here
		 * (add properties to output node based on properties of input node...)
		 */
		System.out.println("Initiating HelloWorld... ");
	    String name = input.getProperty(Vocab.name).getString(); 
	    output.getModel().write(System.out,"RDF/XML");
	    output.addProperty(Vocab.greeting, String.format("Hello, %s!", name));
	}

	@SuppressWarnings("unused")
	private static final class Vocab
	{
		private static Model m_model = ModelFactory.createDefaultModel();
		
		public static final Property greeting = m_model.createProperty("http://sadiframework.org/examples/hello.owl#greeting");
		public static final Property name = m_model.createProperty("http://xmlns.com/foaf/0.1/name");
		public static final Resource GreetedIndividual = m_model.createResource("http://sadiframework.org/examples/hello.owl#GreetedIndividual");
		public static final Resource NamedIndividual = m_model.createResource("http://sadiframework.org/examples/hello.owl#NamedIndividual");
		public static final Resource string = m_model.createResource("http://www.w3.org/2001/XMLSchema#string");
	}
}
