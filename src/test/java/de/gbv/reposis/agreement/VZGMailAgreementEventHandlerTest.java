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

package de.gbv.reposis.agreement;

import org.jdom2.Document;
import org.jdom2.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

import java.util.Map;

public class VZGMailAgreementEventHandlerTest extends MCRTestCase {

    @Test
    public void getHTMLPart() {
        final VZGMailAgreementEventHandler handler = new VZGMailAgreementEventHandler();

        final MCRObject obj = new MCRObject();

        obj.setId(MCRObjectID.getInstance("mir_object_00000001"));

        final Document htmlPart = handler.getHTMLPart(obj);

        final String html = new XMLOutputter().outputString(htmlPart);
        System.out.println(html);

        Assert.assertTrue("Mail should contain the link", html.contains("receive/mir_object_00000001"));
        Assert.assertTrue("Mail should contain the user", html.contains("guest"));
    }

    @Test
    public void getRecipientFallsBackToEditorMail() {
        Assert.assertEquals("editor@example.org", VZGMailAgreementEventHandler.getRecipient());
    }

    @Test
    public void getRecipientUsesMailTo() {
        MCRConfiguration2.set("MIR.Agreement.MailTo", "agreement@example.org");
        Assert.assertEquals("agreement@example.org", VZGMailAgreementEventHandler.getRecipient());
    }

    @Test
    public void getAttachmentURLFallsBackToPublishFolder() {
        final String url = VZGMailAgreementEventHandler.getAttachmentURL("agreement.pdf");
        Assert.assertTrue(url.endsWith("content/publish/agreement.pdf"));
    }

    @Test
    public void getAttachmentURLUsesConfiguredURL() {
        MCRConfiguration2.set("MIR.Agreement.File.URL", "https://example.org/api/v2/agreement.pdf");
        Assert.assertEquals("https://example.org/api/v2/agreement.pdf",
            VZGMailAgreementEventHandler.getAttachmentURL("agreement.pdf"));
    }

    @Override
    protected Map<String, String> getTestProperties() {
        final Map<String, String> testProperties = super.getTestProperties();

        testProperties.put("MCR.Metadata.Type.object", Boolean.TRUE.toString());
        testProperties.put("MCR.mir-module.EditorMail", "editor@example.org");


        return testProperties;
    }
}