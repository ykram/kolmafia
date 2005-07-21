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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.LockableListModel;

public class MuseumManager implements KoLConstants
{
	private KoLmafia client;
	private SortedListModel items;
	private LockableListModel headers, shelves;

	public MuseumManager( KoLmafia client )
	{
		this.client = client;
		this.items = new SortedListModel();
		this.shelves = new LockableListModel();
	}

	public String getHeader( int shelf )
	{	return (String) headers.get( shelf );
	}

	public SortedListModel getItems()
	{	return items;
	}

	public LockableListModel getShelves()
	{	return shelves;
	}

	public void move( Object [] items, int sourceShelf, int destinationShelf )
	{
		List itemList = new ArrayList();
		for ( int i = 0; i < items.length; ++i )
			itemList.add( items[i] );

		((SortedListModel)shelves.get( sourceShelf )).removeAll( itemList );
		((SortedListModel)shelves.get( destinationShelf )).addAll( itemList );
	}

	public void update( String data )
	{
		updateShelves( data );

		Pattern selectedPattern = Pattern.compile( "(\\d+) selected>" );
		Matcher selectedMatcher;
		int itemID, itemCount;

		Matcher optionMatcher = Pattern.compile( "<b>.*?</b>(.*?)</td>.*?<select name=whichshelf(\\d+)>(.*?)</select>" ).matcher( data );
		while ( optionMatcher.find() )
		{
			selectedMatcher = selectedPattern.matcher( optionMatcher.group(3) );

			itemID = Integer.parseInt( optionMatcher.group(2) );
			itemCount = Integer.parseInt( "0" + optionMatcher.group(1).replaceAll( "[ \\(\\)]", "" ) );

			registerItem( new AdventureResult( itemID, itemCount ),
				selectedMatcher.find() ? Integer.parseInt( selectedMatcher.group(1) ) : 0 );
		}
	}

	private void registerItem( AdventureResult item, int shelf )
	{
		items.add( item );
		((SortedListModel)shelves.get( shelf )).add( item );
	}

	private void updateShelves( String data )
	{
		items.clear();
		headers.clear();

		Matcher selectMatcher = Pattern.compile( "<select.*?</select>" ).matcher( data );
		if ( selectMatcher.find() )
		{
			Matcher shelfMatcher = Pattern.compile( "<option.*?>(.*?)</option>" ).matcher( selectMatcher.group() );
			while ( shelfMatcher.find() )
				headers.add( shelfMatcher.group(1) );
		}

		if ( headers.size() == 0 )
			headers.add( "" );

		headers.set( 0, "Display Case" );

		if ( headers.size() != shelves.size() )
		{
			shelves.clear();
			for ( int i = 0; i <= headers.size(); ++i )
				shelves.add( new SortedListModel() );
		}
		else
		{
			for ( int i = 0; i <= headers.size(); ++i )
				shelves.set( i, new SortedListModel() );
		}
	}
}
