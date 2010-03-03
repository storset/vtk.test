// JavaScript Document	
var INITIAL_INPUT_FIELDS = new Array();
var NEED_TO_CONFIRM = true;
var UNSAVED_CAHANGES_CONFIRMATION;

function initPropChange(){
    var i = 0;
    $("input").each(function() {
        INITIAL_INPUT_FIELDS[i++] = this.value;
    });
}

function unsavedChangesInEditor(){
    if(!NEED_TO_CONFIRM)
        return false;
    var dirtyState = false;
    currentStateOfInputFields = $("input");
    for(i = 0; i < INITIAL_INPUT_FIELDS.length; i++){
        if(currentStateOfInputFields[i].value != INITIAL_INPUT_FIELDS[i]){
            dirtyState = true;
            break;
        }
    }
    $("textarea").each(function() {
        if(typeof(FCKeditorAPI) != "undefined"){
            if(FCKeditorAPI.GetInstance(this.name).IsDirty()){
                dirtyState = true;
                return;
            }
        }
     });
     return dirtyState;
}

function unsavedChangesInEditorMessage(){
    if(unsavedChangesInEditor()){
        return UNSAVED_CAHANGES_CONFIRMATION;
    }
}
