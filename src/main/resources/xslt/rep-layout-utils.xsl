<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:template name="rep.generate-single-menu-entry">
    <xsl:param name="menu-id" />
    <xsl:param name="menu-item" />
    <xsl:param name="browser-address" />

    <li class="nav-item">
      <xsl:variable name="active-class">
        <xsl:choose>
          <xsl:when test="$menu-item/@href = $browser-address">
            <xsl:text>active</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>not-active</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="full-url">
        <xsl:call-template name="rep.resolve-full-url">
          <xsl:with-param name="link" select="$menu-item/@href" />
        </xsl:call-template>
      </xsl:variable>
      <a id="{$menu-id}" href="{$full-url}" class="nav-link {$active-class}">
        <xsl:apply-templates select="$menu-item" mode="linkText" />
      </a>
    </li>
  </xsl:template>

  <xsl:template name="rep.resolve-full-url">
    <xsl:param name="link" />
    <xsl:param name="base-url" select="$WebApplicationBaseURL" />

    <xsl:choose>
      <xsl:when test="
        starts-with($link,'http:')
        or starts-with($link,'https:')
        or starts-with($link,'mailto:')
        or starts-with($link,'ftp:')
      ">
        <xsl:value-of select="$link" />
      </xsl:when>
      <xsl:when test="starts-with($link,'/')">
        <xsl:choose>
          <xsl:when test="substring($base-url, string-length($base-url), 1) = '/'">
            <xsl:value-of select="concat(substring($base-url, 1, string-length($base-url) - 1), $link)" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat($base-url, $link)" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="substring($base-url, string-length($base-url), 1) = '/'">
            <xsl:value-of select="concat($base-url, $link)" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat($base-url, '/', $link)" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
