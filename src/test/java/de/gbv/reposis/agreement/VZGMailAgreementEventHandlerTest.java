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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "MCR.Metadata.Type.object", string = "true"),
        @MCRTestProperty(key = "MCR.mir-module.EditorMail", string = "editor@example.org"),
    }
)
public class VZGMailAgreementEventHandlerTest {

    @Test
    public void getHTMLPart() {
        final VZGMailAgreementEventHandler handler = new VZGMailAgreementEventHandler();

        final MCRObject obj = new MCRObject();

        obj.setId(MCRObjectID.getInstance("mir_object_00000001"));

        final Document htmlPart = handler.getHTMLPart(obj);

        final String html = new XMLOutputter().outputString(htmlPart);
        System.out.println(html);

        Assertions.assertTrue(html.contains("receive/mir_object_00000001"), "Mail should contain the link");
        Assertions.assertTrue(html.contains("guest"), "Mail should contain the user");
    }

    @Test
    public void getRecipientFallsBackToEditorMail() {
        Assertions.assertEquals("editor@example.org", VZGMailAgreementEventHandler.getRecipient());
    }

    @Test
    public void getRecipientUsesMailTo() {
        MCRConfiguration2.set("MIR.Agreement.MailTo", "agreement@example.org");
        Assertions.assertEquals("agreement@example.org", VZGMailAgreementEventHandler.getRecipient());
    }

    @Test
    public void getAttachmentURLFallsBackToPublishFolder() {
        final String url = VZGMailAgreementEventHandler.getAttachmentURL("agreement.pdf");
        Assertions.assertTrue(url.endsWith("content/publish/agreement.pdf"));
    }

    @Test
    public void getAttachmentURLUsesConfiguredURL() {
        MCRConfiguration2.set("MIR.Agreement.File.URL", "https://example.org/api/v2/agreement.pdf");
        Assertions.assertEquals("https://example.org/api/v2/agreement.pdf",
            VZGMailAgreementEventHandler.getAttachmentURL("agreement.pdf"));
    }

}
