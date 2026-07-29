<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:output method="xml" indent="yes" encoding="UTF-8" />

  <xsl:mode on-no-match="shallow-copy" />

  <xsl:template match="mods:subject[mods:topic and not(*[not(self::mods:topic)])]">
    <xsl:for-each select="mods:topic">
      <mods:subject>
        <!-- ignore xlink:type -->
        <xsl:copy-of select="../@*[not(name()='xlink:type')]" />
        <xsl:copy-of select="." />
      </mods:subject>
    </xsl:for-each>
  </xsl:template>

</xsl:stylesheet>
