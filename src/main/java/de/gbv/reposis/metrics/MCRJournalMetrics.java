/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.gbv.reposis.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MCRJournalMetrics {

    public MCRJournalMetrics() {
        this.JCR = new HashMap<>();
        this.SJR = new HashMap<>();
        this.snip = new HashMap<>();
    }

    private Map<Integer, Double> snip;

    private Map<Integer, Double> SJR;

    private Map<Integer, Double> JCR;

    public Map<Integer, Double> getJCR() {
        return JCR;
    }

    public Map<Integer, Double> getSJR() {
        return SJR;
    }

    public Map<Integer, Double> getSnip() {
        return snip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MCRJournalMetrics that = (MCRJournalMetrics) o;
        return getSnip().equals(that.getSnip()) && getSJR().equals(that.getSJR()) && getJCR().equals(that.getJCR());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSnip(), getSJR(), getJCR());
    }
}
