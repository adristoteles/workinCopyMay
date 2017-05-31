package ca.wilkinsonlab.sadi.registry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import virtuoso.jena.driver.VirtModel;
import org.sadiframework.SADIException;
import org.sadiframework.beans.RestrictionBean;
import org.sadiframework.beans.ServiceBean;
import org.sadiframework.client.Service;
import org.sadiframework.client.ServiceFactory;
import org.sadiframework.client.ServiceImpl;
import org.sadiframework.service.ontology.MyGridServiceOntologyHelper;
import org.sadiframework.service.ontology.ServiceOntologyException;
import org.sadiframework.utils.LabelUtils;
import org.sadiframework.utils.OwlUtils;
import org.sadiframework.vocab.SADI;

//import org.apache.jena.db.DBConnection;
//import org.apache.jena.db.IDBConnection;
import org.apache.jena.ontology.ConversionException;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ModelMaker;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * The server side registry logic.
 * TODO separate interface and implementation completely?
 * Not really necessary since no one else will be extending or implementing
 * another of these, but it might be a good idea if I ever find myself doing so.
 * @author Luke McCarthy
 */
public class Registry
{
	private static final Logger log = Logger.getLogger(Registry.class);
	
	/**
	 * Returns the registry configuration object. 
	 * Properties are read from a file called sadi.registry.properties; 
	 * see {@link org.apache.commons.configuration.PropertiesConfiguration}
	 * for details about which locations will be searched for that file.
	 * @return the registry configuration object
	 */
	public static Configuration getConfig()
	{
		try {
			return new PropertiesConfiguration("sadi.registry.properties");
		} catch (ConfigurationException e) {
			log.warn( String.format("error reading registry configuration: %s", e) );
			return new PropertiesConfiguration();
		}
	}
	
	/**
	 * Returns the default registry implementation.
	 * The default  configuration is contained in a file called
	 * sadi.registry.properties located in the classpath.
	 * @return the default registry implementation
	 */
	public static Registry getRegistry() throws SADIException
	{
		Configuration config = getConfig();
		String file = config.getString("file");
		String driver = config.getString("driver");
		String graph = config.getString("graph");
		String dsn = config.getString("dsn");
		String username = config.getString("username");
		String password = config.getString("password");
		System.out.println("#INIT jspcall getRegistry in graph :"+graph);
		if (driver == null) { System.out.println("driver null");
			if (file == null) { System.out.println("file null");
				log.warn("no database driver or file specified; creating transient registry model");
				return new Registry(ModelFactory.createDefaultModel());
			} else { System.out.println("file="+file);
				return getFileRegistry(file);
			}
		} else if (driver.equals("virtuoso.jdbc3.Driver")) { 
			return getVirtuosoRegistry(graph, dsn, username, password);
		} else { System.out.println("JDBC GET");
			return getJDBCRegistry(driver, dsn, username, password);
		}
	}
	
	/**
	 * Returns a Registry backed by a Virtuoso model.
	 * @param graph
	 * @param dsn
	 * @param username
	 * @param password
	 * @return
	 */
	public static Registry getVirtuosoRegistry(String graph, String dsn, String username, String password) throws SADIException
	{
		System.out.println(String.format("#getVirtuosoRegistry(),creating Virtuoso-backed registry model from %s(%s)", dsn, graph));
		try {
			Model model = initVirtuosoRegistryModel(graph, dsn, username, password);
			return new Registry(model);
		} catch (Exception e) {
			throw new SADIException(String.format("error connecting to Virtuoso registry at %s", dsn), e);
		}
	}

	private static Model initVirtuosoRegistryModel(String graph, String dsn, String username, String password)
	{
		Model m = VirtModel.openDatabaseModel(graph, dsn, username, password);
		System.out.println("# initVirtuosoRegistryModel - VirtuosoRegistryModel was CREATED");
		return m;
	}
	
