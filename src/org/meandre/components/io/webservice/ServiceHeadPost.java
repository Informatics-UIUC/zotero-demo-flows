package org.meandre.components.io.webservice;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.Component.Mode;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextException;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.ComponentExecutionException;
import org.meandre.core.ExecutableComponent;
import org.meandre.webui.WebUIException;
import org.meandre.webui.WebUIFragmentCallback;

// ------------------------------------------------------------------------- 
@Component(
		baseURL = "meandre://seasr.org/components/zotero/", 
		creator = "Xavier Llor&agrave", 
		description = "Service head for a service that gets data via posts", 
		name = "Service head post", tags = "WebUI, post, process request", 
		mode = Mode.webui, firingPolicy = Component.FiringPolicy.all
)
// -------------------------------------------------------------------------

/**
 *  This class implements a component that using the WebUI accepts post requests
 * 
 * @author Xavier Lor&agrave;
 */
public class ServiceHeadPost implements ExecutableComponent,
		WebUIFragmentCallback {

	// -------------------------------------------------------------------------

	public final static String OUTPUT_VALUEMAP = "value_map";
	@ComponentOutput(
			description = "A map object containing the key elements on the request and the assiciated values", 
			name = OUTPUT_VALUEMAP
	)
	
	public final static String OUTPUT_RESPONSE = "response";
	@ComponentOutput(
			description = "The response to be sent to the Service Tail Post.", 
			name = OUTPUT_RESPONSE
	)
	
	public final static String OUTPUT_SEMAPHORE = "semaphore";
	@ComponentOutput(
			description = "The semaphore to signal the response was sent.", 
			name = OUTPUT_SEMAPHORE
	)

	// -------------------------------------------------------------------------

	private PrintStream console;
	private ComponentContext ccHandle;

	// -------------------------------------------------------------------------

	public void initialize(ComponentContextProperties ccp) {
		console = ccp.getOutputConsole();
		console.println("Initializing service head for " + ccp.getFlowID());
	}

	public void dispose(ComponentContextProperties ccp) {
		console.println("Disposing service head for " + ccp.getFlowID());
	}

	// -------------------------------------------------------------------------

	public void execute(ComponentContext cc)
			throws ComponentExecutionException, ComponentContextException {
		try {
			this.ccHandle = cc;
			cc.startWebUIFragment(this);
			console.println("Starting service head for " + cc.getFlowID());
			while (!cc.isFlowAborting()) {
				Thread.sleep(1000);
			}
			console.println("Abort for service head on " + cc.getFlowID()
					+ " requested");
			cc.stopWebUIFragment(this);
		} catch (Exception e) {
			throw new ComponentExecutionException(e);
		}
	}

	// -------------------------------------------------------------------------

	public void emptyRequest(HttpServletResponse response)
			throws WebUIException {
		try {
			console.println("[WARNING] Empty request recieved");
			response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED);
		} catch (IOException e) {
			throw new WebUIException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public void handle(HttpServletRequest request, HttpServletResponse response)
	throws WebUIException {
		console.println("[INFO] Request recieved from " + request.getRemoteHost()
				+ "/" + request.getRemoteAddr() + ":" + request.getRemotePort()
				+ "[" + request.getRemoteUser() + "]");
		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
		List lstItems;
		try {
			lstItems = upload.parseRequest(request);
			Iterator<FileItem> itr = lstItems.iterator();
			Map<String,byte[]> map = new Hashtable<String,byte[]>();
			while(itr.hasNext()) {
				FileItem item = itr.next();
				map.put(item.getFieldName(),item.get());
				
			}
			Semaphore sem = new Semaphore(1, true);
			sem.acquire();
			ccHandle.pushDataComponentToOutput(OUTPUT_VALUEMAP, map);
			ccHandle.pushDataComponentToOutput(OUTPUT_RESPONSE, response);
			ccHandle.pushDataComponentToOutput(OUTPUT_SEMAPHORE, sem);
			sem.acquire();
			sem.release();
		} catch (FileUploadException e) {
			throw new WebUIException(e);
		} catch (InterruptedException e) {
			throw new WebUIException(e);
		} catch (ComponentContextException e) {
			throw new WebUIException(e);
		}		
	}
}