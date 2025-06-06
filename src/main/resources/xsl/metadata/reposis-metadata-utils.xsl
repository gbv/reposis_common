<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template name="print-field">
    <xsl:param name="i18n"/>
    <xsl:param name="pre-value"/>
    <xsl:param name="value"/>
    <dt>
      <xsl:value-of select="document(concat('i18n:', $i18n))"/>
    </dt>
    <dd>
      <xsl:choose>
        <xsl:when test="$pre-value and $value">
          <strong>
            <xsl:value-of select="$pre-value"/>
          </strong>
          <xsl:text> </xsl:text>
        </xsl:when>
        <xsl:when test="$pre-value">
          <xsl:value-of select="$pre-value"/>
        </xsl:when>
      </xsl:choose>
      <xsl:if test="$value">
        <xsl:copy-of select="$value"/>
      </xsl:if>
    </dd>
  </xsl:template>

  <xsl:template name="build-link">
    <xsl:param name="url"/>
    <xsl:param name="text" select="$url"/>
    <a href="{$url}">
      <xsl:value-of select="$text"/>
    </a>
  </xsl:template>

  <xsl:template name="concat">
    <xsl:param name="input"/>
    <xsl:for-each select="$input">
      <xsl:copy-of select="."/>
      <xsl:if test="position() != last()">
        <xsl:text>, </xsl:text>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="get-classification-label">
    <xsl:param name="classification"/>
    <xsl:value-of select="document(concat('classification:metadata:0:children:', $classification))//category/label[@xml:lang=$CurrentLang]/@text"/>
  </xsl:template>
</xsl:stylesheet>
