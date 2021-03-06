/*=============================================================================#
 # Copyright (c) 2009-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.statet.r.internal.core.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import de.walware.ecommons.ltk.core.model.IModelElement.Filter;

import de.walware.statet.r.core.model.IRElement;
import de.walware.statet.r.core.model.IRFrame;
import de.walware.statet.r.core.model.IRLangElement;
import de.walware.statet.r.core.model.RElementName;


public class CompositeFrame implements IRFrame {
	
	
	private final int fFrameType;
	private final RElementName fElementName;
	
	public final Map<String, RUnitElement> fModelElements;
	private final Lock fLock;
	
	
	public CompositeFrame(final ReadWriteLock lock, final String packageName, final String projectName, final Map<String, RUnitElement> elements) {
		fLock = lock.readLock();
		fModelElements = (elements != null) ? elements : new HashMap<String, RUnitElement>();
		
		if (packageName != null) {
			fFrameType = PACKAGE;
			fElementName = RElementName.create(RElementName.MAIN_PACKAGE, packageName);
		}
		else {
			fFrameType = PROJECT;
			fElementName = RElementName.create(RElementName.MAIN_PROJECT, projectName);
		}
	}
	
	
	@Override
	public RElementName getElementName() {
		return fElementName;
	}
	
	@Override
	public String getFrameId() {
		return null;
	}
	
	@Override
	public int getFrameType() {
		return fFrameType;
	}
	
	@Override
	public List<? extends IRElement> getModelElements() {
		fLock.lock();
		try {
			final Collection<RUnitElement> values = fModelElements.values();
			final List<IRElement> list = new ArrayList<IRElement>(values.size());
			list.addAll(values);
			return list;
		}
		finally {
			fLock.unlock();
		}
	}
	
	@Override
	public boolean hasModelChildren(final Filter filter) {
		fLock.lock();
		try {
			if (fModelElements.isEmpty()) {
				return false;
			}
			for (final IRElement element : fModelElements.values()) {
				if (element.hasModelChildren(filter)) {
					return true;
				}
			}
			return false;
		}
		finally {
			fLock.unlock();
		}
	}
	
	@Override
	public List<? extends IRLangElement> getModelChildren(final Filter filter) {
		fLock.lock();
		try {
			if (fModelElements.isEmpty()) {
				return Collections.EMPTY_LIST;
			}
			final ArrayList<IRLangElement> children = new ArrayList<IRLangElement>();
			for (final IRLangElement element : fModelElements.values()) {
				final List<? extends IRLangElement> elementChildren = element.getModelChildren(null);
				if (!elementChildren.isEmpty()) {
					children.ensureCapacity(children.size() + elementChildren.size());
					for (final IRLangElement child : elementChildren) {
						if (filter == null || filter.include(child)) {
							children.add(child);
						}
					}
				}
			}
			return children;
		}
		finally {
			fLock.unlock();
		}
	}
	
	@Override
	public List<? extends IRFrame> getPotentialParents() {
		return Collections.EMPTY_LIST;
	}
	
	
	public RUnitElement setModelElement(final String suId, final RUnitElement element) {
		element.fEnvir = this;
		return fModelElements.put(suId, element);
	}
	
	public RUnitElement removeModelElement(final String suId) {
		return fModelElements.remove(suId);
	}
	
	public void removeModelElements(final String modelTypeId) {
		for (final Iterator<RUnitElement> iter= fModelElements.values().iterator(); iter.hasNext(); ) {
			final RUnitElement unitElement= iter.next();
			if (unitElement.getModelTypeId() == modelTypeId) {
				iter.remove();
			}
		}
	}
	
}
