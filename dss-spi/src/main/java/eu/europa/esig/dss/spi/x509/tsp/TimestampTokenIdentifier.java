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
package eu.europa.esig.dss.spi.x509.tsp;

import eu.europa.esig.dss.model.identifier.TokenIdentifier;

/**
 * Identifier for a timestamp token
 *
 */
public final class TimestampTokenIdentifier extends TokenIdentifier {

	private static final long serialVersionUID = 4260120806950705848L;

	/** Default identifier prefix for a time-stamp token */
	private static final String TIMESTAMP_TOKEN_PREFIX = "T-";

	/**
	 * Default constructor
	 *
	 * @param timestampToken {@link TimestampToken}
	 */
	public TimestampTokenIdentifier(TimestampToken timestampToken) {
		super(TIMESTAMP_TOKEN_PREFIX, timestampToken);
	}

	/**
	 * Constructor to build a time-stamp identifier from custom binaries
	 *
	 * @param binaries byte array representing the time-stamp token
	 */
	public TimestampTokenIdentifier(byte[] binaries) {
		super(TIMESTAMP_TOKEN_PREFIX, binaries);
	}

}
