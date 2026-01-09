<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="html" encoding="UTF-8" indent="yes" />

  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="baseUrl" select="concat($WebApplicationBaseURL, 'rsc/sitelinks/')" />
  <xsl:param name="Sitelinks.PageSize" />

  <xsl:template match="/">
    <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html&gt;</xsl:text>
    <html lang="en">
      <xsl:apply-templates />
    </html>
  </xsl:template>

  <xsl:template match="years">
    <xsl:variable name="headline">Sitelinks Index for Crawlers</xsl:variable>
    <head>
      <xsl:call-template name="meta" />
      <title>
        <xsl:value-of select="$headline" />
      </title>
      <xsl:call-template name="style" />
    </head>
    <body>
      <h1>
        <xsl:value-of select="$headline" />
      </h1>
      <p>
        This page is intended for crawlers and bots.
        Content is grouped by year and ordered by <em>Date/Year Issued</em> (newest first).
      </p>
      <nav>
        <xsl:for-each select="year">
          <a href="{concat($baseUrl, text(), '/')}"
            aria-label="{concat('Sitelinks for ', text())}">
            <xsl:value-of select="text()" />
          </a>
        </xsl:for-each>
      </nav>
    </body>
  </xsl:template>

  <xsl:template match="page">
    <xsl:variable name="year" select="@year" />
    <xsl:variable name="headline">
      <xsl:value-of select="concat('Sitelinks ', $year, ' - Page ', @number)" />
    </xsl:variable>
    <xsl:variable name="maxPageNumber">
      <xsl:call-template name="max-page-number">
        <xsl:with-param name="totalCount" select="@totalCount" />
      </xsl:call-template>
    </xsl:variable>
    <head>
      <xsl:call-template name="meta" />
      <title>
        <xsl:value-of select="$headline" />
      </title>
      <link rel="canonical" href="{concat($baseUrl, @year, '/')}" />
      <xsl:if test="@number > 1">
        <link rel="prev"
          href="{concat($baseUrl, @year, '/page/', @number - 1, '/')}" />
      </xsl:if>
      <xsl:if test="@number &lt; $maxPageNumber">
        <link rel="next"
          href="{concat($baseUrl, @year, '/page/', @number + 1, '/')}" />
      </xsl:if>
      <xsl:call-template name="style" />
    </head>
    <body>
      <h1>
        <xsl:value-of select="$headline" />
      </h1>
      <nav>
        <a href="{$baseUrl}" aria-label="All sitelinks">All Years</a>
        <xsl:if test="@number > 1">
          <a href="{concat($baseUrl, @year, '/page/', @number - 1)}"
            aria-label="Previous page">
            <xsl:value-of select="'« Previous'" />
          </a>
        </xsl:if>
        <xsl:if test="@number &lt; $maxPageNumber">
          <a href="{concat($baseUrl, @year, '/page/', @number + 1)}"
            aria-label="Next page">
            <xsl:value-of select="'Next »'" />
          </a>
        </xsl:if>
      </nav>
      <xsl:call-template name="objects">
        <xsl:with-param name="objectIds" select="objectIds/objectId" />
      </xsl:call-template>
    </body>
  </xsl:template>

  <xsl:template name="objects">
    <xsl:param name="objectIds" />
    <ul>
      <xsl:for-each select="$objectIds">
        <li>
          <a href="{concat($WebApplicationBaseURL, 'receive/', text())}">
            <xsl:value-of select="text()" />
          </a>
        </li>
      </xsl:for-each>
    </ul>
  </xsl:template>

  <xsl:template name="meta">
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <meta name="description" content="Index of sitelinks for crawlers" />
    <meta name="robots" content="index, follow" />
  </xsl:template>

  <xsl:template name="style">
    <style>
      body { font-family: Arial, sans-serif; background-color: #f4f4f4; color: #333; }
      h1 { font-size: 2em; color: #333; }
      nav { margin-bottom: 1em; }
      nav a { margin-right: 1em; text-decoration: none; color: #0066cc; font-weight: bold; }
      nav a:hover { text-decoration: underline; color: #0044cc; }
      ul { list-style-type: none; padding: 0; }
      li { margin: 0.5em 0; }
      footer { margin-top: 2em; font-size: 0.9em; text-align: center; color: #777; }
      @media (max-width: 768px) {
      body { font-size: 14px; }
      }
    </style>
  </xsl:template>

  <xsl:template name="max-page-number">
    <xsl:param name="totalCount" />
    <xsl:choose>
      <xsl:when test="($totalCount mod $Sitelinks.PageSize) = 0">
        <xsl:value-of select="floor($totalCount div $Sitelinks.PageSize)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="floor($totalCount div $Sitelinks.PageSize) + 1" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
