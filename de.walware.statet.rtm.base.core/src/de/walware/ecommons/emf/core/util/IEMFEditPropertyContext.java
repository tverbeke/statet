/*******************************************************************************
 * Copyright (c) 2012-2013 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.emf.core.util;

import org.eclipse.core.databinding.observable.IObservable;


public interface IEMFEditPropertyContext extends IEMFEditContext, IEMFPropertyContext {
	
	
	IObservable getPropertyObservable();
	
}
