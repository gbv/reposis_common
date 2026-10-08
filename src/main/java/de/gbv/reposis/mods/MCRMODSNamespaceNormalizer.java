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

package de.gbv.reposis.mods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * Bundles the namespace declarations of an XML fragment at its root element, like
 * {@code MCROAIDataProvider#moveNamespacesUp(Element)} does for OAI responses, and drops the declarations that are not
 * needed any more.
 * <p>
 * A namespace that is used by an element or an attribute of the fragment is declared at the root element, unless it is
 * already in scope there, for example because the surrounding mycoreobject element declares it. Below the root element
 * only those declarations survive that bind a prefix to another namespace than the one in scope and are used in the
 * subtree of the declaring element. Declarations that nothing uses, like the xalan and exslt leftovers of former XSL
 * transformations, are removed.
 * <p>
 * The namespace of an element or an attribute is never changed, only where it is declared.
 */
public final class MCRMODSNamespaceNormalizer {

    private MCRMODSNamespaceNormalizer() {
    }

    /**
     * Declares the used namespaces at <code>root</code> and removes the redundant and the unused declarations below
     * it.
     *
     * @param root the element the declarations are bundled at, for a MyCoRe object the mods element and not the
     *            mycoreobject element
     * @return true if the fragment was modified
     */
    public static boolean normalize(Element root) {
        Map<Element, Set<NamespaceKey>> usedNamespaces = new IdentityHashMap<>();
        collectUsedNamespaces(root, usedNamespaces);

        Map<String, String> namespacesInScope = inheritedNamespaces(root);
        namespacesInScope.put(root.getNamespacePrefix(), root.getNamespaceURI());

        boolean changed = declareUsedNamespaces(root, usedNamespaces.get(root), namespacesInScope);
        changed = removeUnneededDeclarations(root, usedNamespaces.get(root), namespacesInScope) || changed;
        for (Namespace namespace : root.getAdditionalNamespaces()) {
            namespacesInScope.put(namespace.getPrefix(), namespace.getURI());
        }
        for (Attribute attribute : root.getAttributes()) {
            namespacesInScope.putIfAbsent(attribute.getNamespacePrefix(), attribute.getNamespaceURI());
        }

        for (Element child : root.getChildren()) {
            changed = cleanUp(child, namespacesInScope, usedNamespaces) || changed;
        }
        return changed;
    }

    /**
     * Collects the namespaces that are used by an element or one of its descendants, either by the elements themselves
     * or by their attributes.
     */
    private static Set<NamespaceKey> collectUsedNamespaces(Element element,
        Map<Element, Set<NamespaceKey>> usedNamespaces) {
        Set<NamespaceKey> used = new HashSet<>();
        used.add(NamespaceKey.of(element.getNamespace()));
        for (Attribute attribute : element.getAttributes()) {
            used.add(NamespaceKey.of(attribute.getNamespace()));
        }
        for (Element child : element.getChildren()) {
            used.addAll(collectUsedNamespaces(child, usedNamespaces));
        }
        usedNamespaces.put(element, used);
        return used;
    }

    /**
     * Declares every used namespace at the root element that is not in scope there yet.
     */
    private static boolean declareUsedNamespaces(Element root, Set<NamespaceKey> usedNamespaces,
        Map<String, String> namespacesInScope) {
        Map<String, String> declared = new HashMap<>(namespacesInScope);
        for (Namespace namespace : root.getAdditionalNamespaces()) {
            declared.put(namespace.getPrefix(), namespace.getURI());
        }

        boolean changed = false;
        for (NamespaceKey namespace : sorted(usedNamespaces)) {
            if (!namespace.isDeclarable() || declared.containsKey(namespace.prefix())) {
                continue;
            }
            root.addNamespaceDeclaration(Namespace.getNamespace(namespace.prefix(), namespace.uri()));
            declared.put(namespace.prefix(), namespace.uri());
            changed = true;
        }
        return changed;
    }

    /**
     * Removes the declarations of an element that neither the element nor one of its descendants uses.
     */
    private static boolean removeUnneededDeclarations(Element element, Set<NamespaceKey> usedNamespaces,
        Map<String, String> namespacesInScope) {
        boolean changed = false;
        for (Namespace namespace : List.copyOf(element.getAdditionalNamespaces())) {
            NamespaceKey key = NamespaceKey.of(namespace);
            boolean redundant = key.uri().equals(namespacesInScope.get(key.prefix()));
            if (redundant || !usedNamespaces.contains(key)) {
                element.removeNamespaceDeclaration(namespace);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean cleanUp(Element element, Map<String, String> inheritedNamespaces,
        Map<Element, Set<NamespaceKey>> usedNamespaces) {
        Map<String, String> namespacesInScope = new HashMap<>(inheritedNamespaces);
        namespacesInScope.put(element.getNamespacePrefix(), element.getNamespaceURI());

        boolean changed = removeUnneededDeclarations(element, usedNamespaces.get(element), namespacesInScope);
        for (Namespace namespace : element.getAdditionalNamespaces()) {
            namespacesInScope.put(namespace.getPrefix(), namespace.getURI());
        }
        for (Attribute attribute : element.getAttributes()) {
            namespacesInScope.putIfAbsent(attribute.getNamespacePrefix(), attribute.getNamespaceURI());
        }

        for (Element child : element.getChildren()) {
            changed = cleanUp(child, namespacesInScope, usedNamespaces) || changed;
        }
        return changed;
    }

    /**
     * Returns the namespaces that are in scope at the given element because of its ancestors, mapped by their prefix.
     * A mods element usually inherits the xlink and the xsi namespace from its mycoreobject element.
     */
    private static Map<String, String> inheritedNamespaces(Element element) {
        List<Element> ancestors = new ArrayList<>();
        for (Element ancestor = element.getParentElement(); ancestor != null; ancestor = ancestor
            .getParentElement()) {
            ancestors.add(ancestor);
        }

        Map<String, String> namespaces = new HashMap<>();
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            Element ancestor = ancestors.get(i);
            namespaces.put(ancestor.getNamespacePrefix(), ancestor.getNamespaceURI());
            for (Namespace namespace : ancestor.getAdditionalNamespaces()) {
                namespaces.put(namespace.getPrefix(), namespace.getURI());
            }
            for (Attribute attribute : ancestor.getAttributes()) {
                namespaces.putIfAbsent(attribute.getNamespacePrefix(), attribute.getNamespaceURI());
            }
        }
        return namespaces;
    }

    /**
     * Sorts the namespaces by prefix and uri, so that the declarations of an object do not depend on the iteration
     * order of a hash set.
     */
    private static List<NamespaceKey> sorted(Set<NamespaceKey> namespaces) {
        List<NamespaceKey> sorted = new ArrayList<>(namespaces);
        sorted.sort((left, right) -> {
            int byPrefix = left.prefix().compareTo(right.prefix());
            return byPrefix != 0 ? byPrefix : left.uri().compareTo(right.uri());
        });
        return sorted;
    }

    /**
     * A namespace as prefix and uri, unlike {@link Namespace} it compares the prefix too.
     */
    private record NamespaceKey(String prefix, String uri) {

        static NamespaceKey of(Namespace namespace) {
            return new NamespaceKey(namespace.getPrefix(), namespace.getURI());
        }

        /**
         * The empty and the xml namespace are always in scope and must not be declared.
         */
        boolean isDeclarable() {
            return !uri.isEmpty() && !Namespace.XML_NAMESPACE.getURI().equals(uri);
        }
    }
}
