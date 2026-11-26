<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcrproperty="http://www.mycore.de/xslt/property"
  xmlns:repsitelinks="http://www.gbv.de/xslt/sitelinks"
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
    <xsl:variable name="year" select="xs:integer(@year)" />
    <xsl:variable name="max-page-number" select="repsitelinks:get-max-page-number(@total-count, $page-size)" />
    <xsl:variable name="page-title">
      Page <xsl:value-of select="@number" /> of <xsl:value-of select="$max-page-number" />
    </xsl:variable>
    <xsl:variable name="title">
      <xsl:value-of select="concat('Sitelinks ', $year, ' - ', $page-title)" />
    </xsl:variable>
    <xsl:variable name="canonical-url">
      <xsl:call-template name="page-url">
        <xsl:with-param name="number" select="@number" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="first-url">
      <xsl:call-template name="page-url">
        <xsl:with-param name="number" select="1" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="prev-url">
      <xsl:call-template name="page-url">
        <xsl:with-param name="number" select="@number - 1" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="next-url">
      <xsl:call-template name="page-url">
        <xsl:with-param name="number" select="@number + 1" />
      </xsl:call-template>
    </xsl:variable>
    <head>
      <xsl:call-template name="basic-head">
        <xsl:with-param name="title" select="$title" />
      </xsl:call-template>
      <link rel="canonical" href="{$canonical-url}" />
      <xsl:if test="@number > 1">
        <link rel="prev" href="{$prev-url}" />
      </xsl:if>
      <xsl:if test="@number &lt; $max-page-number">
        <link rel="next" href="{$next-url}" />
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
              <a href="{$first-url}"><xsl:value-of select="concat('All Pages of ', @year)" /></a>
            </li>
          </xsl:if>
          <xsl:if test="@number > 1">
            <li>
              <a href="{$prev-url}" rel="prev">
                « Previous Page
              </a>
            </li>
          </xsl:if>
          <xsl:choose>
            <xsl:when test="@number = 1">
              <xsl:call-template name="page-links">
                <xsl:with-param name="from" select="1" />
                <xsl:with-param name="to" select="$max-page-number" />
              </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
              <li aria-current="page">
                <span><xsl:value-of select="concat('Page ', @number)" /></span>
              </li>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:if test="@number &lt; $max-page-number">
            <li>
              <a href="{$next-url}" rel="next">
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

  <!-- The first page has no URL of its own, it is the year page. -->
  <xsl:template name="page-url">
    <xsl:param name="number" />
    <xsl:value-of select="concat($base-url, '/', @year)" />
    <xsl:if test="$number > 1">
      <xsl:value-of select="concat('/page/', $number)" />
    </xsl:if>
  </xsl:template>

  <!--
    Links every page of a year, so that all objects are reachable by a small number of links.
    Only the year page lists them, otherwise the navigation of every page would grow with the
    number of pages. The range is split in halves to keep the recursion depth small.
  -->
  <xsl:template name="page-links">
    <xsl:param name="from" />
    <xsl:param name="to" />
    <xsl:choose>
      <xsl:when test="$from &lt; $to">
        <xsl:variable name="middle" select="floor(($from + $to) div 2)" />
        <xsl:call-template name="page-links">
          <xsl:with-param name="from" select="$from" />
          <xsl:with-param name="to" select="$middle" />
        </xsl:call-template>
        <xsl:call-template name="page-links">
          <xsl:with-param name="from" select="$middle + 1" />
          <xsl:with-param name="to" select="$to" />
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="$from = @number">
        <li aria-current="page">
          <span><xsl:value-of select="$from" /></span>
        </li>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="url">
          <xsl:call-template name="page-url">
            <xsl:with-param name="number" select="$from" />
          </xsl:call-template>
        </xsl:variable>
        <li>
          <a href="{$url}"><xsl:value-of select="$from" /></a>
        </li>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="object-id">
    <li>
      <a href="{concat($WebApplicationBaseURL, 'receive/', text())}">
        <xsl:choose>
          <xsl:when test="normalize-space(@title)">
            <xsl:value-of select="@title" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="text()" />
          </xsl:otherwise>
        </xsl:choose>
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

  <xsl:function name="repsitelinks:get-max-page-number" as="xs:integer">
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
