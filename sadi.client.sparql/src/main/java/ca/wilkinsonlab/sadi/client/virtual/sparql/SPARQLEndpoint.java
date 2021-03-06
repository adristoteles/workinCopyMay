package ca.wilkinsonlab.sadi.client.virtual.sparql;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.AccessException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;

import org.sadiframework.utils.FileUtils;
import org.sadiframework.utils.JsonUtils;
import org.sadiframework.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLResultsXMLUtils;
import org.sadiframework.utils.SPARQLStringUtils;
import org.sadiframework.utils.http.HttpClient;
import org.sadiframework.utils.http.HttpUtils;
import org.sadiframework.utils.http.HttpUtils.HttpStatusException;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.StmtIterator;

/**
 * <p>Encapsulates access to a SPARQL endpoint (via HTTP).</p>
 *
 * <p>This is a generic base class which assumes that:
 *
 * => the GET/POST parameter for a SPARQL query is called "query"
 * => the default results format for SELECT queries is SPARQL Results XML
 * => the default results format for CONSTRUCT queries is RDF/XML
 *
 * At the current time (May 29, 2010), these assumptions hold true for both
 * D2R and Virtuoso SPARQL endpoints, and there are no other types
 * of endpoints that we use.
 *
 * @author Ben Vandervalk
 */
public class SPARQLEndpoint
{
	public enum EndpointType { VIRTUOSO, D2R, UNKNOWN };
	public enum SelectQueryResultsFormat  { SPARQL_RESULTS_XML, JSON };
	public enum ConstructQueryResultsFormat { RDFXML, N3 };

	/** use a persistent HTTP client, in order to support efficient HTTP authentication. */
	protected HttpClient httpClient;

	public final static Logger log = Logger.getLogger(SPARQLEndpoint.class);

	String endpointURI;

	protected SelectQueryResultsFormat selectResultsFormat;
	protected ConstructQueryResultsFormat constructResultsFormat;
	protected EndpointType endpointType;
	protected long resultsLimit;

	protected final static String RESULTS_LIMIT_KEY = "sadi.sparqlResultsLimit";
	public final static long NO_RESULTS_LIMIT = -1;

	protected boolean writable;

	public SPARQLEndpoint(String uri)
	{
		this(uri, EndpointType.UNKNOWN, SelectQueryResultsFormat.SPARQL_RESULTS_XML, ConstructQueryResultsFormat.RDFXML);
	}

	public SPARQLEndpoint(String uri, EndpointType type) {
		this(uri, type, SelectQueryResultsFormat.SPARQL_RESULTS_XML, ConstructQueryResultsFormat.RDFXML);
	}

	public SPARQLEndpoint(String uri, EndpointType type, SelectQueryResultsFormat selectFormat,  ConstructQueryResultsFormat constructFormat)
	{
		endpointURI = uri;
		setEndpointType(type);
		setSelectResultsFormat(selectFormat);
		setConstructResultsFormat(constructFormat);
		setWritable(false);
		setResultsLimit(NO_RESULTS_LIMIT);
		httpClient = new HttpClient();
	}

	public int hashCode() {
		return getURI().hashCode();
	}

	public boolean equals(Object o) {
		if(o instanceof SPARQLEndpoint) {
			return getURI().equals(((SPARQLEndpoint) o).getURI());
		}
		return false;
	}

	protected SelectQueryResultsFormat getSelectResultsFormat()	{
		return selectResultsFormat;
	}
	protected void setSelectResultsFormat(SelectQueryResultsFormat selectResultsFormat) {
		this.selectResultsFormat = selectResultsFormat;
	}

	protected ConstructQueryResultsFormat getConstructResultsFormat() {
		return constructResultsFormat;
	}
	protected void setConstructResultsFormat(ConstructQueryResultsFormat constructResultsFormat) {
		this.constructResultsFormat = constructResultsFormat;
	}

	public String getURI()  {
		return endpointURI;
	}

	public String toString() {
		return getURI();
	}

	public EndpointType getEndpointType() {
		return endpointType;
	}