	/**
	 * Returns a Registry backed by a JDBC model.
	 * @param driver (e.g.: "com.mysql.jdbc.driver")
	 * @param dsn
	 * @param username
	 * @param password
	 * @return
	 */
	public static Registry getJDBCRegistry(String driver, String dsn, String username, String password) throws SADIException
	{
		System.out.println(String.format("creating JDBC-backed registry model from %s", dsn));
		try {
			Model model = initJDBCRegistryModel(driver, dsn, username, password);
			return new Registry(model);
		} catch (Exception e) {
			throw new SADIException(String.format("error connecting to JDBC registry at %s", dsn), e);
		}
	}
	
	private static Model initJDBCRegistryModel(String driver, String dsn, String username, String password)
	{
		// load the driver class
		try {
			Class.forName(driver);
		} catch ( ClassNotFoundException e ) {
			throw new RuntimeException(e);
		}
		
		/* create a database connection
		IDBConnection conn = new DBConnection(
				dsn,
				username,
				password,
				driver.matches("(?i).*mysql.*") ? "MySQL" : null
		);
		*/
		// create a model maker with the given connection parameters
		ModelMaker maker = null;
		System.out.println("initJDBCRegistryModel");
		// create a default model
		return maker.createDefaultModel();
	}
	
	/**
	 * Returns a registry backed by a file.
	 * @param path
	 * @return 
	 */
	public static Registry getFileRegistry(String path) throws SADIException
	{
		log.debug(String.format("creating file-backed33 registry model from %s", path));
		try {
			Model model = initFileRegistryModel(path);
			System.out.println("init0");
			return new Registry(model);
		} catch (Exception e) {
			throw new SADIException(String.format("error reading registry from %s", path), e);
		}
	}
	
	private static Model initFileRegistryModel(String path) throws IOException
	{
		System.out.println("init");
		File registryFile = new File(path);
		File parentDirectory = registryFile.getParentFile();
		if (parentDirectory == null)
			parentDirectory = new File(".");
		if (!parentDirectory.isDirectory())
			parentDirectory.mkdirs();
		System.out.println("init 2");
				//ModelMaker maker = ModelFactory.createFileModelMaker(parentDirectory.getAbsolutePath());
				//return maker.openModel(registryFile.getName());
				//Model model = RDFDataMgr.loadModel("C:/tmp/sadi-registryE.rdf") ;
		Model model = ModelFactory.createDefaultModel();
		model.write(System.out, "RDF/XML");
		model = FileManager.get().loadModel(path,"RDF/XML");
		model.write(System.out, "RDF/XML");
		System.out.println("-parent Directory");
		System.out.println(parentDirectory.getAbsolutePath());
		System.out.println(path);
		System.out.println("¡¡¡¡===INIT initFileRegistryModel Registry.java===!!!");
		System.out.println(registryFile.getName());
		return model;
	}
	
	private Model model;

	/**
	 * Constructs a Registry from the specified Jena model.
	 * @param model
	 */
	public Registry(Model model)
	{
		this.model = model;
		model.setNsPrefix("sadi", "http://sadiframework.org/ontologies/sadi.owl#");
		model.setNsPrefix("mygrid", "http://www.mygrid.org.uk/mygrid-moby-service#");
	}
	
	/**
	 * Returns the Jena model containing the registry data.
	 * @return the Jena model containing the registry data
	 */
	public Model getModel()
	{
		return model;
	}
	
	/**
	 * Returns an iterator over the registered service nodes.
	 * @return an iterator over the registered service nodes
	 */
	public ResIterator getRegisteredServiceNodes()
	{
		return getModel().listResourcesWithProperty(RDF.type, SADI.Service);
	}
	
	public Collection<ServiceBean> getRegisteredServices()
	{
		Collection<ServiceBean> services = new ArrayList<ServiceBean>();
		for (ResIterator i = getRegisteredServiceNodes(); i.hasNext(); ) {
			Resource serviceNode = i.nextResource();
			ServiceBean service = getServiceBean(serviceNode);
			services.add(service);
		}
		return services;
	}
	
	/**
	 * Returns true if the registry contains the specified service,
	 * false otherwise.
	 * @param serviceURI
	 * @return true if the registry contains the specified service, false otherwise
	 */
	public boolean containsService(String serviceURI)
	{
		return getServiceBean(serviceURI) != null;
	}
	
