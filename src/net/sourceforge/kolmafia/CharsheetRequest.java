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
import java.util.StringTokenizer;

/**
 * An extension of <code>KoLRequest</code> which retrieves the character's
 * information from the server.  Note that this request only retrieves the
 * character's statistics at the current time; skills and effects will be
 * retrieved at a later date.  Equipment retrieval takes place through a
 * different request.
 */

public class CharsheetRequest extends KoLRequest
{
	private KoLCharacter character;

	/**
	 * Constructs a new <code>CharsheetRequest</code>.  This also
	 * stores the reference to the character to be provided; the
	 * data found in that character will be overridden over the
	 * course of this request.
	 *
	 * @param	client	The client to be notified in case of errors
	 */

	public CharsheetRequest( KoLmafia client )
	{
		// The only thing to do is to retrieve the page from
		// the client - all variable initialization comes from
		// when the request is actually run.

		super( client, "charsheet.php" );
		this.character = client.getCharacterData();
	}

	/**
	 * Runs the request.  Note that only the character's statistics
	 * are retrieved via this retrieval.
	 */

	public void run()
	{
		updateDisplay( DISABLED_STATE, "Retrieving character data..." );
		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

                // Derive the character's gender from the image filename
		character.setGender( responseText.indexOf( "_f.gif" ) == -1 );

		// The easiest way to retrieve the character sheet
		// data is to first strip all of the HTML from the
		// reply, and then tokenize on the stripped-down
		// version.  This can be done through simple regular
		// expression matching.

		StringTokenizer parsedContent = new StringTokenizer( responseText, "<>" );

		try
		{
			// The first two tokens in the stream contains the
			// name, but the character's name was known at login.
			// Therefore, these tokens can be discarded.

			String token = parsedContent.nextToken();
			while ( !token.startsWith( " (" ) )
				token = parsedContent.nextToken();

			character.setUserID( Integer.parseInt( token.substring( 3, token.length() - 1 ) ) );
			skipTokens( parsedContent, 3 );
			character.setClassName( parsedContent.nextToken().trim() );

			// Hit point parsing begins with the first index of
			// the words indicating that the upcoming token will
			// show the HP values (Current, Maximum).

			while ( !token.startsWith( "Current" ) )
				token = parsedContent.nextToken();
			skipTokens( parsedContent, 3 );
			int currentHP = intToken( parsedContent );

			while ( !token.startsWith( "Maximum" ) )
				token = parsedContent.nextToken();
			skipTokens( parsedContent, 3 );
			int maximumHP = intToken( parsedContent );
			character.setHP( currentHP, maximumHP, retrieveBase( parsedContent, maximumHP ) );

			// Mana point parsing is exactly the same as hit point
			// parsing - so this is just a copy-paste of the code.

			while ( !token.startsWith( "Current" ) )
				token = parsedContent.nextToken();
			skipTokens( parsedContent, 3 );
			int currentMP = intToken( parsedContent );

			while ( !token.startsWith( "Maximum" ) )
				token = parsedContent.nextToken();
			skipTokens( parsedContent, 3 );
			int maximumMP = intToken( parsedContent );
			character.setMP( currentMP, maximumMP, retrieveBase( parsedContent, maximumMP ) );

			// Next, you begin parsing the different stat points;
			// this involves hunting for the stat point's name,
			// skipping the appropriate number of tokens, and then
			// reading in the numbers.

			int [] mus = findStatPoints( parsedContent, "Mus" );
			int [] mys = findStatPoints( parsedContent, "Mys" );
			int [] mox = findStatPoints( parsedContent, "Mox" );

			character.setStatPoints( mus[0], mus[1], mys[0], mys[1], mox[0], mox[1] );

			// Drunkenness may or may not exist (in other words,
			// if the character is not drunk, nothing will show
			// up).  Therefore, parse it if it exists; otherwise,
			// parse until the "Adventures remaining:" token.

			while ( !token.startsWith("Temul") && !token.startsWith("Inebr") && !token.startsWith("Tipsi") &&
				!token.startsWith("Drunk") && !token.startsWith("Adven") )
					token = parsedContent.nextToken();

			if ( !token.startsWith( "Adven" ) )
			{
				skipTokens( parsedContent, 3 );
				character.setInebriety( intToken( parsedContent ) );

				while ( !token.startsWith( "Adven" ) )
					token = parsedContent.nextToken();
			}

			// Now parse the number of adventures remaining,
			// the monetary value in the character's pocket,
			// and the number of turns accumulated.

			skipTokens( parsedContent, 3 );
			character.setAdventuresLeft( intToken( parsedContent ) );

			while ( !token.startsWith( "Meat" ) )
				token = parsedContent.nextToken();
			skipTokens( parsedContent, 3 );
			character.setAvailableMeat( intToken( parsedContent ) );

			// Determine the player's ascension count, if any.
			if ( responseText.indexOf( "Ascensions:" ) != -1 )
			{
				while ( !token.startsWith( "Ascensions" ) )
					token = parsedContent.nextToken();
				skipTokens( parsedContent, 4 );
				character.setAscensions( intToken( parsedContent ) );
			}

			while ( !token.startsWith( "Turns" ) )
				token = parsedContent.nextToken();
			skipTokens( parsedContent, 3 );
			character.setTotalTurnsUsed( intToken( parsedContent ) );

			// Determine the player's zodiac sign, if any.

			if ( responseText.indexOf( "Sign:" ) != -1 )
			{
				while ( !parsedContent.nextToken().startsWith( "Sign:" ) );
				skipTokens( parsedContent, 3 );
				character.setSign( parsedContent.nextToken() );
			}

			character.setInteraction( responseText.indexOf( "You may not receive items from other players" ) == -1 &&
				responseText.indexOf( "You are in Hardcore mode" ) == -1 );

			// Determine whether or not the player has any
			// active effects - if so, retrieve them.

			character.clearEffects();
			if ( responseText.indexOf( "Effects:" ) != -1 )
			{
				while ( !parsedContent.nextToken().startsWith( "Eff" ) );
				skipTokens( parsedContent, 13 );
				token = parsedContent.nextToken();

				while ( !token.equals( "/table" ) )
				{
					client.parseEffect( parsedContent.nextToken() );
					skipTokens( parsedContent, 7 );
					token = parsedContent.nextToken();
				}
			}

			if ( responseText.indexOf( "Skills:" ) != -1 )
			{
				while ( !parsedContent.nextToken().startsWith( "Ski" ) );

				token = parsedContent.nextToken();
				List availableSkills = new ArrayList();
				while ( !token.equals( "/table" ) )
				{
					if ( token.startsWith( "a" ) )
					{
						String skillName = parsedContent.nextToken().trim();
						if ( ClassSkillsDatabase.contains( skillName ) )
							availableSkills.add( new UseSkillRequest( client, skillName, "", 1 ) );
					}
					token = parsedContent.nextToken();
				}

				character.setAvailableSkills( availableSkills );
			}

			// Current equipment is also listed on the character
			// sheet -- because we now have consumption types,
			// it is now possible to retrieve all the equipment.

			String [] equipment = new String[8];
			for ( int i = 0; i < 8; ++i )
				equipment[i] = EquipmentRequest.UNEQUIP;

			Matcher equipmentMatcher = Pattern.compile( "<b>Equipment.*?<table>(.*?)</table>" ).matcher( responseText );
			if ( equipmentMatcher.find() )
			{
				String currentItem;
				Matcher itemMatcher = Pattern.compile( "<b>(.*?)</b>" ).matcher( equipmentMatcher.group(1) );

				while ( itemMatcher.find() )
				{
					currentItem = itemMatcher.group(1);

					switch ( TradeableItemDatabase.getConsumptionType( currentItem ) )
					{
						case ConsumeItemRequest.EQUIP_HAT:
							equipment[ KoLCharacter.HAT ] = currentItem;
							break;

						case ConsumeItemRequest.EQUIP_WEAPON:
							equipment[ KoLCharacter.WEAPON ] = currentItem;
							break;

						case ConsumeItemRequest.EQUIP_SHIRT:
							equipment[ KoLCharacter.SHIRT ] = currentItem;
							break;

						case ConsumeItemRequest.EQUIP_PANTS:
							equipment[ KoLCharacter.PANTS ] = currentItem;
							break;

						case ConsumeItemRequest.EQUIP_ACCESSORY:

							if ( equipment[ KoLCharacter.ACCESSORY1 ].equals( EquipmentRequest.UNEQUIP ) )
								equipment[ KoLCharacter.ACCESSORY1 ] = currentItem;
							else if ( equipment[ KoLCharacter.ACCESSORY2 ].equals( EquipmentRequest.UNEQUIP ) )
								equipment[ KoLCharacter.ACCESSORY2 ] = currentItem;
							else
								equipment[ KoLCharacter.ACCESSORY3 ] = currentItem;
					}
				}
			}

			character.setEquipment( equipment, new ArrayList() );

			// Use a regular expression to locate the familiar data
			// and let the familiar data parser handle the rest.

			FamiliarData.registerFamiliarData( client, responseText );
			logStream.println( "Parsing complete." );
		}
		catch ( RuntimeException e )
		{
			logStream.println( e );
			e.printStackTrace( logStream );
		}
	}

