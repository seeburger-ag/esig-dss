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
package eu.europa.esig.dss.cades.signature;

import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.TimestampWrapper;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDigestMatcher;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.DigestMatcherType;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.signature.DocumentSignatureService;
import eu.europa.esig.dss.spi.x509.tsp.KeyEntityTSPSource;
import org.junit.jupiter.api.BeforeEach;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CAdESLevelTSHA1MessageImprintTest extends AbstractCAdESTestSignature {

	private DocumentSignatureService<CAdESSignatureParameters, CAdESTimestampParameters> service;
	private CAdESSignatureParameters signatureParameters;
	private DSSDocument documentToSign;

	@BeforeEach
	void init() throws Exception {
		documentToSign = new InMemoryDocument("Hello World".getBytes());

		signatureParameters = new CAdESSignatureParameters();
		signatureParameters.bLevel().setSigningDate(new Date());
		signatureParameters.setSigningCertificate(getSigningCert());
		signatureParameters.setCertificateChain(getCertificateChain());
		signatureParameters.setSignaturePackaging(SignaturePackaging.ENVELOPING);
		signatureParameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_T);
		CAdESTimestampParameters signatureTimestampParameters = new CAdESTimestampParameters(DigestAlgorithm.SHA1);
		signatureParameters.setSignatureTimestampParameters(signatureTimestampParameters);

		service = new CAdESService(getOfflineCertificateVerifier());

		KeyEntityTSPSource tspSource = getPKITSPSourceByName(GOOD_TSA);
		tspSource.setAcceptedDigestAlgorithms(Collections.singletonList(DigestAlgorithm.SHA1));

		service.setTspSource(tspSource);
	}

	@Override
	protected DocumentSignatureService<CAdESSignatureParameters, CAdESTimestampParameters> getService() {
		return service;
	}

	@Override
	protected CAdESSignatureParameters getSignatureParameters() {
		return signatureParameters;
	}

	@Override
	protected DSSDocument getDocumentToSign() {
		return documentToSign;
	}

	@Override
	protected String getSigningAlias() {
		return GOOD_USER;
	}

	@Override
	protected void checkTimestamps(DiagnosticData diagnosticData) {
		super.checkTimestamps(diagnosticData);

		List<TimestampWrapper> timestampList = diagnosticData.getTimestampList();
		assertEquals(1, timestampList.size());
		TimestampWrapper timestampWrapper = timestampList.get(0);

		List<XmlDigestMatcher> digestMatchers = timestampWrapper.getDigestMatchers();
		assertEquals(1, digestMatchers.size());

		XmlDigestMatcher xmlDigestMatcher = digestMatchers.get(0);
		assertEquals(DigestMatcherType.MESSAGE_IMPRINT, xmlDigestMatcher.getType());
		assertEquals(DigestAlgorithm.SHA1, xmlDigestMatcher.getDigestMethod());

		assertEquals(DigestAlgorithm.SHA512, timestampWrapper.getDigestAlgorithm());
		assertEquals(EncryptionAlgorithm.RSA, timestampWrapper.getEncryptionAlgorithm());
	}

}
