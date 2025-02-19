/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 * <p>
 * This file is part of the "DSS - Digital Signature Services" project.
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package eu.europa.esig.dss.xades.validation.scope;

import eu.europa.esig.dss.enumerations.DigestMatcherType;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DigestDocument;
import eu.europa.esig.dss.model.ReferenceValidation;
import eu.europa.esig.dss.model.scope.SignatureScope;
import eu.europa.esig.dss.spi.validation.scope.AbstractSignatureScopeFinder;
import eu.europa.esig.dss.spi.validation.scope.ContainerContentSignatureScope;
import eu.europa.esig.dss.spi.validation.scope.ContainerSignatureScope;
import eu.europa.esig.dss.spi.validation.scope.CounterSignatureScope;
import eu.europa.esig.dss.spi.validation.scope.DigestSignatureScope;
import eu.europa.esig.dss.spi.validation.scope.FullSignatureScope;
import eu.europa.esig.dss.spi.validation.scope.ManifestSignatureScope;
import eu.europa.esig.dss.spi.validation.scope.SignatureScopeFinder;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.xades.DSSXMLUtils;
import eu.europa.esig.dss.xades.reference.XAdESReferenceValidation;
import eu.europa.esig.dss.xades.validation.XAdESSignature;
import eu.europa.esig.dss.xml.utils.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs operations in order to find all signed data for a XAdES Signature
 */
public class XAdESSignatureScopeFinder extends AbstractSignatureScopeFinder implements SignatureScopeFinder<XAdESSignature> {
	
	/**
	 * Default constructor
	 */
	public XAdESSignatureScopeFinder() {
		// empty
	}

	@Override
	public List<SignatureScope> findSignatureScope(final XAdESSignature xadesSignature) {

		final List<SignatureScope> result = new ArrayList<>();
		
		List<ReferenceValidation> referenceValidations = xadesSignature.getReferenceValidations();
		for (ReferenceValidation referenceValidation : referenceValidations) {
			if (DigestMatcherType.SIGNED_PROPERTIES.equals(referenceValidation.getType()) || 
					DigestMatcherType.KEY_INFO.equals(referenceValidation.getType()) ||
					DigestMatcherType.SIGNATURE_PROPERTIES.equals(referenceValidation.getType())) {
				// not a subject for the Signature Scope
				continue;
			}
			if (!(referenceValidation instanceof XAdESReferenceValidation)) {
				continue;
			}
			XAdESReferenceValidation xadesReferenceValidation = (XAdESReferenceValidation) referenceValidation;
			
			final String uri = xadesReferenceValidation.getUri();
			final String xmlIdOfSignedElement = DomUtils.getId(uri);
			final List<String> transformations = xadesReferenceValidation.getTransformationNames();
			
			if (xadesReferenceValidation.isFound() && DigestMatcherType.XPOINTER.equals(xadesReferenceValidation.getType())) {
				result.add(new XPointerSignatureScope(uri, createInMemoryDocument(xadesReferenceValidation.getOriginalContentBytes()), transformations));
				
			} else if (xadesReferenceValidation.isFound() && DigestMatcherType.OBJECT.equals(xadesReferenceValidation.getType())) {
				Node objectById = DSSXMLUtils.getObjectById(xadesSignature.getSignatureElement(), uri);
				if (objectById != null && objectById.hasChildNodes()) {
					Node referencedObject = objectById.getFirstChild();
					result.add(new XmlElementSignatureScope(xmlIdOfSignedElement, createInMemoryDocument(DomUtils.getNodeBytes(referencedObject)), transformations));
				}
				
			} else if (xadesReferenceValidation.isFound() && DigestMatcherType.MANIFEST.equals(xadesReferenceValidation.getType())) {
				ManifestSignatureScope manifestSignatureScope = new ManifestSignatureScope(
						getReferenceName(xadesReferenceValidation), createDigestDocument(xadesReferenceValidation.getDigest()),
						xadesReferenceValidation.getTransformationNames());
				result.add(manifestSignatureScope);

				for (ReferenceValidation manifestEntry : xadesReferenceValidation.getDependentValidations()) {
					String manifestEntryReferenceName = getReferenceName(manifestEntry);
					if (manifestEntryReferenceName != null && manifestEntry.isFound()) {
						// try to get document digest from list of detached contents
						SignatureScope detachedSignatureScopeResult = getFromDetachedContent(xadesSignature, transformations, getReferencedDocumentName(manifestEntry));
						if (detachedSignatureScopeResult != null) {
							manifestSignatureScope.addChildSignatureScope(detachedSignatureScopeResult);
						} else if (manifestEntry.getDigest() != null) {
							// if the relative detached content is not found, store the reference value
							manifestSignatureScope.addChildSignatureScope(
									new ManifestEntrySignatureScope(manifestEntryReferenceName, createDigestDocument(manifestEntry.getDigest()),
											getReferenceName(xadesReferenceValidation), manifestEntry.getTransformationNames()));
						}
					}
				}
				
			} else if (xadesReferenceValidation.isFound() && DigestMatcherType.COUNTER_SIGNATURE.equals(xadesReferenceValidation.getType()) &&
					xadesSignature.getMasterSignature() != null) {
            	result.add(new CounterSignatureScope(xadesSignature.getMasterSignature(),
						createInMemoryDocument(xadesReferenceValidation.getOriginalContentBytes())));
				
			} else if (xadesReferenceValidation.isFound() && Utils.EMPTY_STRING.equals(uri)) {
				byte[] originalContentBytes = xadesReferenceValidation.getOriginalContentBytes();
				if (originalContentBytes != null) {
					// self contained document
					result.add(new XmlRootSignatureScope(createInMemoryDocument(originalContentBytes), transformations));
				}
				
			} else if (xadesReferenceValidation.isFound() && DomUtils.isElementReference(uri)) {
				Element signedElement = DomUtils.getElementById(
						xadesSignature.getSignatureElement().getOwnerDocument(), DomUtils.getId(uri));
				if (signedElement != null) {
					if (isEverythingCovered(xadesSignature, xmlIdOfSignedElement)) {
						result.add(new XmlRootSignatureScope(
								createInMemoryDocument(DomUtils.getNodeBytes(signedElement)), transformations));
					} else {
						result.add(new XmlElementSignatureScope(
								xmlIdOfSignedElement, createInMemoryDocument(DomUtils.getNodeBytes(signedElement)), transformations));
					}
				}
				
			} else if (xadesReferenceValidation.isIntact() && Utils.isCollectionNotEmpty(xadesSignature.getDetachedContents())) {
				// detached file (the signature must intact in order to be sure in the correctness of the provided file)
				final String referencedDocumentName = getReferencedDocumentName(xadesReferenceValidation);
				SignatureScope signatureScope = getFromDetachedContent(xadesSignature, transformations, referencedDocumentName);
				if (signatureScope != null) {
					result.add(signatureScope);
				}
				
			} else if (Utils.isCollectionEmpty(transformations)) {
				// if a matching file was not found around the detached contents and transformations are not defined, use the original reference data
				result.add(new FullSignatureScope(uri, createDigestDocument(xadesReferenceValidation.getDigest())));
				
			}
		}
		return result;
		
	}

