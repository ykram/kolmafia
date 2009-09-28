/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.swingui.ItemManageFrame;

public class StorageCommand
	extends AbstractCommand
{
	public StorageCommand()
	{
		this.usage = " outfit <name> | <item> [, <item>]... - pull items from Hagnk's storage.";
	}

	public void run( final String cmd, final String parameters )
	{
		if ( KoLCharacter.inBadMoon() && !KoLCharacter.canInteract() )
		{
			KoLmafia.updateDisplay(
				KoLConstants.ERROR_STATE, "Hagnk's Storage is not available in Bad Moon until you free King Ralph." );
			return;
		}

		Object[] items;
		if ( parameters.startsWith( "outfit " ) )
		{
			SpecialOutfit outfit = EquipmentManager.getMatchingOutfit( parameters.substring( 7 ).trim() );
			if ( outfit == null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "No such outfit." );
				return;
			}
			AdventureResult[] pieces = outfit.getPieces();
			ArrayList needed = new ArrayList();
			for ( int i = 0; i < pieces.length; ++i )
			{
				if ( !InventoryManager.hasItem( pieces[ i ] ) )
				{
					needed.add( pieces[ i ] );
				}
			}
			items = needed.toArray();
		}
		else
		{
			items = ItemFinder.getMatchingItemList( KoLConstants.storage, parameters );
		}

		if ( items.length == 0 )
		{
			return;
		}

		int meatAttachmentCount = 0;

		for ( int i = 0; i < items.length; ++i )
		{
			if ( ( (AdventureResult) items[ i ] ).getName().equals( AdventureResult.MEAT ) )
			{
				RequestThread.postRequest( new StorageRequest(
					StorageRequest.PULL_MEAT_FROM_STORAGE, ( (AdventureResult) items[ i ] ).getCount() ) );

				items[ i ] = null;
				++meatAttachmentCount;
			}
		}

		if ( meatAttachmentCount == items.length )
		{
			return;
		}

		// Double check to make sure you have all items on hand
		// since a failure to get something from Hagnk's is bad.

		int storageCount;
		AdventureResult item;

		for ( int i = 0; i < items.length; ++i )
		{
			item = (AdventureResult) items[ i ];
			storageCount = item.getCount( KoLConstants.storage );

			if ( items[ i ] != null && storageCount < item.getCount() )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE,
					"You only have " + storageCount + " " + item.getName() + " in storage (you wanted " + item.getCount() + ")" );
			}
		}

		RequestThread.postRequest( new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, items ) );
		int pulls = ItemManageFrame.getPullsRemaining();
		if ( pulls >= 0 )
		{
			KoLmafia.updateDisplay( pulls + ( pulls == 1 ? " pull" : " pulls" ) + " remaining," + ItemManageFrame.getPullsBudgeted() + " budgeted for automatic use." );
		}
	}
}