package de.gbv.reposis.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.frontend.servlets.MCRContentServlet;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PropertyHelperContentServlet extends MCRContentServlet {

    @Override
    public MCRContent getContent(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {

        if (!MCRSessionMgr.getCurrentSession().getUserInformation().getUserID()
            .equals(MCRSystemUserInformation.getSuperUserInstance().getUserID())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        if(!"analyze".equals(req.getParameter("action"))) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        PropertyHelper propertyHelper = new PropertyHelper();
        Map<String, List<Property>> map = propertyHelper.analyzeProperties();

        Element propertiesElement = new Element("properties-analyze");
        for (String component : map.keySet()) {
            Element componentElement = new Element("component");
            componentElement.setAttribute("name", component);
            propertiesElement.addContent(componentElement);
            for (Property property : map.get(component)) {
                Element propertyElement = new Element("property");
                propertyElement.setAttribute("name", property.getProperty());

                if(property.getOld() != null) {
                    propertyElement.setAttribute("oldValue", property.getOld());

                }

                propertyElement.setAttribute("newValue", property.get_new());
                propertyElement.setAttribute("component", property.getComponent());
                componentElement.addContent(propertyElement);
            }
        }

        try {
            return getLayoutService().getTransformedContent(req, resp, new MCRJDOMContent(propertiesElement));
        } catch (TransformerException|SAXException e) {
            throw new MCRException(e);
        }
    }
}
