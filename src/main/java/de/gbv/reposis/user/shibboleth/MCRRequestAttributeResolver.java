package de.gbv.reposis.user.shibboleth;

/**
 * This interface is used to resolved attributes from a request.
 * It is used by {@link MCRShibbolethUserMapper} to resolve attributes from the request, so that the mapper does not have to know the Servlet API.
 */
public interface MCRRequestAttributeResolver {
    String resolve(String attribute);
}
