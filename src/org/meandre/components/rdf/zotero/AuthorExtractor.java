package org.meandre.components.rdf.zotero;

import java.util.Map;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.Component.Mode;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextException;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.ComponentExecutionException;
import org.meandre.core.ExecutableComponent;


//------------------------------------------------------------------------- 
@Component(
		baseURL = "meandre://seasr.org/components/zotero/", 
		creator = "Xavier Llor&agrave", 
		description = "Extract the authors for each entry of a Zotero RDF", 
		name = "Author extractor", tags = "zotero, authors, information extraction", 
		mode = Mode.compute, firingPolicy = Component.FiringPolicy.all
)
//-------------------------------------------------------------------------

/**
*  This class extracts the list of authors per entry from a Zotero RDF
* 
* @author Xavier Lor&agrave;
*/
public class AuthorExtractor implements ExecutableComponent {



	// -------------------------------------------------------------------------

	@ComponentInput(
			description = "A map object containing the key elements on the request and the assiciated values", 
			name = "value_map"
	)
	public final static String INPUT_VALUEMAP = "value_map";
	
	@ComponentOutput(
			description = "A list of vectors containing the names of the authors. There is one vector for" +
					      "Zotero entry", 
			name = "list_authors"
	)
	public final static String OUTPUT_LIST_AUTHORS = "list_authors";

	// -------------------------------------------------------------------------


	public void initialize(ComponentContextProperties ccp)
	throws ComponentExecutionException, ComponentContextException {}
	
	public void dispose(ComponentContextProperties ccp)
	throws ComponentExecutionException, ComponentContextException {}

	@SuppressWarnings("unchecked")
	public void execute(ComponentContext cc)
	throws ComponentExecutionException, ComponentContextException {
		
		Map<String,byte[]> map = (Map<String, byte[]>) cc.getDataComponentFromInput(INPUT_VALUEMAP);
		
		cc.pushDataComponentToOutput(OUTPUT_LIST_AUTHORS, "Keys: "+map.keySet().toString());
		
	}


}