	/**
	 * Returns a bean describing the specified registered service, 
	 * or null if the service is not registered.
	 * @param serviceURI
	 * @return a bean describing the specified registered service, or null
	 */
	public ServiceBean getServiceBean(String serviceURI)
	{
		Resource serviceNode = getModel().getResource(serviceURI);
		if (serviceNode != null && getModel().containsResource(serviceNode))
			return getServiceBean(serviceNode);
		else
			return null;
	}
	
	/**
	 * Returns a bean describing the specified service node.
	 * @param serviceNode
	 * @return a bean describing the specified service node
	 */
	public ServiceBean getServiceBean(Resource serviceNode)
	{
		ServiceBean service = new ServiceBean();

		log.debug(String.format("Leyendo servicios de service node--> %s", serviceNode));
		try{
			log.debug(String.format("getServiceBEan--> %s", serviceNode));
			new MyGridServiceOntologyHelper().copyServiceDescription(serviceNode, service);
			log.debug(String.format("getServiceBEan--> %s", serviceNode));
		} catch (ServiceOntologyException e) {
			log.error(String.format("error in registered definition for %s", serviceNode), e);
		}
		log.debug(String.format("getServiceBEan--> %s", serviceNode));
		for (StmtIterator i = serviceNode.listProperties(SADI.decoratesWith); i.hasNext(); ) {
			log.debug(String.format("getServiceBEan--> %s", serviceNode));
			Statement statement = i.nextStatement();
			log.debug(String.format("getServiceBEan--> %s", serviceNode));
			try {
				log.debug(String.format("getServiceBEan--> %s", serviceNode));
				Resource restrictionNode = statement.getResource();
				log.debug(String.format("getServiceBEan--> %s", restrictionNode));
				log.debug(OWL.onProperty.getClass());
				Resource onProperty = restrictionNode.getRequiredProperty(OWL.onProperty).getResource();
				log.debug(String.format("getServiceBEan--> %s", serviceNode));
				String onPropertyURI = onProperty.getURI();
				log.debug(String.format("getServiceBEan--> %s", serviceNode));
				String onPropertyLabel = null;
				log.debug(String.format("getServiceBEan--> %s", serviceNode));
				StringBuffer buf = new StringBuffer();
				log.debug(String.format("getServiceBEan--> %s", serviceNode));
				for (Iterator<Statement> j = onProperty.listProperties(RDFS.label); j.hasNext(); ) {
					log.debug(String.format("getServiceBEan--> %s", serviceNode));
					buf.append(j.next().getLiteral().getLexicalForm());
					if (j.hasNext())
						buf.append(" / ");
				}
				log.debug(String.format("getServiceBEan--> %s", serviceNode));
				if (buf.length() > 0)
					onPropertyLabel = buf.toString();
				if (restrictionNode.hasProperty(OWL.someValuesFrom)) {
					log.debug(String.format("getServiceBEan--> %s", serviceNode));
					for (Iterator<Statement> j = restrictionNode.listProperties(OWL.someValuesFrom); j.hasNext(); ) {
						log.debug(String.format("getServiceBEan--> %s", serviceNode));
						Resource valuesFromNode = j.next().getResource();
						RestrictionBean restriction = new RestrictionBean();
						restriction.setOnPropertyURI(onPropertyURI);
						restriction.setOnPropertyLabel(onPropertyLabel);
						restriction.setValuesFromURI(valuesFromNode.getURI());
						buf.setLength(0);
						log.debug(String.format("getServiceBEan--> %s", serviceNode));
						for (Iterator<Statement> k = valuesFromNode.listProperties(RDFS.label); k.hasNext(); ) {
							buf.append(k.next().getLiteral().getLexicalForm());
							if (k.hasNext())
								buf.append(" / ");
							log.debug(String.format("getServiceBEan--> %s", serviceNode));
						}
						if (buf.length() > 0)
							restriction.setValuesFromLabel(buf.toString());
						else
							restriction.setValuesFromLabel(null);
						service.getRestrictionBeans().add(restriction);
						log.debug(String.format("getServiceBEan--> %s", serviceNode));
					}
				} else {
					log.debug(String.format("getServiceBEan--> %s", serviceNode));
					RestrictionBean restriction = new RestrictionBean();
					restriction.setOnPropertyURI(onPropertyURI);
					restriction.setOnPropertyLabel(onPropertyLabel);
					restriction.setValuesFromURI(null);
					restriction.setValuesFromLabel(null);
					service.getRestrictionBeans().add(restriction);
					log.debug(String.format("getServiceBEan--> %s", serviceNode));
				}
			} catch (Exception e) {
				log.error(String.format("bad restriction attached to %s", serviceNode), e);
			}
			
		}
		return service;
	}
	