	public void setEndpointType(EndpointType type) {
		this.endpointType = type;
	}

	public boolean isWritable() {
		return writable;
	}

	protected void setWritable(boolean writable) {
		this.writable = writable;
	}

	public long getResultsLimit() {
		return resultsLimit;
	}

	public void setResultsLimit(long resultsLimit) {
		this.resultsLimit = resultsLimit;
	}

	public boolean ping()
	{
		try {
			selectQuery("SELECT * WHERE { ?s ?p ?o } LIMIT 1");
			return true;
		} catch(IOException e) {
			return false;
		}
	}


	public Model getTriplesMatchingPattern(Triple pattern, long resultsLimit) throws IOException
	{
		Node s = pattern.getSubject();
		Node o = pattern.getObject();

		if(s.isVariable() && o.isVariable())
			throw new IllegalArgumentException("Not allowed to query {?s ?p ?o} or {?s <predicate> ?o} against a SPARQL endpoint");

		String patternStr = SPARQLStringUtils.getTriplePattern(pattern);
		StringBuilder query = new StringBuilder();
		query.append("CONSTRUCT { ");
		query.append(patternStr);
		query.append(" } WHERE { ");
		query.append(patternStr);
		query.append(" }");

		if(resultsLimit != NO_RESULTS_LIMIT) {
			query.append(" LIMIT ");
			query.append(resultsLimit);
		}

		Model triples = constructQuery(query.toString(), ModelFactory.createDefaultModel());

		if(resultsLimit != NO_RESULTS_LIMIT && triples.size() == resultsLimit)
			log.warn("query results may have been truncated at " + resultsLimit + " triples");

		return triples;
	}

	public List<Map<String,String>> selectQuery(String query) throws IOException
	{
		/* We must use syntaxARQ here so that COUNT(*) queries are allowed. */

		Query jenaQuery = QueryFactory.create(query, Syntax.syntaxARQ);
		long queryLimit = (jenaQuery.getLimit() == Query.NOLIMIT) ? NO_RESULTS_LIMIT : jenaQuery.getLimit();

		/*
		 * If the result set for the query is larger than the endpoint
		 * results limit, issue multiple queries to get the full
		 * result set.
		 */

		String origQuery = query;
		List<Map<String, String>> aggregateResults = new ArrayList<Map<String, String>>();

		while(true) {

			InputStream is = null;

			try {

				// We should use GET here so that the HTTP client knows that it is okay to retry on failure
				HttpResponse response = httpClient.GET(new URL(getURI()), getParamsForSelectQuery(query));

				int statusCode = response.getStatusLine().getStatusCode();
				if (HttpUtils.isHttpError(statusCode))
					throw new HttpStatusException(statusCode);

				is = response.getEntity().getContent();
				List<Map<String,String>> results = convertSelectResponseToBindings(is);

				aggregateResults.addAll(results);

				/*
				 * If we get a result set size that is exactly the size of the
				 * endpoint results limit, it probably means that the results
				 * were truncated.  We can't be 100% sure, but we must issue another
				 * query to be safe.
				 */

				if(aggregateResults.size() == queryLimit || getResultsLimit() == NO_RESULTS_LIMIT || results.size() < getResultsLimit()) {
					break;
				}

				query = getQueryForNextChunk(origQuery, aggregateResults.size());
				log.trace(String.format("query results may have been truncated by endpoint limit, issuing additional query: %s", query));

			} catch(IOException e) {

				/*
				 * TODO: If we get at least some results for the query,
				 * we want to return them, and just issue a warning about
				 * the IOException.  In the future, it may be useful for
				 * the client to have a way of determining if only partial
				 * results were retrieved.  We could achieve this by
				 * returning a SPARQLQueryResult object, instead of
				 * List<Map<String,String>>.
				 */

				if(aggregateResults.size() > 0) {
					log.error(String.format("error occurred during query, returning only partial results for: %s", query), e);
					break;
				} else {
					throw e;
				}

			} finally {

				if(is != null) {
					FileUtils.simpleClose(is);
				}

			}

		}

		return aggregateResults;
	}

