/*=============================================================================#
 # Copyright (c) 2008-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.statet.r.core.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.osgi.util.NLS;
import org.eclipse.text.edits.TextEdit;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.IModelManager;
import de.walware.ecommons.ltk.ast.AstSelection;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.core.model.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.core.refactoring.RefactoringAdapter;
import de.walware.ecommons.text.IPartitionConstraint;
import de.walware.ecommons.text.IndentUtil;
import de.walware.ecommons.text.SourceParseInput;
import de.walware.ecommons.text.StringParseInput;

import de.walware.statet.r.core.IRCoreAccess;
import de.walware.statet.r.core.RCodeStyleSettings;
import de.walware.statet.r.core.RCore;
import de.walware.statet.r.core.model.IRSourceUnit;
import de.walware.statet.r.core.model.RModel;
import de.walware.statet.r.core.rlang.RTerminal;
import de.walware.statet.r.core.rsource.IRSourceConstants;
import de.walware.statet.r.core.rsource.RLexer;
import de.walware.statet.r.core.rsource.RSourceIndenter;
import de.walware.statet.r.core.rsource.ast.Assignment;
import de.walware.statet.r.core.rsource.ast.RAstNode;
import de.walware.statet.r.core.rsource.ast.RScanner;
import de.walware.statet.r.core.rsource.ast.SourceComponent;
import de.walware.statet.r.core.source.IRDocumentConstants;
import de.walware.statet.r.core.source.RHeuristicTokenScanner;
import de.walware.statet.r.internal.core.refactoring.Messages;


/**
 * RefactoringAdapter for R
 */
public class RRefactoringAdapter extends RefactoringAdapter {
	
	
	private RLexer fLexer;
	
	
	public RRefactoringAdapter() {
		super(RModel.R_TYPE_ID);
	}
	
	
	@Override
	public String getPluginIdentifier() {
		return RCore.PLUGIN_ID;
	}
	
	@Override
	public RHeuristicTokenScanner getScanner(final ISourceUnit su) {
		return RHeuristicTokenScanner.create(su.getDocumentContentInfo());
	}
	
	@Override
	public boolean isCommentContent(final ITypedRegion partition) {
		return (partition != null
				&& partition.getType() == IRDocumentConstants.R_COMMENT_CONTENT_TYPE );
	}
	
	public IRegion trimToAstRegion(final AbstractDocument document, final IRegion region,
			final RHeuristicTokenScanner scanner) {
		scanner.configure(document, IRDocumentConstants.R_CODE_CONTENT_CONSTRAINT);
		int start = region.getOffset();
		int stop = start+region.getLength();
		int result;
		
		while (stop > start) {
			result = scanner.findNonBlankBackward(stop, start, true);
			if (result >= 0) {
				if (scanner.getChar() == ';') {
					stop = result;
					continue;
				}
				else {
					stop = result + 1;
					break;
				}
			}
			else {
				stop = start;
				break;
			}
		}
		
		while (start < stop) {
			result = scanner.findNonBlankForward(start, stop, true);
			if (result >= 0) {
				if (scanner.getChar() == ';') {
					start = result + 1;
					continue;
				}
				else {
					start = result;
					break;
				}
			}
			else {
				start = stop;
				break;
			}
		}
		
		return new Region(start, stop-start);
	}
	
	public RAstNode searchPotentialNameNode(final ISourceUnit su, IRegion region,
			final boolean allowAssignRegion, final IProgressMonitor monitor) {
		final SubMonitor progress = SubMonitor.convert(monitor, 5);
		
		su.connect(progress.newChild(1));
		try {
			final AbstractDocument document = su.getDocument(progress.newChild(1));
			final RHeuristicTokenScanner scanner = getScanner(su);
			
			region = trimToAstRegion(document, region, scanner);
			
			final ISourceUnitModelInfo modelInfo = su.getModelInfo(RModel.TYPE_ID,
					IModelManager.MODEL_FILE, progress.newChild(1) );
			if (modelInfo != null) {
				final AstSelection astSelection = AstSelection.search(modelInfo.getAst().root,
						region.getOffset(), region.getOffset() + region.getLength(),
						AstSelection.MODE_COVERING_SAME_LAST );
				if (astSelection.getCovering() instanceof RAstNode) {
					final RAstNode node = (RAstNode) astSelection.getCovering();
					return getPotentialNameNode(node, allowAssignRegion);
				}
			}
			return null;
		}
		finally {
			progress.setWorkRemaining(1);
			su.disconnect(progress.newChild(1));
		}
	}
	
