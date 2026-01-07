<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:param name="Sitelinks.PageSize" />

  <xsl:template name="month-name">
    <xsl:param name="month-number" />
    <xsl:choose>
      <xsl:when test="$month-number = 1">January</xsl:when>
      <xsl:when test="$month-number = 2">February</xsl:when>
      <xsl:when test="$month-number = 3">March</xsl:when>
      <xsl:when test="$month-number = 4">April</xsl:when>
      <xsl:when test="$month-number = 5">May</xsl:when>
      <xsl:when test="$month-number = 6">June</xsl:when>
      <xsl:when test="$month-number = 7">July</xsl:when>
      <xsl:when test="$month-number = 8">August</xsl:when>
      <xsl:when test="$month-number = 9">September</xsl:when>
      <xsl:when test="$month-number = 10">October</xsl:when>
      <xsl:when test="$month-number = 11">November</xsl:when>
      <xsl:when test="$month-number = 12">December</xsl:when>
      <xsl:otherwise>Invalid month number</xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="max-page-number">
    <xsl:choose>
      <xsl:when test="(@totalCount mod $Sitelinks.PageSize) = 0">
        <xsl:value-of select="floor(@totalCount div $Sitelinks.PageSize)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="floor(@totalCount div $Sitelinks.PageSize) + 1" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
