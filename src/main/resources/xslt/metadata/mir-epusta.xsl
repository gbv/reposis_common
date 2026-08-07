<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
  xmlns:mcrproperty="http://www.mycore.de/xslt/property"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:import href="xslImport:modsmeta:metadata/mir-epusta.xsl" />

  <xsl:variable name="epusta-prefix" select="mcrproperty:one('MIR.ePuSta.Prefix')" />
  <xsl:variable name="epusta-provider-url" select="mcrproperty:one('MIR.ePuSta.providerURL')" />

  <xsl:template match="/">
    <xsl:if test="mcrproperty:one('MIR.ePuSta')='show'">
      <xsl:variable name="now" select="current-dateTime()" />
      <xsl:variable name="from" select="
        format-date(xs:date($now) - xs:yearMonthDuration('P1Y'), '[Y0001]-[M01]-[D01]')
      " />
      <xsl:variable name="until" select="format-date(xs:date($now), '[Y0001]-[M01]-[D01]')" />
      <xsl:variable name="objID" select="mycoreobject/@ID" />
      <div id="mir-epusta">
        <div class="card">
          <div class="card-header d-flex justify-content-between align-items-center">
            <h3 class="card-title">
              <xsl:value-of select="mcri18n:translate('mir.epusta.panelheading')" />
            </h3>
            <img src="{$WebApplicationBaseURL}images/epusta/epustalogo_small.png" class="mir-epusta-logo" />
          </div>
          <div class="card-body">
            <span>
              <strong>
                <xsl:value-of select="mcri18n:translate('mir.epusta.total') || ':'" />
              </strong>
            </span>
            <div class="row">
              <div class="col-md-7 col-sm-9 col-6 text-end">
                <xsl:value-of select="mcri18n:translate('mir.epusta.counter.fulltext')" />
              </div>
              <div class="col-md-5 col-sm-3 col-6"
                  data-epustaelementtype="ePuStaInline"
                  data-epustaproviderurl="{$epusta-provider-url}"
                  data-epustaidentifier="{$epusta-prefix}{$objID}"
                  data-epustacounttype="counter"
              />
            </div>
            <div class="row">
              <div class="col-md-7 col-sm-9 col-6 text-end">
                <xsl:value-of select="mcri18n:translate('mir.epusta.counter.abstract')" />
              </div>
              <div class="col-md-5 col-sm-3 col-6"
                  data-epustaelementtype="ePuStaInline"
                  data-epustaproviderurl="{$epusta-provider-url}"
                  data-epustaidentifier="{$epusta-prefix}{$objID}"
                  data-epustacounttype="counter_abstract"
              />
            </div>
            <span>
              <strong>
                <xsl:value-of select="concat(mcri18n:translate('mir.epusta.last12Month'),':')" />
              </strong>
            </span>
            <div class="row">
              <div class="col-md-7 col-sm-9 col-6 text-end">
                <xsl:value-of select="mcri18n:translate('mir.epusta.counter.fulltext')" />
              </div>
              <div class="col-md-5 col-sm-3 col-6"
                  data-epustaelementtype="ePuStaInline"
                  data-epustaproviderurl="{$epusta-provider-url}"
                  data-epustaidentifier="{$epusta-prefix}{$objID}"
                  data-epustacounttype="counter"
                  data-epustafrom="{$from}" data-epustauntil="{$until}"
              />
            </div>
            <div class="row">
              <div class="col-md-7 col-sm-9 col-6 text-end">
                <xsl:value-of select="mcri18n:translate('mir.epusta.counter.abstract')" />
              </div>
              <div class="col-md-5 col-sm-3 col-6"
                   data-epustaelementtype="ePuStaInline"
                   data-epustaproviderurl="{$epusta-provider-url}"
                   data-epustaidentifier="{$epusta-prefix}{$objID}"
                   data-epustacounttype="counter_abstract"
                   data-epustafrom="{$from}" data-epustauntil="{$until}"
              />
            </div>
            <div class="text-end">
              <a href="#" data-bs-toggle="modal" data-bs-target="#epustaGraphModal">
                <xsl:value-of select="mcri18n:translate('mir.epusta.open')" />
              </a>
            </div>
            <div
              class="modal fade"
              id="epustaGraphModal"
              tabindex="-1"
              aria-labelledby="epustaGraphTitel"
              aria-hidden="true"
              data-bs-backdrop="static">
              <div class="modal-dialog" role="document">
                <div class="modal-content">
                  <div class="modal-header">
                    <h4 class="modal-title " id="epustaGraphTitel">
                      <xsl:value-of select="mcri18n:translate('mir.epusta.panelheading')" />
                    </h4>
                    <button
                      type="button"
                      class="btn-close modalFrame-cancel"
                      data-bs-dismiss="modal"
                      aria-label="Close"/>
                  </div>
                  <div class="modal-body">
                    <div id="epustaGraph" class="mir-epusta-graph"/>
                    <div class="row mir-epusta-graph-controls" style="margin-top:13px">
                      <div class="col-md-12 text-center">
                        die letzten:
                        <input type="radio" name="granularity" value="day" checked="checked"/> 30 Tage
                        <input type="radio" name="granularity" value="month" class="ms-2"/> 12 Monate
                        <input type="radio" name="granularity" value="year" class="ms-2"/> 10 Jahre
                      </div>
                    </div>
                  </div>
                  <div class="modal-footer">
                    <img
                      src="{$WebApplicationBaseURL}images/epusta/epustalogo.png"
                      class="mir-epusta-logo" />
                  </div>
                </div>
              </div>
            </div>
            <script type="module" src="{$WebApplicationBaseURL}assets/epusta_elements.js/epusta-elements.js" ></script>
            <script type="module" src="{$WebApplicationBaseURL}assets/chart.js/chart.umd.js" ></script>
            <script type="module">
              import {ePuStaGraph} from "<xsl:value-of select="$WebApplicationBaseURL"/>assets/epusta_elements.js/epusta-elements.js";

              const modal = document.getElementById('epustaGraphModal');
              let graphInstance = null;

              const epustaConfig = {
                providerUrl: '<xsl:value-of select="$epusta-provider-url"/>',
                identifier: '<xsl:value-of select="$objID"/>',
                from: 'auto',
                until: '<xsl:value-of select="$until"/>',
                labels: [
                  {
                    label: "Volltextzugriffe",
                    color: "#3b617f",
                    tagquery: "-epusta:filter:httpMethod -epusta:filter:httpStatus -filter:30sek:counter3 -filter:robot oas:content:counter"
                  },
                  {
                    label: "Metadatenansichten",
                    color: "#eb4a66",
                    tagquery: "-epusta:filter:httpMethod -epusta:filter:httpStatus -filter:30sek:counter3 -filter:robot oas:content:counter_abstract"
                  }
                ]
              };

              modal.addEventListener('shown.bs.modal', () => {
                const graphElement = document.getElementById('epustaGraph');

                if (!graphElement) {
                  console.error("epustaGraph Element nicht gefunden");
                  return;
                }

                const granularity = document.querySelector('input[name="granularity"]:checked').value;

                if (graphInstance) {
                  graphElement.innerHTML = '';
                  graphInstance = null;
                }

                graphInstance = new ePuStaGraph(
                  graphElement,
                  epustaConfig.providerUrl,
                  epustaConfig.identifier,
                  epustaConfig.from,
                  epustaConfig.until,
                  epustaConfig.labels,
                  granularity
                );

                graphInstance.requestData();
              });

              document.querySelectorAll('input[name="granularity"]').forEach(radio => {
                radio.addEventListener('change', () => {
                  if (!graphInstance) return;

                  const graphElement = document.getElementById('epustaGraph');
                  const granularity = document.querySelector('input[name="granularity"]:checked').value;

                  graphElement.innerHTML = '';

                  graphInstance = new ePuStaGraph(
                    graphElement,
                    epustaConfig.providerUrl,
                    epustaConfig.identifier,
                    epustaConfig.from,
                    epustaConfig.until,
                    epustaConfig.labels,
                    granularity
                  );

                  graphInstance.requestData();
                });
              });
            </script>
          </div>
        </div>
      </div>
    </xsl:if>
    <xsl:apply-imports />
  </xsl:template>

</xsl:stylesheet>
