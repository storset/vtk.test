/* 
 * Vortex HTML5 Canvas image editor
 *
 */

function VrtxImageEditor() {
  var instance; // Class-like singleton pattern (p.145 JavaScript Patterns)
  VrtxImageEditor = function VrtxImageEditor() {
    return instance;
  };
  VrtxImageEditor.prototype = this;
  instance = new VrtxImageEditor();
  instance.constructor = VrtxImageEditor;

  this.img = null;
  this.canvas = null;
  this.ctx = null;
  this.rw = null;
  this.rh = null;
  this.origw = null;
  this.origh = null;
  this.ratio = 1;
  this.restorePoints = [],
  this.keepAspectRatio = true;
  this.scaledBeforeCrop = false,
  this.hasCropBeenInitialized = false;

  return instance;
};

var vrtxImageEditor = new VrtxImageEditor();

$(function () {
  var imageEditorElm = $("#vrtx-image-editor-wrapper");
  if('getContext' in document.createElement('canvas') && imageEditorElm.length) {
    vrtxImageEditor.init(imageEditorElm);   
  }
});

VrtxImageEditor.prototype.init = function init(imageEditorElm) {
  var editor = this;

  imageEditorElm.addClass("canvas-supported");
  var $canvas = imageEditorElm.find("#vrtx-image-editor");
  editor.canvas = $canvas[0];
  editor.ctx = editor.canvas.getContext('2d');

  editor.img = new Image();
  var path = location.href;
  editor.img.src = path.substring(0, path.indexOf("?"));
  editor.img.onload = function () {
    editor.rw = editor.origw = editor.img.width;
    editor.rh = editor.origh = editor.img.height;
    editor.ratio = editor.origw / editor.origh;
    editor.canvas.setAttribute('width', editor.rw);
    editor.canvas.setAttribute('height', editor.rh);
    editor.canvas.width = editor.rw;
    editor.canvas.height = editor.rh;
    editor.displayDimensions(editor.rw, editor.rh);
    editor.saveRestorePoint();
    editor.ctx.drawImage(editor.img, 0, 0);
    $canvas.resizable({
      aspectRatio: editor.keepAspectRatio,
      grid: [1, 1],
      stop: function (event, ui) {
        var newWidth = Math.round(ui.size.width);
        var newHeight = Math.round(ui.size.height);
        editor.scale(newWidth, newHeight);
      },
      resize: function (event, ui) {
        editor.displayDimensions(Math.round(ui.size.width), Math.round(ui.size.height));
      }
    });
  }

  $("#app-content").delegate("#vrtx-image-crop", "click", function (e) {
    if (editor.hasCropBeenInitialized) {
      editor.rw = editor.origw = theSelection.w;
      editor.rh = editor.origh = theSelection.h;
      editor.ratio = editor.origw / editor.origh;
      editor.updateDimensions(editor.rw, editor.rh);
      editor.ctx.drawImage(editor.img, theSelection.x, theSelection.y, theSelection.w, theSelection.h, 
                                                    0,              0, theSelection.w, theSelection.h);
      editor.resetCropPlugin();
      $(this).val("Start beskjæring...");
      $("#vrtx-image-editor").resizable("enable");
      editor.saveRestorePoint();
      editor.renderRestorePoint();
      editor.hasCropBeenInitialized = false;
    } else {
      if (editor.scaledBeforeCrop) {
        editor.saveRestorePoint();
        editor.img.src = editor.restorePoints[editor.restorePoints.length - 1];
        editor.img.onload = function () {
          editor.ctx.drawImage(editor.img, 0, 0);
          $(this).val("Beskjær bilde");
          $("#vrtx-image-editor").resizable("disable");
          initSelection(editor.canvas, editor.ctx, editor.img);
          editor.scaledBeforeCrop = false;
        }
      } else {
        $(this).val("Beskjær bilde");
        $("#vrtx-image-editor").resizable("disable");
        initSelection(editor.canvas, editor.ctx, editor.img);
      }
      editor.hasCropBeenInitialized = true;
    }
    e.stopPropagation();
    e.preventDefault();
  });

  $("#app-content").delegate("#resource-width, #resource-height", "change", function (e) {
    var w = parseInt($.trim($("#resource-width").val()));
    var h = parseInt($.trim($("#resource-height").val()));
    if (!w.isNaN && !h.isNaN) {
      if (w !== editor.rw) {
        if (editor.keepAspectRatio) {
          h = w / editor.ratio;
          h = Math.round(h);
        }
        $("#resource-height").val(h)
      } else if (h !== editor.rh) {
        if (editor.keepAspectRatio) {
          w = h * editor.ratio;
          w = Math.round(w);
        }
        $("#resource-width").val(w)
      }
      editor.scale(w, h);
    }
  });

  $("#app-content").delegate("#resource-width", "keydown", function (e) {
    if (e.which == 38 || e.which == 40) {
      var w = parseInt($.trim($("#resource-width").val()));
      var h = parseInt($.trim($("#resource-height").val()));
      if (!w.isNaN && !h.isNaN) {
        if (e.which == 38) {
          w++;
        } else {
          if (w > 2) {
            w--;
          }
        }
        if (editor.keepAspectRatio) {
          h = w / editor.ratio;
          h = Math.round(h);
        }
        $("#resource-width").val(w);
        $("#resource-height").val(h);
        editor.scale(w, h);
      }
    }
  });

  $("#app-content").delegate("#resource-height", "keydown", function (e) {
    if (e.which == 38 || e.which == 40) {
      var w = parseInt($.trim($("#resource-width").val()));
      var h = parseInt($.trim($("#resource-height").val()));
      if (!w.isNaN && !h.isNaN) {
        if (e.which == 38) {
          h++;
        } else {
          if (h > 2) {
            h--;
          }
        }
        if (editor.keepAspectRatio) {
          w = h * editor.ratio;
          w = Math.round(w);
        }
        $("#resource-width").val(w);
        $("#resource-height").val(h);
        editor.scale(w, h);
      }
    }
  });
  /*
  $("#app-content").delegate("#vrtx-image-filters-sharpen", "click", function (e) {
    editor.filter(JSManipulate.sharpen.filter)
  });
  $("#app-content").delegate("#vrtx-image-filters-gray", "click", function (e) {
    editor.filter(JSManipulate.grayscale.filter)
  });
  $("#app-content").delegate("#vrtx-image-filters-sepia", "click", function (e) {
    editor.filter(JSManipulate.sepia.filter, {amount:$("#vrtx-image-filters-sepia-slider").slider("option", "value")})
  });
  */
};

VrtxImageEditor.prototype.filter = function filter(filter, options) {
  var editor = this;

  setTimeout(function() {
    $("#vrtx-image-editor-preview").addClass("loading");
    $("#vrtx-image-crop").attr("disabled", "disabled");
    $(".vrtx-image-filters").attr("disabled", "disabled");
  }, 1);
  setTimeout(function() {
    var data = editor.ctx.getImageData(0,0,editor.canvas.width, editor.canvas.height) 
    filter(data, options); 
    editor.ctx.putImageData(data,0,0); 
    $("#vrtx-image-editor-preview").removeClass("loading");
    $("#vrtx-image-crop").removeAttr("disabled");
    $(".vrtx-image-filters").removeAttr("disabled");
  }, 50);
};


VrtxImageEditor.prototype.scale = function scale(newWidth, newHeight) {
  var editor = this;

  if(newWidth < editor.origw && $("#lanczos-downscaling:checked").length) { // Downscaling with Lanczos3
    editor.rw = newWidth;
    editor.rh = newHeight;
    editor.updateDimensions(editor.rw, editor.rh);
    new thumbnailer(editor.canvas, editor.ctx, editor.img, editor.rw, 3);
  } else { // Upscaling (I think with nearest neighbour. TODO: should be bicubic or bilinear)
    editor.rw = newWidth;
    editor.rh = newHeight;
    editor.updateDimensions(editor.rw, editor.rh);
    editor.ctx.drawImage(editor.img, 0, 0, editor.rw, editor.rh); 
  }
  editor.scaledBeforeCrop = true; 
};

VrtxImageEditor.prototype.resetCropPlugin = function resetCropPlugin() {
  $("#vrtx-image-editor").unbind("mousemove").unbind("mousedown").unbind("mouseup");
  iMouseX, iMouseY = 1;
  theSelection;
};

VrtxImageEditor.prototype.updateDimensions = function updateDimensions(w, h) {
  var editor = this;

  editor.canvas.setAttribute('width', w);
  editor.canvas.setAttribute('height', h);
  editor.canvas.width = w;
  editor.canvas.height = h;
  $(".ui-wrapper").css({
    "width": w,
    "height": h
  });
  $("#vrtx-image-editor").css({
    "width": w,
    "height": h
  });
  editor.displayDimensions(w, h);
};

