<#--
  - File: unlock-form.ftl
  - 
  - Description: Display (and if possible, autosubmit unlock form)
  - 
  - Required model data:
  -   form
  -   resourceContext
  -  
  - Optional model data:
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
<title>${vrtx.getMsg("unlockwarning.title")} '${resourceContext.currentResource.name}'</title>
<style type="text/css">
  #vrtx-unlock-buttons {
    clear: left;
    padding: 0.5em 0;
    border-top: 1px solid #777;
    border-bottom: 1px solid #777;
    margin: 1em 0;
  }
</style>
</head>


<#if resourceContext.currentResource.lock?exists>
  <#assign owner = resourceContext.currentResource.lock.principal.qualifiedName />
</#if>
<#assign currentPrincipal = resourceContext.principal.qualifiedName />

<#if !owner?exists || owner = currentPrincipal >
  <body onload="document.forms[0].submit()">
<#else>
  <body>
</#if>
  <h1>${vrtx.getMsg("unlockwarning.title")} '${resourceContext.currentResource.name}'</h1>
  <form method="post" action="${form.url?html}">
    <@vrtx.csrfPreventionToken url=form.url />
    <#if owner != currentPrincipal>
      <p>${vrtx.getMsg("unlockwarning.steal")}: <strong>${owner}</strong>.</p> 
      <p>${vrtx.getMsg("unlockwarning.modified")}: <strong>${resourceContext.currentResource.lastModified?datetime?html}</strong>.</p>
      <p>${vrtx.getMsg("unlockwarning.explanation")}</p>
    </#if>
    <div id="vrtx-unlock-buttons">
      <button tabindex="1" type="submit" name="unlock" value="Unlock" >
        ${vrtx.getMsg("unlockwarning.unlock")}
      </button>
      <button tabindex="2" type="submit" name="cancel" value="Cancel" >
        ${vrtx.getMsg("unlockwarning.cancel")}
      </button>
    </div>
  </form>
</body>
</html>
 