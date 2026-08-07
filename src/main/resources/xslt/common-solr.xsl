<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:repgeoutils="http://www.gbv.de/xslt/geoutils"
  xmlns:mcrproperty="http://www.mycore.de/xslt/property"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:import href="xslImport:solr-document:common-solr.xsl" />

  <xsl:template match="mycoreobject[contains(@ID,'_mods_')]">
    <xsl:apply-templates select="metadata/def.modsContainer/modsContainer/mods:mods" mode="common" />
    <xsl:apply-imports />
  </xsl:template>

  <xsl:template match="mods:mods" mode="common">
    <xsl:variable name="coordinates" select="mods:subject/mods:cartographics/mods:coordinates" />

    <xsl:if test="exists($coordinates)">
      <xsl:variable name="coordinates-string" select="repgeoutils:get-normalized-wkt-string($coordinates)" />
      <xsl:if test="normalize-space($coordinates-string)">
        <field name="{mcrproperty:one('MCR.GeoSearch.Solr.WKT.Field')}">
          <xsl:value-of select="$coordinates-string"/>
        </field>
      </xsl:if>
    </xsl:if>

    <xsl:apply-templates select="$coordinates" mode="common" />
  </xsl:template>

</xsl:stylesheet>
