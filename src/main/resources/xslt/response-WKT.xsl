<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcrproperty="http://www.mycore.de/xslt/property"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:output method="text" indent="yes" media-type="text/plain" />

  <xsl:include href="resource:xslt/default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />

  <xsl:variable name="wkt-field" select="mcrproperty:one('MCR.GeoSearch.Solr.WKT.Field')" />

  <xsl:template match="/response">
    <xsl:text>GEOMETRYCOLLECTION(</xsl:text>
    <xsl:apply-templates select="
      result/doc[count(arr[@name=$wkt-field]/str[string-length(normalize-space(string(text()))) &gt; 0]) &gt; 0]
    " />
    <xsl:text>)</xsl:text>
  </xsl:template>

  <xsl:template match="doc">
    <xsl:for-each select="arr[@name=$wkt-field]/str[string-length(normalize-space(string(text()))) &gt; 0]">
      <xsl:value-of select="text()" />
      <xsl:if test="not(position() = last())">
        <xsl:text>,</xsl:text>
      </xsl:if>
    </xsl:for-each>
    <xsl:if test="not(position() = last())">
      <xsl:text>,</xsl:text>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
