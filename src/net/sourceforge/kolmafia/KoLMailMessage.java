/**
 * Copyright (c) 2005, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;
import java.util.StringTokenizer;

import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class KoLMailMessage
{
	private static SimpleDateFormat sdf = new SimpleDateFormat( "EEEE, MMMM d, hh:mmaa", Locale.US );

	private String messageHTML;
	private String messageID;
	private String senderID;
	private String senderName;
	private String messageDate;

	public KoLMailMessage( String message )
	{
		this.messageHTML = message.substring( message.indexOf( "\">" ) + 2 );
		this.messageID = message.substring( message.indexOf( "name=" ) + 6, message.indexOf( "\">" ) );
		StringTokenizer messageParser = new StringTokenizer( message, "<>" );

		String lastToken = messageParser.nextToken();
		while ( !lastToken.startsWith( "a " ) )
			lastToken = messageParser.nextToken();

		this.senderID = lastToken.substring( lastToken.indexOf( "who=" ) + 4, lastToken.length() - 1 );
		this.senderName = messageParser.nextToken();

		while ( !messageParser.nextToken().startsWith( "Date" ) );
		messageParser.nextToken();

		try
		{
			// This attempts to parse the date from
			// the given string; note it may throw
			// an exception (but probably not)

			this.messageDate = messageParser.nextToken().trim();
		}
		catch ( Exception e )
		{
			// Initialize the date to the current time,
			// since that's about as close as it gets
			this.messageDate = sdf.format( new Date() );
		}
	}

	public String toString()
	{	return senderName + " @ " + messageDate;
	}

	public boolean equals( Object o )
	{
		return o == null ? false :
			o instanceof KoLMailMessage ? equals( (KoLMailMessage)o ) : false;
	}

	public boolean equals( KoLMailMessage kmm )
	{	return messageID.equals( kmm.messageID );
	}

	public String getMessageID()
	{	return messageID;
	}

	public String getMessageHTML()
	{	return messageHTML;
	}

	public String getSenderName()
	{	return senderName;
	}

	public String getSenderID()
	{	return senderID;
	}
}