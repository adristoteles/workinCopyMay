package com.example;

import org.apache.log4j.Logger;
import org.sadiframework.service.annotations.Name;
import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.Description;
import org.sadiframework.service.annotations.InputClass;
import org.sadiframework.service.annotations.OutputClass;
import org.sadiframework.service.simple.SimpleSynchronousServiceServlet;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
//import com.hp.hpl.jena.rdf.model.Statement;
//import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.PrintUtil;

@Name("BMI")
@ContactEmail("adril9@hotmail.com")
@Description("Calculate BMI body index")
@InputClass("http://sadiframework.org/examples/bmi.owl#InputClass")
@OutputClass("http://sadiframework.org/examples/bmi.owl#OutputClass")
public class BMI_servlet extends SimpleSynchronousServiceServlet
{
	private static final Logger log = Logger.getLogger(BMI_servlet.class);
	private static final long serialVersionUID = 1L;

	@Override
	public void processInput(Resource input, Resource output)
	{
		/* your code goes here
		 * (add properties to output node based on properties of input node...)
		 */
		output.addLiteral(Vocab.BMI, getWeightInKg(input)/Math.pow(getHeightInM(input), 2));
		
		System.out.println("###################################SINGLE PROCESS OUTPUT######");
		// create testing data :
				// 1) default graph data
				Model model = ModelFactory.createDefaultModel();
				Resource s = model.createResource("http://eg.com/s");
				Property p = model.createProperty("http://eg.com/p");
				Resource o = model.createResource("http://eg.com/o");
				model.add(s, p, o);
				Dataset dataset = DatasetFactory.create(model);
				// 2) named graph data
				Model model1 = ModelFactory.createDefaultModel();
				Resource s1 = model.createResource("http://eg.com/s1");
				Property p1 = model.createProperty("http://eg.com/p1");
				Resource o1 = model.createResource("http://eg.com/o1");
				model1.add(s1, p1, o1);
				dataset.addNamedModel("http://eg.com/g1", model1);
				
				//PrintUtil.printOut(dataset.asDatasetGraph().find());
				
				
				
				//RDFDataMgr.write(System.out, dataset, Lang.NQUADS) ;
				//RDFDataMgr.write(System.out, dataset, Lang.NQ) ;
				//RDFDataMgr.write(System.out, dataset, Lang.TRIG) ;
				//RDFDataMgr.write(System.out, dataset, Lang.TRIX) ;
				//output.getModel().write(System.out,"TRIG");
				//output.getModel().write(System.out,"TRIX");
				//output.getModel().write(System.out,"N-QUADS");
	}
	
	public void processInput(Resource input, Resource output, Model provenance)
	{
		/* your code goes here
		 * (add properties to output node based on properties of input node...)
		 */
		output.addLiteral(Vocab.BMI, getWeightInKg(input)/Math.pow(getHeightInM(input), 2));
		
		System.out.println("###################################Parameters PROCESS OUTPUT######");
		
		provenance.add(provenance.createResource(output.getURI()), provenance.createProperty("http://www.w3.org/ns/prov#wasCalculatedFrom"), provenance.createResource(input.getURI()));
		
		// create testing data :
				// 1) default graph data
				/*Model model = ModelFactory.createDefaultModel();
				Resource s = model.createResource("http://eg.com/s");
				Property p = model.createProperty("http://eg.com/p");
				Resource o = model.createResource("http://eg.com/o");
				model.add(s, p, o);
				Dataset dataset = DatasetFactory.create(model);*/
				// 2) named graph data
				/*Model model1 = ModelFactory.createDefaultModel();
				Resource s1 = model.createResource("http://eg.com/s1");
				Property p1 = model.createProperty("http://eg.com/p1");
				Resource o1 = model.createResource("http://eg.com/o1");
				model1.add(s1, p1, o1);
				dataset.addNamedModel("http://eg.com/g1", model1);*/
				
				//PrintUtil.printOut(dataset.asDatasetGraph().find());
				
				//RDFDataMgr.write(System.out, dataset, Lang.NQUADS) ;
				//RDFDataMgr.write(System.out, dataset, Lang.NQ) ;
				//RDFDataMgr.write(System.out, dataset, Lang.TRIG) ;
				//RDFDataMgr.write(System.out, dataset, Lang.TRIX) ;
				//output.getModel().write(System.out,"TRIG");
				//output.getModel().write(System.out,"TRIX");
				//output.getModel().write(System.out,"N-QUADS");
	}
	