	public List<Map<String,String>> selectQueryBestEffort(String query) throws IOException
	{
		try {
			return selectQuery(query);

		} catch(HttpStatusException e) {
			if(e.getStatusCode() != HttpURLConnection.HTTP_GATEWAY_TIMEOUT) {
				throw e;
			}
			return getPartialQueryResults(query);
		} catch(IOException e) {
			return getPartialQueryResults(query);
		}
	}

	public Model constructQuery(String query, Model accum) throws IOException
	{
		/* We must use syntaxARQ here so that COUNT(*) queries are allowed. */

		Query jenaQuery = QueryFactory.create(query, Syntax.syntaxARQ);
		long queryLimit = (jenaQuery.getLimit() == Query.NOLIMIT) ? NO_RESULTS_LIMIT : jenaQuery.getLimit();

		String origQuery = query;
//		List<Triple> aggregateResults = new ArrayList<Triple>();
		Model aggregateResults = accum;

		while(true) {

			InputStream is = null;

			try {

				// We must use GET here so that the HTTP client knows that it is okay to retry on failure
				HttpResponse response = httpClient.GET(new URL(getURI()), getParamsForConstructQuery(query));

				int statusCode = response.getStatusLine().getStatusCode();
				if (HttpUtils.isHttpError(statusCode))
					throw new HttpStatusException(statusCode);

				is = response.getEntity().getContent();
				Model results = ModelFactory.createDefaultModel();
				convertConstructResponseToTriples(is, results);

				aggregateResults.add(results);

				/*
				 * If we get a result set size that is exactly the size of the
				 * endpoint results limit, it probably means that the results
				 * were truncated.  We can't be 100% sure, but we must issue another
				 * query to be safe.
				 */

				if(aggregateResults.size() == queryLimit || getResultsLimit() == NO_RESULTS_LIMIT || results.size() < getResultsLimit()) {
					break;
				}

				query = getQueryForNextChunk(origQuery, aggregateResults.size());
				log.trace(String.format("query results may have been truncated by endpoint limit, issuing additional query: %s", query));

				results.close();

			} catch(IOException e) {

				/*
				 * TODO: If we get at least some results for the query,
				 * we want to return them, and just issue a warning about
				 * the IOException.  In the future, it may be useful for
				 * the client to have a way of determining if only partial
				 * results were retrieved.  We could achieve this by
				 * returning a SPARQLQueryResult object, instead of
				 * Collection<Triple>.
				 */

				if(aggregateResults.size() > 0) {
					log.error(String.format("error occurred during query, returning only partial results for: %s", query), e);
					break;
				} else {
					throw e;
				}

			} finally {

				if(is != null) {
					FileUtils.simpleClose(is);
				}

			}

		}

		return aggregateResults;
	}

	protected String getQueryForNextChunk(String origQuery, long numResultsRetrieved)
	{
		Query jenaQuery = QueryFactory.create(origQuery);

		jenaQuery.setOffset(numResultsRetrieved);

		/* We must use syntaxARQ here so that COUNT(*) queries are allowed. */
		long origLimit = QueryFactory.create(origQuery, Syntax.syntaxARQ).getLimit();
		long newLimit = origLimit - numResultsRetrieved;

		if(origLimit == Query.NOLIMIT || (newLimit > getResultsLimit())) {
			jenaQuery.setLimit(Query.NOLIMIT);
		} else {
			jenaQuery.setLimit(newLimit);
		}

		return jenaQuery.serialize();
	}

	protected static String getJenaRDFLangString(ConstructQueryResultsFormat format) {
		switch(format) {
		case N3:
			return "N3";
		case RDFXML:
			return "RDF/XML";
		default:
			throw new RuntimeException("unrecognized value for ConstructQueryResultsFormat");
		}
	}

