<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:local="http://www.w3.org/2005/xquery-local-functions"
  xmlns:mcrproperty="http://www.mycore.de/xslt/property"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:import href="resource:xslt/output-html.xsl" />

  <xsl:include href="resource:xslt/default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />

  <xsl:param name="base-url" select="concat($WebApplicationBaseURL, 'sitelinks')" />

  <xsl:template match="/">
    <html lang="en">
      <xsl:apply-templates />
    </html>
  </xsl:template>

  <xsl:template match="years">
    <xsl:variable name="title">Sitelinks Index for Crawlers</xsl:variable>
    <head>
      <xsl:call-template name="basic-head">
        <xsl:with-param name="title" select="$title" />
      </xsl:call-template>
    </head>
    <body>
      <h1><xsl:value-of select="$title" /></h1>
      <p>
        This page is intended for crawlers and bots. Content is grouped by year and ordered by
        <em>Date/Year Issued</em> (newest first).
      </p>
      <nav aria-label="Year navigation">
        <ul>
          <xsl:apply-templates select="year" />
        </ul>
      </nav>
    </body>
  </xsl:template>

  <xsl:template match="year">
    <li>
      <a href="{concat($base-url, '/', text())}">
        <xsl:value-of select="text()" />
      </a>
    </li>
  </xsl:template>

  <xsl:template match="page">
    <xsl:variable name="page-size" select="xs:integer(mcrproperty:one('Sitelinks.PageSize'))" />
    <xsl:variable name="year" select="xs:integer(year)" />
    <xsl:variable name="max-page-number" select="local:get-max-page-number(@total-count, $page-size)" />
    <xsl:variable name="page-title">
      Page <xsl:value-of select="@number" /> of <xsl:value-of select="$max-page-number" />
    </xsl:variable>
    <xsl:variable name="title">
      <xsl:value-of select="concat('Sitelinks ', $year, ' - ', $page-title)" />
    </xsl:variable>
    <head>
      <xsl:call-template name="basic-head">
        <xsl:with-param name="title" select="$title" />
      </xsl:call-template>
      <link rel="canonical" href="{concat($base-url, '/', @year)}" />
      <xsl:if test="@number > 1">
        <link rel="prev" href="{concat($base-url, '/', @year, '/page/', @number - 1)}" />
      </xsl:if>
      <xsl:if test="@number &lt; $max-page-number">
        <link rel="next" href="{concat($base-url, '/', @year, '/page/', @number + 1)}" />
      </xsl:if>
    </head>
    <body>
      <h1><xsl:value-of select="$title" /></h1>
      <nav aria-label="Page navigation">
        <ul class="pagination">
          <li>
            <a href="{$base-url}" aria-label="All sitelinks">All Years</a>
          </li>
          <xsl:if test="@number > 1">
            <li>
              <a href="{concat($base-url, '/', @year, '/page/', @number - 1)}" rel="prev">
                « Previous Page
              </a>
            </li>
          </xsl:if>
          <li aria-current="page">
            <span><xsl:value-of select="concat('Page ', @number)" /></span>
          </li>
          <xsl:if test="@number &lt; $max-page-number">
            <li>
              <a href="{concat($base-url, '/', @year, '/page/', @number + 1)}" rel="next">
                Next Page »
              </a>
            </li>
          </xsl:if>
        </ul>
      </nav>
      <ul>
        <xsl:apply-templates select="object-ids/object-id" />
      </ul>
    </body>
  </xsl:template>

  <xsl:template match="object-id">
    <li>
      <a href="{concat($WebApplicationBaseURL, 'receive/', text())}">
        <xsl:value-of select="text()" />
      </a>
    </li>
  </xsl:template>

  <xsl:template name="basic-head">
    <xsl:param name="title" />
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <meta name="description" content="Index of sitelinks for crawlers" />
    <meta name="robots" content="index, follow" />
    <meta name="citation_robots" content="index, follow" />
    <link rel="stylesheet" href="{concat($WebApplicationBaseURL, 'css/sitelinks.css')}" />
    <title><xsl:value-of select="$title" /></title>
  </xsl:template>

  <xsl:function name="local:get-max-page-number" as="xs:integer">
    <xsl:param name="total-count" as="xs:integer" />
    <xsl:param name="page-size" as="xs:integer" />

    <xsl:sequence select="
      xs:integer(
        if ($page-size = 0 or $total-count = 0)
        then 1
        else if (($total-count mod $page-size) = 0)
        then floor($total-count div $page-size)
        else floor($total-count div $page-size) + 1
      )
    " />
  </xsl:function>

</xsl:stylesheet>
