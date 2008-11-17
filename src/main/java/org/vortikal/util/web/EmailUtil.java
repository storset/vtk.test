/* Copyright (c) 2008, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.vortikal.util.web;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.springframework.util.StringUtils;

/* Credits
 *  http://www.leshazlewood.com/?p=23
 *  http://www.tunagami.com/articles/2007/11/04/regexp-java-email-rfc-2822-you
 */

public class EmailUtil {
	
	// ex. surname.lastname
	// private static final String sp = "!#$%&\'*+-/=?^_`{|}~";
	// private static final String ftext = "[a-zA-Z0-9]";
	// private static final String atext = "[a-zA-Z0-9" + sp + "]";
	// private static final String atom = atext + "+";
	// private static final String fatom = ftext + "+";
	// private static final String dotAtom = "(\\.|-|_)" + atom;
	// private static final String localPart = fatom + "(" + dotAtom + ")*";
	//	
	// // RFC 1035
	// // ex. usit, uio, no
	// private static final String letter = "[a-zA-Z]";
	// private static final String letDig = "[a-zA-Z0-9]";
	// private static final String letDigHyp = "[a-zA-Z0-9-]";
	// private static final String rfcLabel = letDig + letDigHyp + "{0,61}" + letDig;
	//	
	// // ex. usit.uio.no
	// private static final String domain = rfcLabel + "((\\.|-)" + rfcLabel + ")*\\." + letter + "{2,6}";
	//	
	// // RFC 2822
	// // ex. surname.lastname@usit.uio.no
	// private static final String addrSpec = "^" + localPart + "@" + domain + "$";
	//	
	// private static final String addrNormal =
	// "^[\\_]*([a-z0-9]+(\\.|\\_|\\-*)?)+@([a-z][a-z0-9\\-]+(\\.|\\-*\\.))+[a-z]{2,6}$";
	//	
	// private static final String addrLight =
	// "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[_A-Za-z0-9-]+)";
	//	
	// public static final Pattern VALID_EMAIL_RFC = Pattern.compile(addrSpec);
	// public static final Pattern VALID_EMAIL = Pattern.compile(addrNormal);
	// public static final Pattern VALID_EMAIL_LIGHT = Pattern.compile(addrLight);
	//	
	public static boolean isValidEmail(String emailAddress) {
		// return regExPattern.matcher(emailAddress.trim()).matches();
		try {
			Address addr = new InternetAddress(emailAddress);
			
			if (StringUtils.countOccurrencesOf(emailAddress, "@") == 0) {
				return false;
			}
		} catch (AddressException e) {
			return false;
		}
		return true;
	}
	
	public static boolean isValidMultipleEmails(String[] emailAddresses) {
		
		for (int i = 0; i < emailAddresses.length; i++) {
			// boolean valid = false;
			try {
				Address addr = new InternetAddress(emailAddresses[i]);
				
				if (StringUtils.countOccurrencesOf(emailAddresses[i], "@") == 0) {
					return false;
				}
				
				// valid = regExPattern.matcher(emailAddresses[i].trim()).matches();
			} catch (AddressException e) {
				return false;
			}
			// if (!valid) {
			// return false;
			// }
		}
		return true;
		
	}
	
	public static String[] checkForMultipleEmails(String emailTo) {
		
		return emailTo.split(",");
		
	}
	
}
