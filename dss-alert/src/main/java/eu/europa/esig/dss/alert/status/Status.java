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
package eu.europa.esig.dss.alert.status;

import java.util.Collection;

/**
 * Contains information about the occurred event
 *
 */
public interface Status {

    /**
     * Returns the error message describing the occurred event
     *
     * @return {@link String}
     */
    String getMessage();

    /**
     * Returns a collection of object identifiers associated with the event
     *
     * @return a collection of {@link String} object ids associated with the event
     */
    Collection<String> getRelatedObjectIds();

    /**
     * Returns whether the object is not filled (all values are null)
     *
     * @return TRUE if the Status is empty (underlying even did not occur), FALSE otherwise (event occurred)
     */
    boolean isEmpty();

    /**
     * This method returns a complete error message computed from the main {@code message} and
     * subMessages from the different objects
     *
     * @return {@link String}
     */
    String getErrorString();

}
