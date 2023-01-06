<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
        version="1.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:mods="http://www.loc.gov/mods/v3"
        xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions"
        xmlns:mir="http://www.mycore.de/mir"
        xmlns:xlink="http://www.w3.org/1999/xlink"
        xmlns:geofn="xalan://de.gbv.reposis.geo.GeoFunctions"
        exclude-result-prefixes="mods mcrxsl xlink geofn"
>
    <xsl:import href="xslImport:solr-document:common-solr.xsl" />
    <xsl:param name="MCR.GeoSearch.Solr.WKT.Field" />

    <xsl:template match="mycoreobject[contains(@ID,'_mods_')]">
        <xsl:apply-templates select="metadata/def.modsContainer/modsContainer/mods:mods" mode="common" />
        <xsl:apply-imports />
    </xsl:template>

    <xsl:template match="mods:mods" mode="common">
        <xsl:apply-templates select="mods:subject/mods:cartographics/mods:coordinates" mode="common" />
    </xsl:template>


    <xsl:template match="mods:subject/mods:cartographics/mods:coordinates" mode="common">
        <field name="{$MCR.GeoSearch.Solr.WKT.Field}">
            <xsl:value-of select="geofn:getNormalizedWKTString(string(text()))"/>
        </field>
    </xsl:template>


</xsl:stylesheet>