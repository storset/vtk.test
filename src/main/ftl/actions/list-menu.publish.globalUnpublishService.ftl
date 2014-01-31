<#ftl strip_whitespace=true>
<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/publish.ftl" as publish />

<#assign headerMsg = vrtx.getMsg("unpublish.header") />
<#assign titleMsg = vrtx.getMsg("unpublish.title") />
<#assign actionURL = item.url />

<h3>${headerMsg}</h3>

<@publish.publishMessage resourceContext />

<#if writePermission.permissionsQueryResult = 'true'>
  <#if !resourceContext.currentResource.isCollection()>
    <ul class="publishing-document button-row-small">
      <li class="first">
        <a id="vrtx-unpublish-document" title="${titleMsg}" href="${actionURL?html}">
          ${item.title?html}
        </a>
      </li>
      <li>
        <a id="advanced-publish-settings" href="${resourceContext.currentURI}?vrtx=admin&display=advanced-publish-dialog">
          <@vrtx.msg code="publishing.advanced.link" />
        </a>
      </li>
    </ul>
  <#else>
    <a id="vrtx-unpublish-document" class="vrtx-button-small" title="${titleMsg}" href="${actionURL?html}">${item.title?html}</a>
  </#if>
</#if>

<#recover>
${.error}
</#recover>