	/**
	 * Issue a SPARQL update (SPARUL) query.  These queries are using for inserting triples,
	 * updating triples, deleting triples, etc.
	 * @param query
	 * @throws HttpException if an HTTP protocol error occurs (this will probably never happen)
	 * @throws HttpResponseCodeException if a non-success HTTP response code is generated (e.g. 404: File not found)
	 * @throws IOException if there is a communication problem (e.g. request timeout)
	 * @throws AccessException if the client doesn't have permission to perform the update query
	 */
	public void updateQuery(String query) throws IOException
	{
		if(!isWritable()) {
			throw new IOException(String.format("unable to perform update query on %s, endpoint is not writable (check username/password)", getURI()));
		}

		HttpResponse response = httpClient.POST(new URL(getURI()), getParamsForUpdateQuery(query));
		response.getEntity().getContent().close();

		int statusCode = response.getStatusLine().getStatusCode();
		if (HttpUtils.isHttpError(statusCode))
			throw new HttpStatusException(statusCode);
	}

	public Set<String> getNamedGraphs() throws IOException
	{
		log.debug("querying named graphs from " + getURI());
		Set<String> graphURIs = new HashSet<String>();
		List<Map<String,String>> results = selectQuery("SELECT DISTINCT ?g WHERE { GRAPH ?g { ?s ?p ?o } }");
		for(Map<String,String> binding : results)
			graphURIs.add(binding.get("g"));
		return graphURIs;
	}

	public Set<String> getPredicates() throws IOException
	{
		log.debug("querying predicate list from " + getURI());
		Set<String> predicates = new HashSet<String>();
		List<Map<String,String>> results = selectQuery("SELECT DISTINCT ?p WHERE { ?s ?p ?o }");
		for(Map<String,String> binding : results)
			predicates.add(binding.get("p"));
		return predicates;
	}

	public Set<String> getPredicates(String graphURI) throws IOException
	{
		log.debug("querying predicate list from graph " + graphURI + " of " + getURI());
		Set<String> predicates = new HashSet<String>();
		List<Map<String,String>> results = null;
		String query = SPARQLStringUtils.strFromTemplate("SELECT DISTINCT ?p FROM %u% WHERE { ?s ?p ?o }", graphURI);
		results = selectQuery(query);
		for(Map<String,String> binding : results)
			predicates.add(binding.get("p"));
		return predicates;
	}


	public Set<String> getPredicatesPartial() throws IOException
	{
		log.debug("querying partial predicate list from " + getURI());

		Set<String> predicates = new HashSet<String>();
		String query = "SELECT DISTINCT ?p WHERE { ?s ?p ?o }";
		List<Map<String,String>> results = getPartialQueryResults(query);
		for(Map<String,String> binding : results)
			predicates.add(binding.get("p"));
		return predicates;
	}

	/**
	 * Get the largest result set possible for the given query.  This method is a fallback
	 * if we cannot get the complete answer to a query due to HTTP timeouts.
	 */
	public List<Map<String,String>> getPartialQueryResults(String query) throws IOException
	{
		return getPartialQueryResults(query, 1);
	}

	public List<Map<String,String>> getPartialQueryResults(String query, long startSize) throws IOException
	{
		long limit = getResultsCountLowerBound(query, startSize);
		String partialQuery = query + " LIMIT " + limit;
		return selectQuery(partialQuery);
	}

