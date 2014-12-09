/*
 *  Vortex Admin - Embedded listing
 *
 *  Async actions a la regular admin (but some different "wirering")
 *
 */
 
vrtxAdmin._$(document).ready(function () {
  var vrtxAdm = vrtxAdmin;

  /* Delete action */
  vrtxAdm.getFormAsync({
    selector: ".delete-action",
    selectorClass: "globalmenu",
    insertAfterOrReplaceClass: "#directory-listing",
    nodeType: "div",
    simultanSliding: true,
    funcAfterComplete: updateIframeHeight
  });
  vrtxAdm.completeFormAsync({
    selector: "form#deleteResourceService-form input[type=submit]",
    post: true,
    funcCancel: function() {
      $("#upload-action").click();
    },
    funcComplete: updateListing
  });
  
  /* Upload action */
  $("#upload-action").hide();
  if (vrtxAdm.isIOS5) {
  } else {
    vrtxAdm.getFormAsync({
      selector: "#upload-action",
      selectorClass: "vrtx-admin-form",
      insertAfterOrReplaceClass: "#directory-listing",
      nodeType: "div",
      focusElement: "",
      simultanSliding: true,
      funcComplete: function (p) {
        vrtxAdm.initFileUpload();
      },
      funcAfterComplete: updateIframeHeight
    });
    /*
    $(document).on("click", ".vrtx-file-upload", function () {
      $("#file").click();
    });
    // Auto-trigger Upload when have choosen files
    $(document).on("change", "#file", function () {
      $("form#fileUploadService-form .vrtx-focus-button").click();
    });
    */
    vrtxAdm.completeFormAsync({
      selector: "form#fileUploadService-form .vrtx-focus-button",
      errorContainer: "errorContainer",
      errorContainerInsertAfter: "h3",
      post: true,
      funcProceedCondition: function(opts) {
        updateIframeHeight(250);
        return ajaxUpload(opts);
      },
      funcComplete: updateListing
    });
    $("#upload-action").click();
  }
});

function updateListing() {
  vrtxAdmin.serverFacade.getHtml(location.href, {
    success: function (results, status, resp) {
      var html = $($.parseHTML(results)).filter("#directory-listing").html();
      vrtxAdmin.cachedBody.find("#directory-listing").html(html);
      $("#upload-action").click();
    }
  }); 
}

function updateIframeHeight(minH) {
  if (window != top) {
    var minHeight = (typeof minH === "number") ? minH : 0; /* Use a minimum height if specified in function parameter */
    var iframes = $(window.parent.document).find(".admin-fixed-resources-iframe").filter(":visible");
    for (var i = 0, len = iframes.length; i < len; i++) {
      var iframe = iframes[i];
      /* Taken from iframe-view.js */
      var computedHeight = Math.max(minHeight, Math.ceil(iframe.contentWindow.document.body.offsetHeight) + 15);
      computedHeight = (computedHeight - ($.browser.msie ? 4 : 0));
      iframe.style.height = computedHeight + 'px';
    }
  }
}