VrtxImageEditor.prototype.displayDimensions = function displayDimensions(w, h) {
  if ($("#vrtx-image-dimensions-crop").length) {
    $("#resource-width").val(w);
    $("#resource-height").val(h);
  } else {
    var dimensionHtml = '<div id="vrtx-image-dimensions-crop">'
                      + '<div class="property-label">Bredde</div>'
                      + '<div class="vrtx-textfield" id="vrtx-textfield-width"><input id="resource-width" type="text" value="' + w + '" size="6" /></div>'
                      + '<div class="property-label">Høyde</div>'
                      + '<div class="vrtx-textfield" id="vrtx-textfield-height"><input id="resource-height" type="text" value="' + h + '" size="6" /></div>'
                      + '<div id="vrtx-image-crop-button"><div class="vrtx-button">'
                      + '<input type="button" id="vrtx-image-crop" value="Start beskjæring..." /></div></div>'
                      + '<div id="vrtx-lanczos-downscaling-wrapper"><input type="checkbox" id="lanczos-downscaling" />&nbsp;'
                      + '<label for="lanczos-downscaling" />Bruk Lanczos3 ved nedskalering</label></div>'
                      + '</div>';
                    //  + '<div class="vrtx-button-small vrtx-image-filters"><input type="button" id="vrtx-image-filters-sharpen" value="Skarpere" /></div>'
                    //  + '<div class="vrtx-button-small vrtx-image-filters"><input type="button" id="vrtx-image-filters-gray" value="Gråskala" /></div>'
                    //  + '<div class="vrtx-button-small vrtx-image-filters"><input type="button" id="vrtx-image-filters-sepia" value="Sepia" /></div>'
                    //  + '<div class="vrtx-image-filters-slider" id="vrtx-image-filters-sepia-slider"></div>';
    $(dimensionHtml).insertBefore("#vrtx-image-editor-preview");
    // $("#vrtx-image-filters-sepia-slider").slider({min: 0, max: 25, value: 10});
  }
};

/* Save/render restore points 
 * Credits: http://hyankov.wordpress.com/2010/12/26/how-to-implement-html5-canvas-undo-function/
 * TODO: Undo/redo functionality
 */
VrtxImageEditor.prototype.saveRestorePoint = function saveRestorePoint() {
  var editor = this;

  var imgSrc = editor.canvas.toDataURL("image/png");
  editor.restorePoints.push(imgSrc);
};

VrtxImageEditor.prototype.renderRestorePoint = function renderRestorePoint() {
  var editor = this;

  editor.img.src = editor.restorePoints[editor.restorePoints.length - 1];
  editor.img.onload = function () {
    editor.ctx.drawImage(editor.img, 0, 0);
  }
};

/* Thumbnailer / Lanczos algorithm for downscaling
 * Credits: http://stackoverflow.com/questions/2303690/resizing-an-image-in-an-html5-canvas
 *
 * Modified by USIT to use Web Workers if supported for process1 and process2 (otherwise degrade to setTimeout)
 *
 * TODO: Optimize and multiple Web Workers pr. process (tasking)
 *
 */

/* elem: Canvas element
 * ctx: Canvas 2D context 
 * img: Image element
 * sx: Scaled width
 * lobes: kernel radius (e.g. 3)
 */
function thumbnailer(elem, ctx, img, sx, lobes) {
  var canvas = elem;
  elem.width = img.width;
  elem.height = img.height;
  elem.style.display = "none";
  $("#vrtx-image-editor-preview").addClass("loading");
  $("#vrtx-image-crop").attr("disabled", "disabled");
  ctx.drawImage(img, 0, 0);

  var w = sx;
  var h = Math.round(img.height * w / img.width);
  var ratio = img.width / w;
  var data = {
    src: ctx.getImageData(0, 0, img.width, img.height),
    lobes: lobes,
    dest: {
      width: w,
      height: h,
      data: new Array(w * h * 3)
    },
    ratio: ratio,
    rcp_ratio: 2 / ratio,
    range2: Math.ceil(ratio * lobes / 2),
    cacheLanc: {},
    center: {},
    icenter: {}
  };

  // Used for Web Workers or setTimeout (inject scripts and use methods inside)
  var process1Url = '/vrtx/__vrtx/static-resources/js/image-editor/lanczos-process1.js';
  var process2Url = '/vrtx/__vrtx/static-resources/js/image-editor/lanczos-process2.js';

  if("Worker" in window && !$.browser.mozilla) { // Use Web Workers if supported);
    var workerLanczosProcess1 = new Worker(process1Url);
    var workerLanczosProcess2 = new Worker(process2Url); 
    workerLanczosProcess1.postMessage(data);
    workerLanczosProcess1.addEventListener('message', function(e) {
      var data = e.data;
      if(data) {   
        canvas.width = data.dest.width;
        canvas.height = data.dest.height;
        ctx.drawImage(img, 0, 0);
        data.src = ctx.getImageData(0, 0, data.dest.width, data.dest.height);
        workerLanczosProcess2.postMessage(data);
      } 
    }, false);
    workerLanczosProcess2.addEventListener('message', function(e) { 
      var data = e.data;
      if(data) { 
        ctx.putImageData(data.src, 0, 0); 
        elem.style.display = "block";
        $("#vrtx-image-editor-preview").removeClass("loading");
        $("#vrtx-image-crop").removeAttr("disabled"); 
      }
    }, false);
  } else { // Otherwise gracefully degrade to using setTimeout
    var headID = document.getElementsByTagName("head")[0];  
    var process1Script = document.createElement('script');
    var process2Script = document.createElement('script');
    process1Script.type = 'text/javascript';
    process2Script.type = 'text/javascript';
    process1Script.src = process1Url;
    process2Script.src = process2Url;
    headID.appendChild(process1Script);
    headID.appendChild(process2Script);

    process1Script.onload = function() {
      var u = 0; 
      var lanczos = lanczosCreate(data.lobes);
      var proc1 = setTimeout(function() {
        data = process1(data, u, lanczos);
        if(++u < data.dest.width) {
          setTimeout(arguments.callee, 0);
        } else {
          var proc2 = setTimeout(function() {
            canvas.width = data.dest.width;
            canvas.height = data.dest.height;
            ctx.drawImage(img, 0, 0);
            data.src = ctx.getImageData(0, 0, data.dest.width, data.dest.height);
            data = process2(data);
            ctx.putImageData(data.src, 0, 0);
            elem.style.display = "block";
            $("#vrtx-image-editor-preview").removeClass("loading");
            $("#vrtx-image-crop").removeAttr("disabled"); 
          }, 0);
        }
      }, 0);
    }
  }
}

/*
 * Crop plugin
 * Credits: http://www.script-tutorials.com/demos/197/index.html
 * TODO: optimize
 * Modified slightly by USIT
 */

var iMouseX, iMouseY = 1;
var theSelection;

// Define Selection constructor
function Selection(x, y, w, h) {
  this.x = x; // initial positions
  this.y = y;
  this.w = w; // and size
  this.h = h;
  this.px = x; // extra variables to dragging calculations
  this.py = y;
  this.csize = 6; // resize cubes size
  this.csizeh = 10; // resize cubes size (on hover)
  this.bHow = [false, false, false, false]; // hover statuses
  this.iCSize = [this.csize, this.csize, this.csize, this.csize]; // resize cubes sizes
  this.bDrag = [false, false, false, false]; // drag statuses
  this.bDragAll = false; // drag whole selection
}

// Define Selection draw method
Selection.prototype.draw = function (ctx, img) {
  ctx.strokeStyle = '#000';
  ctx.lineWidth = 2;
  ctx.strokeRect(this.x, this.y, this.w, this.h);
  // draw part of original image
  if (this.w > 0 && this.h > 0) {
    ctx.drawImage(img, this.x, this.y, this.w, this.h, this.x, this.y, this.w, this.h);
  }
  // draw resize cubes
  ctx.fillStyle = '#fff';
  ctx.fillRect(this.x - this.iCSize[0], this.y - this.iCSize[0], this.iCSize[0] * 2, this.iCSize[0] * 2);
  ctx.fillRect(this.x + this.w - this.iCSize[1], this.y - this.iCSize[1], this.iCSize[1] * 2, this.iCSize[1] * 2);
  ctx.fillRect(this.x + this.w - this.iCSize[2], this.y + this.h - this.iCSize[2], this.iCSize[2] * 2, this.iCSize[2] * 2);
  ctx.fillRect(this.x - this.iCSize[3], this.y + this.h - this.iCSize[3], this.iCSize[3] * 2, this.iCSize[3] * 2);
}

function drawScene(ctx, img) { // Main drawScene function
  ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height); // clear canvas
  // draw source image
  ctx.drawImage(img, 0, 0, ctx.canvas.width, ctx.canvas.height);
  // and make it darker
  ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
  ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);
  // draw selection
  theSelection.draw(ctx, img);
}

