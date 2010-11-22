// Set up an fck editor

function newEditor(name, completeEditor, withoutSubSuper, baseFolder, baseUrl, baseDocumentUrl, browsePath,
    defaultLanguage, cssFileList) {

   var completeEditor = completeEditor != null ? completeEditor : false;
  var withoutSubSuper = withoutSubSuper != null ? withoutSubSuper : false;
  
  
    // File browser
  var linkBrowseUrl  = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Connector=' + browsePath;
  var imageBrowseUrl = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Image&Connector=' + browsePath;
  var flashBrowseUrl = baseUrl + '/plugins/filemanager/browser/cddefault/browser.html?BaseFolder=' + baseFolder + '&Type=Flash&Connector=' + browsePath;
  
    var cssFileList = new Array(
          "/vrtx/__vrtx/static-resources/themes/default/editor-container.css",
          "/vrtx/__vrtx/static-resources/themes/default/fck_editorarea.css");

      /* Fix for div contianer display in ie */
      var browser = navigator.userAgent;
      var ieversion = new Number(RegExp.$1);
      if(browser.indexOf("MSIE") > -1 && ieversion <= 7){
        cssFileList[cssFileList.length] = "/vrtx/__vrtx/static-resources/themes/default/editor-container-ie.css";
      }


  
    completeEditorTemplate = {
    	filebrowserBrowseUrl : linkBrowseUrl,
    	filebrowserImageBrowseUrl : imageBrowseUrl,
    	filebrowserFlashBrowseUrl : flashBrowseUrl, 
    	extraPlugins : 'MediaEmbed',
    	contentsCss : cssFileList,
        uiColor : '#C2CEEA',
        toolbar : [

                [ 'Source', 'PasteText', '-', 'Undo', 'Redo', '-', 'Replace',
                        'RemoveFormat', '-', 'Link', 'Unlink', 'Anchor',
                        'Image', 'CreateDiv', 'MediaEmbed', 'Table',
                        'HorizontalRule', 'SpecialChar' ],
                [ 'Format', '-', 'Bold', 'Italic', 'Underline', 'Strike',
                        'Subscript', 'Superscript', 'OrderedList',
                        'UnorderedList', 'Outdent', 'Indent', 'JustifyLeft',
                        'JustifyCenter', 'JustifyRight', 'TextColor',
                        'Maximize' ]

        ],
        resize_enabled : true,
        autoGrow_maxHeight : '400px',
        autoGrow_minHeight : '50px',
        stylesSet : [
                {
                    name : 'Facts left',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-facts-container vrtx-container-left'
                    }
                },
                {
                    name : 'Facts right',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-facts-container vrtx-container-right'
                    }
                },
                {
                    name : 'Image left',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-img-container vrtx-container-left'
                    }
                },
                {
                    name : 'Image center',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-img-container vrtx-container-middle vrtx-img-container-middle-ie'
                    }
                },
                {
                    name : 'Image right',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-img-container vrtx-container-right'
                    }
                },
                {
                    name : 'Img & capt left (800px)',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-xxl vrtx-container-left'
                    }
                },
                {
                    name : 'Img & capt left (700px)',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-xl vrtx-container-left'
                    }
                },
                {
                    name : 'Img & capt left (600px)',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-l vrtx-container-left'
                    }
                },
                {
                    name : 'Img & capt left (500px)',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-m vrtx-container-left'
                    }
                },
                {
                    name : 'Img & capt left (400px)',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-s vrtx-container-left'
                    }
                },
                {
                    name : 'Img & capt left (300px)',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-xs vrtx-container-left'
                    }
                },
                {
                    name : 'Img & capt left (200px)',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-xxs vrtx-container-left'
                    }
                },
                {
                    name : 'Img & capt center (full)',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-full vrtx-container-middle'
                    }
                },
                {
                    name : 'Img & capt center (800px)',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-xxl vrtx-container-middle'
                    }
                },
                {
                    name : 'Img & capt center (700px) ',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-xl vrtx-container-middle'
                    }
                },
                {
                    name : 'Img & capt center (600px) ',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-l vrtx-container-middle'
                    }
                },
                {
                    name : 'Img & capt center (500px) ',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-m vrtx-container-middle'
                    }
                },
                {
                    name : 'Img & capt center (400px) ',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-s vrtx-container-middle'
                    }
                },
                {
                    name : 'Img & capt right (800px) ',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-xxl vrtx-container-right'
                    }
                },
                {
                    name : 'Img & capt right (700px) ',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-xl vrtx-container-right'
                    }
                },
                {
                    name : 'Img & capt right (600px) ',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-l vrtx-container-right'
                    }
                },
                {
                    name : 'Img & capt right (500px) ',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-m vrtx-container-right'
                    }
                },
                {
                    name : 'Img & capt right (400px) ',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-s vrtx-container-right'
                    }
                },
                {
                    name : 'Img & capt right (300px) ',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-xs vrtx-container-right'
                    }
                },
                {
                    name : 'Img & capt right (200px) ',
                    element : 'div',
                    attributes : {
                        'class' : 'vrtx-container vrtx-container-size-xxs vrtx-container-right'
                    }
                } ]
    }

    inlineEditorTemplate = {
        filebrowserBrowseUrl : linkBrowseUrl,
        uiColor : '#C2CEEA',
        toolbar : [ [ 'Source', 'PasteText', 'Link', 'Unlink', 'Bold',
                'Italic', 'Underline', 'Strike', 'Subscript', 'Superscript',
                'SpecialChar' ] ],
        resize_enabled : true,
        height : '40px',
        autoGrow_maxHeight : '400px',
        autoGrow_minHeight : '40px'

    }

    withoutSubSuperEditorTemplate = {
    	filebrowserBrowseUrl : linkBrowseUrl,
        uiColor : '#C2CEEA',
        toolbar : [ [ 'Source', 'PasteText', 'Link', 'Unlink', 'Bold',
                'Italic', 'Underline', 'Strike', 'SpecialChar' ] ],
        resize_enabled : true,
        height : '40px',
        autoGrow_maxHeight : '400px',
        autoGrow_minHeight : '40px'
    }

    introductionEditorTemplate = {
    	filebrowserBrowseUrl : linkBrowseUrl,
        uiColor : '#C2CEEA',
        toolbar : [ [ 'Source', 'PasteText', 'Link', 'Unlink', 'Bold',
                'Italic', 'Underline', 'Strike', 'SpecialChar' ] ],
        resize_enabled : true,
        height : '150px',
        autoGrow_maxHeight : '400px',
        autoGrow_minHeight : '40px'
    }

    captionEditorTemplate = {
    	filebrowserBrowseUrl : linkBrowseUrl,
        uiColor : '#C2CEEA',
        toolbar : [ [ 'Source', 'PasteText', 'Link', 'Unlink', 'Bold',
                'Italic', 'Underline', 'Strike', 'SpecialChar' ] ],
        resize_enabled : true,
        height : '93px',
        autoGrow_maxHeight : '400px',
        autoGrow_minHeight : '40px'
    }

    var completeEditor = completeEditor != null ? completeEditor : false;
    var withoutSubSuper = withoutSubSuper != null ? withoutSubSuper : false;

    if (name == "introduction") {
        $('#' + name.replace(/\./g, "\\.")).ckeditor(function() { /*
                                                                     * callback
                                                                     * code
                                                                     */
        }, introductionEditorTemplate);
    } else if (name == "caption") {
        $('#' + name.replace(/\./g, "\\.")).ckeditor(function() { /*
                                                                     * callback
                                                                     * code
                                                                     */
        }, captionEditorTemplate);
    } else if (completeEditor) {
        $('#' + name.replace(/\./g, "\\.")).ckeditor(function() { /*
                                                                     * callback
                                                                     * code
                                                                     */
        }, completeEditorTemplate);
    } else if (withoutSubSuper) {
        $('#' + name.replace(/\./g, "\\.")).ckeditor(function() { /*
                                                                     * callback
                                                                     * code
                                                                     */
        }, inlineEditorTemplate);
    } else {
        $('#' + name.replace(/\./g, "\\.")).ckeditor(function() { /*
                                                                     * callback
                                                                     * code
                                                                     */
        }, withoutSubSuperEditorTemplate);
    }
  

//  ck.config.LinkUpload = false;
//  ck.config.ImageUpload = false;
 // ck.config.FlashUpload = false;

  // Misc setup
//  ck.config['FullPage'] = false;
 // ck.config['ToolbarCanCollapse'] = false;
//  ck.config['TabSpaces'] = 4;
 // ck.config['FontFormats'] = 'p;h2;h3;h4;h5;h6;pre';
//  ck.config.EMailProtection = 'none';
//  ck.config.DisableFFTableHandles = false;
//  ck.config.ForcePasteAsPlainText = false;

//  ck.config['SkinPath'] = ck.BasePath + 'editor/skins/silver/';
//  ck.config.BaseHref = baseDocumentUrl;



}

function FCKeditor_OnComplete(editorInstance) {
  // Get around bug: http://dev.fckeditor.net/ticket/1482
  editorInstance.ResetIsDirty();
  if ('resource.content' == editorInstance.Name) {
    enableSubmit();
  }
}

function disableSubmit() {
  document.getElementById("saveButton").disabled = true;
  document.getElementById("saveAndViewButton").disabled = true;
  return true;
}

function enableSubmit() {
  document.getElementById("saveButton").disabled = false;
  document.getElementById("saveAndViewButton").disabled = false;
  return true;
}
