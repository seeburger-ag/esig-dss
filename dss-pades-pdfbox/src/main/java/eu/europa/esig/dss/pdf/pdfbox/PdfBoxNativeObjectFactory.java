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
package eu.europa.esig.dss.pdf.pdfbox;


import eu.europa.esig.dss.pdf.AbstractPdfObjFactory;
import eu.europa.esig.dss.pdf.PDFServiceMode;
import eu.europa.esig.dss.pdf.PDFSignatureService;
import eu.europa.esig.dss.pdf.pdfbox.visible.nativedrawer.PdfBoxNativeSignatureDrawerFactory;

/**
 * The PDFBox native implementation of {@code IPdfObjFactory}
 * Creates text content in its native representation
 */
public class PdfBoxNativeObjectFactory extends AbstractPdfObjFactory {

	/**
	 * Default constructor
	 */
	public PdfBoxNativeObjectFactory() {
		// empty
	}

	@Override
	public PDFSignatureService newPAdESSignatureService() {
		return configure(new PdfBoxSignatureService(PDFServiceMode.SIGNATURE, new PdfBoxNativeSignatureDrawerFactory()));
	}

	@Override
	public PDFSignatureService newContentTimestampService() {
		return configure(new PdfBoxSignatureService(PDFServiceMode.CONTENT_TIMESTAMP, new PdfBoxNativeSignatureDrawerFactory()));
	}

	@Override
	public PDFSignatureService newSignatureTimestampService() {
		return configure(new PdfBoxSignatureService(PDFServiceMode.SIGNATURE_TIMESTAMP, new PdfBoxNativeSignatureDrawerFactory()));
	}

	@Override
	public PDFSignatureService newArchiveTimestampService() {
		return configure(new PdfBoxSignatureService(PDFServiceMode.ARCHIVE_TIMESTAMP, new PdfBoxNativeSignatureDrawerFactory()));
	}

}