	private String getReferencedDocumentName(ReferenceValidation referenceValidation) {
		if (Utils.isStringNotEmpty(referenceValidation.getDocumentName())) {
			return referenceValidation.getDocumentName();
		} else if (Utils.isStringNotEmpty(referenceValidation.getUri())) {
			// used for a document extraction, empty URI is not acceptable
			return DomUtils.getId(referenceValidation.getUri());
		}
		return null;
	}

	private String getReferenceName(ReferenceValidation referenceValidation) {
		if (Utils.isStringNotEmpty(referenceValidation.getDocumentName())) {
			return referenceValidation.getDocumentName();
		} else if (Utils.isStringNotEmpty(referenceValidation.getId())) {
			return DomUtils.getId(referenceValidation.getId());
		} else if (referenceValidation.getUri() != null) { // URI can be empty ""
			return DomUtils.getId(referenceValidation.getUri());
		}
		return null;
	}

	private SignatureScope getFromDetachedContent(final XAdESSignature xadesSignature,
												  final List<String> transformations, final String relatedDocumentName) {
		List<DSSDocument> detachedContents = xadesSignature.getDetachedContents();
		if (Utils.isCollectionNotEmpty(detachedContents)) {
			for (DSSDocument detachedDocument : detachedContents) {

				// check the original detached file by its name (or if no name if provided, see {@link DetachedSignatureResolver})
				if (detachedDocument.getName() == null && (relatedDocumentName == null && Utils.collectionSize(detachedContents) == 1)
						|| (relatedDocumentName != null && relatedDocumentName.equals(detachedDocument.getName())) ) {
					String fileName = detachedDocument.getName() != null ? detachedDocument.getName() : relatedDocumentName;
					if (detachedDocument instanceof DigestDocument) {
						DigestDocument digestDocument = (DigestDocument) detachedDocument;
						return new DigestSignatureScope(fileName, digestDocument);
	
					} else if (Utils.isCollectionNotEmpty(transformations)) {
						return new XmlFullSignatureScope(fileName, detachedDocument, transformations);
	
					} else if (isASiCSArchive(xadesSignature)) {
						ContainerSignatureScope containerSignatureScope = new ContainerSignatureScope(relatedDocumentName, detachedDocument);
						for (DSSDocument archivedDocument : xadesSignature.getContainerContents()) {
							containerSignatureScope.addChildSignatureScope(new ContainerContentSignatureScope(archivedDocument));
						}
						return containerSignatureScope;
	
					} else {
						return new FullSignatureScope(fileName, detachedDocument);
					}
				}
			}
		}
		return null;
	}

	private boolean isEverythingCovered(XAdESSignature signature, String coveredObjectId) {
		Element parent = signature.getSignatureElement().getOwnerDocument().getDocumentElement();
		return parent != null && isRelatedToUri(parent, coveredObjectId);
	}

	private boolean isRelatedToUri(Node currentNode, String id) {
		String idValue = DSSXMLUtils.getIDIdentifier(currentNode);
		if (idValue == null) {
			return Utils.isStringBlank(id);
		} else {
			return id.equals(idValue) || id.equals(Utils.EMPTY_STRING);
		}
	}

}
