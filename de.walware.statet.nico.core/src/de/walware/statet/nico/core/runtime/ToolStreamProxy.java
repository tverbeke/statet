/*******************************************************************************
 * Copyright (c) 2005 StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.nico.core.runtime;

import java.io.IOException;

import org.eclipse.debug.core.model.IStreamsProxy;


/**
 * None buffered streams
 */
public class ToolStreamProxy implements IStreamsProxy {

	
	private ToolStreamMonitor fInputMonitor = new ToolStreamMonitor();
	private ToolStreamMonitor fInfoMonitor = new ToolStreamMonitor();
	private ToolStreamMonitor fOutputMonitor = new ToolStreamMonitor();
	private ToolStreamMonitor fErrorMonitor = new ToolStreamMonitor();

	
	public void write(String input) throws IOException {
		
		throw new IOException("Function is not supported.");
	}

	public ToolStreamMonitor getInputStreamMonitor() {
		
		return fInputMonitor;
	}
	
	public ToolStreamMonitor getInfoStreamMonitor() {
		
		return fInfoMonitor;
	}
	
	public ToolStreamMonitor getOutputStreamMonitor() {

		return fOutputMonitor;
	}

	public ToolStreamMonitor getErrorStreamMonitor() {
		
		return fErrorMonitor;
	}
	
	
	/**
	 * 
	 */
	public void dispose() {
		
		fInputMonitor.dispose();
		fInfoMonitor.dispose();
		fOutputMonitor.dispose();
		fErrorMonitor.dispose();
	}

}
