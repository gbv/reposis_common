<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcracl="http://www.mycore.de/xslt/acl"
  xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
  xmlns:mcrproperty="http://www.mycore.de/xslt/property"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:include href="resource:xslt/MyCoReLayout.xsl" />
  <xsl:include href="xslInclude:ErrorPage" />

  <xsl:variable name="production-mode" select="mcrproperty:one('REP.ProductionMode')" />
  <xsl:variable name="mail-general" select="mcrproperty:one('REP.ErrorPage.Mail.General')" />
  <xsl:variable name="mail-technical" select="mcrproperty:one('REP.ErrorPage.Mail.Technical')" />

  <xsl:variable name="is-access-error" select="/mcr_error/@HttpError = ('401', '403')" />
  <xsl:variable name="is-guest" select="mcracl:is-current-user-guest-user()" />
  <xsl:variable name="access-error-title" select="
    if ($is-guest)
    then mcri18n:translate('rep.errorPage.loginRequired')
    else mcri18n:translate('rep.errorPage.accessDenied')
  " />
  <xsl:variable name="PageTitle" select="
    if ($is-access-error)
    then $access-error-title
    else mcri18n:translate-with-params('titles.pageTitle.error', concat(' ', /mcr_error/@HttpError))
  " />

  <xsl:template match="/mcr_error">
    <div class="row">
      <div class="col-md-8">
        <xsl:apply-templates select="." mode="error-content" />
      </div>
    </div>
    <xsl:if test="$production-mode = 'false' and exists(exception/trace)">
      <div class="card">
        <div class="card-header bg-danger">
          <xsl:value-of select="concat(mcri18n:translate('error.stackTrace'), ':')" />
        </div>
        <div class="card-body">
          <xsl:for-each select="exception/trace">
            <pre style="font-size:0.8em;">
              <xsl:value-of select="." />
            </pre>
          </xsl:for-each>
        </div>
      </div>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mcr_error[@HttpError = '500']" mode="error-content">
    <h1><xsl:value-of select="mcri18n:translate('rep.errorPage.internalServerError')" /></h1>
    <p>
      <xsl:value-of select="mcri18n:translate('rep.errorPage.internalServerError.text')" />
      <xsl:if test="string-length($mail-technical) gt 0">
        <xsl:text> </xsl:text>
        <xsl:call-template name="translate-with-placeholder">
          <xsl:with-param name="key" select="'rep.errorPage.internalServerError.mail'" />
          <xsl:with-param name="placeholder">
            <xsl:call-template name="mail">
              <xsl:with-param name="address" select="$mail-technical" />
            </xsl:call-template>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:if>
    </p>
  </xsl:template>

  <xsl:template match="mcr_error[@HttpError = '404']" mode="error-content">
    <h1><xsl:value-of select="mcri18n:translate('rep.errorPage.notFound')" /></h1>
    <p>
      <xsl:call-template name="translate-with-placeholder">
        <xsl:with-param name="key" select="'rep.errorPage.notFound.text'" />
        <xsl:with-param name="placeholder">
          <a href="/index.html">
            <xsl:value-of select="mcri18n:translate('rep.errorPage.notFound.searchLink')" />
          </a>
        </xsl:with-param>
      </xsl:call-template>
      <xsl:if test="string-length($mail-general) gt 0">
        <xsl:text> </xsl:text>
        <xsl:call-template name="translate-with-placeholder">
          <xsl:with-param name="key" select="'rep.errorPage.notFound.mail'" />
          <xsl:with-param name="placeholder">
            <xsl:call-template name="mail">
              <xsl:with-param name="address" select="$mail-general" />
            </xsl:call-template>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:if>
    </p>
  </xsl:template>

  <xsl:template match="mcr_error[@HttpError = ('401', '403')]" mode="error-content">
    <h1><xsl:value-of select="$access-error-title" /></h1>
    <xsl:choose>
      <xsl:when test="$is-guest">
        <xsl:variable name="login-url"
          select="concat($ServletsBaseURL, 'MCRLoginServlet?url=', encode-for-uri(string($RequestURL)))" />
        <p>
          <xsl:call-template name="translate-with-placeholder">
            <xsl:with-param name="key" select="'rep.errorPage.loginRequired.text'" />
            <xsl:with-param name="placeholder">
              <a href="{$login-url}">
                <xsl:value-of select="mcri18n:translate('rep.errorPage.loginRequired.loginLink')" />
              </a>
            </xsl:with-param>
          </xsl:call-template>
          <xsl:if test="string-length($mail-general) gt 0">
            <xsl:text> </xsl:text>
            <xsl:call-template name="translate-with-placeholder">
              <xsl:with-param name="key" select="'rep.errorPage.loginRequired.mail'" />
              <xsl:with-param name="placeholder">
                <xsl:call-template name="mail">
                  <xsl:with-param name="address" select="$mail-general" />
                </xsl:call-template>
              </xsl:with-param>
            </xsl:call-template>
          </xsl:if>
        </p>
      </xsl:when>
      <xsl:otherwise>
        <p>
          <xsl:call-template name="translate-with-placeholder">
            <xsl:with-param name="key" select="'rep.errorPage.accessDenied.text'" />
            <xsl:with-param name="placeholder">
              <strong><xsl:value-of select="$CurrentUser" /></strong>
            </xsl:with-param>
          </xsl:call-template>
          <xsl:if test="string-length($mail-general) gt 0">
            <xsl:text> </xsl:text>
            <xsl:call-template name="translate-with-placeholder">
              <xsl:with-param name="key" select="'rep.errorPage.accessDenied.mail'" />
              <xsl:with-param name="placeholder">
                <xsl:call-template name="mail">
                  <xsl:with-param name="address" select="$mail-general" />
                </xsl:call-template>
              </xsl:with-param>
            </xsl:call-template>
          </xsl:if>
        </p>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mcr_error" mode="error-content">
    <h1><xsl:value-of select="mcri18n:translate('rep.errorPage.generic')" /></h1>
    <h2><xsl:value-of select="." /></h2>
    <p>
      <xsl:value-of select="mcri18n:translate('rep.errorPage.generic.text')" />
      <xsl:if test="string-length($mail-technical) gt 0">
        <xsl:text> </xsl:text>
        <xsl:call-template name="translate-with-placeholder">
          <xsl:with-param name="key" select="'rep.errorPage.generic.mail'" />
          <xsl:with-param name="placeholder">
            <xsl:call-template name="mail">
              <xsl:with-param name="address" select="$mail-technical" />
            </xsl:call-template>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:if>
    </p>
  </xsl:template>

  <xsl:template name="translate-with-placeholder">
    <xsl:param name="key" as="xs:string" />
    <xsl:param name="placeholder" />

    <xsl:variable name="text" select="mcri18n:translate($key)" />
    <xsl:choose>
      <xsl:when test="contains($text, '{0}')">
        <xsl:value-of select="substring-before($text, '{0}')" />
        <xsl:copy-of select="$placeholder" />
        <xsl:value-of select="substring-after($text, '{0}')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$text" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="mail">
    <xsl:param name="address" />

    <span class="madress">
      <xsl:value-of select="concat(substring-before($address, '@'), ' [at] ', substring-after($address, '@'))" />
    </span>
  </xsl:template>

</xsl:stylesheet>