	/**
	 * Registers the service at the specified URL as a SADI service that
	 * consumes and produces RDF.  If the service is already present in
	 * the registry, the old registration data is deleted before the new
	 * data is stored.
	 * @param serviceUrl the SADI service URL:
	 *   a GET on this URL should produce an RDF description of the service,
	 *   a POST of RDF data to this URL calls the service
	 * @throws SADIException 
	 */
	public ServiceBean registerService(String serviceUrl) throws SADIException
	{	System.out.println("###INIT RegisterService###");
		if (getModel().containsResource(getModel().getResource(serviceUrl))) {
			log.debug(String.format("unregistering service %s", serviceUrl));
			unregisterService(serviceUrl);
		}
		
		/* fetch the service definition and cache in our model so it can be
		 * queried...
		 */
		log.debug(String.format("#fetching service definition from %s", serviceUrl));
		Service service = ServiceFactory.createService(serviceUrl);
		
		return registerService(service);
	}
	
	public ServiceBean registerService(Service service) throws SADIException
	{ 
		Model model = ModelFactory.createDefaultModel();		
		model.add(((ServiceImpl)service).getServiceModel());
		/* attach SADI type only after the service definition has been
		 * successfully fetched...
		 */
		Resource serviceNode = model.createResource(service.getURI(), SADI.Service);
		attachMetaData(serviceNode, service);

		getModel().add(model);
		System.out.println("###MODELO FINAL REGISTRADO###");
		getModel().write(System.out,"RDF/XML");
		return getServiceBean(serviceNode);
	}
	
	private void attachMetaData(Resource serviceNode, Service service) throws SADIException
	{
		for (Restriction restriction: service.getRestrictions()) {
			/* TODO skip restrictions that will become duplicates...
			 */
			attachRestriction(serviceNode, restriction);
		}
		Resource reg = serviceNode.getModel().createResource();
		serviceNode.addProperty(SADI.registration, reg);
		reg.addLiteral(DC.date, Calendar.getInstance().getTime().toString());
	}
	
	/* TODO in some cases, we're ending up with multiple copies of what is
	 * effectively the same restriction; filter these somehow...
	 */
	private void attachRestriction(Resource serviceNode, Restriction restriction)
	{
		Model model = ModelFactory.createDefaultModel();		
		Resource restrictionNode = model.createResource();		
		OntResource p;
		try {		
			p = restriction.getOnProperty();
		} catch (ConversionException e) {
			/* the property is not actually defined in the ontology, nor was
			 * it resolved or created by OwlUtils.decompose (according to the
			 * configured policy), so just do what we can...
			 */
			System.out.println("HUBO CATCH getOnProperty"+e);			
			p = restriction.getOntModel().getOntResource(restriction.getProperty(OWL.onProperty).getResource());			
		}		
		restrictionNode.addProperty(OWL.onProperty, p);
		serviceNode.addProperty(SADI.decoratesWith, restrictionNode);		
		model.add(p, RDFS.label, LabelUtils.getLabel(p));		
		getModel().add(model);
		OntResource valuesFrom = OwlUtils.getValuesFrom(restriction);
		if (valuesFrom != null)
			attachRestrictionValuesFrom(restrictionNode, valuesFrom);
	}
	
