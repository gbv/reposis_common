/*
 * $Id$
 * $Revision: 34897 $ $Date: 2016-03-18 17:14:12 +0100 (Fr, 18 Mär 2016) $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package de.gbv.reposis.metrics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.crypt.MCRCryptKeyFileNotFoundException;
import org.mycore.crypt.MCRCryptKeyNoPermissionException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;

import java.util.List;

/**
 * update the mods
 *
 * @author Paul Borchert
 * @author Sebastian Hofmann
 */
public class MCRUpdateJournalMetricsEventHandler extends MCREventHandlerBase {

    private final static Logger LOGGER = LogManager.getLogger(MCRUpdateJournalMetricsEventHandler.class);

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectCreated(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectCreated(final MCREvent evt, final MCRObject obj) {
        if (!MCRMODSWrapper.isSupported(obj)) {
            return;
        }
        MCRMODSWrapper newObjMw = new MCRMODSWrapper(obj);
        List<String> issnNew = MCRMODSJournalMetricsHelper.getIssn(newObjMw);
        try {
            MCRMODSJournalMetricsHelper.updateMetrics(issnNew, newObjMw);
        } catch (MCRCryptKeyNoPermissionException | MCRCryptKeyFileNotFoundException e) {
            throw new MCRException("Error while updating metrics!", e);
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectUpdated(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectUpdated(final MCREvent evt, final MCRObject obj) {
        if (!MCRMODSWrapper.isSupported(obj)) {
            return;
        }
        // only update if there is a new issn or one got removed
        MCRObject oldObj = MCRMetadataManager.retrieveMCRObject(obj.getId());
        List<String> issnOld = MCRMODSJournalMetricsHelper.getIssn(new MCRMODSWrapper(oldObj));
        issnOld.sort(String::compareTo);
        MCRMODSWrapper newObjMw = new MCRMODSWrapper(obj);
        List<String> issnNew = MCRMODSJournalMetricsHelper.getIssn(newObjMw);
        issnNew.sort(String::compareTo);

        if (!issnOld.equals(issnNew)) {
            try {
                MCRMODSJournalMetricsHelper.updateMetrics(issnNew, newObjMw);
            } catch (MCRCryptKeyNoPermissionException | MCRCryptKeyFileNotFoundException e) {
                throw new MCRException("Error while updating metrics!", e);
            }
        }
    }

}
