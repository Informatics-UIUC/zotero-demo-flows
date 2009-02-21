package org.meandre.components.rdf.zotero;

import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.commons.collections.KeyValue;
import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.Component.Mode;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextException;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.ComponentExecutionException;
import org.meandre.core.ExecutableComponent;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

//-------------------------------------------------------------------------
@Component(
		baseURL = "meandre://seasr.org/components/zotero/",
		creator = "Xavier Llor&agrave",
		description = "Extract the urls for each of the entry of a Zotero RDF",
		name = "URLs extractor", tags = "zotero, authors, information extraction",
		mode = Mode.compute, firingPolicy = Component.FiringPolicy.all
)
//-------------------------------------------------------------------------

/**
 * This class extracts the list of authors per entry from a Zotero RDF
 * For each zotero item, we have an output for the url, title, and a flag
 * indicating whether this is the last item or not.
 *
 * @author Xavier Llor&agrave;
 * @author Loretta Auvil, modified
 */
public class URLsExtractor implements ExecutableComponent {

	private static final String HTTP_WWW_GUTENBERG_ORG_FILES = "http://www.gutenberg.org/files/";

	private static final String HTTP_WWW_GUTENBERG_ORG_ETEXT = "http://www.gutenberg.org/etext/";

	// -------------------------------------------------------------------------

	@ComponentInput(
			description = "A map object containing the key elements on the request and the assiciated values",
			name = "value_map"
	)
	public final static String INPUT_VALUEMAP = "value_map";

	@ComponentOutput(
			description = "A URL for the current item.",
			name = "item_url"
	)
	public final static String OUTPUT_ITEM_URL = "item_url";

	@ComponentOutput(
			description = "Boolean value for whether the current item passed is the last item.",
			name = "last_item"
	)
	public final static String OUTPUT_LAST_ITEM = "last_item";

	@ComponentOutput(
			description = "Title for the current item.",
			name = "item_title"
	)
	public final static String OUTPUT_ITEM_TITLE = "item_title";
	
	@ComponentOutput(
			description = "No data to dipslay.",
			name = "no_data"
	)
	public final static String OUTPUT_NO_DATA = "no_data";
	// -------------------------------------------------------------------------

	private PrintStream console;
	private ComponentContext ccHandle;

	public void initialize(ComponentContextProperties ccp)
	throws ComponentExecutionException, ComponentContextException {
		console = ccp.getOutputConsole();
	}

	public void dispose(ComponentContextProperties ccp)
	throws ComponentExecutionException, ComponentContextException {}

	@SuppressWarnings("unchecked")
	public void execute(ComponentContext cc)
	throws ComponentExecutionException, ComponentContextException {

		ccHandle = cc;

		Map<String,byte[]> map = (Map<String, byte[]>) cc.getDataComponentFromInput(INPUT_VALUEMAP);
		for ( String sKey:map.keySet() ) {
			Map<String, String> mapURLs = null;
			int itemCount = 0;
			try {
				ByteArrayInputStream bais = new ByteArrayInputStream(map.get(sKey));
				Model model = ModelFactory.createDefaultModel();
				model.read(bais, "meandre://specialUri");
				mapURLs = pullURLs(model);
				itemCount = mapURLs.size();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				ccHandle.pushDataComponentToOutput(OUTPUT_NO_DATA, "Error in data format. "+e1.getMessage());
				return;
			}

			if (itemCount == 0){
				ccHandle.pushDataComponentToOutput(OUTPUT_NO_DATA, "Your items contained no url information. Check to see that the URL element has a valid url.");
				return;
			}
			for (Entry<String, String> item : mapURLs.entrySet()) {
			    String sURI = item.getKey();
			    String sTitle = item.getValue();
			    console.println("{ uri= " + sURI + " } { title= " + sTitle + " }");

	            try {
	                ccHandle.pushDataComponentToOutput(OUTPUT_ITEM_URL, sURI);
	                ccHandle.pushDataComponentToOutput(OUTPUT_ITEM_TITLE, sTitle);

	                if (itemCount-- > 1)
	                    ccHandle.pushDataComponentToOutput(OUTPUT_LAST_ITEM, "false");
	                else
	                    ccHandle.pushDataComponentToOutput(OUTPUT_LAST_ITEM, "true");
	            } catch (ComponentContextException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
	            }
			}
		}
	}

	private Map<String, String> pullURLs(Model model) {
		// Query to extract the item type, uri and title from the zotero rdf
		final String QUERY_TYPE_URI_TITLE = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
			+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
			+ "PREFIX bib: <http://purl.org/net/biblio#>\n"
			+ "PREFIX dcterms:  <http://purl.org/dc/terms/>\n"
			+ "PREFIX z:       <http://www.zotero.org/namespaces/export#> \n"
			+ "SELECT ?type ?uri ?title ?a ?n \n"
			+ "WHERE { "
			+ "      ?n rdf:value ?uri . "
			+ "      ?n rdf:type dcterms:URI . "
			+ "      ?a z:itemType ?type . "
			+ "      ?a dc:title ?title . "
			+ "      ?a dc:identifier ?n . "
			+ "} order by ?type ?uri ?title ?a ?n ";

		Query query = QueryFactory.create(QUERY_TYPE_URI_TITLE) ;
		QueryExecution exec = QueryExecutionFactory.create(query, model, null);//qsmBindings);
		ResultSet results = exec.execSelect();

		Map<String, String> mapURLs = new HashMap<String, String>();

		while ( results.hasNext() ) {
			QuerySolution resProps = results.nextSolution();
			String typeValue = resProps.getLiteral("type").toString();

			if (typeValue.equalsIgnoreCase("attachment")){
//				System.out.println("skipping ... { type= attachment } { uri= " +
//						resProps.getLiteral("uri").toString() + " } { title= " +
//						resProps.getLiteral("title").toString() + " }"
//				);
				continue;
			}

			String sURI = resProps.getLiteral("uri").toString();
			String sTitle = resProps.getLiteral("title").toString();
			sURI = processURL(sURI);
			mapURLs.put(sURI, sTitle);
		}

		return mapURLs;
	}

	private String processURL(String sUrl) {
		if ( sUrl.startsWith(HTTP_WWW_GUTENBERG_ORG_ETEXT) ) {
			// URL adjustment for Gutenberg items
			String sTmp = sUrl.substring(HTTP_WWW_GUTENBERG_ORG_ETEXT.length());
			sUrl = HTTP_WWW_GUTENBERG_ORG_FILES+sTmp+"/"+sTmp+".txt";
		}
		return sUrl;
	}
}
