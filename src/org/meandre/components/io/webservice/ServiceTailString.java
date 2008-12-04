package org.meandre.components.io.webservice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.concurrent.Semaphore;

import javax.servlet.http.HttpServletResponse;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
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
		description = "Service tail for a web service", 
		name = "Service tail text", tags = "WebUI, post, process request", 
		mode = Mode.compute, firingPolicy = Component.FiringPolicy.all
)
//-------------------------------------------------------------------------

/**
 *  This class implements a component that using the WebUI accepts post requests
 * 
 * @author Xavier Lor&agrave;
 */
public class ServiceTailString implements ExecutableComponent {

	// -------------------------------------------------------------------------

	@ComponentInput(
			description = "A string containing the output to response", 
			name = "string"
	)
	public final static String INPUT_STRING = "string";
	
	
	@ComponentInput(
			description = "The response sent by the Service Head.", 
			name = "response"
	)
	public final static String INPUT_RESPONSE = "response";
	
	
	@ComponentInput(
			description = "The semaphore to signal the response was sent.", 
			name = "semaphore"
	)
	public final static String INPUT_SEMAPHORE = "semaphore";
	
	// -------------------------------------------------------------------------

	public void initialize(ComponentContextProperties ccp)
	throws ComponentExecutionException, ComponentContextException {}
	
	public void dispose(ComponentContextProperties ccp)
	throws ComponentExecutionException, ComponentContextException {}

	public void execute(ComponentContext cc)
			throws ComponentExecutionException, ComponentContextException {

		String sResponse = cc.getDataComponentFromInput(INPUT_STRING).toString();
		Semaphore sem = (Semaphore) cc.getDataComponentFromInput(INPUT_SEMAPHORE);
		HttpServletResponse response = (HttpServletResponse) cc.getDataComponentFromInput(INPUT_RESPONSE);
		PrintStream ccHandle = cc.getOutputConsole();
		
		ccHandle.println("[INFO] Sending requested results");
		try {
			PrintWriter pw = response.getWriter();
			pw.println(sResponse.toString());
			response.getWriter().flush();
			sem.release();
		} catch (IOException e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			e.printStackTrace(ps);
			ccHandle.println(baos.toString());
		}
	}

}