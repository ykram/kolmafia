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

import java.awt.Component;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import tab.CloseTabbedPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public class MailboxFrame extends KoLFrame implements ChangeListener
{
	private KoLMailMessage displayed;
	private RequestPane messageContent;
	private LimitedSizeChatBuffer mailBuffer;

	private MailSelectList messageListInbox;
	private MailSelectList messageListPvp;
	private MailSelectList messageListOutbox;
	private MailSelectList messageListSaved;

	public MailboxFrame()
	{
		super( "IcePenguin Express" );

		addTab( "Inbox", this.messageListInbox = new MailSelectList( "Inbox" ) );
		addTab( "PvP", this.messageListPvp = new MailSelectList( "PvP" ) );
		addTab( "Outbox", this.messageListOutbox = new MailSelectList( "Outbox" ) );
		addTab( "Saved", this.messageListSaved = new MailSelectList( "Saved" ) );

		tabs.addChangeListener( this );
		tabs.setMinimumSize( new Dimension( 0, 150 ) );

		this.messageContent = new RequestPane();
		messageContent.addHyperlinkListener( new MailLinkClickedListener() );

		this.mailBuffer = new LimitedSizeChatBuffer( false );
		JScrollPane messageContentDisplay = mailBuffer.setChatDisplay( messageContent );
		messageContentDisplay.setMinimumSize( new Dimension( 0, 150 ) );

		JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, true,
			tabs, messageContentDisplay );

		splitPane.setOneTouchExpandable( true );
		JComponentUtilities.setComponentSize( splitPane, 500, 300 );
		getContentPane().add( splitPane );

		JToolBar toolbarPanel = getToolbar();

		if ( toolbarPanel != null )
		{
			toolbarPanel.add( new SaveAllButton() );
			toolbarPanel.add( new DeleteButton() );
			toolbarPanel.add( new RefreshButton() );
		}
	}

	public void setEnabled( boolean isEnabled )
	{
		if ( tabs == null || messageListInbox == null || messageListPvp == null || messageListOutbox == null || messageListSaved == null )
			return;

		for ( int i = 0; i < tabs.getTabCount(); ++i )
			tabs.setEnabledAt( i, isEnabled );

		messageListInbox.setEnabled( isEnabled );
		messageListPvp.setEnabled( isEnabled );
		messageListOutbox.setEnabled( isEnabled );
		messageListSaved.setEnabled( isEnabled );
	}

	/**
	 * Whenever the tab changes, this method is used to retrieve
	 * the messages from the appropriate if the mailbox
	 * is currently empty.
	 */

	public void stateChanged( ChangeEvent e )
	{
		refreshMailManager();
		mailBuffer.clearBuffer();

		boolean requestMailbox;
		String currentTabName = tabs.getTitleAt( tabs.getSelectedIndex() );

		if ( currentTabName.equals( "Inbox" ) )
		{
			if ( messageListInbox.isInitialized() )
				messageListInbox.valueChanged( null );
			requestMailbox = !messageListInbox.isInitialized();
		}
		else if ( currentTabName.equals( "PvP" ) )
		{
			if ( messageListPvp.isInitialized() )
				messageListPvp.valueChanged( null );

			requestMailbox = !messageListPvp.isInitialized();
		}
		else if ( currentTabName.equals( "Outbox" ) )
		{
			if ( messageListOutbox.isInitialized() )
				messageListOutbox.valueChanged( null );
			requestMailbox = !messageListOutbox.isInitialized();
		}
		else
		{
			if ( messageListSaved.isInitialized() )
				messageListSaved.valueChanged( null );
			requestMailbox = !messageListSaved.isInitialized();
		}

		if ( requestMailbox )
			(new MailRefresher( currentTabName )).run();
	}

	private void refreshMailManager()
	{
		messageListInbox.setModel( KoLMailManager.getMessages( "Inbox" ) );
		messageListPvp.setModel( KoLMailManager.getMessages( "PvP" ) );
		messageListOutbox.setModel( KoLMailManager.getMessages( "Outbox" ) );
		messageListSaved.setModel( KoLMailManager.getMessages( "Saved" ) );
	}

	private class MailRefresher implements Runnable
	{
		private String mailboxName;
		private MailboxRequest refresher;

		public MailRefresher( String mailboxName )
		{
			this.mailboxName = mailboxName;
			this.refresher = new MailboxRequest( mailboxName );
		}

		public void run()
		{
			refreshMailManager();
			mailBuffer.append( "Retrieving messages from server..." );

			RequestThread.postRequest( refresher );
			mailBuffer.clearBuffer();

			if ( mailboxName.equals( "Inbox" ) )
				messageListInbox.setInitialized( true );
			else if ( mailboxName.equals( "Outbox" ) )
				messageListOutbox.setInitialized( true );
			else
				messageListSaved.setInitialized( true );
		}
	}

	/**
	 * An internal class used to handle selection of a specific
	 * message from the mailbox list.
	 */

	private class MailSelectList extends JList implements ListSelectionListener
	{
		private String mailboxName;
		private boolean initialized;

		public MailSelectList( String mailboxName )
		{
			super( KoLMailManager.getMessages( mailboxName ) );
			setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			this.mailboxName = mailboxName;
			addListSelectionListener( this );
			addKeyListener( new MailboxKeyListener() );
		}

		public void valueChanged( ListSelectionEvent e )
		{
			if ( mailBuffer == null )
				return;

			mailBuffer.clearBuffer();
			int newIndex = getSelectedIndex();

			if ( newIndex >= 0 && getModel().getSize() > 0 )
			{
				displayed = ((KoLMailMessage)KoLMailManager.getMessages( mailboxName ).get( newIndex ));
				mailBuffer.append( displayed.getDisplayHTML() );
			}
		}

		private boolean isInitialized()
		{	return initialized;
		}

		public void setInitialized( boolean initialized )
		{	this.initialized = initialized;
		}

		private class MailboxKeyListener extends KeyAdapter
		{
			public void keyPressed( KeyEvent e )
			{
				if ( e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE )
				{
					Object [] messages = getSelectedValues();
					if ( messages.length == 0 )
						return;

					if ( JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null,
						"Would you like to delete the selected messages?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) )
							return;

					KoLMailManager.deleteMessages( mailboxName, messages );
				}
				else if ( e.getKeyCode() == KeyEvent.VK_S && mailboxName.equals( "Inbox" ) )
				{
					Object [] messages = getSelectedValues();
					if ( messages.length == 0 )
						return;

					if ( JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null,
						"Would you like to save the selected messages?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) )
							return;

					KoLMailManager.saveMessages( messages );
				}
			}
		}
	}

	private class SaveAllButton extends ThreadedButton
	{
		private Object [] messages = null;

		public SaveAllButton()
		{
			super( JComponentUtilities.getImage( "saveall.gif" ) );
			setToolTipText( "Save Selected" );
		}

		public void run()
		{
			messages = null;
			String currentTabName = tabs.getTitleAt( tabs.getSelectedIndex() );

			if ( currentTabName.equals( "Inbox" ) )
				messages = messageListInbox.getSelectedValues();
			if ( currentTabName.equals( "PvP" ) )
				messages = messageListPvp.getSelectedValues();

			if ( messages == null || messages.length == 0 )
				return;

			if ( JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null,
					"Would you like to save the selected messages?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) )
						return;

			KoLMailManager.saveMessages( messages );
		}
	}

	private class DeleteButton extends ThreadedButton
	{
		private String currentTabName = null;
		private Object [] messages = null;

		public DeleteButton()
		{
			super( JComponentUtilities.getImage( "delete.gif" ) );
			setToolTipText( "Delete Selected" );
		}

		public void run()
		{
			messages = null;
			currentTabName = tabs.getTitleAt( tabs.getSelectedIndex() );
			if ( currentTabName.equals( "Inbox" ) )
				messages = messageListInbox.getSelectedValues();
			else if ( currentTabName.equals( "PvP" ) )
				messages = messageListPvp.getSelectedValues();
			else if ( currentTabName.equals( "Outbox" ) )
				messages = messageListOutbox.getSelectedValues();
			else if ( currentTabName.equals( "Saved" ) )
				messageListSaved.getSelectedValues();

			if ( messages ==  null || messages.length == 0 )
				return;

			if ( JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null,
					"Would you like to delete the selected messages?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) )
						return;

			KoLMailManager.deleteMessages( currentTabName, messages );
		}
	}

	private class RefreshButton extends ThreadedButton
	{
		public RefreshButton()
		{
			super( JComponentUtilities.getImage( "refresh.gif" ) );
			setToolTipText( "Refresh" );
		}

		public void run()
		{
			String currentTabName = tabs.getTitleAt( tabs.getSelectedIndex() );
			(new MailRefresher( currentTabName.equals( "PvP" ) ? "Inbox" : currentTabName )).run();
		}
	}

	/**
	 * Action listener responsible for opening a sendmessage frame when
	 * reply or quote is clicked or opening a frame in the browser when
	 * something else is clicked.
	 */

	private class MailLinkClickedListener extends KoLHyperlinkAdapter
	{
		public void handleInternalLink( String location )
		{
			// If you click on the player name:
			//     showplayer.php?who=<playerid>

			if ( !location.startsWith( "sendmessage.php" ) )
			{
				StaticEntity.openRequestFrame( location );
				return;
			}

			// If you click on [reply]:
			//     sendmessage.php?toid=<playerid>
			// If you click on [quote]:
			//     sendmessage.php?toid=<playerid>&quoteid=xxx&box=xxx

			StringTokenizer tokens = new StringTokenizer( location, "?=&" );
			tokens.nextToken();  tokens.nextToken();

			String recipient = tokens.nextToken();

			Object [] parameters = new Object[ tokens.hasMoreTokens() ? 2 : 1 ];
			parameters[0] = recipient;

			if ( parameters.length == 2 )
			{
				String rawText = displayed.getCompleteHTML();
				int start = rawText.indexOf( "<br><br>" ) + 8;
				String text =  rawText.substring( start );

				// Replace <br> tags with a line break and
				// quote the following line

				text = text.replaceAll( "<br>", LINE_BREAK + "> " );

				// Remove all other HTML tags

				text = text.replaceAll( "><", "" ).replaceAll( "<.*?>", "" );

				// Quote first line and end with a line break

				text = "> " + text + LINE_BREAK;

				parameters[1] = text;
			}

			createDisplay( SendMessageFrame.class, parameters );
		}
	}
}
