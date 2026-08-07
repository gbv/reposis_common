<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:template name="print-field">
    <xsl:param name="i18n" />
    <xsl:param name="pre-value" />
    <xsl:param name="value" />
    <dt>
      <xsl:value-of select="mcri18n:translate($i18n)" />
    </dt>
    <dd>
      <xsl:choose>
        <xsl:when test="$pre-value and $value">
          <strong>
            <xsl:value-of select="$pre-value" />
          </strong>
          <xsl:text> </xsl:text>
        </xsl:when>
        <xsl:when test="$pre-value">
          <xsl:value-of select="$pre-value" />
        </xsl:when>
      </xsl:choose>
      <xsl:if test="$value">
        <xsl:copy-of select="$value" />
      </xsl:if>
    </dd>
  </xsl:template>

  <xsl:template name="build-link">
    <xsl:param name="url" />
    <xsl:param name="text" select="$url" />
    <a href="{$url}">
      <xsl:value-of select="$text" />
    </a>
  </xsl:template>

  <xsl:template name="concat">
    <xsl:param name="input" />
    <xsl:for-each select="$input">
      <xsl:copy-of select="." />
      <xsl:if test="position() != last()">
        <xsl:text>, </xsl:text>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

</xsl:stylesheet>
