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
package eu.europa.esig.dss.asic.cades.extension.asice;

import eu.europa.esig.dss.asic.cades.extract.ASiCWithCAdESContainerExtractor;
import eu.europa.esig.dss.asic.cades.ASiCWithCAdESSignatureParameters;
import eu.europa.esig.dss.asic.cades.signature.ASiCWithCAdESService;
import eu.europa.esig.dss.asic.common.ASiCContent;
import eu.europa.esig.dss.asic.common.extract.DefaultASiCContainerExtractor;
import eu.europa.esig.dss.asic.common.validation.ASiCManifestValidator;
import eu.europa.esig.dss.asic.common.validation.ASiCManifestParser;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.TimestampWrapper;
import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.ASiCManifestTypeEnum;
import eu.europa.esig.dss.enumerations.MimeTypeEnum;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.ManifestEntry;
import eu.europa.esig.dss.model.ManifestFile;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.test.PKIFactoryAccess;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ASiCECAdESMultipleArchiveTimestampsTest extends PKIFactoryAccess {

	@Test
	void test() throws Exception {
		List<DSSDocument> documentToSigns = new ArrayList<>();
		documentToSigns.add(new InMemoryDocument("Hello World !".getBytes(), "test.text", MimeTypeEnum.TEXT));
		documentToSigns.add(new InMemoryDocument("Bye World !".getBytes(), "test2.text", MimeTypeEnum.TEXT));

		ASiCWithCAdESSignatureParameters signatureParameters = new ASiCWithCAdESSignatureParameters();
		signatureParameters.bLevel().setSigningDate(new Date());
		signatureParameters.setSigningCertificate(getSigningCert());
		signatureParameters.setCertificateChain(getCertificateChain());
		signatureParameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_LTA);
		signatureParameters.aSiC().setContainerType(ASiCContainerType.ASiC_E);

		ASiCWithCAdESService service = new ASiCWithCAdESService(getCompleteCertificateVerifier());
		service.setTspSource(getGoodTsa());

		ToBeSigned dataToSign = service.getDataToSign(documentToSigns, signatureParameters);
		SignatureValue signatureValue = getToken().sign(dataToSign, signatureParameters.getDigestAlgorithm(), getPrivateKeyEntry());
		DSSDocument signedDocument = service.signDocument(documentToSigns, signatureParameters, signatureValue);

		// signedDocument.save("target/signed.asice");

		service.setTspSource(getAlternateGoodTsa());

		ASiCWithCAdESSignatureParameters extendParameters = new ASiCWithCAdESSignatureParameters();
		extendParameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_LTA);
		extendParameters.aSiC().setContainerType(ASiCContainerType.ASiC_E);
		DSSDocument extendDocument = service.extendDocument(signedDocument, extendParameters);
		
		// extendDocument.save("target/extended.asice");
		
		DSSDocument doubleExtendedDocument = service.extendDocument(extendDocument, extendParameters);
		
		// doubleExtendedDocument.save("target/doubleExtendedDocument.asice");
		
		SignedDocumentValidator validator = SignedDocumentValidator.fromDocument(doubleExtendedDocument);

		validator.setCertificateVerifier(getCompleteCertificateVerifier());

		Reports reports = validator.validateDocument();
		DiagnosticData diagnosticData = reports.getDiagnosticData();
		List<String> signatureIdList = diagnosticData.getSignatureIdList();
		assertEquals(1, signatureIdList.size());
		assertTrue(diagnosticData.isBLevelTechnicallyValid(diagnosticData.getFirstSignatureId()));

		SignatureWrapper signatureWrapper = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		List<TimestampWrapper> timestampList = signatureWrapper.getTimestampList();
		assertEquals(4, timestampList.size());

		for (TimestampWrapper timestampWrapper : timestampList) {
			assertTrue(timestampWrapper.isSignatureValid());
			assertTrue(timestampWrapper.isMessageImprintDataFound());
			assertTrue(timestampWrapper.isMessageImprintDataIntact());
		}

		DefaultASiCContainerExtractor extractor = new ASiCWithCAdESContainerExtractor(doubleExtendedDocument);
		ASiCContent result = extractor.extract();

		assertEquals(0, result.getUnsupportedDocuments().size());

		List<DSSDocument> signatureDocuments = result.getSignatureDocuments();
		assertEquals(1, signatureDocuments.size());
		DSSDocument signatureDocument = signatureDocuments.get(0);
		String signatureFilename = signatureDocument.getName();
		assertTrue(signatureFilename.startsWith("META-INF/signature"));
		assertTrue(signatureFilename.endsWith(".p7s"));

		List<DSSDocument> timestamps = result.getTimestampDocuments();

		List<DSSDocument> manifestDocuments = result.getManifestDocuments();
		assertEquals(1, manifestDocuments.size());
		String manifestFilename = manifestDocuments.get(0).getName();
		assertTrue(manifestFilename.startsWith("META-INF/ASiCManifest"));
		assertTrue(manifestFilename.endsWith(".xml"));

		List<DSSDocument> signedDocuments = result.getSignedDocuments();
		assertEquals(2, signedDocuments.size());

		DSSDocument linkedManifest = ASiCManifestParser.getLinkedManifest(manifestDocuments, signatureDocument.getName());
		assertNotNull(linkedManifest);
		ManifestFile linkedManifestFile = ASiCManifestParser.getManifestFile(linkedManifest);
		assertNotNull(linkedManifestFile);
		assertEquals(ASiCManifestTypeEnum.SIGNATURE, linkedManifestFile.getManifestType());
		
		ASiCManifestValidator linkedManifestFileValidator = new ASiCManifestValidator(linkedManifestFile, signedDocuments);
		List<ManifestEntry> linkedManifestFileEntries = linkedManifestFileValidator.validateEntries();
		validateEntries(linkedManifestFileEntries);

		List<DSSDocument> archiveManifestDocuments = result.getArchiveManifestDocuments();
		assertEquals(3, archiveManifestDocuments.size());

		ManifestFile lastCreatedArchiveManifestFile = null;
		for (DSSDocument timestamp : timestamps) {
			linkedManifest = ASiCManifestParser.getLinkedManifest(archiveManifestDocuments, timestamp.getName());
			assertNotNull(linkedManifest);
			lastCreatedArchiveManifestFile = ASiCManifestParser.getManifestFile(linkedManifest);
			assertNotNull(lastCreatedArchiveManifestFile);
			assertEquals(ASiCManifestTypeEnum.ARCHIVE_MANIFEST, lastCreatedArchiveManifestFile.getManifestType());
			ASiCManifestValidator archiveManifestValidator =
					new ASiCManifestValidator(lastCreatedArchiveManifestFile, result.getAllDocuments());
			List<ManifestEntry> archiveManifestEntries = archiveManifestValidator.validateEntries();
			validateEntries(archiveManifestEntries);
			
			ManifestEntry rootfile = getRootfile(lastCreatedArchiveManifestFile);
			if ("META-INF/ASiCArchiveManifest001.xml".equals(lastCreatedArchiveManifestFile.getFilename())) {
				assertNull(rootfile); // first created ArchiveManifest does not contain a "Rootfile" element
			} else {
				assertNotNull(rootfile);
			}
		}
		assertNotNull(lastCreatedArchiveManifestFile);
		assertEquals("META-INF/ASiCArchiveManifest.xml", lastCreatedArchiveManifestFile.getFilename());
		
		ManifestEntry rootfile = getRootfile(lastCreatedArchiveManifestFile);
		assertEquals("META-INF/ASiCArchiveManifest002.xml", rootfile.getUri());

		DSSDocument mimeTypeDocument = result.getMimeTypeDocument();

		byte[] mimeTypeContent = DSSUtils.toByteArray(mimeTypeDocument);
		assertEquals(MimeTypeEnum.ASICE.getMimeTypeString(), new String(mimeTypeContent, StandardCharsets.UTF_8));

	}
	
	private ManifestEntry getRootfile(ManifestFile manifestFile) {
		int rootfiles = 0;
		ManifestEntry rootfile = null; 
		for (ManifestEntry entry : manifestFile.getEntries()) {
			if (entry.isRootfile()) {
				rootfile = entry;
				rootfiles++;
			}
		}
		assertTrue(rootfiles < 2); // 0 or 1 is allowed
		return rootfile;
	}
	
	private void validateEntries(List<ManifestEntry> entries) {
		assertTrue(Utils.isCollectionNotEmpty(entries));
		for (ManifestEntry entry : entries) {
			assertNotNull(entry.getUri());
			assertNotNull(entry.getMimeType());
			assertNotNull(entry.getDigest());
			assertTrue(entry.isFound());
			assertTrue(entry.isIntact());
		}
	}

	@Override
	protected String getSigningAlias() {
		return GOOD_USER;
	}
}
