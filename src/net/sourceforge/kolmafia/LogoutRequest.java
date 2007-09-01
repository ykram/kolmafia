/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

public class LogoutRequest extends KoLRequest
{
	private static boolean isRunning = false;

	public LogoutRequest()
	{	super( "logout.php" );
	}

	protected boolean retryOnTimeout()
	{	return false;
	}

	public void run()
	{
		if ( isRunning )
			return;

		isRunning = true;
		KoLmafia.updateDisplay( "Preparing for logout..." );

		if ( KoLDesktop.instanceExists() )
			KoLDesktop.getInstance().dispose();

		KoLFrame [] frames = StaticEntity.getExistingFrames();
		for ( int i = 0; i < frames.length; ++i )
			frames[i].dispose();

		KoLAdventure.resetAutoAttack();
		if ( KoLDesktop.instanceExists() )
			KoLDesktop.getInstance().dispose();

		KoLMessenger.dispose();
		BuffBotHome.setBuffBotActive( false );

		String scriptSetting = KoLSettings.getUserProperty( "logoutScript" );
		if ( !scriptSetting.equals( "" ) )
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( scriptSetting );

		super.run();
		KoLCharacter.reset( "" );

		RequestLogger.closeSessionLog();
		RequestLogger.closeDebugLog();
		RequestLogger.closeMirror();

		KoLmafia.updateDisplay( ABORT_STATE, "Logout request submitted." );
		isRunning = false;
	}
}