function initSelection(canvas, ctx, img) {
  // create initial selection
  theSelection = new Selection(40, 40, $(canvas).width() - 40, $(canvas).height() - 40);
  $('#vrtx-image-editor').bind("mousemove", function (e) { // binding mouse move event
    var canvasOffset = $(canvas).offset();
    iMouseX = Math.floor(e.pageX - canvasOffset.left);
    iMouseY = Math.floor(e.pageY - canvasOffset.top);
    // in case of drag of whole selector
    if (theSelection.bDragAll) {
      theSelection.x = iMouseX - theSelection.px;
      theSelection.y = iMouseY - theSelection.py;
    }
    for (i = 0; i < 4; i++) {
      theSelection.bHow[i] = false;
      theSelection.iCSize[i] = theSelection.csize;
    }
    // hovering over resize cubes
    if (iMouseX > theSelection.x - theSelection.csizeh 
     && iMouseX < theSelection.x + theSelection.csizeh
     && iMouseY > theSelection.y - theSelection.csizeh
     && iMouseY < theSelection.y + theSelection.csizeh) {
      theSelection.bHow[0] = true;
      theSelection.iCSize[0] = theSelection.csizeh;
    }
    if (iMouseX > theSelection.x + theSelection.w - theSelection.csizeh 
     && iMouseX < theSelection.x + theSelection.w + theSelection.csizeh
     && iMouseY > theSelection.y - theSelection.csizeh
     && iMouseY < theSelection.y + theSelection.csizeh) {
      theSelection.bHow[1] = true;
      theSelection.iCSize[1] = theSelection.csizeh;
    }
    if (iMouseX > theSelection.x + theSelection.w - theSelection.csizeh
     && iMouseX < theSelection.x + theSelection.w + theSelection.csizeh
     && iMouseY > theSelection.y + theSelection.h - theSelection.csizeh
     && iMouseY < theSelection.y + theSelection.h + theSelection.csizeh) {
      theSelection.bHow[2] = true;
      theSelection.iCSize[2] = theSelection.csizeh;
    }
    if (iMouseX > theSelection.x - theSelection.csizeh
     && iMouseX < theSelection.x + theSelection.csizeh
     && iMouseY > theSelection.y + theSelection.h - theSelection.csizeh
     && iMouseY < theSelection.y + theSelection.h + theSelection.csizeh) {
      theSelection.bHow[3] = true;
      theSelection.iCSize[3] = theSelection.csizeh;
    }
    // in case of dragging of resize cubes
    var iFW, iFH;
    if (theSelection.bDrag[0]) {
      var iFX = iMouseX - theSelection.px;
      var iFY = iMouseY - theSelection.py;
      iFW = theSelection.w + theSelection.x - iFX;
      iFH = theSelection.h + theSelection.y - iFY;
    }
    if (theSelection.bDrag[1]) {
      var iFX = theSelection.x;
      var iFY = iMouseY - theSelection.py;
      iFW = iMouseX - theSelection.px - iFX;
      iFH = theSelection.h + theSelection.y - iFY;
    }
    if (theSelection.bDrag[2]) {
      var iFX = theSelection.x;
      var iFY = theSelection.y;
      iFW = iMouseX - theSelection.px - iFX;
      iFH = iMouseY - theSelection.py - iFY;
    }
    if (theSelection.bDrag[3]) {
      var iFX = iMouseX - theSelection.px;
      var iFY = theSelection.y;
      iFW = theSelection.w + theSelection.x - iFX;
      iFH = iMouseY - theSelection.py - iFY;
    }
    if (iFW > theSelection.csizeh * 2 && iFH > theSelection.csizeh * 2) {
      theSelection.w = iFW;
      theSelection.h = iFH;
      theSelection.x = iFX;
      theSelection.y = iFY;
    }
    drawScene(ctx, img);
  });
  $('#vrtx-image-editor').bind("mousedown", function (e) { // binding mousedown event
    var canvasOffset = $(canvas).offset();
    iMouseX = Math.floor(e.pageX - canvasOffset.left);
    iMouseY = Math.floor(e.pageY - canvasOffset.top);
    theSelection.px = iMouseX - theSelection.x;
    theSelection.py = iMouseY - theSelection.y;
    if (theSelection.bHow[0]) {
      theSelection.px = iMouseX - theSelection.x;
      theSelection.py = iMouseY - theSelection.y;
    }
    if (theSelection.bHow[1]) {
      theSelection.px = iMouseX - theSelection.x - theSelection.w;
      theSelection.py = iMouseY - theSelection.y;
    }
    if (theSelection.bHow[2]) {
      theSelection.px = iMouseX - theSelection.x - theSelection.w;
      theSelection.py = iMouseY - theSelection.y - theSelection.h;
    }
    if (theSelection.bHow[3]) {
      theSelection.px = iMouseX - theSelection.x;
      theSelection.py = iMouseY - theSelection.y - theSelection.h;
    }
    if (iMouseX > theSelection.x + theSelection.csizeh
     && iMouseX < theSelection.x + theSelection.w - theSelection.csizeh
     && iMouseY > theSelection.y + theSelection.csizeh
     && iMouseY < theSelection.y + theSelection.h - theSelection.csizeh) {
      theSelection.bDragAll = true;
    }
    for (i = 0; i < 4; i++) {
      if (theSelection.bHow[i]) {
        theSelection.bDrag[i] = true;
      }
    }
  });
  $('#vrtx-image-editor').bind("mouseup", function (e) { // binding mouseup event
    theSelection.bDragAll = false;
    for (i = 0; i < 4; i++) {
      theSelection.bDrag[i] = false;
    }
    theSelection.px = 0;
    theSelection.py = 0;
  });
  drawScene(ctx, img);
}

/* Filters */

