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

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;

import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A static class which retrieves all the concoctions available in
 * the Kingdom of Loathing.  This class technically uses up a lot
 * more memory than it needs to because it creates an array storing
 * all possible item combinations, but that's okay!  Because it's
 * only temporary.  Then again, this is supposedly true of all the
 * flow-control using exceptions, but that hasn't been changed.
 */

public class ConcoctionsDatabase implements UtilityConstants
{
	private static final String ITEM_DBASE_FILE = "concoctions.dat";
	private static final int ITEM_COUNT = 1000;

	private static Concoction [] concoctions = new Concoction[ ITEM_COUNT ];
	private static int [] quantityPossible = new int[ ITEM_COUNT ];

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and double-referenced: once in the name-lookup,
		// and again in the ID lookup.

		BufferedReader itemdata = DataUtilities.getReaderForSharedDataFile( ITEM_DBASE_FILE );

		try
		{
			String line;
			while ( (line = itemdata.readLine()) != null )
			{
				StringTokenizer strtok = new StringTokenizer( line, "\t" );
				if ( strtok.countTokens() == 4 )
				{
					int itemID = TradeableItemDatabase.getItemID( strtok.nextToken() );
					concoctions[ itemID ] = new Concoction( itemID, Integer.parseInt( strtok.nextToken() ), strtok.nextToken(), strtok.nextToken() );
				}
			}
		}
		catch ( IOException e )
		{
			// If an IOException is thrown, that means there was
			// a problem reading in the appropriate data file;
			// that means that no item database exists.  This
			// exception is strange enough that it won't be
			// handled at the current time.
		}
	}

	/**
	 * Returns the concoctions which are available given the list of
	 * ingredients.  The list returned contains formal requests for
	 * item creation.
	 *
	 * @param	client	The client to be consulted of the results
	 * @param	availableIngredients	The list of ingredients available for mixing
	 * @return	A list of possible concoctions
	 */

	public static LockableListModel getConcoctions( KoLmafia client, List availableIngredients )
	{
		for ( int i = 0; i < ITEM_COUNT; ++i )
		{
			if ( concoctions[i] == null )
			{
				String itemName = TradeableItemDatabase.getItemName(i);
				if ( itemName != null )
				{
					int index = availableIngredients.indexOf( new AdventureResult( itemName, 0 ) );
					quantityPossible[i] = (index == -1) ? 0 : ((AdventureResult)availableIngredients.get( index )).getCount();
				}
				else
					quantityPossible[i] = 0;
			}
			else
				quantityPossible[i] = -1;
		}

		for ( int i = 0; i < ITEM_COUNT; ++i )
			if ( concoctions[i] != null )
				concoctions[i].calculateQuantityPossible( availableIngredients );

		for ( int i = 0; i < availableIngredients.size(); ++i )
		{
			AdventureResult result = (AdventureResult) availableIngredients.get(i);

			if ( result.isItem() )
				quantityPossible[ TradeableItemDatabase.getItemID( result.getName() ) ] -= result.getCount();
		}

		LockableListModel concoctionsList = new LockableListModel();

		for ( int i = 0; i < ITEM_COUNT; ++i )
			if ( quantityPossible[i] > 0 )
				concoctionsList.add( new ItemCreationRequest( client, i, concoctions[i].getMixingMethod(), quantityPossible[i] ) );

		return concoctionsList;
	}

	/**
	 * Returns the item IDs of the ingredients for the given item.
	 * Note that if there are no ingredients, then <code>null</code>
	 * will be returned instead.
	 */

	public static int [][] getIngredients( int itemID )
	{	return (concoctions[ itemID ] == null) ? null : concoctions[ itemID ].getIngredients();
	}

	/**
	 * Internal class used to represent a single concoction.  It
	 * contains all the information needed to actually make the item.
	 */

	private static class Concoction
	{
		private int concoctionID;
		private int mixingMethod;
		private AdventureResult asResult;
		private int ingredient1, ingredient2;

		public Concoction( int concoctionID, int mixingMethod, String ingredient1, String ingredient2 )
		{
			this.concoctionID = concoctionID;
			this.mixingMethod = mixingMethod;

			this.asResult = new AdventureResult( TradeableItemDatabase.getItemName( concoctionID ), 0 );

			this.ingredient1 = TradeableItemDatabase.getItemID( ingredient1 );
			this.ingredient2 = TradeableItemDatabase.getItemID( ingredient2 );
		}

		public int getMixingMethod()
		{	return mixingMethod;
		}

		public int [][] getIngredients()
		{
			int [][] ingredients = new int[2][2];

			ingredients[0][0] = ingredient1;
			ingredients[0][1] = (concoctions[ingredient1] == null) ?
				ItemCreationRequest.NOCREATE : concoctions[ingredient1].getMixingMethod();

			ingredients[1][0] = ingredient2;
			ingredients[1][1] = (concoctions[ingredient2] == null) ?
				ItemCreationRequest.NOCREATE : concoctions[ingredient2].getMixingMethod();

			return ingredients;
		}

		public void calculateQuantityPossible( List availableIngredients )
		{
			if ( quantityPossible[ concoctionID ] != -1 )
				return;

			int index = availableIngredients.indexOf( asResult );
			quantityPossible[ concoctionID ] = (index == -1) ? 0 : ((AdventureResult)availableIngredients.get( index )).getCount();

			if ( concoctions[ ingredient1 ] != null )
				concoctions[ ingredient1 ].calculateQuantityPossible( availableIngredients );

			if ( concoctions[ ingredient2 ] != null )
				concoctions[ ingredient2 ].calculateQuantityPossible( availableIngredients );

			quantityPossible[ concoctionID ] +=
				Math.min( quantityPossible[ ingredient1 ], quantityPossible[ ingredient2 ] );
		}
	}
}