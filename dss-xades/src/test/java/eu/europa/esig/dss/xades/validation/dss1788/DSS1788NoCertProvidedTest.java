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
package eu.europa.esig.dss.xades.validation.dss1788;

import eu.europa.esig.dss.diagnostic.CertificateRefWrapper;
import eu.europa.esig.dss.diagnostic.CertificateWrapper;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.FoundCertificatesProxy;
import eu.europa.esig.dss.diagnostic.OrphanCertificateTokenWrapper;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.enumerations.CertificateRefOrigin;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SubIndication;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.simplereport.SimpleReport;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.spi.signature.AdvancedSignature;
import eu.europa.esig.dss.spi.SignatureCertificateSource;
import eu.europa.esig.dss.xades.validation.AbstractXAdESTestValidation;
import eu.europa.esig.validationreport.jaxb.SignatureAttributesType;
import eu.europa.esig.validationreport.jaxb.SignatureValidationReportType;
import eu.europa.esig.validationreport.jaxb.ValidationReportType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DSS1788NoCertProvidedTest extends AbstractXAdESTestValidation {

	@Override
	protected DSSDocument getSignedDocument() {
		return new FileDocument("src/test/resources/validation/dss1788/dss1788-noCertProvided.xml");
	}
	
	@Override
	protected void checkSigningCertificateValue(DiagnosticData diagnosticData) {
		SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		
		CertificateWrapper signingCertificate = signature.getSigningCertificate();
		assertNull(signingCertificate);
		byte[] signingCertificatePublicKey = signature.getSigningCertificatePublicKey();
		assertNotNull(signingCertificatePublicKey);
		assertTrue(signature.isSigningCertificateReferencePresent());
		
		CertificateRefWrapper signingCertificateReference = signature.getSigningCertificateReference();
		assertNotNull(signingCertificateReference);
		assertTrue(signingCertificateReference.isDigestValuePresent());
		assertFalse(signingCertificateReference.isDigestValueMatch());
		assertTrue(signingCertificateReference.isIssuerSerialPresent());
		assertFalse(signingCertificateReference.isIssuerSerialMatch());
		
		assertFalse(Utils.isCollectionEmpty(signature.foundCertificates().getOrphanCertificatesByRefOrigin(CertificateRefOrigin.SIGNING_CERTIFICATE)));
	}
	
	@Override
	protected void verifySimpleReport(SimpleReport simpleReport) {
		super.verifySimpleReport(simpleReport);

		assertEquals(Indication.INDETERMINATE, simpleReport.getIndication(simpleReport.getFirstSignatureId()));
		assertEquals(SubIndication.NO_SIGNING_CERTIFICATE_FOUND, simpleReport.getSubIndication(simpleReport.getFirstSignatureId()));
	}
	
	@Override
	protected void verifySourcesAndDiagnosticData(List<AdvancedSignature> signatures, DiagnosticData diagnosticData) {
		AdvancedSignature advancedSignature = signatures.get(0);
		SignatureCertificateSource certificateSource = advancedSignature.getCertificateSource();
		
		SignatureWrapper signatureWrapper = diagnosticData.getSignatureById(advancedSignature.getId());
		assertNotNull(signatureWrapper);
		FoundCertificatesProxy foundCertificates = signatureWrapper.foundCertificates();
		
		assertEquals(certificateSource.getSigningCertificateRefs().size(), 
				foundCertificates.getOrphanCertificatesByRefOrigin(CertificateRefOrigin.SIGNING_CERTIFICATE).size());

		checkOrphanTokens(diagnosticData);
	}
	
	@Override
	protected void checkCertificateChain(DiagnosticData diagnosticData) {
		SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		assertTrue(Utils.isCollectionEmpty(signature.getCertificateChain()));
	}

	@Override
	protected void checkSignatureLevel(DiagnosticData diagnosticData) {
		super.checkSignatureLevel(diagnosticData);
		// The SigningCertificate shall be present in ds:KeyInfo -> not baseline
		assertEquals(SignatureLevel.XAdES_LT, diagnosticData.getSignatureFormat(diagnosticData.getFirstSignatureId()));
	}
	
	@Override
	protected void checkOrphanTokens(DiagnosticData diagnosticData) {
		List<OrphanCertificateTokenWrapper> allOrphanCertificateReferences = diagnosticData.getAllOrphanCertificateReferences();
		assertEquals(1, allOrphanCertificateReferences.size());
		OrphanCertificateTokenWrapper orphanCertificateTokenWrapper = allOrphanCertificateReferences.iterator().next();
		assertNotNull(orphanCertificateTokenWrapper);
		assertNotNull(orphanCertificateTokenWrapper.getDigestAlgoAndValue());
		assertEquals("", orphanCertificateTokenWrapper.getCertificateDN());
		assertEquals("", orphanCertificateTokenWrapper.getCertificateIssuerDN());
		assertEquals(0, diagnosticData.getAllOrphanRevocationReferences().size());
	}
	
	@Override
	protected void verifyETSIValidationReport(ValidationReportType etsiValidationReportJaxb) {
		List<SignatureValidationReportType> reports = etsiValidationReportJaxb.getSignatureValidationReport();
		for (SignatureValidationReportType signatureValidationReport : reports) {
			assertNotNull(signatureValidationReport);
			
			SignatureAttributesType signatureAttributes = signatureValidationReport.getSignatureAttributes();
			validateETSISignatureAttributes(signatureAttributes);
		}
	}

}
