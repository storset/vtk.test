<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign titleMsg = vrtx.getMsg("confirm-publish.title.unpublish") />

(&nbsp;<a href="${item.url?html}&amp;showAsHtml=true&amp;height=80&amp;width=230" class="thickbox" title="${titleMsg}">${item.title}</a>&nbsp;)
<#recover>
${.error}
</#recover>
