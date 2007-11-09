<#ftl strip_whitespace=true>

<#--
  - File: fckeditor.ftl
  - 
  - Required model data:
  -  
  -  fckeditorBase.url
  -  fckSource.getURL
  -  fckCleanup.url
  -  fckBrowse.url
  -  
  - Optional model data:
  -
  -->
<#import "/lib/ping.ftl" as ping />
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
  <head>
    <title>Editor</title>
    <@ping.ping url=pingURL['url'] interval=300 />
    <script type="text/javascript" src="${fckeditorBase.url?html}/fckeditor.js"></script>

    <script type="text/javascript">
      window.onload = function()
      {
        var fck = new FCKeditor( 'content' ) ;
        fck.BasePath = "${fckeditorBase.url?html}/";

         // The toolbar: JSON string
          fck.Config['ToolbarSets'] = "( {'Vortikal' : [\
            ['PasteText','PasteWord','-','Undo','Redo','-','Replace','RemoveFormat','-','Link','Unlink','Anchor','Image','Flash','Table','Rule','SpecialChar'],\'/',\
            ['FontFormat','-','Bold','Italic','Underline','StrikeThrough','Subscript','Superscript','OrderedList','UnorderedList','Outdent','Indent','JustifyLeft','JustifyCenter','JustifyRight','TextColor','FitWindow']]} )";
         fck.ToolbarSet = "Vortikal";

        // File browser
         var baseFolder = "${resourceContext.parentURI?html}";
         fck.Config['LinkBrowserURL']  = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Connector=${fckBrowse.url.pathRepresentation}';
         fck.Config['ImageBrowserURL'] = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Image&Connector=${fckBrowse.url.pathRepresentation}';
         fck.Config['FlashBrowserURL'] = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Flash&Connector=${fckBrowse.url.pathRepresentation}';


         // Misc setup
         fck.Config['FullPage'] = true;
         fck.Config['ToolbarCanCollapse'] = false;
         fck.Config['FontFormats'] = 'p;h1;h2;h3;h4;h5;h6;pre' ;        

         fck.Config.DisableFFTableHandles = false;
	 fck.Config.ForcePasteAsPlainText = false;

         fck.Config['SkinPath'] = fck.BasePath + 'editor/skins/silver/';


        fck.ReplaceTextarea() ;
      }

  </script>

    <script language="JavaScript">
      var needToConfirm = true;

    function performSave() {
      needToConfirm = false;
      var oEditor = FCKeditorAPI.GetInstance('content');
      var srcxhtml = oEditor.GetXHTML();
      var title = document.getElementById("title");

      // Title
      // srcxhtml = srcxhtml.replace(/<title.*<\/title>/i, "<title>" + title.value + "</title>");
      document.getElementById('content').value = srcxhtml;
    }  

    </script>
  </head>
  <body>
    <form action="" method="POST">
     
      <div style="padding: 7px; border: 1px solid #aaa;">
        <@handleProps />
      </div>
      <br/>
      <div style="padding: 7px; border: 1px solid #aaa;">
       <textarea name="content" rows="8" cols="60" id="content">${command.content?html}</textarea>
       </div>
       <p>Noe tredje</p>
      <div style="padding: 7px; border: 1px solid #aaa;">
       <input type="submit" onClick="performSave();" value="Lagre og publiser" />
        <#if command.tooltips?exists>
          <#list command.tooltips as tooltip>
           <div class="contextual-help"><a href="javascript:void(0);" onclick="javascript:open('${tooltip.url?html}', 'componentList', 'width=650,height=450,resizable=yes,right=0,top=0,screenX=0,screenY=0,scrollbars=yes');">
              <@vrtx.msg code=tooltip.messageKey default=tooltip.messageKey/>
            </a>
           </div>
          </#list>
        </#if>

      </div>
     </form>

    </body>
</html>

<#macro handleProps>
    <script language="JavaScript">
      window.onbeforeunload = confirmExit;

      function propChange() {
        <#local keyNames = command.editableProperties />
        <#list keyNames as propDef>
          <#local name = propDef.name />
          <#local value = command.getValue(propDef) />
          if ('${value}' != document.getElementById('resource.${name}').value) {
            return true;
          }
        </#list>
        return false;
      }

      function confirmExit() {
        var contentChange = (propChange() || FCKeditorAPI.GetInstance('content').IsDirty());
        if (needToConfirm && contentChange) {
          return "You have unsaved changes. Are you sure you want to leave this page?";
        }
      }
    </script>

    <#local keys = command.editableProperties />
    <#list keys as propDef>
      <#local name = propDef.name />
      <#local value = command.getValue(propDef) />
      <#local errors = command.errors?if_exists />
      <p><#if errors?exists && errors[name]?exists>${errors[name]}<br/></#if>
         ${name}: <input type="text" id="resource.${name}" name="resource.${name}" value="${value}" /></p> 
    </#list>


</#macro>
