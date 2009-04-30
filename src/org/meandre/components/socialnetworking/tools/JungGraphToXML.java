/**
 * 
 */
package org.meandre.components.socialnetworking.tools;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.Component.Mode;
import org.meandre.components.socialnetworking.AuthorDegreeDistributionAnalysis;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextException;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.ComponentExecutionException;
import org.meandre.core.ExecutableComponent;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;

/** Generate a graph ML out of a Jung graph
 * 
 * @author Xavier Llor&agrave;
 *
 */

//------------------------------------------------------------------------- 
@Component(
		baseURL = "meandre://seasr.org/components/zotero/", 
		creator = "Xavier Llor&agrave", 
		description = "Given a Jung graph generates the Graph ML that describes it.", 
		name = "Author Hits Analysis", tags = "zotero, authors, social network analysis", 
		mode = Mode.compute, firingPolicy = Component.FiringPolicy.all
)
//-------------------------------------------------------------------------

public class JungGraphToXML implements ExecutableComponent {


	// -------------------------------------------------------------------------

	@ComponentInput(
			description = "The graph generated.", 
			name = "graph"
	)
	public final static String INPUT_GRAPH = "graph";
	

	@ComponentOutput(
			description = "The graphml generated.", 
			name = "graphml"
	)
	public final static String OUTPUT_GRAPH = "graphml";
	
	// -------------------------------------------------------------------------

	/* (non-Javadoc)
	 * @see org.meandre.core.ExecutableComponent#initialize(org.meandre.core.ComponentContextProperties)
	 */
	public void initialize(ComponentContextProperties arg0)
			throws ComponentExecutionException, ComponentContextException {
		// TODO Auto-generated method stub

	}
	
	/* (non-Javadoc)
	 * @see org.meandre.core.ExecutableComponent#dispose(org.meandre.core.ComponentContextProperties)
	 */
	public void dispose(ComponentContextProperties arg0)
			throws ComponentExecutionException, ComponentContextException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.meandre.core.ExecutableComponent#execute(org.meandre.core.ComponentContext)
	 */
	public void execute(ComponentContext cc)
			throws ComponentExecutionException, ComponentContextException {
		
		Graph g = (Graph) cc.getDataComponentFromInput(INPUT_GRAPH);
		StringBuffer sb = new StringBuffer();
		
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" \n"+  
		          "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"+
		          "         xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">\n");
		
		sb.append("\t<graph id=\"G\" edgedefault=\"directed\" parse.order=\"nodesfirst\">\n");
		sb.append("\t\t<key id=\"site\" for=\"node\" attr.name=\"label\" attr.type=\"string\">unknown</key>\n");
		 
		for ( Object obj:g.getVertices() ) {
			Vertex v = (Vertex)obj;
			sb.append("\t\t<node id=\"n"+v.hashCode()+"\">\n");
			sb.append("\t\t\t<data key=\"label\">"+v.getUserDatum(AuthorDegreeDistributionAnalysis.AUTHOR)+"</data>\n");
			sb.append("\t\t</node>\n");
		}

		for ( Object obj:g.getEdges() ) {
			Pair pe = ((Edge)obj).getEndpoints();
			sb.append("\t\t<edge id=\"e"+obj.hashCode()+"\" source=\"n"+pe.getFirst().hashCode()+"\" target=\"n"+pe.getSecond().hashCode()+"\" />\n");
			
		}
		 
		sb.append("\t</graph>\n");
		sb.append("</graphml>");
		
		cc.pushDataComponentToOutput(OUTPUT_GRAPH, sb.toString());
	}


}
