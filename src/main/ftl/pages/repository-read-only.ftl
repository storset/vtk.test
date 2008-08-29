<#ftl strip_whitespace=true>

<#--
  - File: repository-read-only.ftl
  - 
  - Description: A HTML page that gives access to switch read only status
  - 
  - Required model data:
  - message
  -  
  - Optional model data:
  - repository.readOnlySwitchUrl 
  -->
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Read only status</title>
  </head>
  <body>
    <h1>Read only status</h1>
    <p><#if message?exists>
          Repository is in read only mode
          <#if unsetReadOnlyUrl?exists>
            (<a href="${unsetReadOnlyUrl.url?html}">switch</a>)
          </#if>
       <#else> 
          Repository is not in read only mode
          <#if setReadOnlyUrl?exists>
            (<a href="${setReadOnlyUrl.url?html}">switch</a>)
          </#if>
       </#if>
    </p>
  </body>
</html>