	public IRegion expandSelectionRegion(final AbstractDocument document, final IRegion region,
			final IRegion limit, final RHeuristicTokenScanner scanner) {
		scanner.configure(document, new IPartitionConstraint() {
			@Override
			public boolean matches(final String partitionType) {
				return (partitionType != IRDocumentConstants.R_COMMENT_CONTENT_TYPE);
			}
		});
		final int min = limit.getOffset();
		final int max = limit.getOffset()+limit.getLength();
		int start = region.getOffset();
		int stop = start+region.getLength();
		int result;
		
		while (start > min) {
			result = scanner.findNonBlankBackward(start, min, true);
			if (result >= 0) {
				if (IRDocumentConstants.R_DEFAULT_CONTENT_CONSTRAINT.matches(scanner.getPartition(result).getType())
						&& scanner.getChar() == ';') {
					start = result;
					continue;
				}
				else {
					start = result + 1;
					break;
				}
			}
			else {
				start = min;
				break;
			}
		}
		
		while (stop < max) {
			result = scanner.findNonBlankForward(stop, max, true);
			if (result >= 0) {
				if (IRDocumentConstants.R_DEFAULT_CONTENT_CONSTRAINT.matches(scanner.getPartition(result).getType())
						&& scanner.getChar() == ';') {
					stop = result + 1;
					continue;
				}
				else {
					stop = result;
					break;
				}
			}
			else {
				stop = max;
				break;
			}
		}
		
		return new Region(start, stop-start);
	}
	
	
	public String validateIdentifier(final String value, final String identifierMessageName) {
		if (value == null || value.isEmpty()) { 
			return (identifierMessageName != null) ?
					NLS.bind(Messages.RIdentifiers_error_EmptyFor_message, identifierMessageName, Messages.RIdentifiers_error_Empty_message) :
					Messages.RIdentifiers_error_Empty_message;
		}
		if (fLexer == null) {
			fLexer = new RLexer();
		}
		final StringParseInput input = new StringParseInput(value);
		input.init();
		fLexer.reset(input);
		final RTerminal nextToken = fLexer.next();
		if (nextToken == RTerminal.EOF) {
			return (identifierMessageName != null) ?
					NLS.bind(Messages.RIdentifiers_error_EmptyFor_message, identifierMessageName, Messages.RIdentifiers_error_Empty_message) :
					Messages.RIdentifiers_error_Empty_message;
		}
		if ((nextToken != RTerminal.SYMBOL && nextToken != RTerminal.SYMBOL_G)
				|| ((fLexer.getStatusCode() & IRSourceConstants.STATUSFLAG_REAL_ERROR) != 0)
				|| (fLexer.next() != RTerminal.EOF)) {
			return (identifierMessageName != null) ?
					NLS.bind(Messages.RIdentifiers_error_InvalidFor_message, identifierMessageName, Messages.RIdentifiers_error_Empty_message) :
					Messages.RIdentifiers_error_Invalid_message;
		}
		return null;
	}
	
	static RAstNode getPotentialNameNode(final RAstNode node, final boolean allowAssignRegion) {
		switch (node.getNodeType()) {
		case A_LEFT:
		case A_EQUALS:
		case A_RIGHT:
			if (allowAssignRegion) {
				final Assignment assignment = (Assignment) node;
				if (assignment.isSearchOperator()) {
					switch (assignment.getTargetChild().getNodeType()) {
					case SYMBOL:
					case STRING_CONST:
						return assignment.getTargetChild();
					}
				}
			}
			return null;
		case SYMBOL:
		case STRING_CONST:
			return node;
		default:
			return null;
		}
	}
	
