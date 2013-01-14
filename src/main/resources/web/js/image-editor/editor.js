/*
 * Vortex HTML5 Canvas image editor
 * w/ scale and crop on client (processed on server)
 *
 * Copyright (C) 2012 Øyvind Hatland - University Of Oslo / USIT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

var IMAGE_EDITOR_INITIALIZED = $.Deferred();

function VrtxImageEditor() {
  var instance; // Class-like singleton pattern (p.145 JavaScript Patterns)
  VrtxImageEditor = function VrtxImageEditor() {
    return instance;
  };
  VrtxImageEditor.prototype = this;
  instance = new VrtxImageEditor();
  instance.constructor = VrtxImageEditor;
  
  this.url = null;
  this.img = null;
  this.scaledImg = null;

  this.canvasSupported = null;
  this.isIE8 = null;
  this.canvas = null;
  this.ctx = null;
  this.lastWidth = null;
  this.lastHeight = null;
  this.rw = null;
  this.rh = null;
  this.cropX = 0;
  this.cropY = 0;
  this.cropWidth = null;
  this.cropHeight = null;
  this.scaleRatio = 1;
  this.reversedScaleRatio = 1;
  this.aspectRatioOver = 1;
  this.aspectRatioUnder = 1;
  this.keepAspectRatio = true;
  this.hasCropBeenInitialized = false;
  this.savedImage = false;
  
  this.selection = null;
  this.iMouseX = 1;
  this.iMouseY = 1;

  return instance;
};

var vrtxImageEditor = new VrtxImageEditor();

VrtxImageEditor.prototype.init = function init(imageEditorElm, imageURL, imageSupported) {
  var editor = this;
  
  editor.canvas = document.getElementById("vrtx-image-editor");
  editor.isIE8 = vrtxAdmin.isIE && vrtxAdmin.browserVersion < 9;
  editor.canvasSupported = (editor.isIE8 || ('getContext' in document.createElement('canvas'))) && imageSupported === "true";
  if(editor.canvasSupported) {
    if(editor.isIE8 && typeof editor.canvas.getContext === "undefined") {
      G_vmlCanvasManager.initElement(editor.canvas); 
    }
    editor.ctx = editor.canvas.getContext('2d');
  }
  editor.img = new Image();
  editor.scaledImg = new Image();
  editor.url = imageURL;
  editor.img.src = editor.url;
  editor.img.onload = function () {
    editor.rw = editor.lastWidth = editor.cropWidth = editor.img.width;
    editor.rh = editor.lastHeight = editor.cropHeight = editor.img.height;
    editor.canvasSupported = editor.canvasSupported && ((editor.rw * editor.rh) < 100000000); // Limit to 400MB(32bpp)
    if(!editor.canvasSupported) {
      $(editor.canvas).replaceWith("<img src='" + editor.url + "' alt='preview image' />");
      editor.displayDimensions(editor.rw, editor.rh);
      editor.rw = editor.lastWidth = editor.cropWidth = 0;
      editor.rh = editor.lastHeight = editor.cropHeight = 0;
      $("#resource-width").attr("disabled", "disabled");
      $("#resource-height").attr("disabled", "disabled");
    } else {
      var gcd = editor.gcd(editor.rw, editor.rh);
      editor.aspectRatioOver = editor.rw/gcd;
      editor.aspectRatioUnder = editor.rh/gcd;    
    
      editor.updateDimensions(editor.rw, editor.rh);
  
      editor.ctx.drawImage(editor.img, 0, 0);
      $(editor.canvas).resizable({
        aspectRatio: editor.keepAspectRatio,
        grid: [1, 1],
        maxHeight: editor.rh,
        maxWidth: editor.rw,
        stop: function (event, ui) {
            var newWidth = Math.floor(ui.size.width);
            var newHeight = Math.round(newWidth / (editor.aspectRatioOver / editor.aspectRatioUnder));
            editor.lastWidth = newWidth;
            editor.lastHeight = newHeight;
            editor.scale(newWidth, newHeight);
        },
        resize: function (event, ui) {
          editor.displayDimensions(Math.floor(ui.size.width), Math.floor(ui.size.height));
        }
      });
    }
  }

  if(editor.canvasSupported) {
    $("#app-content").on("dblclick", "#vrtx-image-editor", function (e) {
      $("#vrtx-image-crop").click();
    });
  
    $("#app-content").on("click", "#vrtx-image-crop", function (e) {
      if (editor.hasCropBeenInitialized) {
        editor.cropX += Math.round(editor.selection.x * editor.reversedScaleRatio);
        editor.cropY += Math.round(editor.selection.y * editor.reversedScaleRatio);
        editor.cropWidth = Math.round(editor.selection.w * editor.reversedScaleRatio);
        editor.cropHeight = Math.round(editor.selection.h * editor.reversedScaleRatio);
        editor.rw = editor.lastWidth = Math.round(editor.cropWidth * editor.scaleRatio);
        editor.rh = editor.lastHeight = Math.round(editor.cropHeight * editor.scaleRatio);
        
        var gcd = editor.gcd(editor.rw, editor.rh);
        editor.aspectRatioOver = editor.rw/gcd;
        editor.aspectRatioUnder = editor.rh/gcd;
        
        editor.updateDimensions(editor.rw, editor.rh);
        editor.ctx.drawImage(editor.img, editor.cropX, editor.cropY, editor.cropWidth, editor.cropHeight, 
                                                    0,            0,        editor.rw,         editor.rh);                                             
        editor.resetCropPlugin();
        $("#vrtx-image-crop-coordinates").remove();
        $(this).val(startCropText + "...");
        $("#vrtx-image-editor").resizable("option", "maxWidth", editor.cropWidth);  
        $("#vrtx-image-editor").resizable("option", "maxHeight", editor.cropHeight);  
        $("#vrtx-image-editor").resizable("enable");  

        editor.hasCropBeenInitialized = false;
      } else {
        var shortestSide = Math.min(editor.rw, editor.rh);
        if(shortestSide >= 400) {
          var distEdge = 40;
        } else if(shortestSide < 400 && shortestSide >= 200) {
          var distEdge = 30;
        } else if (shortestSide < 200 && shortestSide >= 120) {
          var distEdge = 20;
        } else if (shortestSide < 120 && shortestSide > 40) {
          var distEdge = 10;
        } else {
          var distEdge = 2;
        }
        
        editor.initSelection(distEdge, distEdge, editor.rw - (distEdge * 2), editor.rh - (distEdge * 2));
        
        $(this).val(cropText);
        $("#vrtx-image-editor").resizable("disable");
        
        var cropInfoHtml = "<p id='vrtx-image-crop-coordinates'>" 
                             + widthText.substring(0,1) + ": <span id='vrtx-image-crop-coordinates-width'>" + editor.cropWidth + "</span>&nbsp;&nbsp;"
                             + heightText.substring(0,1) + ": <span id='vrtx-image-crop-coordinates-height'>" + editor.cropHeight+ "</span>"
                           + "</p>";    
        $(cropInfoHtml).insertAfter("#vrtx-image-crop-button");
        
        editor.hasCropBeenInitialized = true;
      }
      e.stopPropagation();
      e.preventDefault();
    });

    $("#app-content").on("change", "#resource-width, #resource-height", function (e) {
      var w = parseInt($.trim($("#resource-width").val()));
      var h = parseInt($.trim($("#resource-height").val()));
      if (!isNaN(w) && !isNaN(h) && ((w / editor.cropWidth) <= 1)) {
        if (w !== editor.rw) {
          if (editor.keepAspectRatio) {
            h = Math.round(w / (editor.aspectRatioOver / editor.aspectRatioUnder));
          }
          $("#resource-height").val(h)
        } else if (h !== editor.rh) {
          if (editor.keepAspectRatio) {
            w = Math.round(h * (editor.aspectRatioOver / editor.aspectRatioUnder));
          }
          $("#resource-width").val(w)
        }
        editor.lastWidth = w;
        editor.lastHeight = h;
        editor.scale(w, h);
      } else {
        $("#resource-width").val(editor.lastWidth);
        $("#resource-height").val(editor.lastHeight);
      }
    });

    $("#app-content").on("keydown", "#resource-width, #resource-height", function (e) {
      if (e.which == 38 || e.which == 40) {
        var isWidth = $(this).attr("id") == "resource-width";
        var x = parseInt($.trim($(this).val()));
        if (e.which == 38) {
          x++;
        } else {
          if (x > 2) {
            x--;
          }
        }
        if(isWidth) {
          y = Math.round(x / (editor.aspectRatioOver / editor.aspectRatioUnder));
        } else {
          y = Math.round(x * (editor.aspectRatioOver / editor.aspectRatioUnder));
        }
        var w = isWidth ? x : y;
        var h = isWidth ? y : x;
        if ((w / editor.cropWidth) <= 1) {
          editor.lastWidth = w;
          editor.lastHeight = h;
          $("#resource-width").val(w);
          $("#resource-height").val(h);
          editor.scale(w, h);
        }
      } else if (e.which == 13) {
        $(this).trigger("change");
        return false;
      }
    });
  
    $(document).click(function(e) {
      if(editor.hasCropBeenInitialized && $(e.target).parents().index($('#vrtx-image-editor-inner-wrapper')) == -1) {
        editor.cropNone(editor);
      }
    });
  }
};

VrtxImageEditor.prototype.gcd = function gcd(a, b) {
  return (b == 0) ? a : this.gcd (b, a%b);
}

VrtxImageEditor.prototype.updateDimensions = function updateDimensions(w, h) {
  var editor = this;
  editor.canvas.setAttribute('width', w);
  editor.canvas.setAttribute('height', h);
  editor.canvas.width = w;
  editor.canvas.height = h;
  $(".ui-wrapper").css({"width": w, "height": h});
  $("#vrtx-image-editor").css({"width": w, "height": h});
  editor.displayDimensions(w, h);
};

VrtxImageEditor.prototype.displayDimensions = function displayDimensions(w, h) {
  if ($("#vrtx-image-dimensions-crop").length) {
    $("#resource-width").val(w);
    $("#resource-height").val(h);
  } else {
    var dimensionHtml = '<div id="vrtx-image-dimensions-crop">'
                        + '<div class="vrtx-label-and-text">'
                          + '<div class="property-label">' + widthText + '</div>'
                          + '<div class="vrtx-textfield" id="vrtx-textfield-width"><input id="resource-width" type="text" value="' + w + '" size="4" /></div>'
                        + '</div>'
                        + '<div class="vrtx-label-and-text">'
                          + '<div class="property-label">' + heightText + '</div>'
                          + '<div class="vrtx-textfield" id="vrtx-textfield-height"><input id="resource-height" type="text" value="' + h + '" size="4" /></div>'
                        + '</div>';
    if(this.canvasSupported) {                      
      dimensionHtml += '<div id="vrtx-image-crop-button"><div class="vrtx-button">'
                     + '<input type="button" id="vrtx-image-crop" value="' + startCropText + '..." /></div></div>'
                     + '<div id="vrtx-image-info" style="margin-top: 10px"></div>';
    }
    dimensionHtml  += '</div>';
    $(dimensionHtml).insertBefore("#vrtx-image-editor-preview");
    $("#resource-width").attr("autocomplete", "off");
    $("#resource-height").attr("autocomplete", "off");
    IMAGE_EDITOR_INITIALIZED.resolve();
  }
};

VrtxImageEditor.prototype.save = function save(buttonId) {
  var editor = this;
  
  if(editor.hasCropBeenInitialized) {
    editor.cropNone(editor); // Remove selection
  }

  var form = $("form#editor");
  var dataString = "<input style='display: none' name='crop-x' value='" + editor.cropX + "' />"
                 + "<input style='display: none' name='crop-y' value='" + editor.cropY + "' />"
                 + "<input style='display: none' name='crop-width' value='" + editor.cropWidth + "' />"
                 + "<input style='display: none' name='crop-height' value='" + editor.cropHeight + "' />"
                 + "<input style='display: none' name='new-width' value='" + editor.rw + "' />"
                 + "<input style='display: none' name='new-height' value='" + editor.rh + "' />";
   form.append(dataString);
   if(typeof buttonId !== "undefined") {
     editor.savedImage = true;
     $("#" + buttonId).click();
   }
};

VrtxImageEditor.prototype.scale = function scale(newWidth, newHeight) {
  var editor = this;
  editor.scaleRatio = newWidth / editor.cropWidth;
  editor.reversedScaleRatio = editor.cropWidth / newWidth;
  editor.rw = newWidth;
  editor.rh = newHeight;
  editor.updateDimensions(editor.rw, editor.rh);
  editor.ctx.drawImage(editor.img, editor.cropX, editor.cropY, editor.cropWidth, editor.cropHeight, 
                                              0,            0,        editor.rw,        editor.rh);      
};

/*
 * Crop plugin
 *
 * Credits: http://www.script-tutorials.com/demos/197/index.html
 *
 * Modified slightly by USIT
 *
 * TODO: Optimize
 *
 */

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

