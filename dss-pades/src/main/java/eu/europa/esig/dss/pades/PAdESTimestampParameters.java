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
package eu.europa.esig.dss.pades;

import eu.europa.esig.dss.cades.signature.CAdESTimestampParameters;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.pdf.PAdESConstants;
import eu.europa.esig.dss.pdf.PdfSignatureCache;
import eu.europa.esig.dss.spi.DSSUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * Parameters for a PAdES timestamp creation
 *
 */
@SuppressWarnings("serial")
public class PAdESTimestampParameters extends CAdESTimestampParameters implements PAdESCommonParameters {

	/**
	 * The internal signature processing variable
	 */
	protected PdfSignatureCache pdfSignatureCache;
	
	/**
	 * Date of the timestamp
	 */
	protected Date timestampDate = new Date();
	
	/**
	 * This attribute defines a length of a reserved space for the timestamp inside a /Contents attribute
	 * 
	 * Default value is 9472 (from PDFBox)
	 */
	private int timestampSize = 9472;

	/**
	 * This attribute allows to override the used Filter for a Timestamp.
	 * 
	 * Default value is Adobe.PPKLite
	 */
	private String timestampFilter = PAdESConstants.TIMESTAMP_DEFAULT_FILTER;

	/**
	 * This attribute allows to override the used subFilter for a Timestamp.
	 * 
	 * Default value is ETSI.RFC3161
	 */
	private String timestampSubFilter = PAdESConstants.TIMESTAMP_DEFAULT_SUBFILTER;

	/**
	 * The signing app name
	 */
	private String appName;

	/**
	 * This attribute is used to create a visible timestamp in PAdES form
	 */
	private SignatureImageParameters timestampImageParameters;
	
	/**
	 * Password used to encrypt a PDF
	 */
	private char[] passwordProtection;

	/**
	 * Empty constructor
	 */
	public PAdESTimestampParameters() {
		// empty
	}

	/**
	 * Default constructor
	 *
	 * @param digestAlgorithm {@link DigestAlgorithm}
	 */
	public PAdESTimestampParameters(DigestAlgorithm digestAlgorithm) {
		super(digestAlgorithm);
	}

	/**
	 * The constructor is used internally to recreate parameters from CAdES Timestamp Parameters
	 *
	 * @param cadesTimestampParameters {@link CAdESTimestampParameters}
	 */
	PAdESTimestampParameters(CAdESTimestampParameters cadesTimestampParameters) {
		this(cadesTimestampParameters.getDigestAlgorithm());
	}

	@Override
	public String getFilter() {
		return timestampFilter;
	}

	/**
	 * Sets the filter
	 *
	 * @param timestampFilter {@link String}
	 */
	public void setFilter(String timestampFilter) {
		this.timestampFilter = timestampFilter;
	}

	@Override
	public String getSubFilter() {
		return timestampSubFilter;
	}

	/**
	 * Sets the sub filter
	 *
	 * @param timestampSubFilter {@link String}
	 */
	public void setSubFilter(String timestampSubFilter) {
		this.timestampSubFilter = timestampSubFilter;
	}

	@Override
	public String getAppName() {
		return appName;
	}

	/**
	 * Sets signing application name
	 *
	 * @param appName {@link String}
	 */
	public void setAppName(String appName) {
		this.appName = appName;
	}

	@Override
	public EncryptionAlgorithm getEncryptionAlgorithm() {
		// not implemented
		return null;
	}

	@Override
	public SignatureImageParameters getImageParameters() {
		if (timestampImageParameters == null) {
			timestampImageParameters = new SignatureImageParameters();
		}
		return timestampImageParameters;
	}

	/**
	 * Sets the {@code SignatureImageParameters} for a visual timestamp creation
	 *
	 * @param timestampImageParameters {@link SignatureImageParameters}
	 */
	public void setImageParameters(SignatureImageParameters timestampImageParameters) {
		this.timestampImageParameters = timestampImageParameters;
	}

	@Override
	public int getContentSize() {
		return timestampSize;
	}

	/**
	 * This setter allows reserving more than the default size for a timestamp
	 *
	 * Default : 9472 bytes
	 *
	 * @param timestampSize representing the reserved space for /Context element
	 */
	public void setContentSize(int timestampSize) {
		this.timestampSize = timestampSize;
	}

	@Override
	public Date getSigningDate() {
		return timestampDate;
	}

	@Override
	public char[] getPasswordProtection() {
		return passwordProtection;
	}

	/**
	 * Sets password to the document
	 *
	 * @param passwordProtection char array representing a password of the document
	 */
	public void setPasswordProtection(char[] passwordProtection) {
		this.passwordProtection = passwordProtection;
	}

	@Override
	public String getDeterministicId() {
		return DSSUtils.getDeterministicId(timestampDate, null);
	}

	@Override
	public PdfSignatureCache getPdfSignatureCache() {
		if (pdfSignatureCache == null) {
			pdfSignatureCache = new PdfSignatureCache();
		}
		return pdfSignatureCache;
	}

	@Override
	public void reinit() {
		pdfSignatureCache = null;
	}

	@Override
	public String toString() {
		return "PAdESTimestampParameters [" +
				"pdfSignatureCache=" + pdfSignatureCache +
				", timestampDate=" + timestampDate +
				", timestampSize=" + timestampSize +
				", timestampFilter='" + timestampFilter + '\'' +
				", timestampSubFilter='" + timestampSubFilter + '\'' +
				", appName='" + appName + '\'' +
				", timestampImageParameters=" + timestampImageParameters +
				", passwordProtection=" + Arrays.toString(passwordProtection) +
				"] " + super.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		PAdESTimestampParameters that = (PAdESTimestampParameters) o;
		return timestampSize == that.timestampSize
				&& Objects.equals(pdfSignatureCache, that.pdfSignatureCache)
				&& Objects.equals(timestampDate, that.timestampDate)
				&& Objects.equals(timestampFilter, that.timestampFilter)
				&& Objects.equals(timestampSubFilter, that.timestampSubFilter)
				&& Objects.equals(appName, that.appName)
				&& Objects.equals(timestampImageParameters, that.timestampImageParameters)
				&& Arrays.equals(passwordProtection, that.passwordProtection);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + Objects.hashCode(pdfSignatureCache);
		result = 31 * result + Objects.hashCode(timestampDate);
		result = 31 * result + timestampSize;
		result = 31 * result + Objects.hashCode(timestampFilter);
		result = 31 * result + Objects.hashCode(timestampSubFilter);
		result = 31 * result + Objects.hashCode(appName);
		result = 31 * result + Objects.hashCode(timestampImageParameters);
		result = 31 * result + Arrays.hashCode(passwordProtection);
		return result;
	}

}
