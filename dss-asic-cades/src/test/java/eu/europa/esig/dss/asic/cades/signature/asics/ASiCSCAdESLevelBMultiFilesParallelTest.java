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
package eu.europa.esig.dss.asic.cades.signature.asics;

import eu.europa.esig.dss.asic.cades.ASiCWithCAdESSignatureParameters;
import eu.europa.esig.dss.asic.cades.extract.ASiCWithCAdESContainerExtractor;
import eu.europa.esig.dss.asic.cades.signature.ASiCWithCAdESService;
import eu.europa.esig.dss.asic.common.ASiCContent;
import eu.europa.esig.dss.asic.common.extract.DefaultASiCContainerExtractor;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDigestAlgoAndValue;
import eu.europa.esig.dss.diagnostic.jaxb.XmlSignatureScope;
import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.enumerations.MimeTypeEnum;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.test.PKIFactoryAccess;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ASiCSCAdESLevelBMultiFilesParallelTest extends PKIFactoryAccess {

	@Test
	void test() throws Exception {
		List<DSSDocument> documentToSigns = new ArrayList<>();
		DSSDocument firstDocument = new InMemoryDocument("Hello World !".getBytes(), "test.text", MimeTypeEnum.TEXT);
		documentToSigns.add(firstDocument);
		DSSDocument secondDocument = new InMemoryDocument("Bye World !".getBytes(), "test2.text", MimeTypeEnum.TEXT);
		documentToSigns.add(secondDocument);

		ASiCWithCAdESSignatureParameters signatureParameters = new ASiCWithCAdESSignatureParameters();
		signatureParameters.bLevel().setSigningDate(new Date());
		signatureParameters.setSigningCertificate(getSigningCert());
		signatureParameters.setCertificateChain(getCertificateChain());
		signatureParameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_B);
		signatureParameters.aSiC().setContainerType(ASiCContainerType.ASiC_S);

		ASiCWithCAdESService service = new ASiCWithCAdESService(getOfflineCertificateVerifier());

		ToBeSigned dataToSign = service.getDataToSign(documentToSigns, signatureParameters);
		SignatureValue signatureValue = getToken().sign(dataToSign, signatureParameters.getDigestAlgorithm(), getPrivateKeyEntry());
		DSSDocument signedDocument = service.signDocument(documentToSigns, signatureParameters, signatureValue);
		
		SignedDocumentValidator validator = SignedDocumentValidator.fromDocument(signedDocument);
		validator.setCertificateVerifier(getOfflineCertificateVerifier());

		Reports reports = validator.validateDocument();
		DiagnosticData diagnosticData = reports.getDiagnosticData();
		
		validateSignatureScope(diagnosticData, firstDocument, secondDocument);

		signatureParameters.bLevel().setSigningDate(new Date());

		service = new ASiCWithCAdESService(getOfflineCertificateVerifier());

		dataToSign = service.getDataToSign(signedDocument, signatureParameters);
		signatureValue = getToken().sign(dataToSign, signatureParameters.getDigestAlgorithm(), getPrivateKeyEntry());
		DSSDocument resignedDocument = service.signDocument(signedDocument, signatureParameters, signatureValue);

		validator = SignedDocumentValidator.fromDocument(resignedDocument);
		validator.setCertificateVerifier(getOfflineCertificateVerifier());

		reports = validator.validateDocument();

		diagnosticData = reports.getDiagnosticData();
		List<String> signatureIdList = diagnosticData.getSignatureIdList();
		assertEquals(2, signatureIdList.size());
		for (String sigId : signatureIdList) {
			assertTrue(diagnosticData.isBLevelTechnicallyValid(sigId));
			assertNotEquals(Indication.FAILED, reports.getSimpleReport().getIndication(sigId));
		}

		DefaultASiCContainerExtractor extractor = new ASiCWithCAdESContainerExtractor(resignedDocument);
		ASiCContent result = extractor.extract();

		assertEquals(0, result.getUnsupportedDocuments().size());

		List<DSSDocument> signatureDocuments = result.getSignatureDocuments();
		assertEquals(1, signatureDocuments.size());
		String signatureFilename = signatureDocuments.get(0).getName();
		assertTrue(signatureFilename.startsWith("META-INF/signature"));
		assertTrue(signatureFilename.endsWith(".p7s"));

		List<DSSDocument> manifestDocuments = result.getManifestDocuments();
		assertEquals(0, manifestDocuments.size());

		List<DSSDocument> signedDocuments = result.getSignedDocuments();
		assertEquals(1, signedDocuments.size());
		assertEquals("package.zip", signedDocuments.get(0).getName());

		List<DSSDocument> containerDocuments = result.getContainerDocuments();
		assertEquals(2, containerDocuments.size());

		DSSDocument mimeTypeDocument = result.getMimeTypeDocument();

		byte[] mimeTypeContent = DSSUtils.toByteArray(mimeTypeDocument);
		try {
			assertEquals(MimeTypeEnum.ASICS.getMimeTypeString(), new String(mimeTypeContent, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			fail(e.getMessage());
		}
		
		validateSignatureScope(diagnosticData, firstDocument, secondDocument);

	}
	
	private void validateSignatureScope(DiagnosticData diagnosticData, DSSDocument firstDocument, DSSDocument secondDocument) {
		
		SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		assertNotNull(signature);
		List<XmlSignatureScope> signatureScopes = signature.getSignatureScopes();
		assertNotNull(signatureScopes);
		assertEquals(3, signatureScopes.size());
		
		// first get(0) is an archive
		XmlSignatureScope xmlSignatureScopeFirstDocument = signatureScopes.get(1);
		assertNotNull(xmlSignatureScopeFirstDocument.getName());
		assertNotNull(xmlSignatureScopeFirstDocument.getSignerData());
		XmlDigestAlgoAndValue digestAlgoAndValue = xmlSignatureScopeFirstDocument.getSignerData().getDigestAlgoAndValue();
		assertNotNull(digestAlgoAndValue);
		DigestAlgorithm digestAlgorithm = digestAlgoAndValue.getDigestMethod();
        assertArrayEquals(firstDocument.getDigestValue(digestAlgorithm), digestAlgoAndValue.getDigestValue());
		
		XmlSignatureScope xmlSignatureScopeSecondDocument = signatureScopes.get(2);
		assertNotNull(xmlSignatureScopeSecondDocument.getName());
		assertNotNull(xmlSignatureScopeSecondDocument.getSignerData());
		digestAlgoAndValue = xmlSignatureScopeSecondDocument.getSignerData().getDigestAlgoAndValue();
		assertNotNull(digestAlgoAndValue);
		digestAlgorithm = digestAlgoAndValue.getDigestMethod();
		assertArrayEquals(secondDocument.getDigestValue(digestAlgorithm), digestAlgoAndValue.getDigestValue());
		
	}

	@Override
	protected String getSigningAlias() {
		return GOOD_USER;
	}
}