VrtxImageEditor.prototype.draw = function draw() {
  var editor = this;
  var selection = editor.selection;
  
  editor.ctx.strokeStyle = '#000';
  editor.ctx.lineWidth = 2;
  editor.ctx.strokeRect(selection.x, selection.y, selection.w, selection.h);
  // draw part of original image
  if (selection.w > 0 && selection.h > 0) {
    editor.ctx.drawImage(editor.img,
                         editor.cropX + Math.round(editor.selection.x * editor.reversedScaleRatio),
                         editor.cropY + Math.round(editor.selection.y * editor.reversedScaleRatio),
                         Math.round(selection.w * editor.reversedScaleRatio), 
                         Math.round(selection.h * editor.reversedScaleRatio), 
                         selection.x, selection.y,
                         selection.w, selection.h);
  }
  // draw resize cubes
  editor.ctx.fillStyle = '#fff';
  editor.ctx.fillRect(selection.x - selection.iCSize[0], selection.y - selection.iCSize[0], selection.iCSize[0] * 2, selection.iCSize[0] * 2);
  editor.ctx.fillRect(selection.x + selection.w - selection.iCSize[1], selection.y - selection.iCSize[1], selection.iCSize[1] * 2, selection.iCSize[1] * 2);
  editor.ctx.fillRect(selection.x + selection.w - selection.iCSize[2], selection.y + selection.h - selection.iCSize[2], selection.iCSize[2] * 2, selection.iCSize[2] * 2);
  editor.ctx.fillRect(selection.x - selection.iCSize[3], selection.y + selection.h - selection.iCSize[3], selection.iCSize[3] * 2, selection.iCSize[3] * 2);
  
  $("#vrtx-image-crop-coordinates-width").text(selection.w);
  $("#vrtx-image-crop-coordinates-height").text(selection.h);
};