	public static String getQuotedIdentifier(final String identifier) {
		int length;
		if (identifier == null || (length = identifier.length()) == 0) {
			return "";
		}
		if (identifier.charAt(0) == '`') {
			if (length > 1 && identifier.charAt(length-1) == '`') {
				return identifier;
			}
			else {
				return identifier + '`';
			}
		}
		else {
			return '`' + identifier + '`';
		}
	}
	
	public static String getUnquotedIdentifier(final String identifier) {
		int length;
		if (identifier == null || (length = identifier.length()) == 0) {
			return "";
		}
		if (identifier.charAt(0) == '`') {
			if (length > 1 && identifier.charAt(length-1) == '`') {
				return identifier.substring(1, length-2);
			}
			else {
				return identifier.substring(1, length-1);
			}
		}
		else {
			return identifier;
		}
	}
	
	static String indent(final StringBuilder sb, final AbstractDocument orgDoc, final int offset,
			final ISourceUnit su, final RHeuristicTokenScanner scanner) throws BadLocationException, CoreException {
		final IRCoreAccess coreConfig = (su instanceof IRSourceUnit) ? ((IRSourceUnit) su).getRCoreAccess() : RCore.getWorkbenchAccess();
		
		final IndentUtil indentUtil = new IndentUtil(orgDoc, coreConfig.getRCodeStyle());
		final int column = indentUtil.getColumnAtOffset(offset);
		final String initial = indentUtil.createIndentString(column);
		final String prefix = initial+"1\n"; //$NON-NLS-1$
		sb.insert(0, prefix);
		String text = sb.toString();
		final Document doc = new Document(text);
		final SourceParseInput parseInput = new StringParseInput(text);
		text = null;
		
		final RScanner astScanner = new RScanner(parseInput, AstInfo.LEVEL_MINIMAL);
		final SourceComponent rootNode = astScanner.scanSourceUnit();
		
		final RSourceIndenter indenter = new RSourceIndenter(scanner, coreConfig);
		final TextEdit edits = indenter.getIndentEdits(doc, rootNode, 0, 1, doc.getNumberOfLines()-1);
		edits.apply(doc, 0);
		return doc.get(prefix.length(), doc.getLength()-prefix.length());
	}
	
	/**
	 * Prepare the insertion of a command (text) before another command (offset)
	 * 
	 * The method prepares the insertion by modifying the text and returning the offset
	 * where to insert the modified text
	 * 
	 * @param text the command to insert, will be modified
	 * @param orgDoc the document
	 * @param offset the offset where to insert the command
	 * @param su the source unit, if available
	 * @return the offset to insert the modified text
	 * @throws BadLocationException
	 * @throws CoreException
	 */
	static int prepareInsertBefore(final StringBuilder text, final AbstractDocument orgDoc, final int offset,
			final ISourceUnit su) throws BadLocationException, CoreException {
		final IRCoreAccess coreConfig = (su instanceof IRSourceUnit) ? ((IRSourceUnit) su).getRCoreAccess() : RCore.getWorkbenchAccess();
		
		final IndentUtil indentUtil = new IndentUtil(orgDoc, coreConfig.getRCodeStyle());
		final int line = orgDoc.getLineOfOffset(offset);
		final int[] lineIndent = indentUtil.getLineIndent(line, false);
		if (lineIndent[IndentUtil.OFFSET_IDX] == offset) { // first char/command in line
			text.insert(0, indentUtil.createIndentString(lineIndent[IndentUtil.COLUMN_IDX]));
			text.append(orgDoc.getDefaultLineDelimiter());
			return orgDoc.getLineOffset(line);
		}
		else {
			text.append("; "); //$NON-NLS-1$
			return offset;
		}
	}
	
	static RCodeStyleSettings getCodeStyle(final ISourceUnit su) {
		if (su instanceof IRSourceUnit) {
			return ((IRSourceUnit) su).getRCoreAccess().getRCodeStyle();
		}
		return RCore.getWorkbenchAccess().getRCodeStyle();
	}
	
}