/* 
=========================================================================
   JSManipulate v1.0 (2011-08-01)

Javascript image filter & effect library

Developed by Joel Besada (http://www.joelb.me)
Demo page: http://www.joelb.me/jsmanipulate

MIT LICENSED (http://www.opensource.org/licenses/mit-license.php)
Copyright (c) 2011, Joel Besada
=========================================================================
*/
function FilterUtils(){this.HSVtoRGB=function(a,b,f){var d,g,c,e=Math.floor(a*6),h=a*6-e,a=f*(1-b),k=f*(1-h*b),b=f*(1-(1-h)*b);switch(e%6){case 0:d=f;g=b;c=a;break;case 1:d=k;g=f;c=a;break;case 2:d=a;g=f;c=b;break;case 3:d=a;g=k;c=f;break;case 4:d=b;g=a;c=f;break;case 5:d=f,g=a,c=k}return[d*255,g*255,c*255]};this.RGBtoHSV=function(a,b,f){a/=255;b/=255;f/=255;var d=Math.max(a,b,f),g=Math.min(a,b,f),c,e=d-g;if(d===g)c=0;else{switch(d){case a:c=(b-f)/e+(b<f?6:0);break;case b:c=(f-a)/e+2;break;case f:c=
(a-b)/e+4}c/=6}return[c,d===0?0:e/d,d]};this.getPixel=function(a,b,f,d,g){var c=(f*d+b)*4;if(b<0||b>=d||f<0||f>=g)return[a[(this.clampPixel(f,0,g-1)*d+this.clampPixel(b,0,d-1))*4],a[(this.clampPixel(f,0,g-1)*d+this.clampPixel(b,0,d-1))*4+1],a[(this.clampPixel(f,0,g-1)*d+this.clampPixel(b,0,d-1))*4+2],a[(this.clampPixel(f,0,g-1)*d+this.clampPixel(b,0,d-1))*4+3]];return[a[c],a[c+1],a[c+2],a[c+3]]};var h=!1,e;this.gaussianRandom=function(){if(h)return h=!1,e;else{var a,b,f;do a=2*Math.random()-1,b=2*
Math.random()-1,f=a*a+b*b;while(f>=1||f===0);f=Math.sqrt(-2*Math.log(f)/f);e=b*f;h=!0;return a*f}};this.clampPixel=function(a,b,f){return a<b?b:a>f?f:a};this.triangle=function(a){a=this.mod(a,1);return 2*(a<0.5?a:1-a)};this.mod=function(a,b){var f=parseInt(a/b,10);a-=f*b;if(a<0)return a+b;return a};this.mixColors=function(a,b,f){var d=this.linearInterpolate(a,b[0],f[0]),g=this.linearInterpolate(a,b[1],f[1]),c=this.linearInterpolate(a,b[2],f[2]),a=this.linearInterpolate(a,b[3],f[3]);return[d,g,c,a]};
this.linearInterpolate=function(a,b,f){return b+a*(f-b)};this.bilinearInterpolate=function(a,b,f,d,g,c){var e=f[0],h=f[1],k=f[2],i=d[0],o=d[1],p=d[2],s=g[0],n=g[1],m=g[2],q=g[3],t=c[0],r=c[1],g=c[2],u=c[3],c=1-a,v=1-b,f=c*f[3]+a*d[3],f=v*f+b*(c*q+a*u),e=v*(c*e+a*i)+b*(c*s+a*t),h=v*(c*h+a*o)+b*(c*n+a*r);return[e,h,v*(c*k+a*p)+b*(c*m+a*g),f]};this.tableFilter=function(a,b,f,d){for(var g=0;g<d;g++)for(var c=0;c<f;c++)for(var e=(g*f+c)*4,h=0;h<3;h++)a[e+h]=b[a[e+h]]};this.convolveFilter=function(a,b,
f,d){var g=[],c,e;c=e=Math.sqrt(b.length);c=parseInt(c/2,10);for(var h=parseInt(e/2,10),k=0;k<d;k++)for(var i=0;i<f;i++){for(var o=(k*f+i)*4,p=0,s=0,n=0,m=-c;m<=c;m++)for(var q=k+m,q=0<=q&&q<d?q*f:k*f,t=e*(m+c)+h,r=-h;r<=h;r++){var u=b[t+r];if(u!==0){var v=i+r;0<=v&&v<f||(v=i);v=(q+v)*4;p+=u*a[v];s+=u*a[v+1];n+=u*a[v+2]}}g[o]=parseInt(p+0.5,10);g[o+1]=parseInt(s+0.5,10);g[o+2]=parseInt(n+0.5,10);g[o+3]=a[o+3]}for(b=0;b<g.length;b++)a[b]=g[b]};this.transformFilter=function(a,b,f,d){for(var g=[],c=
[],e=0;e<a.length;e++)c[e]=a[e];for(e=0;e<d;e++)for(var h=0;h<f;h++){var k=(e*f+h)*4;b.apply(this,[h,e,g]);var i=Math.floor(g[0]),o=Math.floor(g[1]),p=g[0]-i,s=g[1]-o,n,m,q;i>=0&&i<f-1&&o>=0&&o<d-1?(i=(f*o+i)*4,n=[a[i],a[i+1],a[i+2],a[i+3]],m=[a[i+4],a[i+5],a[i+6],a[i+7]],q=[a[i+f*4],a[i+f*4+1],a[i+f*4+2],a[i+f*4+3]],i=[a[i+(f+1)*4],a[i+(f+1)*4+1],a[i+(f+1)*4+2],a[i+(f+1)*4+3]]):(n=this.getPixel(a,i,o,f,d),m=this.getPixel(a,i+1,o,f,d),q=this.getPixel(a,i,o+1,f,d),i=this.getPixel(a,i+1,o+1,f,d));p=
this.bilinearInterpolate(p,s,n,m,q,i);c[k]=p[0];c[k+1]=p[1];c[k+2]=p[2];c[k+3]=p[3]}for(b=0;b<c.length;b++)a[b]=c[b]}}
function BlurFilter(){this.name="Blur";this.isDirAnimatable=!1;this.defaultValues={amount:3};this.valueRanges={amount:{min:0,max:10}};this.filter=function(h,e){var a=h.width,b=a<<2,f=h.height,d=h.data,g;g=e.amount;g<0&&(g=0);g=g>=2.5?0.98711*g-0.9633:g>=0.5?3.97156-4.14554*Math.sqrt(1-0.26891*g):2*g*(3.97156-4.14554*Math.sqrt(0.865545));var c=g*g,j=c*g,l=1.57825+2.44413*g+1.4281*c+0.422205*j;g=(2.44413*g+2.85619*c+1.26661*j)/l;for(var c=-(1.4281*c+1.26661*j)/l,j=0.422205*j/l,l=1-(g+c+j),k=0,i,o,p,
s,n,m,k=0;k<3;k++)for(var q=0;q<f;q++){i=q*b+k;o=q*b+(a-1<<2)+k;for(m=n=s=p=d[i];i<=o;i+=4)p=l*d[i]+g*s+c*n+j*m,d[i]=p,m=n,n=s,s=p;i=q*b+(a-1<<2)+k;o=q*b+k;for(m=n=s=p=d[i];i>=o;i-=4)p=l*d[i]+g*s+c*n+j*m,d[i]=p,m=n,n=s,s=p}for(k=0;k<3;k++)for(q=0;q<a;q++){i=(q<<2)+k;o=(f-1)*b+(q<<2)+k;for(m=n=s=p=d[i];i<=o;i+=b)p=l*d[i]+g*s+c*n+j*m,d[i]=p,m=n,n=s,s=p;i=(f-1)*b+(q<<2)+k;o=(q<<2)+k;for(m=n=s=p=d[i];i>=o;i-=b)p=l*d[i]+g*s+c*n+j*m,d[i]=p,m=n,n=s,s=p}}}
function BrightnessFilter(){this.name="Brightness";this.isDirAnimatable=!0;this.defaultValues={amount:0};this.valueRanges={amount:{min:-1,max:1}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;for(var g=a.amount===void 0?this.defaultValues.amount:a.amount,c=0;c<f;c++)for(var j=0;j<b;j++){var l=(c*b+j)*4,k=h.RGBtoHSV(d[l],d[l+1],d[l+2]);k[2]+=g;k[2]<0?k[2]=0:k[2]>1&&(k[2]=1);for(var k=h.HSVtoRGB(k[0],k[1],k[2]),i=0;i<3;i++)d[l+i]=
k[i]}}}function BumpFilter(){this.name="Bump";this.isDirAnimatable=!0;this.defaultValues={};this.valueRanges={};var h=new FilterUtils;this.filter=function(e){h.convolveFilter(e.data,[-1,-1,0,-1,1,1,0,1,1],e.width,e.height)}}
function CircleSmearFilter(){this.name="Circle Smear";this.isDirAnimatable=!1;this.defaultValues={size:4,density:0.5,mix:0.5};this.valueRanges={size:{min:1,max:10},density:{min:0,max:1},mix:{min:0,max:1}};var h=new FilterUtils;this.filter=function(e,a){for(var b=e.width,f=e.height,d=e.data,g=[],c=0;c<d.length;c++)g[c]=d[c];if(a===void 0)a=this.defaultValues;var j=a.size===void 0?this.defaultValues.size:a.size;j<1&&(j=1);j=parseInt(j,10);c=a.mix===void 0?this.defaultValues.mix:a.mix;j+=1;for(var l=
j*j,k=parseInt(2*(a.density===void 0?this.defaultValues.density:a.density)/30*b*f/2,10),i=0;i<k;i++)for(var o=(Math.random()*Math.pow(2,32)&2147483647)%b,p=(Math.random()*Math.pow(2,32)&2147483647)%f,s=[d[(p*b+o)*4],d[(p*b+o)*4+1],d[(p*b+o)*4+2],d[(p*b+o)*4+3]],n=o-j;n<o+j+1;n++)for(var m=p-j;m<p+j+1;m++){var q=(n-o)*(n-o)+(m-p)*(m-p);if(n>=0&&n<b&&m>=0&&m<f&&q<=l)for(var q=h.mixColors(c,[g[(m*b+n)*4],g[(m*b+n)*4+1],g[(m*b+n)*4+2],g[(m*b+n)*4+3]],s),t=0;t<3;t++)g[(m*b+n)*4+t]=q[t]}for(b=0;b<g.length;b++)d[b]=
g[b]}}
function ContrastFilter(){this.name="Contrast";this.isDirAnimatable=!0;this.defaultValues={amount:1};this.valueRanges={amount:{min:0,max:2}};if(FilterUtils){var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;var g=a.amount===void 0?this.defaultValues.amount:a.amount;g<0&&(g=0);for(var c=[],j=0;j<256;j++)c[j]=parseInt(255*((j/255-0.5)*g+0.5),10);h.tableFilter(d,c,b,f)}}else console&&console.error("Unable to find filterutils.js, please include this file! (Required by "+this.name+
" filter)")}
function CrossSmearFilter(){this.name="Cross Smear";this.isDirAnimatable=!1;this.defaultValues={distance:8,density:0.5,mix:0.5};this.valueRanges={distance:{min:0,max:30},density:{min:0,max:1},mix:{min:0,max:1}};var h=new FilterUtils;this.filter=function(e,a){for(var b=e.width,f=e.height,d=e.data,g=[],c=0;c<d.length;c++)g[c]=d[c];if(a===void 0)a=this.defaultValues;c=a.distance===void 0?this.defaultValues.distance:a.distance;c<0&&(c=0);for(var c=parseInt(c,10),j=a.mix===void 0?this.defaultValues.mix:a.mix,
l=parseInt(2*(a.density===void 0?this.defaultValues.density:a.density)*b*f/(c+1),10),k=0;k<l;k++){for(var i=(Math.random()*Math.pow(2,32)&2147483647)%b,o=(Math.random()*Math.pow(2,32)&2147483647)%f,p=Math.random()*Math.pow(2,32)%c+1,s=[d[(o*b+i)*4],d[(o*b+i)*4+1],d[(o*b+i)*4+2],d[(o*b+i)*4+3]],n,m,q=i-p;q<i+p+1;q++)if(q>=0&&q<b){n=[g[(o*b+q)*4],g[(o*b+q)*4+1],g[(o*b+q)*4+2],g[(o*b+q)*4+3]];n=h.mixColors(j,n,s);for(m=0;m<3;m++)g[(o*b+q)*4+m]=n[m]}for(q=o-p;q<o+p+1;q++)if(q>=0&&q<f){n=[g[(q*b+i)*4],
g[(q*b+i)*4+1],g[(q*b+i)*4+2],g[(q*b+i)*4+3]];n=h.mixColors(j,n,s);for(m=0;m<3;m++)g[(q*b+i)*4+m]=n[m]}}for(b=0;b<g.length;b++)d[b]=g[b]}}
function DiffusionFilter(){this.name="Diffusion";this.isDirAnimatable=!1;this.defaultValues={scale:4};this.valueRanges={scale:{min:1,max:100}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;for(var g=a.scale===void 0?this.defaultValues.scale:a.scale,c=[],j=[],l=0;l<256;l++){var k=Math.PI*2*l/256;c[l]=g*Math.sin(k);j[l]=g*Math.cos(k)}transInverse=function(a,b,g){var d=parseInt(Math.random()*255,10),f=Math.random();g[0]=a+f*c[d];
g[1]=b+f*j[d]};h.transformFilter(d,transInverse,b,f)}}
function DitherFilter(){this.name="Dither";this.isDirAnimatable=!1;this.defaultValues={levels:3,color:!0};this.valueRanges={levels:{min:2,max:30},color:{min:!1,max:!0}};new FilterUtils;this.filter=function(h,e){var a=h.width,b=h.height,f=h.data,d=[],g,c;for(c=0;c<f.length;c++)d[c]=0;if(e===void 0)e=this.defaultValues;var j=e.levels===void 0?this.defaultValues.levels:e.levels,l=e.color===void 0?this.defaultValues.color:e.color;j<=1&&(j=1);var k=[0,0,0,0,0,7,3,5,1],i=0,o=[];for(g=0;g<j;g++)o[g]=parseInt(255*
g/(j-1),10);var p=[];for(g=0;g<256;g++)p[g]=parseInt(j*g/256,10);for(j=0;j<b;j++){var s=(j&1)==1,n;s?(i=(j*a+a-1)*4,n=-1):(i=j*a*4,n=1);for(var m=0;m<a;m++){var q=f[i],t=f[i+1],r=f[i+2];l||(q=t=r=parseInt((q+t+r)/3,10));var u=o[p[q]],v=o[p[t]];g=o[p[r]];d[i]=u;d[i+1]=v;d[i+2]=g;d[i+3]=f[i+3];var u=q-u,v=t-v,y=r-g;for(g=-1;g<=1;g++)if(q=g+j,0<=q&&q<b)for(c=-1;c<=1;c++)if(q=c+m,0<=q&&q<a){var w;w=s?k[(g+1)*3-c+1]:k[(g+1)*3+c+1];if(w!==0){var x=s?i-c*4:i+c*4,q=f[x],t=f[x+1],r=f[x+2];w/=16;q+=u*w;t+=
v*w;r+=y*w;f[x]=q;f[x+1]=t;f[x+2]=r}}i+=n*4}}for(c=0;c<d.length;c++)f[c]=d[c]}}
function EdgeFilter(){this.name="Edge Detection";this.isDirAnimatable=!0;this.defaultValues={};this.valueRanges={};var h=[-1,-2,-1,0,0,0,1,2,1],e=[-1,0,1,-2,0,2,-1,0,1];this.filter=function(a){for(var b=a.width,f=a.height,a=a.data,d=[],g=0;g<f;g++)for(var c=0;c<b;c++){var j=(g*b+c)*4,l=0,k=bh=gh=0;bv=gv=0;for(var i=-1;i<=1;i++)for(var o=g+i,o=o>=0&&o<f?o*b*4:g*b*4,p=3*(i+1)+1,s=-1;s<=1;s++){var n=c+s;n>=0&&n<b||(n=c);n*=4;var m=a[o+n],q=a[o+n+1],n=a[o+n+2],t=h[p+s],r=e[p+s];l+=parseInt(t*m,10);bh+=
parseInt(t*q,10);gh+=parseInt(t*n,10);k+=parseInt(r*m,10);gv+=parseInt(r*q,10);bv+=parseInt(r*n,10)}m=parseInt(Math.sqrt(l*l+k*k)/1.8,10);q=parseInt(Math.sqrt(gh*gh+gv*gv)/1.8,10);n=parseInt(Math.sqrt(bh*bh+bv*bv)/1.8,10);d[j]=m;d[j+1]=q;d[j+2]=n;d[j+3]=a[j+3]}for(b=0;b<d.length;b++)a[b]=d[b]}}
function EmbossFilter(){this.name="Emboss";this.isDirAnimatable=!1;this.defaultValues={height:1,angle:135,elevation:30};this.valueRanges={height:{min:1,max:10},angle:{min:0,max:360},elevation:{min:0,max:180}};this.filter=function(h,e){var a=h.width,b=h.height,f=h.data;if(e===void 0)e=this.defaultValues;for(var d=e.height===void 0?this.defaultValues.height:e.height,g=e.angle===void 0?this.defaultValues.angle:e.angle,c=e.elevation===void 0?this.defaultValues.elevation:e.elevation,g=g/180*Math.PI,c=
c/180*Math.PI,j=3*d,d=[],l=0;l<f.length;l+=4)d[l/4]=(f[l]+f[l+1]+f[l+2])/3;var k,i,o,p,l=parseInt(Math.cos(g)*Math.cos(c)*255.9,10),g=parseInt(Math.sin(g)*Math.cos(c)*255.9,10),c=parseInt(Math.sin(c)*255.9,10);o=parseInt(1530/j,10);j=o*o;o*=c;for(var s=0,n=0;n<b;n++,s+=a)for(var m=s,q=m+a,t=q+a,r=0;r<a;r++,m++,q++,t++){var u=(n*a+r)*4;n!==0&&n<b-2&&r!==0&&r<a-2?(k=d[m-1]+d[q-1]+d[t-1]-d[m+1]-d[q+1]-d[t+1],i=d[t-1]+d[t]+d[t+1]-d[m-1]-d[m]-d[m+1],k=k===0&&i===0?c:(p=k*l+i*g+o)<0?0:parseInt(p/Math.sqrt(k*
k+i*i+j),10)):k=c;f[u]=f[u+1]=f[u+2]=k}}}function ExposureFilter(){this.name="Exposure";this.isDirAnimatable=!0;this.defaultValues={exposure:1};this.valueRanges={exposure:{min:0,max:5}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;for(var g=a.exposure===void 0?this.defaultValues.exposure:a.exposure,c=[],j=0;j<256;j++)c[j]=parseInt(255*(1-Math.exp(-(j/255)*g)),10);h.tableFilter(d,c,b,f)}}
function GainFilter(){this.name="Gain/Bias";this.isDirAnimatable=!0;this.defaultValues={gain:0.5,bias:0.5};this.valueRanges={gain:{min:0,max:1},bias:{min:0,max:1}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;for(var g=a.gain===void 0?this.defaultValues.gain:a.gain,c=a.bias===void 0?this.defaultValues.bias:a.bias,j=[],l=0;l<256;l++){var k=l/255,i=(1/g-2)*(1-2*k),k=k<0.5?k/(i+1):(i-k)/(i-1);k/=(1/c-2)*(1-k)+1;j[l]=parseInt(255*
k,10)}h.tableFilter(d,j,b,f)}}
function GammaFilter(){this.name="Gamma";this.isDirAnimatable=!0;this.defaultValues={amount:1};this.valueRanges={amount:{min:0,max:2}};this.filter=function(h,e){var a=h.width,b=h.height,f=h.data;if(e===void 0)e=this.defaultValues;var d=e.amount===void 0?this.defaultValues.amount:e.amount;d<0&&(d=0);if(FilterUtils){for(var g=new FilterUtils,c=[],j=0;j<256;j++)c[j]=255*Math.pow(j/255,1/d)+0.5;g.tableFilter(f,c,a,b)}else console&&console.error("Unable to find filterutils.js, please include this file! (Required by "+this.name+
" filter)")}}function GrayscaleFilter(){this.name="Grayscale";this.isDirAnimatable=!0;this.defaultValues={};this.valueRanges={};this.filter=function(h){for(var e=h.width,a=h.height,h=h.data,b=0;b<a;b++)for(var f=0;f<e;f++){var d=(b*e+f)*4;h[d]=h[d+1]=h[d+2]=h[d]*0.3+h[d+1]*0.59+h[d+2]*0.11}}}
function HueFilter(){this.name="Hue";this.isDirAnimatable=!0;this.defaultValues={amount:0};this.valueRanges={amount:{min:-1,max:1}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;for(var g=a.amount===void 0?this.defaultValues.amount:a.amount,c=0;c<f;c++)for(var j=0;j<b;j++){var l=(c*b+j)*4,k=h.RGBtoHSV(d[l],d[l+1],d[l+2]);for(k[0]+=g;k[0]<0;)k[0]+=360;for(var k=h.HSVtoRGB(k[0],k[1],k[2]),i=0;i<3;i++)d[l+i]=k[i]}}}
function InvertFilter(){this.name="Invert";this.isDirAnimatable=!0;this.defaultValues={};this.valueRanges={};this.filter=function(h){for(var e=h.width,a=h.height,h=h.data,b=0;b<a;b++)for(var f=0;f<e;f++)for(var d=(b*e+f)*4,g=0;g<3;g++)h[d+g]=255-h[d+g]}}
function KaleidoscopeFilter(){this.name="Kaleidoscope";this.isDirAnimatable=!1;this.defaultValues={angle:0,rotation:0,sides:3,centerX:0.5,centerY:0.5};this.valueRanges={angle:{min:0,max:360},rotation:{min:0,max:360},sides:{min:1,max:30},centerX:{min:0,max:1},centerY:{min:0,max:1}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;var g=a.angle===void 0?this.defaultValues.angle:a.angle,c=a.rotation===void 0?this.defaultValues.rotation:
a.rotation,j=a.sides===void 0?this.defaultValues.sides:a.sides,l=b*(a.centerX===void 0?this.defaultValues.centerX:a.centerX),k=f*(a.centerY===void 0?this.defaultValues.centerY:a.centerY),g=g/180*Math.PI,c=c/180*Math.PI;h.transformFilter(d,function(a,b,d){a-=l;var f=b-k,b=Math.sqrt(a*a+f*f),a=Math.atan2(f,a)-g-c,a=h.triangle(a/Math.PI*j*0.5);a+=g;d[0]=l+b*Math.cos(a);d[1]=k+b*Math.sin(a)},b,f)}}
function LensDistortionFilter(){this.name="Lens Distortion";this.isDirAnimatable=!1;this.defaultValues={refraction:1.5,radius:50,centerX:0.5,centerY:0.5};this.valueRanges={refraction:{min:1,max:10},radius:{min:1,max:200},centerX:{min:0,max:1},centerY:{min:0,max:1}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;var g=a.refraction===void 0?this.defaultValues.refraction:a.refraction,c=a.radius===void 0?this.defaultValues.radius:a.radius,
j=c*c,l=b*(a.centerX===void 0?this.defaultValues.centerX:a.centerX),k=f*(a.centerY===void 0?this.defaultValues.centerY:a.centerY);h.transformFilter(d,function(a,b,c){var d=a-l,f=b-k,e=d*d,h=f*f;if(h>=j-j*e/j)c[0]=a,c[1]=b;else{var t=1/g,r=Math.sqrt((1-e/j-h/j)*j),u=r*r,d=Math.acos(d/Math.sqrt(e+u)),e=Math.PI/2-d,e=Math.asin(Math.sin(e)*t),e=Math.PI/2-d-e;c[0]=a-Math.tan(e)*r;a=Math.acos(f/Math.sqrt(h+u));e=Math.PI/2-a;e=Math.asin(Math.sin(e)*t);e=Math.PI/2-a-e;c[1]=b-Math.tan(e)*r}},b,f)}}
function LineSmearFilter(){this.name="Line Smear";this.isDirAnimatable=!1;this.defaultValues={distance:8,density:0.5,angle:0,mix:0.5};this.valueRanges={distance:{min:1,max:30},density:{min:0,max:1},angle:{min:0,max:360},mix:{min:0,max:1}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data,g=[],c;for(c=0;c<d.length;c++)g[c]=d[c];if(a===void 0)a=this.defaultValues;var j=a.distance===void 0?this.defaultValues.distance:a.distance;j<1&&(j=1);for(var j=parseInt(j,10),l=a.density===
void 0?this.defaultValues.density:a.density,k=a.angle===void 0?this.defaultValues.angle:a.angle,i=a.mix===void 0?this.defaultValues.mix:a.mix,k=k/180*Math.PI,o=Math.sin(k),k=Math.cos(k),l=parseInt(2*l*b*f/2,10),p=0;p<l;p++){var s=(Math.random()*Math.pow(2,32)&2147483647)%b,n=(Math.random()*Math.pow(2,32)&2147483647)%f,m=(Math.random()*Math.pow(2,32)&2147483647)%j+1,q=[d[(n*b+s)*4],d[(n*b+s)*4+1],d[(n*b+s)*4+2],d[(n*b+s)*4+3]],t=parseInt(m*k,10),m=parseInt(m*o,10),r=s-t,u=n-m;s+=t;n+=m;var v,y,w,x;
w=s<r?-1:1;x=n<u?-1:1;var t=s-r,m=n-u,t=Math.abs(t),m=Math.abs(m),z;if(r<b&&r>=0&&u<f&&u>=0){c=[g[(u*b+r)*4],g[(u*b+r)*4+1],g[(u*b+r)*4+2],g[(u*b+r)*4+3]];z=h.mixColors(i,c,q);for(c=0;c<3;c++)g[(u*b+r)*4+c]=z[c]}if(Math.abs(t)>Math.abs(m)){v=2*m-t;y=2*m;for(t=2*(m-t);r!=s;)if(v<=0?v+=y:(v+=t,u+=x),r+=w,r<b&&r>=0&&u<f&&u>=0){c=[g[(u*b+r)*4],g[(u*b+r)*4+1],g[(u*b+r)*4+2],g[(u*b+r)*4+3]];z=h.mixColors(i,c,q);for(c=0;c<3;c++)g[(u*b+r)*4+c]=z[c]}}else{v=2*t-m;y=2*t;for(t=2*(t-m);u!=n;)if(v<=0?v+=y:(v+=
t,r+=w),u+=x,r<b&&r>=0&&u<f&&u>=0){c=[g[(u*b+r)*4],g[(u*b+r)*4+1],g[(u*b+r)*4+2],g[(u*b+r)*4+3]];z=h.mixColors(i,c,q);for(c=0;c<3;c++)g[(u*b+r)*4+c]=z[c]}}}for(c=0;c<g.length;c++)d[c]=g[c]}}
function MaximumFilter(){this.name="Maximum";this.isDirAnimatable=!0;this.defaultValues={};this.valueRanges={};this.filter=function(h){for(var e=h.width,a=h.height,h=h.data,b=[],f=0;f<a;f++)for(var d=0;d<e;d++){for(var g=(f*e+d)*4,c=0,j=0,l=0,k=-1;k<=1;k++){var i=f+k;if(i>=0&&i<a)for(var o=-1;o<=1;o++){var p=d+o;p>=0&&p<e&&(p=(i*e+p)*4,c=Math.max(c,h[p]),j=Math.max(j,h[p+1]),l=Math.max(l,h[p+2]))}}b[g]=c;b[g+1]=j;b[g+2]=l;b[g+3]=h[g+3]}for(e=0;e<b.length;e++)h[e]=b[e]}}
function MedianFilter(){this.name="Median";this.isDirAnimatable=!1;this.defaultValues={};this.valueRanges={};this.filter=function(h){for(var e=h.width,a=h.height,h=h.data,b=[],f=0;f<a;f++)for(var d=0;d<e;d++){for(var g=(f*e+d)*4,c=[],j=[],l=[],k=-1;k<=1;k++){var i=f+k;if(i>=0&&i<a)for(var o=-1;o<=1;o++){var p=d+o;p>=0&&p<e&&(p=(i*e+p)*4,c.push(h[p]),j.push(h[p+1]),l.push(h[p+2]))}}k=function(a,c){return a-c};c.sort(k);j.sort(k);l.sort(k);b[g]=c[4];b[g+1]=j[4];b[g+2]=l[4];b[g+3]=h[g+3]}for(e=0;e<b.length;e++)h[e]=
b[e]}}function MinimumFilter(){this.name="Minimum";this.isDirAnimatable=!0;this.defaultValues={};this.valueRanges={};this.filter=function(h){for(var e=h.width,a=h.height,h=h.data,b=[],f=0;f<a;f++)for(var d=0;d<e;d++){for(var g=(f*e+d)*4,c=255,j=255,l=255,k=-1;k<=1;k++){var i=f+k;if(i>=0&&i<a)for(var o=-1;o<=1;o++){var p=d+o;p>=0&&p<e&&(p=(i*e+p)*4,c=Math.min(c,h[p]),j=Math.min(j,h[p+1]),l=Math.min(l,h[p+2]))}}b[g]=c;b[g+1]=j;b[g+2]=l;b[g+3]=h[g+3]}for(e=0;e<b.length;e++)h[e]=b[e]}}
function NoiseFilter(){this.name="Noise";this.isDirAnimatable=!0;this.defaultValues={amount:25,density:1,monochrome:!0};this.valueRanges={amount:{min:0,max:100},density:{min:0,max:1},monochrome:{min:!1,max:!0}};this.filter=function(h,e){var a=h.width,b=h.height,f=h.data;if(e===void 0)e=this.defaultValues;for(var d=e.amount===void 0?this.defaultValues.amount:e.amount,g=e.density===void 0?this.defaultValues.density:e.density,c=e.monochrome===void 0?this.defaultValues.monochrome:e.monochrome,j=0;j<b;j++)for(var l=
0;l<a;l++){var k=(j*a+l)*4;if(Math.random()<=g){var i;if(c)i=parseInt((2*Math.random()-1)*d,10),f[k]+=i,f[k+1]+=i,f[k+2]+=i;else for(var o=0;o<3;o++)i=parseInt((2*Math.random()-1)*d,10),f[k+o]+=i}}}}
function OilFilter(){this.name="Oil Painting";this.isDirAnimatable=!1;this.defaultValues={range:3};this.valueRanges={range:{min:0,max:5}};this.filter=function(h,e){var a=h.width,b=h.height,f=h.data,d=[];if(e===void 0)e=this.defaultValues;for(var g=e.range===void 0?this.defaultValues.range:e.range,g=parseInt(g,10),c=[],j=[],l=[],k=[],i=[],o=[],p=0;p<b;p++)for(var s=0;s<a;s++){for(var n=(p*a+s)*4,m=0;m<256;m++)c[m]=j[m]=l[m]=k[m]=i[m]=o[m]=0;for(m=-g;m<=g;m++){var q=p+m;if(0<=q&&q<b){q*=a;for(var t=
-g;t<=g;t++){var r=s+t;if(0<=r&&r<a){var u=f[(q+r)*4],v=f[(q+r)*4+1],r=f[(q+r)*4+2],y=u*256/256,w=v*256/256,x=r*256/256;k[y]+=u;i[w]+=v;o[x]+=r;c[y]++;j[w]++;l[x]++}}}}t=q=m=0;for(u=1;u<256;u++)c[u]>c[m]&&(m=u),j[u]>j[q]&&(q=u),l[u]>l[t]&&(t=u);m=k[m]/c[m];q=i[q]/j[q];t=o[t]/l[t];d[n]=m;d[n+1]=q;d[n+2]=t;d[n+3]=f[n+3]}for(a=0;a<d.length;a++)f[a]=d[a]}}
function OpacityFilter(){this.name="Opacity";this.isDirAnimatable=!0;this.defaultValues={amount:1};this.valueRanges={amount:{min:0,max:1}};this.filter=function(h,e){var a=h.width,b=h.height,f=h.data;if(e===void 0)e=this.defaultValues;for(var d=e.amount===void 0?this.defaultValues.amount:e.amount,g=0;g<b;g++)for(var c=0;c<a;c++)f[(g*a+c)*4+3]=255*d}}
function PinchFilter(){this.name="Pinch/Whirl";this.isDirAnimatable=!1;this.defaultValues={amount:0.5,radius:100,angle:0,centerX:0.5,centerY:0.5};this.valueRanges={amount:{min:-1,max:1},radius:{min:1,max:200},angle:{min:0,max:360},centerX:{min:0,max:1},centerY:{min:0,max:1}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;var g=a.amount===void 0?this.defaultValues.amount:a.amount,c=a.angle===void 0?this.defaultValues.angle:a.angle,
j=a.centerX===void 0?this.defaultValues.centerX:a.centerX,l=a.centerY===void 0?this.defaultValues.centerY:a.centerY,k=a.radius===void 0?this.defaultValues.radius:a.radius,i=k*k,c=c/180*Math.PI,o=b*j,p=f*l;h.transformFilter(d,function(a,b,d){var e=a-o,f=b-p,h=e*e+f*f;h>i||h===0?(d[0]=a,d[1]=b):(a=Math.sqrt(h/i),b=Math.pow(Math.sin(Math.PI*0.5*a),-g),e*=b,f*=b,a=1-a,b=c*a*a,a=Math.sin(b),b=Math.cos(b),d[0]=o+b*e-a*f,d[1]=p+a*e+b*f)},b,f)}}
function PixelationFilter(){this.name="Pixelation";this.isDirAnimatable=!1;this.defaultValues={size:5};this.valueRanges={size:{min:1,max:50}};this.filter=function(h,e){var a=h.width,b=h.height,f=h.data;if(e===void 0)e=this.defaultValues;for(var d=e.size===void 0?this.defaultValues.size:e.size,d=parseInt(d,10),g,c,j,l=0;l<b;l+=d)for(var k=0;k<a;k+=d){var i=Math.min(d,a-k),o=Math.min(d,b-l),p=i*o,s=0,n=0,m=0;for(g=l;g<l+o;g++)for(c=k;c<k+i;c++)j=(g*a+c)*4,s+=f[j],n+=f[j+1],m+=f[j+2];for(g=l;g<l+o;g++)for(c=
k;c<k+i;c++)j=(g*a+c)*4,f[j]=s/p,f[j+1]=n/p,f[j+2]=m/p}}}
function PosterizeFilter(){this.name="Posterize";this.isDirAnimatable=!1;this.defaultValues={levels:6};this.valueRanges={levels:{min:2,max:30}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;var g=a.levels===void 0?this.defaultValues.levels:parseInt(a.levels,10);if(!(g<=1)){for(var c=[],j=0;j<256;j++)c[j]=parseInt(255*parseInt(j*g/256,10)/(g-1),10);h.tableFilter(d,c,b,f)}}}
function RGBAdjustFilter(){this.name="RGBAdjust";this.isDirAnimatable=!0;this.defaultValues={red:1,green:1,blue:1};this.valueRanges={red:{min:0,max:2},green:{min:0,max:2},blue:{min:0,max:2}};this.filter=function(h,e){var a=h.width,b=h.height,f=h.data;if(e===void 0)e=this.defaultValues;var d=e.red===void 0?this.defaultValues.red:e.red,g=e.green===void 0?this.defaultValues.green:e.green,c=e.blue===void 0?this.defaultValues.blue:e.blue;d<0&&(d=0);g<0&&(g=0);c<0&&(c=0);for(var j=0;j<b;j++)for(var l=0;l<
a;l++){var k=(j*a+l)*4;f[k]*=d;f[k+1]*=g;f[k+2]*=c}}}
function SaturationFilter(){this.name="Saturation";this.isDirAnimatable=!0;this.defaultValues={amount:1};this.valueRanges={amount:{min:0,max:2}};this.filter=function(h,e){var a=h.width,b=h.height,f=h.data;if(e===void 0)e=this.defaultValues;for(var d=e.amount===void 0?this.defaultValues.amount:e.amount,g=(1-d)*0.3+d,c=(1-d)*0.3,j=(1-d)*0.3,l=(1-d)*0.59,k=(1-d)*0.59+d,i=(1-d)*0.59,o=(1-d)*0.11,p=(1-d)*0.11,d=(1-d)*0.11+d,s=0;s<b;s++)for(var n=0;n<a;n++){var m=(s*a+n)*4,q=f[m],t=f[m+1],r=f[m+2];f[m]=
g*q+l*t+o*r;f[m+1]=c*q+k*t+p*r;f[m+2]=j*q+i*t+d*r}}}
function SawtoothRippleFilter(){this.name="Sawtooth Ripples";this.isDirAnimatable=!1;this.defaultValues={xAmplitude:5,yAmplitude:5,xWavelength:16,yWavelength:16};this.valueRanges={xAmplitude:{min:0,max:30},yAmplitude:{min:0,max:30},xWavelength:{min:1,max:50},yWavelength:{min:1,max:50}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;var g=a.xAmplitude===void 0?this.defaultValues.xAmplitude:a.xAmplitude,c=a.yAmplitude===void 0?this.defaultValues.yAmplitude:
a.yAmplitude,j=a.xWavelength===void 0?this.defaultValues.xWavelength:a.xWavelength,l=a.yWavelength===void 0?this.defaultValues.yWavelength:a.yWavelength;h.transformFilter(d,function(a,b,d){var e=a/l,f=h.mod(b/j,1),e=h.mod(e,1);d[0]=a+g*f;d[1]=b+c*e},b,f)}}
function SepiaFilter(){this.name="Sepia";this.isDirAnimatable=!0;this.defaultValues={amount:10};this.valueRanges={amount:{min:0,max:30}};new FilterUtils;this.filter=function(h,e){var a=h.width,b=h.height,f=h.data;if(e===void 0)e=this.defaultValues;var d=e.amount===void 0?this.defaultValues.amount:e.amount;d*=2.55;for(var g=0;g<b;g++)for(var c=0;c<a;c++){var j=(g*a+c)*4,l,k,i;l=k=i=f[j]*0.3+f[j+1]*0.59+f[j+2]*0.11;l+=40;k+=20;i-=d;f[j]=l;f[j+1]=k;f[j+2]=i}}}
function SharpenFilter(){this.name="Sharpen";this.isDirAnimatable=!0;this.defaultValues={};this.valueRanges={};var h=new FilterUtils;this.filter=function(e){h.convolveFilter(e.data,[0,-0.2,0,-0.2,1.8,-0.2,0,-0.2,0],e.width,e.height)}}
function SineRippleFilter(){this.name="Sine Ripples";this.isDirAnimatable=!1;this.defaultValues={xAmplitude:5,yAmplitude:5,xWavelength:16,yWavelength:16};this.valueRanges={xAmplitude:{min:0,max:30},yAmplitude:{min:0,max:30},xWavelength:{min:1,max:50},yWavelength:{min:1,max:50}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;var g=a.xAmplitude===void 0?this.defaultValues.xAmplitude:a.xAmplitude,c=a.yAmplitude===void 0?this.defaultValues.yAmplitude:
a.yAmplitude,j=a.xWavelength===void 0?this.defaultValues.xWavelength:a.xWavelength,l=a.yWavelength===void 0?this.defaultValues.yWavelength:a.yWavelength;h.transformFilter(d,function(a,b,d){var e=Math.sin(a/l);d[0]=a+g*Math.sin(b/j);d[1]=b+c*e},b,f)}}
function SolarizeFilter(){this.name="Solarize";this.isDirAnimatable=!0;this.defaultValues={};this.valueRanges={};var h=new FilterUtils;this.filter=function(e){for(var a=e.width,b=e.height,e=e.data,f=[],d=0;d<256;d++)f[d]=parseInt(255*(d/255>0.5?2*(d/255-0.5):2*(0.5-d/255)),10);h.tableFilter(e,f,a,b)}}
function SparkleFilter(){this.name="Sparkle";this.isDirAnimatable=!1;this.defaultValues={rays:50,size:25,amount:50,randomness:25,centerX:0.5,centerY:0.5};this.valueRanges={rays:{min:1,max:100},size:{min:1,max:200},amount:{min:0,max:100},randomness:{min:0,max:50},centerX:{min:0,max:1},centerY:{min:0,max:1}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;for(var g=a.rays===void 0?this.defaultValues.rays:a.rays,g=parseInt(g,10),c=
a.size===void 0?this.defaultValues.size:a.size,j=a.amount===void 0?this.defaultValues.amount:a.amount,l=a.randomness===void 0?this.defaultValues.randomness:a.randomness,k=(a.centerX===void 0?this.defaultValues.centerX:a.centerX)*b,i=(a.centerY===void 0?this.defaultValues.centerY:a.centerY)*f,o=[],p=0;p<g;p++)o[p]=c+l/100*c*h.gaussianRandom();for(l=0;l<f;l++)for(p=0;p<b;p++){var s=(l*b+p)*4,n=p-k,m=l-i,q=n*n+m*m,n=(Math.atan2(m,n)+Math.PI)/(Math.PI*2)*g,m=parseInt(n,10);n-=m;c!==0&&(m=h.linearInterpolate(n,
o[m%g],o[(m+1)%g]),q=m*m/(q+1.0E-4),q=Math.pow(q,(100-j)/50),n-=0.5,n=1-n*n,n*=q);n=h.clampPixel(n,0,1);q=h.mixColors(n,[d[s],d[s+1],d[s+2],d[s+3]],[255,255,255,255]);for(n=0;n<3;n++)d[s+n]=q[n]}}}
function SquareSmearFilter(){this.name="Square Smear";this.isDirAnimatable=!1;this.defaultValues={size:4,density:0.5,mix:0.5};this.valueRanges={size:{min:1,max:10},density:{min:0,max:1},mix:{min:0,max:1}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data,g=[],c;for(c=0;c<d.length;c++)g[c]=d[c];if(a===void 0)a=this.defaultValues;c=a.size===void 0?this.defaultValues.size:a.size;c<1&&(c=1);c=parseInt(c,10);for(var j=a.mix===void 0?this.defaultValues.mix:a.mix,l=c+1,k=
parseInt(2*(a.density===void 0?this.defaultValues.density:a.density)/30*b*f/2,10),i=0;i<k;i++)for(var o=(Math.random()*Math.pow(2,32)&2147483647)%b,p=(Math.random()*Math.pow(2,32)&2147483647)%f,s=[d[(p*b+o)*4],d[(p*b+o)*4+1],d[(p*b+o)*4+2],d[(p*b+o)*4+3]],n=o-l;n<o+l+1;n++)for(var m=p-l;m<p+l+1;m++)if(n>=0&&n<b&&m>=0&&m<f){var q=h.mixColors(j,[g[(m*b+n)*4],g[(m*b+n)*4+1],g[(m*b+n)*4+2],g[(m*b+n)*4+3]],s);for(c=0;c<3;c++)g[(m*b+n)*4+c]=q[c]}for(c=0;c<g.length;c++)d[c]=g[c]}}
function ThresholdFilter(){this.name="Black & White";this.isDirAnimatable=!0;this.defaultValues={threshold:127};this.valueRanges={threshold:{min:0,max:255}};this.filter=function(h,e){var a=h.width,b=h.height,f=h.data;if(e===void 0)e=this.defaultValues;for(var d=e.threshold===void 0?this.defaultValues.threshold:e.threshold,g=0;g<b;g++)for(var c=0;c<a;c++){var j=(g*a+c)*4,l=0;(f[j]+f[j+1]+f[j+2])/3>d&&(l=255);f[j]=f[j+1]=f[j+2]=l}}}
function TriangleRippleFilter(){this.name="Triangle Ripples";this.isDirAnimatable=!1;this.defaultValues={xAmplitude:5,yAmplitude:5,xWavelength:16,yWavelength:16};this.valueRanges={xAmplitude:{min:0,max:30},yAmplitude:{min:0,max:30},xWavelength:{min:1,max:50},yWavelength:{min:1,max:50}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;var g=a.xAmplitude===void 0?this.defaultValues.xAmplitude:a.xAmplitude,c=a.yAmplitude===void 0?this.defaultValues.yAmplitude:
a.yAmplitude,j=a.xWavelength===void 0?this.defaultValues.xWavelength:a.xWavelength,l=a.yWavelength===void 0?this.defaultValues.yWavelength:a.yWavelength;h.transformFilter(d,function(a,b,d){var e=a/l,f=h.triangle(b/j,1),e=h.triangle(e,1);d[0]=a+g*f;d[1]=b+c*e},b,f)}}
function TwirlFilter(){this.name="Twirl";this.isDirAnimatable=!1;this.defaultValues={radius:100,angle:180,centerX:0.5,centerY:0.5};this.valueRanges={radius:{min:1,max:200},angle:{min:0,max:360},centerX:{min:0,max:1},centerY:{min:0,max:1}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;var g=a.angle===void 0?this.defaultValues.angle:a.angle,c=a.centerX===void 0?this.defaultValues.centerX:a.centerX,j=a.centerY===void 0?this.defaultValues.centerY:
a.centerY,l=a.radius===void 0?this.defaultValues.radius:a.radius,k=l*l,g=g/180*Math.PI,i=b*c,o=f*j;h.transformFilter(d,function(a,b,c){var d=a-i,e=b-o,f=d*d+e*e;f>k?(c[0]=a,c[1]=b):(f=Math.sqrt(f),a=Math.atan2(e,d)+g*(l-f)/l,c[0]=i+f*Math.cos(a),c[1]=o+f*Math.sin(a))},b,f)}}
function VignetteFilter(){this.name="Vignette";this.isDirAnimatable=!1;this.defaultValues={amount:0.3};this.valueRanges={amount:{min:0,max:1}};this.filter=function(h,e){var a=h.width,b=h.height,f=h.data,d=[];if(e===void 0)e=this.defaultValues;var d=e.amount===void 0?this.defaultValues.amount:e.amount,g=document.createElement("canvas");g.width=a;g.height=b;var g=g.getContext("2d"),c;c=Math.sqrt(Math.pow(a/2,2)+Math.pow(b/2,2));g.putImageData(h,0,0);g.globalCompositeOperation="source-over";c=g.createRadialGradient(a/
2,b/2,0,a/2,b/2,c);c.addColorStop(0,"rgba(0,0,0,0)");c.addColorStop(0.5,"rgba(0,0,0,0)");c.addColorStop(1,"rgba(0,0,0,"+d+")");g.fillStyle=c;g.fillRect(0,0,a,b);d=g.getImageData(0,0,a,b).data;for(a=0;a<d.length;a++)f[a]=d[a]}}
function WaterRippleFilter(){this.name="Water Ripples";this.isDirAnimatable=!1;this.defaultValues={phase:0,radius:50,wavelength:16,amplitude:10,centerX:0.5,centerY:0.5};this.valueRanges={phase:{min:0,max:100},radius:{min:1,max:200},wavelength:{min:1,max:100},amplitude:{min:1,max:100},centerX:{min:0,max:1},centerY:{min:0,max:1}};var h=new FilterUtils;this.filter=function(e,a){var b=e.width,f=e.height,d=e.data;if(a===void 0)a=this.defaultValues;var g=a.wavelength===void 0?this.defaultValues.wavelength:
a.wavelength,c=a.amplitude===void 0?this.defaultValues.amplitude:a.amplitude,j=a.phase===void 0?this.defaultValues.phase:a.phase,l=a.radius===void 0?this.defaultValues.radius:a.radius,k=l*l,i=b*(a.centerX===void 0?this.defaultValues.centerX:a.centerX),o=f*(a.centerY===void 0?this.defaultValues.centerY:a.centerY);h.transformFilter(d,function(a,b,d){var e=a-i,f=b-o,h=e*e+f*f;if(h>k)d[0]=a,d[1]=b;else{var h=Math.sqrt(h),r=c*Math.sin(h/g*Math.PI*2-j);r*=(l-h)/l;h!==0&&(r*=g/h);d[0]=a+e*r;d[1]=b+f*r}},
b,f)}}
var JSManipulate={blur:new BlurFilter,brightness:new BrightnessFilter,bump:new BumpFilter,circlesmear:new CircleSmearFilter,contrast:new ContrastFilter,crosssmear:new CrossSmearFilter,diffusion:new DiffusionFilter,dither:new DitherFilter,edge:new EdgeFilter,emboss:new EmbossFilter,exposure:new ExposureFilter,gain:new GainFilter,gamma:new GammaFilter,grayscale:new GrayscaleFilter,hue:new HueFilter,invert:new InvertFilter,kaleidoscope:new KaleidoscopeFilter,lensdistortion:new LensDistortionFilter,linesmear:new LineSmearFilter,
maximum:new MaximumFilter,median:new MedianFilter,minimum:new MinimumFilter,noise:new NoiseFilter,oil:new OilFilter,opacity:new OpacityFilter,pinch:new PinchFilter,pixelate:new PixelationFilter,posterize:new PosterizeFilter,rgbadjust:new RGBAdjustFilter,saturation:new SaturationFilter,sawtoothripple:new SawtoothRippleFilter,sepia:new SepiaFilter,sharpen:new SharpenFilter,sineripple:new SineRippleFilter,solarize:new SolarizeFilter,sparkle:new SparkleFilter,squaresmear:new SquareSmearFilter,threshold:new ThresholdFilter,
triangleripple:new TriangleRippleFilter,twirl:new TwirlFilter,vignette:new VignetteFilter,waterripple:new WaterRippleFilter};

/* ^ Filters */

/* ^ Vortex HTML5 Canvas image editor */
