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

package de.gbv.reposis.classification;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * Cached replacements for {@link MCRXMLFunctions#getDisplayName(String, String)},
 * {@link MCRXMLFunctions#getDisplayName(String, String, String)} and
 * {@link MCRXMLFunctions#isCategoryID(String, String)}.
 * <p>
 * The original functions query the database twice per call. They are called for every facet value and every badge
 * on the search page, which leads to several hundred queries per request. This class loads each classification once
 * as a whole and keeps its categories in memory. A cached classification is reloaded as soon as
 * {@link MCRCategoryDAO#getLastModified(String)} reports a change. Unknown classifications are cached as well,
 * because MIR also calls {@code isCategoryID} for facets that are not backed by a classification.
 * <p>
 * The methods are public static with String parameters, so they can be used both via the
 * {@code xalan://de.gbv.reposis.classification.ReposisClassificationFunctions} namespace and via
 * {@code callJava:de.gbv.reposis.classification.ReposisClassificationFunctions:...}.
 */
public final class ReposisClassificationFunctions {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, CachedClassification> CACHE = new ConcurrentHashMap<>();

    private ReposisClassificationFunctions() {
    }

    /**
     * Returns the label of the given category in the current language.
     *
     * @param classificationId the id of the classification
     * @param categoryId the category id
     * @return the label in the current language or an empty string if the category does not exist
     */
    public static String getDisplayName(String classificationId, String categoryId) {
        try {
            MCRCategoryID categID = new MCRCategoryID(classificationId, categoryId);
            return Optional.ofNullable(getCachedCategory(categID))
                .flatMap(MCRCategory::getCurrentLabel)
                .map(MCRLabel::getText)
                .orElse("");
        } catch (Throwable e) {
            LOGGER.error("Could not determine display name for classification id {} and category id {}",
                classificationId, categoryId, e);
            return "";
        }
    }

    /**
     * Returns the label of the given category in the desired language.
     *
     * @param classificationId the id of the classification
     * @param categoryId the category id
     * @param lang the desired language
     * @return the label in the desired language, if there is none the label in the current language, if the
     *         category does not exist an empty string
     */
    public static String getDisplayName(String classificationId, String categoryId, String lang) {
        try {
            MCRCategoryID categID = new MCRCategoryID(classificationId, categoryId);
            MCRCategory category = getCachedCategory(categID);
            Optional<MCRLabel> label = category.getLabel(lang);
            return label.isEmpty() ? getDisplayName(classificationId, categoryId) : label.get().getText();
        } catch (Throwable e) {
            LOGGER.error("Could not determine display name for classification id {} and category id {}",
                classificationId, categoryId, e);
            return "";
        }
    }

    /**
     * Checks whether the given category exists.
     *
     * @param classificationId the id of the classification
     * @param categoryId the category id
     * @return <code>true</code> if the category exists, otherwise <code>false</code>
     */
    public static boolean isCategoryID(String classificationId, String categoryId) {
        if (classificationId == null || classificationId.isEmpty()) {
            LOGGER.debug("Classification identifier is null or empty");
            return false;
        }

        if (categoryId == null || categoryId.isEmpty()) {
            LOGGER.debug("Category identifier is null or empty");
            return false;
        }

        if (classificationId.length() > MCRCategoryID.ROOT_ID_LENGTH) {
            LOGGER.debug("Could not determine state for classification id {} and category id {}", classificationId,
                categoryId);
            return false;
        }

        MCRCategory category = null;
        try {
            MCRCategoryID categID = MCRCategoryID.fromString(classificationId + ":" + categoryId);
            category = getCachedCategory(categID);
        } catch (Throwable e) {
            LOGGER.error("Could not determine state for classification id {} and category id {}", classificationId,
                categoryId, e);
        }

        return category != null;
    }

    private static MCRCategory getCachedCategory(MCRCategoryID categID) {
        String rootId = categID.getRootID();
        MCRCategoryDAO dao = MCRCategoryDAOFactory.getInstance();
        long lastModified = dao.getLastModified(rootId);
        CachedClassification cached = CACHE.get(rootId);
        if (cached == null || cached.lastModified() != lastModified) {
            cached = new CachedClassification(lastModified, loadCategories(dao, rootId));
            CACHE.put(rootId, cached);
        }
        return cached.categories().get(categID.isRootID() ? "" : categID.getId());
    }

    private static Map<String, MCRCategory> loadCategories(MCRCategoryDAO dao, String rootId) {
        MCRCategoryID rootID = MCRCategoryID.rootID(rootId);
        if (!dao.exist(rootID)) {
            LOGGER.debug("Classification {} does not exist, caching empty result", rootId);
            return Collections.emptyMap();
        }
        MCRCategory root = dao.getCategory(rootID, -1);
        if (root == null) {
            return Collections.emptyMap();
        }
        Map<String, MCRCategory> categories = new HashMap<>();
        Deque<MCRCategory> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            MCRCategory category = stack.pop();
            MCRCategoryID id = category.getId();
            categories.put(id.isRootID() ? "" : id.getId(), category);
            category.getChildren().forEach(stack::push);
        }
        LOGGER.debug("Cached {} categories of classification {}", categories.size(), rootId);
        return Collections.unmodifiableMap(categories);
    }

    private record CachedClassification(long lastModified, Map<String, MCRCategory> categories) {
    }
}
