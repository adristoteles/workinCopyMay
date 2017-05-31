package org.sadiframework.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;

import org.sadiframework.SADIException;
import org.sadiframework.utils.http.HttpUtils;
import org.sadiframework.utils.http.HttpUtils.HttpStatusException;

//import org.apache.jena.db.DBConnection;
//import org.apache.jena.db.IDBConnection;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ModelMaker;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.FileManager;

/**
 * @author Luke McCarthy
 */
public class QueryExecutorFactory
{
	public static QueryExecutor createJenaModelQueryExecutor(Model model)
	{
		return new JenaModelQueryExecutor(model);
	}

	public static QueryExecutor createFileModelQueryExecutor(String file)
	{
		return new JenaModelQueryExecutor(createFileModel(file));
	}
/*
	public static QueryExecutor createJDBCJenaModelQueryExecutor(String driver, String dsn, String username, String password)
	{
		return createJDBCJenaModelQueryExecutor(driver, dsn, username, password, null);
	}
	
	public static QueryExecutor createJDBCJenaModelQueryExecutor(String driver, String dsn, String username, String password, String graphName)
	{
		return new JenaModelQueryExecutor(createJDBCJenaModel(driver, dsn, username, password, graphName));
	}
*/
	/**
	 * 
	 * @param endpointURL the URL of the Virtuoso SPARQL endpoint
	 * @return
	 * @throws IOException if the URL is invalid
	 */
	public static QueryExecutor createVirtuosoSPARQLEndpointQueryExecutor(String endpointURL) throws IOException
	{
		return createVirtuosoSPARQLEndpointQueryExecutor(endpointURL, null);
	}
	
	public static QueryExecutor createVirtuosoSPARQLEndpointQueryExecutor(String endpointURL, String graphName) throws IOException
	{
		return new VirtuosoSPARQLEndpointQueryExecutor(new URL(endpointURL), graphName);
	}
	/*
	private static Model createJDBCJenaModel(String driver, String dsn, String username, String password, String graphName)
	{
		// load the driver class
		try {
			Class.forName(driver);
		} catch ( ClassNotFoundException e ) {
			throw new RuntimeException(e);
		}
		
		// create a database connection
		IDBConnection conn = new DBConnection(
				dsn,
				username,
				password,
				driver.matches("(?i).*mysql.*") ? "MySQL" : null
		);
		
		// create a model maker with the given connection parameters
		ModelMaker maker = ModelFactory.createModelRDBMaker(conn);

		// create a default model
		if (graphName == null)
			return maker.createDefaultModel();
		else 
			return maker.createModel(graphName, false);
	}
	*/
	private static Model createFileModel(String path)
	{ System.out.println("init");
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
		System.out.println("¡¡¡¡===INIT createFileModel SADI.CLIENT===!!!1");
		System.out.println(registryFile.getName());
		return model;
	}
	
	private static class JenaModelQueryExecutor implements QueryExecutor
	{
		private Model model;
		
		public JenaModelQueryExecutor(Model model)
		{
			this.model = model;
		}
		
		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.utils.QueryExecutor#executeQuery(java.lang.String)
		 */
		@Override
		public List<Map<String, String>> executeQuery(String query)
		{
			List<Map<String, String>> localBindings = new ArrayList<Map<String, String>>();
			QueryExecution qe = QueryExecutionFactory.create(query, model);
			ResultSet resultSet = qe.execSelect();
			while (resultSet.hasNext()) {
				QuerySolution binding = resultSet.nextSolution();
				Map<String, String> ourBinding = new HashMap<String, String>();
				for (Iterator<String> i = binding.varNames(); i.hasNext(); ) {
					String variable = i.next();
					ourBinding.put(variable, RdfUtils.getPlainString(binding.get(variable).asNode()));
				}
				localBindings.add(ourBinding);
			}
			qe.close();
			return localBindings;
		}

		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.utils.QueryExecutor#executeConstructQuery(java.lang.String)
		 */
		@Override
		public Model executeConstructQuery(String query) throws SADIException
		{
			QueryExecution qe = QueryExecutionFactory.create(query, model);
			Model result = qe.execConstruct();
			qe.close();
			return result;
		}
	}
	
	private static class VirtuosoSPARQLEndpointQueryExecutor implements QueryExecutor
	{
		protected URL endpointURL;
		protected String graphName;
		
		public VirtuosoSPARQLEndpointQueryExecutor(URL endpointURL, String graphName)
		{
			this.endpointURL = endpointURL;
			this.graphName = graphName;
		}
		
		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.utils.QueryExecutor#executeQuery(java.lang.String)
		 */
		@Override
		public List<Map<String, String>> executeQuery(String query) throws SADIException
		{
			try {
				Object result = HttpUtils.postAndFetchJson(endpointURL, getPostParameters(query));
				return convertResults(result);
			} catch (IOException e) {
				throw new SADIException(e.toString());
			}
		}

		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.utils.QueryExecutor#executeConstructQuery(java.lang.String)
		 */
		@Override
		public Model executeConstructQuery(String query) throws SADIException
		{
			Map<String, String> params = getPostParameters(query);
			params.put("format", "application/rdf+xml");
			InputStream is = null;
			try {
				HttpResponse response = HttpUtils.POST(endpointURL, params);
				is = response.getEntity().getContent();
				int statusCode = response.getStatusLine().getStatusCode();
				if (HttpUtils.isHttpError(statusCode)) {
					throw new HttpStatusException(statusCode);
				}
				Model model = ModelFactory.createDefaultModel();
//				model.read(HttpUtils.POST(endpointURL, params), endpointURL.toString());
				model.read(is, endpointURL.toString());
				return model;
			} catch (IOException e) {
				throw new SADIException(e.toString());
			} finally {
				if (is != null)
					FileUtils.simpleClose(is);
			}
		}
		
		public String toString()
		{
			return String.format("%s(%s)", endpointURL, graphName);
		}

		private Map<String, String> getPostParameters(String query)
		{
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("query", query);
			parameters.put("format", "JSON");
			if (graphName != null)
				parameters.put("default-graph-uri", graphName);
			return parameters;
		}
		
		@SuppressWarnings("unchecked")
		public static List<Map<String, String>> convertResults(Object result)
		{
			List<Map<String, Map<?, ?>>> virtuosoBindings = (List<Map<String, Map<?, ?>>>)((Map<?, ?>)((Map<?, ?>)result).get("results")).get("bindings");
			List<Map<String, String>> localBindings = new ArrayList<Map<String, String>>(virtuosoBindings.size());
			for (Map<String, Map<?, ?>> virtuosoBinding: virtuosoBindings) {
				Map<String, String> ourBinding = new HashMap<String, String>();
				for (String variable: virtuosoBinding.keySet()) {
					ourBinding.put(variable, (String)virtuosoBinding.get(variable).get("value"));
				}
				localBindings.add(ourBinding);
			}
			return localBindings;
		}
	}
}
