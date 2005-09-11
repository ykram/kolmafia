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

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemStorageRequest extends SendMessageRequest
{
	private int moveType;

	public static final int RETRIEVE_STORAGE = 0;
	public static final int INVENTORY_TO_CLOSET = 1;
	public static final int CLOSET_TO_INVENTORY = 2;
	public static final int MEAT_TO_CLOSET = 4;
	public static final int MEAT_TO_INVENTORY = 5;
	public static final int STORAGE_TO_INVENTORY = 6;
	public static final int PULL_MEAT_FROM_STORAGE = 7;

	public ItemStorageRequest( KoLmafia client )
	{
		super( client, "storage.php" );
		this.moveType = RETRIEVE_STORAGE;
	}

	/**
	 * Constructs a new <code>ItemStorageRequest</code>.
	 * @param	client	The client to be notified of the results
	 * @param	amount	The amount of meat involved in this transaction
	 * @param	moveType	Whether or not this is a deposit or withdrawal, or if it's to the clan stash
	 */

	public ItemStorageRequest( KoLmafia client, int amount, int moveType )
	{
		super( client, moveType == PULL_MEAT_FROM_STORAGE ? "storage.php" : "closet.php",
			new AdventureResult( AdventureResult.MEAT, moveType == PULL_MEAT_FROM_STORAGE ? amount : 0 ) );

		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "amt", String.valueOf( amount ) );
		addFormField( "action", moveType == MEAT_TO_CLOSET ? "addmeat" : "takemeat" );

		this.moveType = moveType;

		if ( moveType == PULL_MEAT_FROM_STORAGE )
		{
			source = client.getStorage();
			destination = client.getInventory();
		}
	}

	/**
	 * Constructs a new <code>ItemStorageRequest</code>.
	 * @param	client	The client to be notified of the results
	 * @param	moveType	The identifier for the kind of action taking place
	 * @param	attachments	The list of attachments involved in the request
	 */

	public ItemStorageRequest( KoLmafia client, int moveType, Object [] attachments )
	{
		super( client, moveType == STORAGE_TO_INVENTORY ? "storage.php" : "closet.php", attachments, 0 );

		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "action", moveType == INVENTORY_TO_CLOSET ? "put" : "take" );

		this.moveType = moveType;

		if ( moveType == CLOSET_TO_INVENTORY )
		{
			source = client.getCloset();
			destination = client.getInventory();
		}
		else if ( moveType == INVENTORY_TO_CLOSET )
		{
			source = client.getInventory();
			destination = client.getCloset();
		}
		else if ( moveType == STORAGE_TO_INVENTORY )
		{
			source = client.getStorage();
			destination = client.getInventory();
		}
	}

	public int getMoveType()
	{	return moveType;
	}

	public List getItems()
	{
		List itemList = new ArrayList();

		if ( attachments == null )
			return itemList;

		for ( int i = 0; i < attachments.length; ++i )
			itemList.add( attachments[i] );

		return itemList;
	}

	protected int getCapacity()
	{	return 11;
	}

	protected void repeat( Object [] attachments )
	{	(new ItemStorageRequest( client, moveType, attachments )).run();
	}

	protected String getSuccessMessage()
	{	return "";
	}

	/**
	 * Executes the <code>ItemStorageRequest</code>.
	 */

	public void run()
	{
		switch ( moveType )
		{
			case RETRIEVE_STORAGE:
				updateDisplay( DISABLED_STATE, "Retrieving list of items in storage..." );
				parseStorage();
				updateDisplay( ENABLED_STATE, "Item list retrieved." );
				break;

			case INVENTORY_TO_CLOSET:
			case CLOSET_TO_INVENTORY:
			case STORAGE_TO_INVENTORY:
				updateDisplay( DISABLED_STATE, "Moving items..." );
				super.run();
				break;

			case MEAT_TO_CLOSET:
			case MEAT_TO_INVENTORY:
			case PULL_MEAT_FROM_STORAGE:
				updateDisplay( DISABLED_STATE, "Executing transaction..." );
				meat();
				updateDisplay( ENABLED_STATE, "" );
				break;
		}
	}

	private void meat()
	{
		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 || moveType == PULL_MEAT_FROM_STORAGE )
			return;

		// Now, determine how much is left in your closet
		// by locating "Your closet contains x meat" and
		// update the display with that information.

		int beforeMeatInCloset = client.getCharacterData().getClosetMeat();
		int afterMeatInCloset = 0;

		Matcher meatInClosetMatcher = Pattern.compile( "<b>Your closet contains ([\\d,]+) meat\\.</b>" ).matcher( responseText );

		if ( meatInClosetMatcher.find() )
		{
			try
			{
				afterMeatInCloset = df.parse( meatInClosetMatcher.group(1) ).intValue();
			}
			catch ( Exception e )
			{
				// This really should not happen, since the numbers
				// are getting grouped properly.  But, just in case,
				// the exception is caught and nothing changes.
			}
		}

		client.getCharacterData().setClosetMeat( afterMeatInCloset );
		client.processResult( new AdventureResult( AdventureResult.MEAT, beforeMeatInCloset - afterMeatInCloset ) );
	}

	private void parseStorage()
	{
		super.run();

		List storageContents = client.getStorage();

		// Start with an empty list

		storageContents.clear();
		Matcher storageMatcher = Pattern.compile( "name=\"whichitem1\".*?</select>" ).matcher( responseText );

		// If there's nothing inside storage, return
		// because there's nothing to parse.

		if ( !storageMatcher.find() )
			return;

		int lastFindIndex = 0;
		Matcher optionMatcher = Pattern.compile( "<option value='([\\d]+)'>(.*?)\\(([\\d,]+)\\)" ).matcher( storageMatcher.group() );
		while ( optionMatcher.find( lastFindIndex ) )
		{
			try
			{
				lastFindIndex = optionMatcher.end();
				int itemID = df.parse( optionMatcher.group(1) ).intValue();

				if ( TradeableItemDatabase.getItemName( itemID ) == null )
					TradeableItemDatabase.registerItem( itemID, optionMatcher.group(2).trim() );

				AdventureResult result = new AdventureResult( itemID, df.parse( optionMatcher.group(3) ).intValue() );
				AdventureResult.addResultToList( storageContents, result );
			}
			catch ( Exception e )
			{
				// If an exception occurs during the parsing, just
				// continue after notifying the LogStream of the
				// error.  This could be handled better, but not now.

				logStream.println( e );
				e.printStackTrace( logStream );
			}
		}

		storageMatcher = Pattern.compile( "(\\d+) more" ).matcher( responseText );
		client.setPullsRemaining( storageMatcher.find() ? Integer.parseInt( storageMatcher.group(1) ) : 0 );
	}
}