	/**
	 * <p>Get a lower bound for the number of results for the given query.  This method is a fallback
	 * if we cannot do a full COUNT(*) query on the endpoint due to HTTP timeouts.</p>
	 *
	 * <p>We do this by adjusting the limit and seeing where we start to fail (i.e. timeout).  If
	 * the current limit is good, we double it. If the current limit times out, we take the
	 * halfway point between the current limit and the last successful limit.  And so on.</p>
	 *
	 * <p>We don't actually need to download the results each time we try a new limit, because the
	 * majority of the processing time is on the server side (finding and materializing the results).</p>
	 *
	 * @param query
	 * @param startSize Start with the lower bound at this number, and then adjust it by factors of
	 * two to find the real limit.
	 * @return A maximum lower bound for the number of results to the query
	 */
	public long getResultsCountLowerBound(String query, long startSize) throws IOException
	{
		long curPoint = startSize;
		long lastSuccessPoint = 0;
		long lastFailurePoint = -1;
		String curQuery;
		boolean answerIsExact = false;

		while(true) {
			if(curPoint == 0)
				break;

			List<Map<String,String>> results;
			try {
				curQuery = query + " OFFSET " + (curPoint - 1) + " LIMIT 1";
				results = selectQuery(curQuery);
			}
			catch(HttpStatusException e) {
				if(e.getStatusCode() == HttpURLConnection.HTTP_GATEWAY_TIMEOUT) {
					log.debug("query timed out for LIMIT = " + curPoint);

					if(curPoint == lastSuccessPoint) {
						// The limit lastSuccessPoint succeeded last time but not
						// this time. To be a bit safer, subtract 10%
						// from the limit value and call it good.
						curPoint = (curPoint*90)/100;
						break;
					}

					lastFailurePoint = curPoint;
					if(lastSuccessPoint > -1)
						curPoint = lastSuccessPoint + ((curPoint - lastSuccessPoint) / 2);
					else
						curPoint /= 2;
					if(curPoint == 0)
						throw e;
					continue;
				} else {
					throw e;
				}
			}
			if(results.size() == 0) {
				// A successful query with no results means that we have
				// gone beyond the total number of results for the given query.
				// And that means that we could have done the full query in the
				// first place. Nothing we can do about that, so carry on anyhow.
				log.debug("query succeeded for LIMIT = " + curPoint + ", but " + String.valueOf(curPoint) +
						" is greater than the full number of results for the query.");
				answerIsExact = true;
				lastFailurePoint = curPoint;
				curPoint = lastSuccessPoint + ((curPoint - lastSuccessPoint) / 2);
				continue;
			}
			else if(curPoint == lastFailurePoint - 1) {
				// If we can't go any higher, we've found the limit.
				break;
			}
			else if(lastFailurePoint > -1) {
				log.debug("query succeeded for LIMIT = " + curPoint);
				lastSuccessPoint = curPoint;
				curPoint = curPoint + ((lastFailurePoint - curPoint) / 2);
			}
			else {
				log.debug("query succeeded for LIMIT = " + curPoint);
				lastSuccessPoint = curPoint;
				curPoint *= 2;
			}
		}

		if(answerIsExact)
			log.warn("LIMIT testing indicates that it was possible to do the full query.");

		return curPoint;
	}

	public boolean isDatatypeProperty(String predicateURI, boolean checkForAmbiguousProperty) throws IOException, AmbiguousPropertyTypeException
	{
		log.debug("checking if " + predicateURI + " is a datatype property or an object property");

		boolean isDatatypeProperty = false;
		String predicateQuery = "CONSTRUCT { ?s %u% ?o } WHERE { ?s %u% ?o } LIMIT 1";
		predicateQuery = SPARQLStringUtils.strFromTemplate(predicateQuery, predicateURI, predicateURI);
		Model results = constructQuery(predicateQuery, ModelFactory.createDefaultModel());
		if(results.size() == 0) {
			throw new RuntimeException("the endpoint " + getURI() + " doesn't have any triples containing the predicate " + predicateURI);
		}
		else {
			StmtIterator i = results.listStatements();
			RDFNode o = i.next().getObject();
			if(o.isLiteral())
				isDatatypeProperty = true;
			else
				isDatatypeProperty = false;
			i.close();

			// Sanity check: Make sure this is not an RDF predicate that has both URIs and literals as values.
			if(checkForAmbiguousProperty) {
				String query;
				if(isDatatypeProperty)
					query = "SELECT ?o WHERE { ?s %u% ?o . FILTER (!isLiteral(?o)) } LIMIT 1";
				else
					query = "SELECT ?o WHERE { ?s %u% ?o . FILTER isLiteral(?o) } LIMIT 1";

				query = SPARQLStringUtils.strFromTemplate(query, predicateURI);
				if(selectQuery(query).size() > 0)
					throw new AmbiguousPropertyTypeException(predicateURI + " is an RDF predicate which has both URIs and literals as values.");
			}
		}
		return isDatatypeProperty;
	}