VrtxImageEditor.prototype.drawScene = function drawScene() { // Main drawScene function
  var editor = this;

  editor.ctx.clearRect(0, 0, editor.canvas.width, editor.canvas.height); // clear canvas
  // draw source image
  editor.ctx.drawImage(editor.img, editor.cropX, editor.cropY, editor.cropWidth, editor.cropHeight, 
                                              0,            0,        editor.rw,        editor.rh);  
  // and make it darker
  editor.ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
  editor.ctx.fillRect(0, 0, editor.canvas.width, editor.canvas.height);
  // draw selection
  editor.draw();
};

VrtxImageEditor.prototype.initSelection = function initSelection(x, y, w, h) {
  var editor = this;

  editor.selection = new Selection(x, y, w, h);
  var selection = editor.selection;

  $('#vrtx-image-editor').bind("mousemove", function (e) { // binding mouse move event
    var canvasOffset = $(editor.canvas).offset();
    selection.iMouseX = Math.floor(e.pageX - canvasOffset.left);
    selection.iMouseY = Math.floor(e.pageY - canvasOffset.top);
    // in case of drag of whole selector
    if (selection.bDragAll) {
      selection.x = Math.min((selection.iMouseX - selection.px), (editor.rw - selection.w));
      selection.y = Math.min((selection.iMouseY - selection.py), (editor.rh - selection.h));
    }
    for (i = 0; i < 4; i++) {
      selection.bHow[i] = false;
      selection.iCSize[i] = selection.csize;
    }
    // hovering over resize cubes
    if (selection.iMouseX > selection.x - selection.csizeh 
     && selection.iMouseX < selection.x + selection.csizeh
     && selection.iMouseY > selection.y - selection.csizeh
     && selection.iMouseY < selection.y + selection.csizeh) {
      selection.bHow[0] = true;
      selection.iCSize[0] = selection.csizeh;
    }
    if (selection.iMouseX > selection.x + selection.w - selection.csizeh 
     && selection.iMouseX < selection.x + selection.w + selection.csizeh
     && selection.iMouseY > selection.y - selection.csizeh
     && selection.iMouseY < selection.y + selection.csizeh) {
      selection.bHow[1] = true;
      selection.iCSize[1] = selection.csizeh;
    }
    if (selection.iMouseX > selection.x + selection.w - selection.csizeh
     && selection.iMouseX < selection.x + selection.w + selection.csizeh
     && selection.iMouseY > selection.y + selection.h - selection.csizeh
     && selection.iMouseY < selection.y + selection.h + selection.csizeh) {
      selection.bHow[2] = true;
      selection.iCSize[2] = selection.csizeh;
    }
    if (selection.iMouseX > selection.x - selection.csizeh
     && selection.iMouseX < selection.x + selection.csizeh
     && selection.iMouseY > selection.y + selection.h - selection.csizeh
     && selection.iMouseY < selection.y + selection.h + selection.csizeh) {
      selection.bHow[3] = true;
      selection.iCSize[3] = selection.csizeh;
    }
    // in case of dragging of resize cubes
    var iFW, iFH;
    if (selection.bDrag[0]) {
      var iFX = selection.iMouseX - selection.px;
      var iFY = selection.iMouseY - selection.py;
      iFW = selection.w + selection.x - iFX;
      iFH = selection.h + selection.y - iFY;
    }
    if (selection.bDrag[1]) {
      var iFX = selection.x;
      var iFY = selection.iMouseY - selection.py;
      iFW = selection.iMouseX - selection.px - iFX;
      iFH = selection.h + selection.y - iFY;
    }
    if (selection.bDrag[2]) {
      var iFX = selection.x;
      var iFY = selection.y;
      iFW = selection.iMouseX - selection.px - iFX;
      iFH = selection.iMouseY - selection.py - iFY;
    }
    if (selection.bDrag[3]) {
      var iFX = selection.iMouseX - selection.px;
      var iFY = selection.y;
      iFW = selection.w + selection.x - iFX;
      iFH = selection.iMouseY - selection.py - iFY;
    }
    if (iFW > selection.csizeh * 2 && iFH > selection.csizeh * 2) {
      selection.w = iFW;
      selection.h = iFH;
      selection.x = iFX;
      selection.y = iFY;
    }
    selection.w = Math.min((editor.rw - selection.x), selection.w);
    selection.h = Math.min((editor.rh - selection.y), selection.h);
    selection.x = Math.max(0, selection.x);
    selection.y = Math.max(0, selection.y);
    editor.drawScene();
  });
  $('#vrtx-image-editor').bind("mousedown", function (e) { // binding mousedown event
    var canvasOffset = $(editor.canvas).offset();
    selection.iMouseX = Math.floor(e.pageX - canvasOffset.left);
    selection.iMouseY = Math.floor(e.pageY - canvasOffset.top);
    selection.px = selection.iMouseX - selection.x;
    selection.py = selection.iMouseY - selection.y;
    if (selection.bHow[0]) {
      selection.px = selection.iMouseX - selection.x;
      selection.py = selection.iMouseY - selection.y;
    }
    if (selection.bHow[1]) {
      selection.px = selection.iMouseX - selection.x - selection.w;
      selection.py = selection.iMouseY - selection.y;
    }
    if (selection.bHow[2]) {
      selection.px = selection.iMouseX - selection.x - selection.w;
      selection.py = selection.iMouseY - selection.y - selection.h;
    }
    if (selection.bHow[3]) {
      selection.px = selection.iMouseX - selection.x;
      selection.py = selection.iMouseY - selection.y - selection.h;
    }
    if (selection.iMouseX > selection.x + selection.csizeh
     && selection.iMouseX < selection.x + selection.w - selection.csizeh
     && selection.iMouseY > selection.y + selection.csizeh
     && selection.iMouseY < selection.y + selection.h - selection.csizeh) {
      selection.bDragAll = true;
    }
    for (i = 0; i < 4; i++) {
      if (selection.bHow[i]) {
        selection.bDrag[i] = true;
      }
    }
  });
  $('#vrtx-image-editor').bind("mouseup", function (e) { // binding mouseup event
    selection.bDragAll = false;
    for (i = 0; i < 4; i++) {
      selection.bDrag[i] = false;
    }
    selection.px = 0;
    selection.py = 0;
  });
  editor.drawScene();
};

VrtxImageEditor.prototype.cropNone = function cropNone() {
  var editor = this;
  editor.selection.x = 0;
  editor.selection.y = 0;
  editor.selection.w = editor.rw;
  editor.selection.h = editor.rh;
  $("#vrtx-image-crop").click();
};

VrtxImageEditor.prototype.resetCropPlugin = function resetCropPlugin() {
  var editor = this;
  editor.selection = null;
  $("#vrtx-image-editor").unbind("mousemove").unbind("mousedown").unbind("mouseup");
};

/* ^ Vortex HTML5 Canvas image editor */
