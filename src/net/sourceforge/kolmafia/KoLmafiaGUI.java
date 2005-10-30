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
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * The main class for the <code>KoLmafia</code> package.  This
 * class encapsulates most of the data relevant to any given
 * session of <code>Kingdom of Loathing</code> and currently
 * functions as the blackboard in the architecture.  When data
 * listeners are implemented, it will continue to manage most
 * of the interactions.
 */

public class KoLmafiaGUI extends KoLmafia
{
	protected boolean shouldRefresh;
	protected Runnable refresher;

	private boolean isEnabled;
	private CreateFrameRunnable displayer;
	private LimitedSizeChatBuffer buffer;

	public KoLmafiaGUI()
	{
		this.shouldRefresh = true;
		this.refresher = new ListRefresher();
	}

	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafia</code> client after setting the default
	 * look and feel of all <code>JFrame</code> objects to decorated.
	 */

	public static void main( String [] args )
	{
		javax.swing.JFrame.setDefaultLookAndFeelDecorated( true );
    	KoLmafiaGUI session = new KoLmafiaGUI();

		StaticEntity.setClient( session );

		String login = session.settings.getProperty( "autoLogin" );
		String password = session.getSaveState( login );

		if ( password != null )
			(new LoginRequest( session, login, password, false, false, false )).run();
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public synchronized void updateDisplay( int state, String message )
	{
		super.updateDisplay( state, message );

		if ( state != NOCHANGE )
		{
			if ( displayer != null && displayer.getCreation() != null )
			{
				((KoLFrame)displayer.getCreation()).updateDisplay( state, message );
				if ( BuffBotHome.isBuffBotActive() )
					BuffBotHome.updateStatus( message );
			}

			isEnabled = state != DISABLED_STATE;
		}
	}

	public boolean isEnabled()
	{	return isEnabled;
	}

	public void setEnabled( boolean isEnabled )
	{
		this.isEnabled = isEnabled;
		if ( displayer != null && displayer.getCreation() != null )
			((KoLFrame)displayer.getCreation()).setEnabled( isEnabled );
	}

	/**
	 * Initializes the <code>KoLmafia</code> session.  Called after
	 * the login has been confirmed to notify the client that the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String loginname, String sessionID, boolean getBreakfast, boolean isQuickLogin )
	{
		super.initialize( loginname, sessionID, getBreakfast, isQuickLogin );

		KoLCharacter.getInventory().addListDataListener( new KoLCharacterAdapter( null, refresher ) );
		this.refresher.run();

		if ( displayer == null || displayer.getCreation() instanceof AdventureFrame )
			return;

		if ( !isLoggingIn )
		{
			CreateFrameRunnable previousDisplayer = displayer;
			int userInterfaceMode = Integer.parseInt( GLOBAL_SETTINGS.getProperty( "userInterfaceMode" ) );
			Class frameClass = null;

			switch ( userInterfaceMode )
			{
				case 0:
					frameClass = AdventureFrame.class;
					break;

				case 1:
					frameClass = BuffBotFrame.class;
					break;

				case 2:
					frameClass = null;
					KoLMessenger.initialize();
					break;

				case 3:
					frameClass = ClanManageFrame.class;
					break;
			}

			if ( frameClass == null )
				displayer = null;
			else
			{
				Object [] parameters = new Object[1];
				parameters[0] = this;

				displayer = new CreateFrameRunnable( frameClass, parameters );
				displayer.run();
			}

			((KoLFrame)previousDisplayer.getCreation()).setVisible( false );
			((KoLFrame)previousDisplayer.getCreation()).dispose();

		}
	}

	public class ListRefresher implements Runnable
	{
		public synchronized void run()
		{
			// If there is already an instance of this thread
			// running, then there is nothing left to do.

			if ( shouldRefresh )
			{
				KoLCharacter.updateEquipmentLists();
				ConcoctionsDatabase.refreshConcoctions();
			}
		}
	}

	/**
	 * Deinitializes the <code>KoLmafia</code> session.  Called after
	 * the user has logged out.
	 */

	public void deinitialize()
	{
		super.deinitialize();

		if ( displayer == null )
		{
			Object [] parameters = new Object[2];
			parameters[0] = this;
			parameters[1] = saveStateNames;

			displayer = new CreateFrameRunnable( LoginFrame.class, parameters );
			displayer.run();
		}
	}

	/**
	 * Makes a request which attempts to remove the given effect.
	 * This method should prompt the user to determine which effect
	 * the player would like to remove.
	 */

	public void makeUneffectRequest()
	{
		Object selectedValue = JOptionPane.showInputDialog(
			null, "I want to remove this effect...", "It's Soft Green Martian Time!", JOptionPane.INFORMATION_MESSAGE, null,
			KoLCharacter.getEffects().toArray(), KoLCharacter.getEffects().get(0) );

		if ( selectedValue == null )
			return;

		(new RequestThread( new UneffectRequest( this, (AdventureResult) selectedValue ) )).start();
	}

	/**
	 * Makes a request to the hermit, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hermit.
	 */

	public void makeHermitRequest()
	{
		Object selectedValue = JOptionPane.showInputDialog(
			null, "I want this from the hermit...", "Mugging Hermit for...", JOptionPane.INFORMATION_MESSAGE, null,
			hermitItemNames, hermitItemNames[0] );

		if ( selectedValue == null )
			return;

		int selected = -1;
		for ( int i = 0; selected == -1 && i < hermitItemNames.length; ++i )
			if ( selectedValue.equals( hermitItemNames[i] ) )
				selected = hermitItemNumbers[i];

		int trinkets = HermitRequest.TRINKET.getCount( KoLCharacter.getInventory() ) + 
			HermitRequest.GEWGAW.getCount( KoLCharacter.getInventory() ) + 
			HermitRequest.KNICK_KNACK.getCount( KoLCharacter.getInventory() );
		int tradeCount = KoLFrame.getQuantity( "How many " + selectedValue + " to get?", trinkets );

		if ( tradeCount == 0 )
			return;

		(new HermitRequest( this, selected, tradeCount )).run();
	}

	/**
	 * Makes a request to the trapper, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the trapper.
	 */

	public void makeTrapperRequest()
	{
		Object selectedValue = JOptionPane.showInputDialog(
			null, "I want this from the trapper...", "1337ing Trapper for...", JOptionPane.INFORMATION_MESSAGE, null,
			trapperItemNames, trapperItemNames[0] );

		if ( selectedValue == null )
			return;

		int selected = -1;
		for ( int i = 0; i < trapperItemNames.length; ++i )
			if ( selectedValue.equals( trapperItemNames[i] ) )
			{
				selected = trapperItemNumbers[i];
				break;
			}

		// Should not be possible...
		if ( selected == -1 )
			return;

		int tradeCount = KoLFrame.getQuantity( "How many " + selectedValue + " to get?", TrapperRequest.YETI_FUR.getCount( KoLCharacter.getInventory() ) );
		if ( tradeCount == 0 )
			return;

		(new TrapperRequest( this, selected, tradeCount )).run();
	}

	/**
	 * Makes a request to the hunter, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hunter.
	 */

	public void makeHunterRequest()
	{
		if ( hunterItems.isEmpty() )
			(new BountyHunterRequest( this )).run();

		Object [] hunterItemArray = hunterItems.toArray();

		String selectedValue = (String) JOptionPane.showInputDialog(
			null, "I want to sell this to the hunter...", "The Quilted Thicker Picker Upper!", JOptionPane.INFORMATION_MESSAGE, null,
			hunterItemArray, hunterItemArray[0] );

		if ( selectedValue != null )
			(new BountyHunterRequest( this, TradeableItemDatabase.getItemID( selectedValue ) )).run();
	}

	/**
	 * Makes a request to the restaurant, looking for a meal.  This method
	 * should prompt the user to determine which meal to buy.
	 */

	public void makeRestaurantRequest()
	{
		if ( restaurantItems.isEmpty() )
			(new RestaurantRequest( this )).run();

		Object [] restaurantItemArray = restaurantItems.toArray();

		String selectedValue = (String) JOptionPane.showInputDialog(
			null, "I want to buy this from Chez Snootee...", "Chez Snootee", JOptionPane.INFORMATION_MESSAGE, null,
			restaurantItemArray, restaurantItemArray[0] );

		if ( selectedValue != null )
			(new RestaurantRequest( this, selectedValue )).run();
	}

	/**
	 * Makes a request to the microbrewery, looking for a drink.  This
	 * method should prompt the user to determine which drink to buy.
	 */

	public void makeMicrobreweryRequest()
	{
		if ( microbreweryItems.isEmpty() )
			(new MicrobreweryRequest( this )).run();

		Object [] microbreweryItemArray = microbreweryItems.toArray();

		String selectedValue = (String) JOptionPane.showInputDialog(
			null, "I want to buy this from the Gnomish Micromicrobrewery...", "Gnomish Micromicrobrewery", JOptionPane.INFORMATION_MESSAGE, null,
			microbreweryItemArray, microbreweryItemArray[0] );

		if ( selectedValue != null )
			(new MicrobreweryRequest( this, selectedValue )).run();
	}

	/**
	 * Makes a request to the hunter, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hunter.
	 */

	public void makeUntinkerRequest()
	{
		AdventureResult currentItem;
		List untinkerItems = new ArrayList();

		for ( int i = 0; i < KoLCharacter.getInventory().size(); ++i )
		{
			currentItem = (AdventureResult) KoLCharacter.getInventory().get(i);
			if ( ConcoctionsDatabase.getMixingMethod( currentItem.getItemID() ) == ItemCreationRequest.COMBINE )
				untinkerItems.add( currentItem );
		}

		if ( untinkerItems.isEmpty() )
		{
			updateDisplay( ERROR_STATE, "You don't have any untinkerable items." );
			return;
		}

		Object [] untinkerItemArray = untinkerItems.toArray();
		Arrays.sort( untinkerItemArray );

		AdventureResult selectedValue = (AdventureResult) JOptionPane.showInputDialog(
			null, "I want to untinker this item...", "You can unscrew meat paste?", JOptionPane.INFORMATION_MESSAGE, null,
			untinkerItemArray, untinkerItemArray[0] );

		if ( selectedValue != null )
			(new UntinkerRequest( this, selectedValue.getItemID() )).run();
	}

	/**
	 * Set the Canadian Mind Control device to selected setting.
	 */

	public void makeMindControlRequest()
	{
		try
		{
			// Make sure we know current setting

			if ( !CharpaneRequest.wasRunOnce() )
				(new CharpaneRequest( this )).run();

			String [] levelArray = new String[12];
			for ( int i = 0; i < 12; ++i )
				levelArray[i] = "Level " + i;

			String selectedLevel = (String) JOptionPane.showInputDialog(
				null, "Set the device to what level?", "Change mind control device from level " + KoLCharacter.getMindControlLevel(),
					JOptionPane.INFORMATION_MESSAGE, null, levelArray, levelArray[ KoLCharacter.getMindControlLevel() ] );

			(new MindControlRequest( this, df.parse( selectedLevel.split( " " )[1] ).intValue() )).run();
		}
		catch ( Exception e )
		{
		}
	}

	/**
	 * Confirms whether or not the user wants to make a drunken
	 * request.  This should be called before doing requests when
	 * the user is in an inebrieted state.
	 *
	 * @return	<code>true</code> if the user wishes to adventure drunk
	 */

	protected boolean confirmDrunkenRequest()
	{
		return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
			" You see flying penguins and a dancing hermit!\nThe mafia has stolen your shoes!\n(KoLmafia thinks you're too drunk!)\nContinue adventuring anyway?\n",
			"You're not drunk!?", JOptionPane.YES_NO_OPTION );
	}

	/**
	 * Utility method used to print a list to the given output
	 * stream.  If there's a need to print to the current output
	 * stream, simply pass the output stream to this method.
	 */

	protected void printList( List printing )
	{
		if ( printing.isEmpty() )
			return;

		if ( printing.size() == 1 )
		{
			updateDisplay( ERROR_STATE, "You need " + printing.get(0).toString() + " before continuing." );
			return;
		}

		JOptionPane.showInputDialog( null, "The following items are still missing...", "Oops, you did it again!",
			JOptionPane.INFORMATION_MESSAGE, null, printing.toArray(), null );
	}

	public boolean processResults( String results )
	{
		shouldRefresh = false;
		boolean hadResults = super.processResults( results );
		shouldRefresh = true;
		refresher.run();
		return hadResults;
	}

	public void visitCakeShapedArena()
	{
		if ( CakeArenaManager.getOpponentList().isEmpty() )
			(new CakeArenaRequest( this )).run();

		Object [] parameters = new KoLmafia[1];
		parameters[0] = this;

		SwingUtilities.invokeLater( new CreateFrameRunnable( CakeArenaFrame.class, parameters ) );
	}
}
