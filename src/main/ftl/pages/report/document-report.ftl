<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

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
        <#if report.backURL?exists>
        <a href="${report.backURL?html}" ><@vrtx.msg code="report.${report.backURLname}" default="Back" /></a>
        <#else>
        <a href="${serviceURL?html}" ><@vrtx.msg code="report.back" default="Back" /></a>
        </#if>
      </div>
    </div>
    <h2><@vrtx.msg code="report.${report.reportname}" /></h2>
    
  <#if (report.result?exists && report.result?size > 0)>
    <p id="vrtx-report-info-paging-top">
      <@vrtx.msg code="report.${report.reportname}.about"
                 args=[report.from, report.to, report.total]
                 default="Listing results " + report.from + " - "
                 +  report.to + " of total " + report.total + " resources" />
      <#if report.prev?exists || report.next?exists>
        <@displayPaging />  
      </#if>
    </p>
    <div class="vrtx-report">
    <table id="directory-listing" class="report-document-listing">
      <thead>
        <tr>
          <th id="vrtx-report-name"><@vrtx.msg code="report.title" default="Name" /></th>
          <th id="vrtx-report-last-modified"><@vrtx.msg code="report.last-modified" default="Last modified" /></th>
          <th id="vrtx-report-modified-by"><@vrtx.msg code="report.modified-by" default="Modified by" /></th>
          <th id="vrtx-report-permission-set"><@vrtx.msg code="report.permission-set" default="Permissions set" /></th>
          <th id="vrtx-report-permissions"><@vrtx.msg code="collectionListing.permissions" default="Permissions"/></th>
          <th id="vrtx-report-published"><@vrtx.msg code="report.published" default="Published" /></th>
        </tr>
      </thead>
      <tbody>
      <#assign count = 1 />
      <#assign collectionSize = report.result?size />
      <#list report.result as res >
        <#assign lastModifiedTime = vrtx.propValue(res, 'lastModified') />
        <#assign modifiedBy = vrtx.propValue(res, 'modifiedBy', 'name-link') />
        <#assign aclIsInherited = vrtx.getMsg("report.yes", "Yes")>
        <#if res.isInheritedAcl() >
          <#assign aclIsInherited = vrtx.getMsg("report.no", "No")>
        </#if>
        <#assign isReadRestricted = vrtx.getMsg("collectionListing.permissions.readAll") >
        <#if report.isReadRestricted[res_index] >
          <#assign isReadRestricted = vrtx.getMsg("collectionListing.permissions.restricted")>
        </#if>
        <#assign url = "">
        <#if report.viewURLs[res_index]?exists>
          <#assign url = report.viewURLs[res_index] />
        </#if>
        <#assign published = vrtx.propValue(res, 'published') />
        <#assign publishedStatus = vrtx.getMsg("report.yes", "Yes")>
        <#if published = "false">
          <#assign publishedStatus = vrtx.getMsg("report.no", "No")>
        </#if>
        
          <#assign contentType = vrtx.propValue(res, 'contentType') />
          <#assign isCollection = vrtx.propValue(res, 'collection') />
          <#assign title = vrtx.propValue(res, 'title') />
        
          <#assign rowType = "odd" />
          <#if (res_index % 2 == 0) >
            <#assign rowType = "even" />
          </#if>

          <#assign firstLast = ""  />
          <#if (res_index == 0) && (res_index == (collectionSize - 1))>
            <#assign firstLast = " first last" />
          <#elseif (res_index == 0)>
            <#assign firstLast = " first" />
          <#elseif (res_index == (collectionSize - 1))>
            <#assign firstLast = " last" />     
          </#if>
          
          <tr class="${rowType} <@vrtx.iconResolver res.resourceType contentType /> ${isCollection}${firstLast}">
            <td class="vrtx-report-name"><a href="${url?html}">${title?html}</a></td>
            <td class="vrtx-report-last-modified">${lastModifiedTime?html}</td>
            <td class="vrtx-report-last-modified-by">${modifiedBy}</td>
            <td class="vrtx-report-permission-set">${aclIsInherited?html}</td>
            <#if report.isReadRestricted[res_index] >
              <td class="vrtx-report-permissions"><span class="restricted">${isReadRestricted?html}</span></td>
            <#else>
              <td class="vrtx-report-permissions"><span class="allowed-for-all">${isReadRestricted?html}</span></td>         
            </#if>
            <td class="vrtx-report-published">${publishedStatus?html}</td>
          </tr>
        </#list>
        </tbody>
      </table>
    </div>

    <#if report.prev?exists || report.next?exists>
      <p id="vrtx-report-paging-bottom">
        <@displayPaging />
      </p>
    </#if>
  <#else>
    <p><@vrtx.msg code="report.document-reporter.no.documents.found" /></p>
  </#if>
  
  </div>
  
  <#macro displayPaging>
    <span class="vrtx-report-paging">
      <#if report.prev?exists>
        <a href="${report.prev?html}">
        <@vrtx.msg code="report.prev-page" default="Previous page" /></a><#if report.next?exists>&nbsp;&nbsp;&nbsp;<a href="${report.next?html}"><@vrtx.msg code="report.next-page" default="Next page" /></a></#if>
      <#elseif report.next?exists>
        <a href="${report.next?html}"><@vrtx.msg code="report.next-page" default="Next page" /></a>
      </#if>
    </span>
  </#macro>
  
  </body>
</html>