<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="mcrxsl xlink">

  <xsl:import href="xslImport:modsmeta:metadata/rep-citation-pdf.xsl" />

  <xsl:param name="ServletsBaseURL" />

  <xsl:key use="@id" name="rights" match="/mycoreobject/rights/right" />

  <!-- Remove this stylesheet as soon as MIR provides citation_pdf_url. -->
  <xsl:template match="/">
    <citation_meta>
      <xsl:apply-templates mode="citation-pdf-url"
        select="mycoreobject/structure/derobjects/derobject[classification[@classid='derivate_types' and @categid='content']][key('rights', @xlink:href)/@read][1]" />
    </citation_meta>
    <xsl:apply-imports />
  </xsl:template>

  <xsl:template match="derobject" mode="citation-pdf-url">
    <xsl:variable name="maindoc" select="string(maindoc)" />
    <xsl:variable name="extension" select="substring($maindoc, string-length($maindoc) - 3)" />
    <xsl:if test="translate($extension, 'PDF', 'pdf') = '.pdf'">
      <xsl:variable name="path">
        <xsl:if test="not(starts-with($maindoc, '/'))">
          <xsl:text>/</xsl:text>
        </xsl:if>
        <xsl:value-of select="$maindoc" />
      </xsl:variable>
      <meta name="citation_pdf_url"
        content="{$ServletsBaseURL}MCRFileNodeServlet/{@xlink:href}{mcrxsl:encodeURIPath(string($path))}" />
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
