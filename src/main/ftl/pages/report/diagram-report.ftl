
<#import "/lib/vortikal.ftl" as vrtx />

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
  <#if cssURLs?exists>
    <#list cssURLs as cssURL>
    <link rel="stylesheet" href="${cssURL}" />
    </#list>
  </#if>
  </head>
  <body id="vrtx-report-documents">
  <div class="resourceInfo">
    <div class="vrtx-report-nav">
      <div class="back">
        <a href="${serviceURL}" ><@vrtx.msg code="report.back" default="Back" /></a>
      </div>
    </div>
    <h2><@vrtx.msg code="report.${report.reportname}" /></h2>
  <#if (report.firsttotal?exists && report.firsttotal > 0)>
    <div class="vrtx-report-diagram">
      <div class="vrtx-report-diagram-table">
        <table id="filesandfolders">
          <tr>
            <th>Type</th>
            <th><@vrtx.msg code="report.count" /></th>
          </tr>
          <tr>
            <td>File</td>
            <td>${report.files}</td>
          </tr>
          <tr>
            <td>Folder</td>
            <td>${report.folders}</td>
          </tr>
          <tr>
            <td class="total">Total</td>
            <td class="total">${report.firsttotal}</td>
          </tr>
        </table>
      </div>
      <div class="vrtx-report-diagram-img">
        <img id="filesandfoldersimg" width="440" height="180" alt="<@vrtx.msg code="report.${report.reportname}.filesandfolderspiechart" />" src="https://chart.googleapis.com/chart?chs=440x180&cht=p3&chd=s:Sm&chdl=Mapper|Filer&chl=<#if (report.folders > 0)>Mapper</#if>|<#if (report.files > 0)>Filer</#if>&chtt=<@vrtx.msg code="report.${report.reportname}.folderandfiletitle" />&chd=t:${((report.folders/report.firsttotal)*100)?string("0.#")},${((report.files/report.firsttotal)*100)?string("0.#")}" />
      </div>
    </div>
  </#if>
  <#if (report.secondtotal?exists && report.secondtotal > 0)>
    <div class="vrtx-report-diagram">
      <div class="vrtx-report-diagram-table">
        <table id="filetypes">
          <tr>
            <th><@vrtx.msg code="report.${report.reportname}.filetype" /></th>
            <th><@vrtx.msg code="report.count" /></th>
          </tr>
          <tr>
            <td><#if (report.webpage > 0)><a href="${report.webpageURL?html}">Web page</a><#else>Web page</#if></td>
            <td>${report.webpage}</td>
          </tr>
          <tr>
            <td><#if (report.image > 0)><a href="${report.imageURL?html}">Image</a><#else>Image</#if></td>
            <td>${report.image}</td>
          </tr>
          <tr>
            <td><#if (report.audio > 0)><a href="${report.audioURL?html}">Audio</a><#else>Audio</#if></td>
            <td>${report.audio}</td>
          </tr>
          <tr>
            <td><#if (report.video > 0)><a href="${report.videoURL?html}">Video</a><#else>Video</#if></td>
            <td>${report.video}</td>
          </tr>
          <tr>
            <td><#if (report.pdf > 0)><a href="${report.pdfURL?html}">PDF</a><#else>PDF</#if></td>
            <td>${report.pdf}</td>
          </tr>
          <tr>
            <td><#if (report.doc > 0)><a href="${report.docURL?html}">Word</a><#else>Word</#if></td>
            <td>${report.doc}</td>
          </tr>
          <tr>
            <td><#if (report.ppt > 0)><a href="${report.pptURL?html}">Power Point</a><#else>Power Point</#if></td>
            <td>${report.ppt}</td>
          </tr>
          <tr>
            <td><#if (report.xls > 0)><a href="${report.xlsURL?html}">Excel</a><#else>Excel</#if></td>
            <td>${report.xls}</td>
          </tr>
          <tr>
            <td class="total">Total</td>
            <td class="total">${report.secondtotal}</td>
          </tr>
        </table>
      </div>
      <div class="vrtx-report-diagram-img">
        <img id="filetypesimg" width="440" height="180" alt="<@vrtx.msg code="report.${report.reportname}.filetypepiechart" />" src="https://chart.googleapis.com/chart?chs=440x180&cht=p3&chd=s:Sm&chdl=Webpage|Image|Audio|Video|Pdf|Word|Power+Point|Excel&chl=<#if (report.webpage > 0)>Webpage</#if>|<#if (report.image > 0)>Image</#if>|<#if (report.audio > 0)>Audio</#if>|<#if (report.video > 0)>Video</#if>|<#if (report.pdf > 0)>Pdf</#if>|<#if (report.doc > 0)>Word</#if>|<#if (report.ppt > 0)>Power+Point</#if>|<#if (report.xls > 0)>Excel</#if>&chtt=<@vrtx.msg code="report.${report.reportname}.filetypetitle" />&chd=t:${((report.webpage/report.secondtotal)*100)?string("0.#")},${((report.image/report.secondtotal)*100)?string("0.#")},${((report.audio/report.secondtotal)*100)?string("0.#")},${((report.video/report.secondtotal)*100)?string("0.#")},${((report.pdf/report.secondtotal)*100)?string("0.#")},${((report.doc/report.secondtotal)*100)?string("0.#")},${((report.ppt/report.secondtotal)*100)?string("0.#")},${((report.xls/report.secondtotal)*100)?string("0.#")}" />
      </div>
    </div>
  </#if>
  <#if !((report.firsttotal?exists && report.firsttotal > 0) || (report.secondtotal?exists && report.secondtotal > 0))>
        <p><@vrtx.msg code="report.diagram.error" /></p>
  </#if>
  </div>
  </body>
</html>