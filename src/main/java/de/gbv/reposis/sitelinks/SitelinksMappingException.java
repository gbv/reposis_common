/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.gbv.reposis.sitelinks;

import java.io.Serial;

import org.mycore.common.MCRException;

/**
 * Exception thrown when an error occurs while mapping sitelinks page.
 * <p>
 * This can happen, for example, if the data is inconsistent or
 * the mapper encounters unexpected content.
 */
public class SitelinksMappingException extends MCRException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new SitelinksMappingException with the specified detail message.
     *
     * @param message the detail message
     */
    public SitelinksMappingException(String message) {
        super(message);
    }

    /**
     * Constructs a new SitelinksMappingException with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public SitelinksMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
