/*****************************************************
*  
*  Copyright 2010 Adobe Systems Incorporated.  All Rights Reserved.
*  
*****************************************************
*  The contents of this file are subject to the Mozilla Public License
*  Version 1.1 (the "License"); you may not use this file except in
*  compliance with the License. You may obtain a copy of the License at
*  http://www.mozilla.org/MPL/
*   
*  Software distributed under the License is distributed on an "AS IS"
*  basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
*  License for the specific language governing rights and limitations
*  under the License.
*   
*  
*  The Initial Developer of the Original Code is Adobe Systems Incorporated.
*  Portions created by Adobe Systems Incorporated are Copyright (C) 2010 Adobe Systems 
*  Incorporated. All Rights Reserved. 
*  
*****************************************************/

package org.osmf.player.chrome.widgets
{
	import flash.display.DisplayObject;
	import flash.text.TextField;
	import flash.text.TextFormatAlign;
	
	import org.osmf.player.chrome.assets.AssetsManager;
	import org.osmf.player.chrome.assets.FontAsset;

	public class TimeHintWidget extends LabelWidget
	{
		
		// Overrides
		//
		
		override public function set text(value:String):void
		{
			if (value != text)
			{
				super.text = value;	
				
				// center the text horizontally
				// and vertically within the bubble area
				textField.width = textField.textWidth;
				textField.x = getChildAt(0).width/2 - textField.width/2;
				textField.y = textFieldSpacing;
			}
		}
		
		// Internals
		//
		
		private var textFieldSpacing:uint = 7;
	}
}