	protected Map<String,String> getParamsForUpdateQuery(String query) {
		throw new UnsupportedOperationException();
	}

	protected Model convertConstructResponseToTriples(InputStream response, Model model)
	{
		switch(getConstructResultsFormat()) {
		case N3:
			model.read(response, endpointURI, "N3");
			break;
		default:
		case RDFXML:
			model.read(response, endpointURI, "RDF/XML");
		}
		return model;
	}

	protected List<Map<String,String>> convertSelectResponseToBindings(InputStream response) throws IOException
	{
		switch(getSelectResultsFormat()) {
		case JSON:
			String responseAsString = IOUtils.toString(response);
			return JsonUtils.convertJSONToResults(JsonUtils.read(responseAsString));
		default:
		case SPARQL_RESULTS_XML:
			return SPARQLResultsXMLUtils.getResultsFromSPARQLXML(response);
		}
	}

	protected Map<String,String> getParamsForConstructQuery(String query)
	{
		Map<String,String> params = new HashMap<String,String>();
		params.put("query", query);
		return params;
	}

	protected Map<String,String> getParamsForSelectQuery(String query)
	{
		Map<String,String> params = new HashMap<String,String>();
		params.put("query", query);
		return params;
	}

	public TripleIterator iterator()
	{
		return new TripleIterator(this);
	}

	public TripleIterator iterator(long blockSize)
	{
		return new TripleIterator(this, blockSize);
	}

	/**
	 * This exception is thrown when a predicate has both literal and URIs values, and thus cannot
	 * be typed as a datatype property or an object property.
	 */
	@SuppressWarnings("serial")
	public static class AmbiguousPropertyTypeException extends Exception
	{
		public AmbiguousPropertyTypeException(String message) { super(message); }
	}

	/**
	 * This class iterates over all the triples in a given endpoint.  I couldn't implement the real Iterator
	 * interface here, because I need to be able to throw an IOException from next() and hasNext().
	 */
	public static class TripleIterator
	{
		private final static long  DEFAULT_BLOCK_SIZE = 50000; // triples
		private List<Triple> tripleCache = null;
		private int cacheIndex = 0;
		private int cacheOffset = 0;
		private SPARQLEndpoint endpoint;
		private long blockSize;

		public TripleIterator(SPARQLEndpoint endpoint)
		{
			this(endpoint, DEFAULT_BLOCK_SIZE);
		}

		public TripleIterator(SPARQLEndpoint endpoint, long blockSize)
		{
			this.endpoint = endpoint;
			this.blockSize = blockSize;
		}

		public boolean hasNext() throws IOException
		{
			if(tripleCache == null) {
				return retrieveNextBlock();
			} else if(cacheIndex >= tripleCache.size()) {
				// if current block is not the full size, then it is the last one
				if(tripleCache.size() < blockSize) {
					return false;
				}
				return retrieveNextBlock();
			} else {
				return true;
			}
		}

		public Triple next() throws IOException
		{
			if(!hasNext())
				throw new NoSuchElementException();
			return tripleCache.get(cacheIndex++);
		}

		private boolean retrieveNextBlock() throws IOException
		{
			log.debug("retrieving triples " + cacheOffset + " through " + (cacheOffset + blockSize - 1) + " from " + endpoint.getURI());

			String query = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o } LIMIT " + blockSize + " OFFSET " + cacheOffset;
			tripleCache = new ArrayList<Triple>();
//			tripleCache.addAll(endpoint.constructQuery(query));
			Model model = endpoint.constructQuery(query, ModelFactory.createDefaultModel());
			tripleCache.addAll(RdfUtils.modelToTriples(model));
			cacheOffset += blockSize;
			cacheIndex = 0;
			if(tripleCache.size() > 0)
				return true;
			else
				return false;
		}
	}
}