	/**
	 * Return the height in centimeters, assuming the input resource conforms
	 * to the MGED ontology for measurements.
	 * @param input
	 */
	public double getWeightInKg(Resource input)
	{
		Resource measurement = input.getRequiredProperty(Vocab.has_mass).getResource();
		Resource units = measurement.getRequiredProperty(Vocab.has_units).getResource();
		String value = measurement.getRequiredProperty(Vocab.has_value).getString();
		if (units.equals(Vocab.kg))
			return Double.parseDouble(value);
		else
			throw new IllegalArgumentException("weight measurement in unknown units");
	}
	/**
	 * Return the height in meters, assuming the input resource conforms to the
	 * MGED ontology for measurements.
	 * @param input
	 */
	public double getHeightInM(Resource input)
	{
		Resource measurement = input.getRequiredProperty(Vocab.has_height).getResource();
		Resource units = measurement.getRequiredProperty(Vocab.has_units).getResource();
		String value = measurement.getRequiredProperty(Vocab.has_value).getString();
		if (units.equals(Vocab.cm))
			return Double.parseDouble(value)/100;
		else if (units.equals(Vocab.m))
			return Double.parseDouble(value);
		else
			throw new IllegalArgumentException("height measurement in unknown units");
	}

	@SuppressWarnings("unused")
	private static final class Vocab
	{
		private static Model m_model = ModelFactory.createDefaultModel();		

		public static String MGED_NS = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#";
		public static final Property has_editor = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_editor");
		public static final Property has_phone = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_phone");
		public static final Property has_value = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_value");
		public static final Property has_mass = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_mass");
		public static final Property BMI = m_model.createProperty("http://sadiframework.org/examples/bmi.owl#BMI");
		public static final Property has_URI = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_URI");
		public static final Property has_authors = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_authors");
		public static final Property has_ID = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_ID");
		public static final Property has_address = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_address");
		public static final Property has_volume = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_volume");
		public static final Property has_measurement_type = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_measurement_type");
		public static final Property has_email = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_email");
		public static final Property has_height = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_height");
		public static final Property has_title = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_title");
		public static final Property has_publication = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_publication");
		public static final Property has_pages = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_pages");
		public static final Property has_toll_free_phone = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_toll_free_phone");
		public static final Property has_issue = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_issue");
		public static final Property is_user_defined = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#is_user_defined");
		public static final Property has_type = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_type");
		public static final Property has_year = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_year");
		public static final Property has_publisher = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_publisher");
		public static final Property has_units = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_units");
		public static final Property has_fax = m_model.createProperty("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_fax");
		public static final Resource Roles = m_model.createResource("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Roles");
		public static final Resource string = m_model.createResource("http://www.w3.org/2001/XMLSchema#string");
		public static final Resource date = m_model.createResource("http://www.w3.org/2001/XMLSchema#date");
		public static final Resource OutputClass = m_model.createResource("http://sadiframework.org/examples/bmi.owl#OutputClass");
		public static final Resource InputClass = m_model.createResource("http://sadiframework.org/examples/bmi.owl#InputClass");
		public static final Resource BibliographicReference = m_model.createResource("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#BibliographicReference");
		public static final Resource PublicationType = m_model.createResource("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#PublicationType");
		public static final Resource Unit = m_model.createResource("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Unit");
		public static final Resource Measurement = m_model.createResource("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Measurement");
		public static final Resource Contact = m_model.createResource("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Contact");
		public static final Resource URI = m_model.createResource("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#URI");
		public static final Resource MeasurementType = m_model.createResource("http://mged.sourceforge.net/ontologies/MGEDOntology.owl#MeasurementType");
		public static Resource kg = ResourceFactory.createResource(MGED_NS + "kg");
		public static Resource cm = ResourceFactory.createResource(MGED_NS + "cm");
		public static Resource m = ResourceFactory.createResource(MGED_NS + "m");
	}
}
