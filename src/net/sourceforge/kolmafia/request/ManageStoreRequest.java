/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.request.MallPurchaseRequest;

import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.StoreManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ManageStoreRequest
	extends GenericRequest
{
	private static Pattern ITEMID_PATTERN = Pattern.compile( "itemid=(h)?(\\d+)" );
	private static Pattern PRICE_PATTERN = Pattern.compile( "price=(\\d+)?" );
	private static Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+|\\*|)" );
	private static Pattern LIMIT_PATTERN = Pattern.compile( "limit=(\\d+)?" );

	// (2) breath mints stocked for 999,999,999 meat each.
	private static Pattern STOCKED_PATTERN = Pattern.compile( "\\(([\\d,]+)\\) (.*?) stocked for ([\\d,]+) meat each( \\(([\\d,]+)/day\\))?" );

	private static final int ITEM_ADDITION = 1;
	private static final int ITEM_REMOVAL = 2;
	private static final int PRICE_MANAGEMENT = 3;
	private static final int VIEW_STORE_LOG = 4;

	private int takenItemId;
	private final int requestType;

	public ManageStoreRequest()
	{
		super( "manageprices.php" );
		this.requestType = ManageStoreRequest.PRICE_MANAGEMENT;
	}

	public ManageStoreRequest( final boolean isStoreLog )
	{
		super( "backoffice.php" );
		this.addFormField( "which", "3" );
		this.requestType =  ManageStoreRequest.VIEW_STORE_LOG;
	}

	public ManageStoreRequest( final int itemId, int qty )
	{
		super( "backoffice.php" );
		this.addFormField( "itemid", String.valueOf( itemId ) );
		this.addFormField( "action", "removeitem" );

		// Cannot ask for more to be removed than are really in the store
		qty = Math.min( qty, StoreManager.shopAmount( itemId ) );
		if ( qty > 1 )
		{
			AdventureResult item = new AdventureResult( itemId, 1 );
			if ( KoLConstants.profitableList.contains( item ) )
			{
				KoLConstants.profitableList.remove( item );
			}
		}

		this.addFormField( "qty", String.valueOf( qty ) );
		this.addFormField( "ajax", "1" );

		this.requestType = ManageStoreRequest.ITEM_REMOVAL;
		this.takenItemId = itemId;
	}

	public ManageStoreRequest( final int[] itemId, final int[] prices, final int[] limits )
	{
		super( "manageprices.php" );
		this.addFormField( "action", "update" );
		int formInt;

		this.requestType = ManageStoreRequest.PRICE_MANAGEMENT;
		for ( int i = 0; i < itemId.length; ++i )
		{
			formInt = ( ( i - 1 ) / 100 ); //Group the form fields for every 100 items.
			this.addFormField( "price" + formInt + "[" + itemId[ i ] + "]", prices[ i ] == 0 ? "" : String.valueOf( Math.max(
				prices[ i ], Math.max( ItemDatabase.getPriceById( itemId[ i ] ), 100 ) ) ) );
			this.addFormField( "limit" + formInt + "[" + itemId[ i ] + "]", String.valueOf( limits[ i ] ) );
		}
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		switch ( this.requestType )
		{
		case ITEM_ADDITION:
			this.addItem();
			break;

		case ITEM_REMOVAL:
			this.removeItem();
			break;

		case PRICE_MANAGEMENT:
			this.managePrices();
			break;

		case VIEW_STORE_LOG:
			this.viewStoreLogs();
			break;
		}
	}

	private void addItem()
	{
		// AdventureResult takenItem = new AdventureResult( this.takenItemId, 0 );
		// String name = takenItem.getName();

		// KoLmafia.updateDisplay( "Removing " + name + " from store..." );
		super.run();
		// KoLmafia.updateDisplay( takenItem.getCount() + " " + name + " removed from your store." );

		ManageStoreRequest.parseResponse( this.getURLString(), this.responseText );
	}

	private void removeItem()
	{
		AdventureResult takenItem = new AdventureResult( this.takenItemId, 0 );
		String name = takenItem.getName();

		KoLmafia.updateDisplay( "Removing " + name + " from store..." );
		super.run();
		KoLmafia.updateDisplay( takenItem.getCount() + " " + name + " removed from your store." );

		ManageStoreRequest.parseResponse( this.getURLString(), this.responseText );
	}

	private void managePrices()
	{
		KoLmafia.updateDisplay( "Requesting store inventory..." );

		super.run();

		if ( this.responseText != null )
		{
			StoreManager.update( this.responseText, true );
		}

		KoLmafia.updateDisplay( "Store inventory request complete." );
	}

	private void viewStoreLogs()
	{
		KoLmafia.updateDisplay( "Examining store logs..." );

		super.run();

		if ( this.responseText != null )
		{
			StoreManager.parseLog( this.responseText );
		}

		KoLmafia.updateDisplay( "Store purchase logs retrieved." );
	}

	@Override
	public void processResults()
	{
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "backoffice.php" ) )
		{
			return;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return;
		}

		if ( action.equals( "additem" ) )
		{
			// (2) breath mints stocked for 999,999,999 meat each.
			Matcher stockedMatcher = ManageStoreRequest.STOCKED_PATTERN.matcher( responseText );
			if ( !stockedMatcher.find() )
			{
				return;
			}

			int quantity = StringUtilities.parseInt( stockedMatcher.group( 1 ) );
			int price = StringUtilities.parseInt( stockedMatcher.group( 3 ) );
			int limit = stockedMatcher.group( 4 ) == null ? 0 : StringUtilities.parseInt( stockedMatcher.group( 5 ) );

			// backoffice.php?itemid=h362&price=180&quantity=1&limit=&pwd&action=additem&ajax=1
			// backoffice.php?itemid=362&price=180&quantity=1&limit=&pwd&action=additem&ajax=1

			// get the item ID - and whether it is from Hagnk's - from the URL submitted.
			// ignore price, quantity, and limit, since the response told us those

			Matcher itemMatcher = ManageStoreRequest.ITEMID_PATTERN.matcher( urlString );
			if ( !itemMatcher.find() )
			{
				return;
			}

			boolean storage = itemMatcher.group( 1 ) != null;
			int itemId = StringUtilities.parseInt( itemMatcher.group( 2 ) );

			AdventureResult item = ItemPool.get( itemId, -quantity );
			if ( storage)
			{
				AdventureResult.addResultToList( KoLConstants.storage, item );
			}
			else
			{
				ResultProcessor.processItem( itemId, -quantity );
			}

			StoreManager.addItem( itemId, quantity, price, limit );

			return;
		}

		if ( action.equals( "removeitem" ) )
		{
			// backoffice.php?qty=1&pwd&action=removeitem&itemid=362&ajax=1

			Matcher itemMatcher = MallPurchaseRequest.ITEM_PATTERN.matcher( responseText );
			if ( !itemMatcher.find() )
			{
				return;
			}

			String result = itemMatcher.group( 0 );
			ArrayList<AdventureResult> results = new ArrayList<AdventureResult>();
			ResultProcessor.processResults( false, result, results );

			if ( results.isEmpty() )
			{
				// Shouldn't happen
				return;
			}

			AdventureResult item = results.get( 0 );
			if ( itemMatcher.group( 2 ) == null)
			{
				ResultProcessor.processItem( item.getItemId(), item.getCount() );
			}
			else
			{
				AdventureResult.addResultToList( KoLConstants.storage, item );
			}

			StoreManager.removeItem( item.getItemId(), item.getCount() );
			return;
		}
	}

	public static boolean registerRequest( final String urlString )
	{
		// backoffice.php?itemid=h362&price=180&quantity=1&limit=&pwd&action=additem&ajax=1
		// backoffice.php?itemid=362&price=180&quantity=1&limit=&pwd&action=additem&ajax=1
		// backoffice.php?qty=1&pwd&action=removeitem&itemid=362&ajax=1

		if ( urlString.startsWith( "backoffice.php" ) )
		{
			return false;
		}

		return false;
	}
}