	/**
	 * Helper method used to find the statistic points.  This method was
	 * created because statistic-point finding is exactly the same for
	 * every statistic point.
	 *
	 * @param	st	The <code>StringTokenizer</code> containing the tokens to be parsed
	 * @param	searchString	The search string indicating the beginning of the statistic
	 * @return	The 2-element array containing the parsed statistics
	 */

	private static int [] findStatPoints( StringTokenizer st, String searchString )
	{
		int [] stats = new int[2];
		String token = st.nextToken();

		while ( !token.startsWith( searchString ) )
			token = st.nextToken();
		skipTokens( st, 6 );
		stats[0] = intToken( st );
		int base = retrieveBase( st, stats[0] );
		while ( !token.equals( "b" ) )
			token = st.nextToken();
		stats[1] = KoLCharacter.calculateSubpoints( base, intToken( st ) );

		return stats;
	}

	/**
	 * Utility method for retrieving the base value for a statistic, given
	 * the tokenizer, and assuming that the base might be located in the
	 * next token.  If it isn't, the default value is returned instead.
	 * Note that this advances the <code>StringTokenizer</code> one token
	 * ahead of the base value for the statistic.
	 *
	 * @param	st	The <code>StringTokenizer</code> possibly containing the base value
	 * @param	defaultBase	The value to return, if no base value is found
	 * @return	The parsed base value, or the default value if no base value is found
	 */

	private static int retrieveBase( StringTokenizer st, int defaultBase )
	{
		skipTokens( st, 1 );
		int possibleBase = intToken( st, 8, 1 );
		return possibleBase == 0 ? defaultBase : possibleBase;
	}
}
