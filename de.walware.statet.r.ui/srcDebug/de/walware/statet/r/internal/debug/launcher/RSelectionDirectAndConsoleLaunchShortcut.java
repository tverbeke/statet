/*******************************************************************************
 * Copyright (c) 2006 StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.internal.debug.launcher;


/**
 * Launch shortcut, which submits the current line/selection directly to R
 * and goes to the console.
 * 
 * Supports only text editors.
 */
public class RSelectionDirectAndConsoleLaunchShortcut extends
		RSelectionDirectLaunchShortcut {
	
	
	public RSelectionDirectAndConsoleLaunchShortcut() {
		
		fGotoConsole = true;
	}
}