	/* The point of storing the restricted valuesFrom of attached properties
	 * in the registry is that they can be queried without reasoning.  It is
	 * therefore not useful to store anything anonymous.  Instead, we store
	 * the first named superclass, but it might be better to store all named
	 * superclasses (see TODO below...)
	 * Also, we split up union classes into their components so they
	 * can be individually queried.  This feels like a slight abuse of
	 * owl:someValuesFrom, but there's no actual cardinality restriction on
	 * that property, so I'm okay with it.
	 */
	private void attachRestrictionValuesFrom(Resource restrictionNode, OntResource valuesFrom)
	{
		if (valuesFrom.isClass()) {
			OntClass valuesFromClass = valuesFrom.asClass();
			if (valuesFromClass.isUnionClass()) {
				for (Iterator<? extends OntClass> i = valuesFromClass.asUnionClass().listOperands(); i.hasNext(); ) {
					attachRestrictionValuesFrom(restrictionNode, i.next());
				}
			}
			/* TODO should we do this? can we usefully encapsulate reasoning
			 * in the registry?
			 */
//			for (Iterator<? extends OntClass> i = valuesFromClass.listSuperClasses(); i.hasNext(); ) {
//				attachRestrictionValuesFrom(restrictionNode, i.next());
//			}
//			for (Iterator<? extends OntClass> i = valuesFromClass.listEquivalentClasses(); i.hasNext(); ) {
//				attachRestrictionValuesFrom(restrictionNode, i.next());
//			}
		}
		
		if (valuesFrom.isURIResource()) {
			restrictionNode.addProperty(OWL.someValuesFrom, valuesFrom);
			getModel().add(valuesFrom, RDFS.label, LabelUtils.getLabel(valuesFrom));
		} else if (valuesFrom.isDataRange()) {
//			DataRange range = valuesFrom.asDataRange();
			restrictionNode.addProperty(OWL.someValuesFrom, "anonymous data range");
		} else if (valuesFrom.isClass()) {
			OntClass firstNamedSuperClass = OwlUtils.getFirstNamedSuperClass(valuesFrom.asClass());
			restrictionNode.addProperty(OWL.someValuesFrom, firstNamedSuperClass);
			getModel().add(valuesFrom, RDFS.label, LabelUtils.getLabel(firstNamedSuperClass));
		}
	}
	
	/**
	 * Unregister the service at the specified URL.
	 * @param serviceUrl the SADI service URL
	 */
	public void unregisterService(String serviceUrl)
	{	System.out.println("###EL SERVICIO NO EXISTE SE VA A CREAR AHORA.");
		Resource service = getModel().getResource(serviceUrl);
		if (service != null) {
			Model serviceModel = ResourceUtils.reachableClosure(service);
			maybeBackupServiceModel(serviceUrl, serviceModel);
			getModel().remove(serviceModel);
		} else {
			log.warn("attempt to unregister non-registered service " + serviceUrl);
		}
	}
	
	private void maybeBackupServiceModel(String serviceUrl, Model serviceModel)
	{     System.out.println("###MaybeBack3");
		Configuration config = getConfig();
		String backupPath = config.getString("backupDirectory");
		if (backupPath != null) {
			File backupDirectory = new File(backupPath);
			if ( backupDirectory.isDirectory() && backupDirectory.canWrite() ) {
				String modelName = String.format("%s.rdf", serviceUrl);
				log.trace(String.format("backing up service defintion to %s", modelName));
				try {
					modelName = new URLCodec().encode(modelName);
				} catch (EncoderException e) {
					log.error( String.format("error encoding URL %s: %s", modelName, e) );
				}
				File file;
				while ( (file = new File(backupDirectory, modelName)).exists() ) {
					modelName = modelName + "~";
				}
				try {
					serviceModel.getWriter("RDF/XML-ABBREV").write(serviceModel, new FileOutputStream(file), "");
				} catch (Exception e) {
					log.error(String.format("error writing backup service model to %s", file));
				}
			} else {
				log.error(String.format("specified backup path %s is not a writeable directory", backupDirectory));
				return;
			}
		}
	}

	/**
	 * Execute a SPARQL query on the registry.
	 * Note that only SELECT queries are supported.
	 * @param sparql the SPARQL query
	 * @return a Jena ResultSet
	 * @throws SADIException
	 */
	public ResultSet doSPARQL(String query) throws SADIException
	{
		Query q = QueryFactory.create(query);
		if (q.isSelectType())
			return QueryExecutionFactory.create(q, getModel()).execSelect();
		else
			throw new SADIException("only SELECT queries are supported");
	}
}
