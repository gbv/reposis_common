<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:import href="xslImport:modsmeta:metadata/rep-citation-pdf.xsl" />

  <xsl:param name="ServletsBaseURL" />

  <xsl:key use="@id" name="rights" match="/mycoreobject/rights/right" />

  <!-- Remove this stylesheet as soon as MIR provides citation_pdf_url. -->
  <xsl:template match="/">
    <citation_meta>
      <xsl:apply-templates mode="citation-pdf-url"
        select="(mycoreobject/structure/derobjects/derobject
          [classification[@classid='derivate_types' and @categid='content']]
          [key('rights', @xlink:href)/@read]
          [ends-with(lower-case(maindoc), '.pdf')])[1]" />
    </citation_meta>
    <xsl:apply-imports />
  </xsl:template>

  <xsl:template match="derobject" mode="citation-pdf-url">
    <xsl:variable name="path" select="replace(string(maindoc), '^/', '')" />
    <meta name="citation_pdf_url"
      content="{$ServletsBaseURL}MCRFileNodeServlet/{@xlink:href}/{string-join(tokenize($path, '/') ! encode-for-uri(.), '/')}" />
  </xsl:template>

</xsl:stylesheet>
