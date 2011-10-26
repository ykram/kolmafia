/**
 * Copyright (c) 2005-2011, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

package net.sourceforge.kolmafia.swingui.button;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.webui.RelayLoader;

public class RelayBrowserButton
	extends ThreadedButton
{
	public RelayBrowserButton( final String label, final String location )
	{
		super( label, new RelayBrowserRunnable( location ) );
		JComponentUtilities.setComponentSize( this, 32, 32 );
	}

	public RelayBrowserButton( final String tooltip, final String icon, final String location )
	{
		super( JComponentUtilities.getImage( icon ), new RelayBrowserRunnable( location ) );
		JComponentUtilities.setComponentSize( this, 32, 32 );
		this.setToolTipText( tooltip );
	}

	private static class RelayBrowserRunnable
		implements Runnable
	{
		private String location;

		public RelayBrowserRunnable( String location )
		{
			this.location = location;
		}

		public void run()
		{
			if ( this.location == null )
			{
				RelayLoader.openRelayBrowser();
			}
			else
			{
				RelayLoader.openSystemBrowser( this.location );
			}
		}
	}

	public String toString()
	{
		return this.getText();
	}
}
