<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:repgeoutils="http://www.gbv.de/xslt/geoutils"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:function name="repgeoutils:get-normalized-wkt-string" as="xs:string">
    <xsl:param name="input" as="element(mods:coordinates)*" />

    <xsl:variable name="request" select="string-join($input ! encode-for-uri(normalize-space(string())), ',')" />
    <xsl:sequence select="document('normalizeWKT:' || $request)/string" />
  </xsl:function>

</xsl:stylesheet>
