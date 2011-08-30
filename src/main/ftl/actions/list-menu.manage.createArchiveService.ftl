<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign titleMsg = vrtx.getMsg("actions.createArchive.title") />
<#assign actionURL = item.url />

<a id="manage.createArchiveService" title="${titleMsg}" href="${actionURL?html}">${item.title?html}</a>

<#recover>
${.error}
</#recover>