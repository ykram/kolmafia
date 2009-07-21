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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MoodManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollablePanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import net.sourceforge.kolmafia.utilities.BooleanArray;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MaximizerFrame
	extends GenericFrame
	implements ListSelectionListener
{
	public static final LockableListModel boosts = new LockableListModel();
	public static Evaluator eval;

	private static boolean firstTime = true;
	public static final JComboBox expressionSelect = new JComboBox( "mainstat|mus|mys|mox|familiar weight|HP|MP|ML|DA|DR|+combat|-combat|initiative|exp|meat drop|item drop|2.0 meat, 1.0 item|weapon damage|ranged damage|spell damage|adv|hot res|cold res|spooky res|stench res|sleaze res|all res|ML, 0.001 slime res".split( "\\|") );
	private SmartButtonGroup equipmentSelect, mallSelect;
	private AutoHighlightTextField maxPriceField;
	private JCheckBox includeAll;
	private final ShowDescriptionList boostList;
	private JLabel listTitle = null;
	
	private static Spec best;
	private static int bestChecked;
	private static long bestUpdate;
	
	private static final String TIEBREAKER = "10 familiar weight, 1 initiative, 5 exp, 1 item, 1 meat, 0.1 DA 1000 max, 1 DR, 0.5 all res, -10 mana cost, 1.0 mus, 0.5 mys, 1.0 mox, 1.5 mainstat, 1 HP, 1 MP, 1 weapon damage, 1 ranged damage, 1 spell damage, 1 cold damage, 1 hot damage, 1 sleaze damage, 1 spooky damage, 1 stench damage, 1 cold spell damage, 1 hot spell damage, 1 sleaze spell damage, 1 spooky spell damage, 1 stench spell damage, 1 critical, -1 fumble, 1 HP regen max, 3 MP regen max, 1 critical hit percent, 0.1 food drop, 0.1 booze drop, 0.1 hat drop, 0.1 weapon drop, 0.1 offhand drop, 0.1 shirt drop, 0.1 pants drop, 0.1 accessory drop";
	
	private static final String HELP_STRING = "<html><table width=750><tr><td>" +
		"<h3>General</h3>" +
		"The specification of what attributes to maximize is made by a comma-separated list of keywords, each possibly preceded by a numeric weight.  Commas can be omitted if the next item starts with a +, -, or digit.  Using just a +, or omitting the weight entirely, is equivalent to a weight of 1.  Likewise, using just a - is equivalent to a weight of -1.  Non-integer weights can be used, but may not be meaningful with all keywords." +
		"<h3>Modifiers</h3>" +
		"The full name of any numeric modifier (as shown by the <b>modref</b> CLI command) is a valid keyword, requesting that its value be maximized.  If multiple modifiers are given, their weights specify their relative importance.  Negative weights mean that smaller values are more desirable for that modifier." +
		"<p>" +
		"Shorter forms are allowed for many commonly used modifiers.  They can be abbreviated down to just the bold letters:" +
		"<br><b>mus</b>, <b>mys</b>, <b>mox</b>, <b>main</b>stat, <b>HP</b>, <b>MP</b>, <b>ML</b>, <b>DA</b>, <b>DR</b>, <b>com</b>bat rate, <b>item</b> drop, <b>meat</b> drop, <b>exp</b>erience, <b>adv</b>entures" +
		"<br>Also, resistance (of any type) can be abbreviated as <b>res</b>.  <b>all res</b>istance is a shortcut for giving the same weight to all five basic elements." +
		"<p>" +
		"Note that many modifiers come in pairs: a base value, plus a percentage boost (such as Moxie and Moxie Percent), or a penalty value.  In general, you only need to specify the base modifier, and any related modifiers will automatically be taken into account." +
		"<h3>Limits</h3>" +
		"Any modifier keyword can be followed by one or both of these special keywords:" +
		"<br><b>min</b> - The weight specifies the minimum acceptable value for the preceding modifier.  If the value is lower, the results will be flagged as a failure." +
		"<br><b>max</b> - The weight specifies the largest useful value for the preceding modifier.  Larger values will be ignored in the score calculation, allowing other specified modifiers to be boosted instead." +
		"<br>Note that the limit keywords won't quite work as expected for a modifier that you're trying to minimize." +
		"<h3>Equipment</h3>" +
		"Equipment suggestions are only partially implemented at the moment, with many planned features missing.  There will eventually be keywords for specifying which slots are allowed, constraining the type or handedness of your weapon, etc." +
		"<h3>Assumptions</h3>" +
		"All suggestions are based on the assumption that you will be adventuring in the currently selected location, with all your current effects, prior to the next rollover (since some things depend on the moon phases).  For best results, make sure the proper location is selected before maximizing.  This is especially true in The Sea and clan dungeons, which have many location-specific modifiers." +
		"<p>" +
		"Among effects, stat equalizer potions have a major effect on the suggested boosts, since they change the relative importance of additive and percentage stat boosts.  Likewise, elemental phials make certain resistance boosts pointless.  If you plan to use an equalizer or phial while adventuring, please use them first so that the suggestions take them into account." +
		"<h3>GUI Use</h3>" +
		"If the Max Price field is zero or blank, the limit will be the smaller of your available meat, or your autoBuyPriceLimit (default 20,000).  The other options should be self-explanatory." +
		"<p>" +
		"You can select multiple boosts, and the title of the list will indicate the net effect of applying them all - note that this isn't always just the sum of their individual effects." +
		"<h3>CLI Use</h3>" +
		"The Modifier Maximizer can be invoked from the gCLI or a script via <b>maximize <i>expression</i></b>, and will behave as if you'd selected Equipment: on-hand only, Max Price: don't check, and turned off the Include option.  The best equipment will automatically be equipped (once equipment suggestions are implemented, and you didn't invoke the command as <b>maximize? <i>expression</i></b>), but you'll still need to visit the GUI to apply effect boosts - there are too many factors in choosing between the available boosts for that to be safely automated." +
		"<h3>Limitations &amp; Bugs</h3>" +
		"This is still a work-in-progress, so don't expect ANYTHING to work perfectly at the moment.  However, here are some details that are especially broken:" +
		"<br>\u2022 Items that can be installed at your campground for a bonus (such as Hobopolis bedding) aren't considered." +
		"<br>\u2022 Your song limit isn't considered when recommending buffs, nor are any daily casting limits." +
		"<br>\u2022 Mutually exclusive effects aren't handled properly." +
		"<br>\u2022 Weapon Damage, Ranged Damage, and Spell Damage are calculated assuming 100 points of base damage - in other words, additive and percentage boosts are considered to have exactly equal worth.  It's possible that Weapon and Ranged damage might use a better estimate of the base damage in the future, but for Spell Damage, the proper base depends on which spell you end up using." +
		"<br>\u2022 Effects which vary in power based on how many turns are left (love songs, Mallowed Out, etc.) are handled poorly.  If you don't have the effect, they'll be suggested based on the results you'd get from having a single turn of it.  If you have the effect already, extending it to raise the power won't even be considered.  Similar problems occur with effects that are based on how full or drunk you currently are." +
		"</td></tr></table></html>";

	public MaximizerFrame()
	{
		super( "Modifier Maximizer" );

		this.framePanel.add( new MaximizerPanel(), BorderLayout.NORTH );

		this.boostList = new ShowDescriptionList( MaximizerFrame.boosts, 12 );
		this.boostList.addListSelectionListener( this );

		this.framePanel.add( new BoostsPanel( this.boostList ), BorderLayout.CENTER );
		if ( this.eval != null )
		{
			this.valueChanged( null );
		}
	}

	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	public void valueChanged( final ListSelectionEvent e )
	{
		float current = MaximizerFrame.eval.getScore(
			KoLCharacter.getCurrentModifiers() );
		boolean failed = MaximizerFrame.eval.failed;
		Object[] items = this.boostList.getSelectedValues();
		
		StringBuffer buff = new StringBuffer( "Current score: " );
		buff.append( KoLConstants.FLOAT_FORMAT.format( current ) );
		if ( failed )
		{
			buff.append( " (FAILED)" );		
		}
		buff.append( " \u25CA Predicted: " );
		if ( items.length == 0 )
		{
			buff.append( "---" );		
		}
		else
		{
			Spec spec = new Spec();
			for ( int i = 0; i < items.length; ++i )
			{
				if ( items[ i ] instanceof Boost )
				{
					((Boost) items[ i ]).addTo( spec );
				}
			}
			float score = spec.getScore();
			buff.append( KoLConstants.FLOAT_FORMAT.format( score ) );
			buff.append( " (" );
			buff.append( KoLConstants.MODIFIER_FORMAT.format( score - current ) );
			if ( spec.failed )
			{
				buff.append( ", FAILED)" );		
			}
			else
			{
				buff.append( ")" );		
			}
		}
		if ( this.listTitle != null )
		{
			this.listTitle.setText( buff.toString() );
		}
	}
	
	public void maximize()
	{
		MaximizerFrame.maximize( this.equipmentSelect.getSelectedIndex(),
			InputFieldUtilities.getValue( this.maxPriceField ),
			this.mallSelect.getSelectedIndex(),
			this.includeAll.isSelected() );
		this.valueChanged( null );
	}
	
	public static void maximize( int equipLevel, int maxPrice, int priceLevel, boolean includeAll )
	{
		KoLmafia.forceContinue();
		MaximizerFrame.eval = new Evaluator( (String)
			MaximizerFrame.expressionSelect.getSelectedItem() );
		if ( !KoLmafia.permitsContinue() ) return;	// parsing error

		float current = MaximizerFrame.eval.getScore(
			KoLCharacter.getCurrentModifiers() );
		if ( maxPrice <= 0 )
		{
			maxPrice = Math.min( Preferences.getInteger( "autoBuyPriceLimit" ),
				KoLCharacter.getAvailableMeat() );
		}
	
		RequestThread.openRequestSequence();
		KoLmafia.updateDisplay( MaximizerFrame.firstTime ?
			"Maximizing (1st time may take a while)..." : "Maximizing..." );
		MaximizerFrame.firstTime = false;
	
		MaximizerFrame.boosts.clear();
		if ( equipLevel != 0 )
		{
			if ( equipLevel > 1 )
			{
				MaximizerFrame.boosts.add( new Boost( "", "(creating/folding/pulling/buying equipment not considered yet)", -1, null, 0.0f ) );
			}
			MaximizerFrame.boosts.add( new Boost( "", "(only weapons and accessories are considered at the moment)", -1, null, 0.0f ) );
			MaximizerFrame.best = new Spec();
			MaximizerFrame.bestChecked = 0;
			MaximizerFrame.bestUpdate = System.currentTimeMillis() + 5000;
			MaximizerFrame.eval.enumerateEquipment( equipLevel, maxPrice, priceLevel );
			Spec.showProgress();
			
			for ( int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot )
			{	
				String slotname = EquipmentRequest.slotNames[ slot ];
				AdventureResult item = MaximizerFrame.best.equipment[ slot ];
				AdventureResult curr = EquipmentManager.getEquipment( slot );
				if ( curr.equals( item ) )
				{
					if ( slot >= EquipmentManager.STICKER1 ||
						curr.equals( EquipmentRequest.UNEQUIP ) )
					{
						continue;
					}
					MaximizerFrame.boosts.add( new Boost( "", "keep " + slotname + ": " + item.getName(), -1, item, 0.0f ) );
					continue;
				}
				Spec spec = new Spec();
				spec.equip( slot, item );
				float delta = spec.getScore() - current;
				String cmd, text;
				if ( item == null || item.equals( EquipmentRequest.UNEQUIP ) )
				{
					cmd = "unequip " + slotname;
					text = cmd + " (" + curr.getName() + ", " +
						KoLConstants.MODIFIER_FORMAT.format( delta ) + ")";
				}
				else
				{
					cmd = "equip " + slotname + " " + item.getName();
					text = cmd + " (" + KoLConstants.MODIFIER_FORMAT.format(
						delta ) + ")";
				}
			
				MaximizerFrame.boosts.add( new Boost( cmd, text, slot, item, delta ) );
			}
		}
		Iterator i = Modifiers.getAllModifiers();
		while ( i.hasNext() )
		{
			String name = (String) i.next();
			if ( !EffectDatabase.contains( name ) )
			{
				continue;
			}

			float delta;
			Spec spec = new Spec();
			AdventureResult effect = new AdventureResult( name, 1, true );
			name = effect.getName();
			boolean hasEffect = KoLConstants.activeEffects.contains( effect );

			String cmd, text;
			AdventureResult item = null;
			int price = 0;
			if ( !hasEffect )
			{
				spec.addEffect( effect );
				delta = spec.getScore() - current;
				if ( delta <= 0.0f ) continue;
				cmd = MoodManager.getDefaultAction( "lose_effect", name );
				if ( cmd.length() == 0 )
				{
					if ( includeAll )
					{
						text = "(no direct source of " + name + ")";
					}
					else continue;
				}
				else
				{
					text = cmd;
				}
			}
			else
			{
				spec.removeEffect( effect );
				delta = spec.getScore() - current;
				if ( delta <= 0.0f ) continue;
				cmd = MoodManager.getDefaultAction( "gain_effect", name );
				if ( cmd.length() == 0 )
				{
					if ( includeAll )
					{
						text = "(find some way to remove " + name + ")";
					}
					else continue;
				}
				else
				{
					text = cmd;
					if ( cmd.toLowerCase().indexOf( name.toLowerCase() ) == -1 )
					{
						text = text + " (to remove " + name + ")";
					}
				}
			}
			
			if ( cmd.startsWith( "use " ) || cmd.startsWith( "chew " ) ||
				cmd.startsWith( "drink " ) || cmd.startsWith( "eat " ) )
			{
				item = ItemFinder.getFirstMatchingItem(
					cmd.substring( cmd.indexOf( " " ) + 1 ).trim(), false );
			}
			else if ( cmd.startsWith( "gong " ) )
			{
				item = ItemPool.get( ItemPool.GONG, 1 );
			}
			else if ( cmd.startsWith( "cast " ) )
			{
				if ( !KoLCharacter.hasSkill( UneffectRequest.effectToSkill( name ) ) )
				{
					if ( includeAll )
					{
						text = "(learn to " + cmd + ", or get it from a buffbot)";
						cmd = "";
					}
					else continue;
				}
			}
			else if ( cmd.startsWith( "concert " ) )
			{
				if ( Preferences.getBoolean( "concertVisited" ) )
				{
					cmd = "";
				}
			}
			else if ( cmd.startsWith( "telescope " ) )
			{
				if ( Preferences.getBoolean( "telescopeLookedHigh" ) )
				{
					cmd = "";
				}
			}
			else if ( cmd.startsWith( "styx " ) )
			{
				if ( !KoLCharacter.inBadMoon() )
				{
					continue;
				}
			}
			
			if ( item != null )
			{
				String iname = item.getName();
				
				int full = ItemDatabase.getFullness( iname );
				if ( full > 0 &&
					KoLCharacter.getFullness() + full > KoLCharacter.getFullnessLimit() )
				{
					cmd = "";
				}
				full = ItemDatabase.getInebriety( iname );
				if ( full > 0 &&
					KoLCharacter.getInebriety() + full > KoLCharacter.getInebrietyLimit() )
				{
					cmd = "";
				}
				full = ItemDatabase.getSpleenHit( iname );
				if ( full > 0 &&
					KoLCharacter.getSpleenUse() + full > KoLCharacter.getSpleenLimit() )
				{
					cmd = "";
				}
				if ( !ItemDatabase.meetsLevelRequirement( iname ) )
				{
					if ( includeAll )
					{
						text = "level up & " + text;
						cmd = "";
					}
					else continue;
				}
				
				if ( cmd.length() > 0 )
				{
					Concoction c = ConcoctionPool.get( item );
					price = c.price;
					int count = Math.max( 0, item.getCount() - c.initial );
					if ( count > 0 )
					{
						int create = Math.min( count, c.creatable );
						count -= create;
						if ( create > 0 )
						{
							text = create > 1 ? "make " + create + " & " + text
								: "make & " + text;
						}
						int buy = price > 0 ? Math.min( count, KoLCharacter.getAvailableMeat() / price) : 0;
						count -= buy;
						if ( buy > 0 )
						{
							text = buy > 1 ? "buy " + buy + " & " + text
								: "buy & " + text;
							cmd = "buy " + buy + " \u00B6" + item.getItemId() +
								";" + cmd;
						}
						if ( count > 0 )
						{
							text = count > 1 ? "acquire " + count + " & " + text
								: "acquire & " + text;
						}
					}
					if ( priceLevel == 2 || (priceLevel == 1 && count > 0) )
					{
						if ( price <= 0 && KoLCharacter.canInteract() &&
							ItemDatabase.isTradeable( item.getItemId() ) )
						{
							price = StoreManager.getMallPrice( item );
						}
					}
					if ( price > maxPrice ) continue;
				}
			}
			
			if ( price > 0 )
			{
				text = text + " (" + KoLConstants.COMMA_FORMAT.format( price ) +
					" meat, " + 
					KoLConstants.MODIFIER_FORMAT.format( delta ) + ")";
			}
			else
			{
				text = text + " (" + KoLConstants.MODIFIER_FORMAT.format(
					delta ) + ")";
			}
			MaximizerFrame.boosts.add( new Boost( cmd, text, effect, hasEffect,
				item, delta ) );
		}
		if ( MaximizerFrame.boosts.size() == 0 )
		{
			MaximizerFrame.boosts.add( new Boost( "", "(nothing useful found)", 0, null, 0.0f ) );
		}
		MaximizerFrame.boosts.sort();
		RequestThread.closeRequestSequence();
	}

	private class MaximizerPanel
		extends GenericPanel
	{
		public MaximizerPanel()
		{
			super( "update", "help", new Dimension( 80, 20 ), new Dimension( 420, 20 ) );

			MaximizerFrame.expressionSelect.setEditable( true );
			MaximizerFrame.this.maxPriceField = new AutoHighlightTextField();
			JComponentUtilities.setComponentSize( MaximizerFrame.this.maxPriceField, 80, -1 );
			MaximizerFrame.this.includeAll = new JCheckBox( "effects with no direct source, skills you don't have, etc." );
			
			JPanel equipPanel = new JPanel( new FlowLayout( FlowLayout.LEADING, 0, 0 ) );
			MaximizerFrame.this.equipmentSelect = new SmartButtonGroup( equipPanel );
			MaximizerFrame.this.equipmentSelect.add( new JRadioButton( "none" ) );
			MaximizerFrame.this.equipmentSelect.add( new JRadioButton( "on hand", true ) );
			MaximizerFrame.this.equipmentSelect.add( new JRadioButton( "creatable/foldable" ) );
			MaximizerFrame.this.equipmentSelect.add( new JRadioButton( "pullable/buyable" ) );
			
			JPanel mallPanel = new JPanel( new FlowLayout( FlowLayout.LEADING, 0, 0 ) );
			mallPanel.add( MaximizerFrame.this.maxPriceField );
			MaximizerFrame.this.mallSelect = new SmartButtonGroup( mallPanel );
			MaximizerFrame.this.mallSelect.add( new JRadioButton( "don't check", true ) );
			MaximizerFrame.this.mallSelect.add( new JRadioButton( "buyable only" ) );
			MaximizerFrame.this.mallSelect.add( new JRadioButton( "all consumables" ) );

			VerifiableElement[] elements = new VerifiableElement[ 4 ];
			elements[ 0 ] = new VerifiableElement( "Maximize: ", MaximizerFrame.expressionSelect );
			elements[ 1 ] = new VerifiableElement( "Equipment: ", equipPanel );
			elements[ 2 ] = new VerifiableElement( "Max price: ", mallPanel );
			elements[ 3 ] = new VerifiableElement( "Include: ", MaximizerFrame.this.includeAll );

			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			MaximizerFrame.this.maximize();
		}

		public void actionCancelled()
		{
			//InputFieldUtilities.alert( MaximizerFrame.HELP_STRING );
			JLabel help = new JLabel( MaximizerFrame.HELP_STRING );
			//JComponentUtilities.setComponentSize( help, 750, -1 );
			GenericScrollPane content = new GenericScrollPane( help );
			JComponentUtilities.setComponentSize( content, -1, 500 );
			JOptionPane.showMessageDialog( this, content, "Modifier Maximizer help",
				JOptionPane.PLAIN_MESSAGE );
		}
	}

	private class BoostsPanel
		extends ScrollablePanel
	{
		private final ShowDescriptionList elementList;
	
		public BoostsPanel( final ShowDescriptionList list )
		{
			super( "Current score: --- \u25CA Predicted: ---",
				"equip all", "execute", list );
			this.elementList = (ShowDescriptionList) this.scrollComponent;
			MaximizerFrame.this.listTitle = this.titleComponent;
		}
	
		public void actionConfirmed()
		{
			KoLmafia.forceContinue();
			boolean any = false;
			Iterator i = MaximizerFrame.boosts.iterator();
			while ( i.hasNext() )
			{
				Object boost = i.next();
				if ( boost instanceof Boost )
				{
					boolean did = ((Boost) boost).execute( true );
					if ( !KoLmafia.permitsContinue() ) return;
					any |= did;
				}
			}
			if ( any )
			{
				MaximizerFrame.this.maximize();
			}
		}
	
		public void actionCancelled()
		{
			KoLmafia.forceContinue();
			boolean any = false;
			Object[] boosts = this.elementList.getSelectedValues();
			for ( int i = 0; i < boosts.length; ++i )
			{
				if ( boosts[ i ] instanceof Boost )
				{
					boolean did = ((Boost) boosts[ i ]).execute( false );
					if ( !KoLmafia.permitsContinue() ) return;
					any |= did;
				}
			}
			if ( any )
			{
				MaximizerFrame.this.maximize();
			}
		}
	}

	public static class SmartButtonGroup
		extends ButtonGroup
	{	// A version of ButtonGroup that actually does useful things:
		// * Constructor takes a parent container, adding buttons to
		// the group adds them to the container as well.  This generally
		// removes any need for a temp variable to hold the individual
		// buttons as they're being created.
		// * getSelectedIndex() to determine which button (0-based) is
		// selected.  How could that have been missing???
		
		private Container parent;
		
		public SmartButtonGroup( Container parent )
		{
			this.parent = parent;
		}
		
		public void add( AbstractButton b )
		{
			super.add( b );
			parent.add( b );
		}
	
		public int getSelectedIndex()
		{
			int i = 0;
			Enumeration e = this.getElements();
			while ( e.hasMoreElements() )
			{
				if ( ((AbstractButton) e.nextElement()).isSelected() )
				{
					return i;
				}
				++i;
			}
			return -1;
		}
	}
	
	private static class Evaluator
	{
		public boolean failed;
		private Evaluator tiebreaker;
		private float[] weight, min, max;
		private boolean dump = false;

		private static final Pattern KEYWORD_PATTERN = Pattern.compile( "\\G\\s*(\\+|-|)([\\d.]*)\\s*((?:[^-+,0-9]|(?<! )[-+0-9])+),?\\s*" );
		// Groups: 1=sign 2=weight 3=keyword
		
		private Evaluator()
		{
		}
		
		public Evaluator( String expr )
		{
			this.tiebreaker = new Evaluator();
			this.weight = new float[ Modifiers.FLOAT_MODIFIERS ];
			this.tiebreaker.weight = new float[ Modifiers.FLOAT_MODIFIERS ];
			this.tiebreaker.min = new float[ Modifiers.FLOAT_MODIFIERS ];
			this.tiebreaker.max = new float[ Modifiers.FLOAT_MODIFIERS ];
			Arrays.fill( this.tiebreaker.min, Float.NEGATIVE_INFINITY );
			Arrays.fill( this.tiebreaker.max, Float.POSITIVE_INFINITY );
			this.tiebreaker.parse( MaximizerFrame.TIEBREAKER );
			this.min = (float[]) this.tiebreaker.min.clone();
			this.max = (float[]) this.tiebreaker.max.clone();
			this.parse( expr );
		}
		
		private void parse( String expr )
		{
			expr = expr.trim().toLowerCase();
			Matcher m = KEYWORD_PATTERN.matcher( expr );
			int pos = 0;
			int index = -1;
			while ( pos < expr.length() )
			{
				if ( !m.find() )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
						"Unable to interpret: " + expr.substring( pos ) );
					return;
				}
				pos = m.end();
				float weight = StringUtilities.parseFloat(
					m.end( 2 ) == m.start( 2 ) ? m.group( 1 ) + "1"
						: m.group( 1 ) + m.group( 2 ) );
				
				String keyword = m.group( 3 ).trim();
				if ( keyword.equals( "min" ) )
				{
					if ( index >= 0 )
					{
						this.min[ index ] = weight;
						continue;
					}
					else
					{
						KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
							"'min' without preceding modifier" );
						return;
					}
				}
				else if ( keyword.equals( "max" ) )
				{
					if ( index >= 0 )
					{
						this.max[ index ] = weight;
						continue;
					}
					else
					{
						KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
							"'max' without preceding modifier" );
						return;
					}
				}
				else if ( keyword.equals( "dump" ) )
				{
					this.dump = true;
					continue;
				}
				
				index = Modifiers.findName( keyword );
				if ( index < 0 )
				{	// try generic abbreviations
					if ( keyword.endsWith( " res" ) )
					{
						keyword += "istance";
					}
					index = Modifiers.findName( keyword );
				}
				
				if ( index >= 0 )
				{	// exact match
				}
				else if ( keyword.equals( "all resistance" ) )
				{
					this.weight[ Modifiers.COLD_RESISTANCE ] = weight;
					this.weight[ Modifiers.HOT_RESISTANCE ] = weight;
					this.weight[ Modifiers.SLEAZE_RESISTANCE ] = weight;
					this.weight[ Modifiers.SPOOKY_RESISTANCE ] = weight;
					this.weight[ Modifiers.STENCH_RESISTANCE ] = weight;
					continue;
				}
				else if ( keyword.equals( "hp" ) )
				{
					index = Modifiers.HP;
				}
				else if ( keyword.equals( "mp" ) )
				{
					index = Modifiers.MP;
				}
				else if ( keyword.equals( "da" ) )
				{
					index = Modifiers.DAMAGE_ABSORPTION;
				}
				else if ( keyword.equals( "dr" ) )
				{
					index = Modifiers.DAMAGE_REDUCTION;
				}
				else if ( keyword.equals( "ml" ) )
				{
					index = Modifiers.MONSTER_LEVEL;
				}
				else if ( keyword.startsWith( "mus" ) )
				{
					index = Modifiers.MUS;
				}
				else if ( keyword.startsWith( "mys" ) )
				{
					index = Modifiers.MYS;
				}
				else if ( keyword.startsWith( "mox" ) )
				{
					index = Modifiers.MOX;
				}
				else if ( keyword.startsWith( "main" ) )
				{
					switch ( KoLCharacter.getPrimeIndex() )
					{
					case 0:
						index = Modifiers.MUS;
						break;
					case 1:
						index = Modifiers.MYS;
						break;
					case 2:
						index = Modifiers.MOX;
						break;
					}
				}
				else if ( keyword.startsWith( "com" ) )
				{
					index = Modifiers.COMBAT_RATE;
				}
				else if ( keyword.startsWith( "item" ) )
				{
					index = Modifiers.ITEMDROP;
				}
				else if ( keyword.startsWith( "meat" ) )
				{
					index = Modifiers.MEATDROP;
				}
				else if ( keyword.startsWith( "adv" ) )
				{
					index = Modifiers.ADVENTURES;
				}
				else if ( keyword.startsWith( "exp" ) )
				{
					index = Modifiers.EXPERIENCE;
				}
				
				if ( index >= 0 )
				{
					this.weight[ index ] = weight;
					continue;
				}
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"Unrecognized keyword: " + keyword );
				return;
			}
		}
		
		public float getScore( Modifiers mods )
		{
			this.failed = false;
			int[] predicted = mods.predict();
			
			float score = 0.0f;
			for ( int i = 0; i < Modifiers.FLOAT_MODIFIERS; ++i )
			{
				float weight = this.weight[ i ];
				float min = this.min[ i ];
				if ( weight == 0.0f && min == Float.NEGATIVE_INFINITY ) continue;
				float val = mods.get( i );
				float max = this.max[ i ];
				switch ( i )
				{
				case Modifiers.MUS:
					val = predicted[ Modifiers.BUFFED_MUS ];
					break;
				case Modifiers.MYS:
					val = predicted[ Modifiers.BUFFED_MYS ];
					break;
				case Modifiers.MOX:
					val = predicted[ Modifiers.BUFFED_MOX ];
					break;
				case Modifiers.FAMILIAR_WEIGHT:
					val += mods.get( Modifiers.HIDDEN_FAMILIAR_WEIGHT );
					if ( mods.get( Modifiers.FAMILIAR_WEIGHT_PCT ) < 0.0f )
					{
						val *= 0.5f;
					}
					break;
				case Modifiers.MANA_COST:
					val += mods.get( Modifiers.STACKABLE_MANA_COST );
					break;
				case Modifiers.INITIATIVE:
					val += Math.min( 0.0f, mods.get( Modifiers.INITIATIVE_PENALTY ) );
					break;
				case Modifiers.MEATDROP:
					val += 100.0f + Math.min( 0.0f, mods.get( Modifiers.MEATDROP_PENALTY ) );
					break;
				case Modifiers.ITEMDROP:
					val += 100.0f + Math.min( 0.0f, mods.get( Modifiers.ITEMDROP_PENALTY ) );
					break;
				case Modifiers.HP:
					val = predicted[ Modifiers.BUFFED_HP ];
					break;
				case Modifiers.MP:
					val = predicted[ Modifiers.BUFFED_MP ];
					break;
				case Modifiers.WEAPON_DAMAGE:	
					// Incorrect - needs to estimate base damage
					val += mods.get( Modifiers.WEAPON_DAMAGE_PCT );
					break;
				case Modifiers.RANGED_DAMAGE:	
					// Incorrect - needs to estimate base damage
					val += mods.get( Modifiers.RANGED_DAMAGE_PCT );
					break;
				case Modifiers.SPELL_DAMAGE:	
					// Incorrect - base damage depends on spell used
					val += mods.get( Modifiers.SPELL_DAMAGE_PCT );
					break;
				case Modifiers.COLD_RESISTANCE:
				case Modifiers.HOT_RESISTANCE:
				case Modifiers.SLEAZE_RESISTANCE:
				case Modifiers.SPOOKY_RESISTANCE:
				case Modifiers.STENCH_RESISTANCE:
					if ( mods.getBoolean( i - Modifiers.COLD_RESISTANCE + Modifiers.COLD_IMMUNITY ) )
					{
						val = 100.0f;
					}
					else if ( mods.getBoolean( i - Modifiers.COLD_RESISTANCE + Modifiers.COLD_VULNERABILITY ) )
					{
						val -= 100.0f;
					}
					break;
				}
				if ( val < min ) this.failed = true;
				score += weight * Math.min( val, max );
			}
			return score;
		}
		
		public float getTiebreaker( Modifiers mods )
		{
			return this.tiebreaker.getScore( mods );
		}
	
		public void enumerateEquipment( int equipLevel, int maxPrice, int priceLevel )
		{
			// Items automatically considered regardless of their score -
			// outfit pieces, synergies, hobo power, brimstone, etc.
			ArrayList[] automatic = new ArrayList[ EquipmentManager.ALL_SLOTS ];
			// Items with a positive score
			ArrayList[] ranked = new ArrayList[ EquipmentManager.ALL_SLOTS ];
			// Items with zero score (in case no positive scores were found)
			ArrayList[] neutral = new ArrayList[ EquipmentManager.ALL_SLOTS ];
			for ( int i = 0; i < EquipmentManager.ALL_SLOTS; ++i )
			{
				automatic[ i ] = new ArrayList();
				ranked[ i ] = new ArrayList();
				neutral[ i ] = new ArrayList();
			}
			
			float nullScore = this.getScore( new Modifiers() );

			BooleanArray usefulOutfits = new BooleanArray();
			TreeMap outfitPieces = new TreeMap();
			for ( int i = 1; i < EquipmentDatabase.normalOutfits.size(); ++i )
			{
				SpecialOutfit outfit = EquipmentDatabase.normalOutfits.get( i );
				if ( outfit == null ) continue;
				Modifiers mods = Modifiers.getModifiers( outfit.getName() );
				if ( mods == null )	continue;
				float delta = this.getScore( mods ) - nullScore;
				if ( delta <= 0.0f ) continue;
				usefulOutfits.set( i, true );
			}
			
			int usefulSynergies = 0;
			Iterator syn = Modifiers.getSynergies();
			while ( syn.hasNext() )
			{
				Modifiers mods = Modifiers.getModifiers( (String) syn.next() );
				int value = ((Integer) syn.next()).intValue();
				if ( mods == null )	continue;
				float delta = this.getScore( mods ) - nullScore;
				if ( delta > 0.0f ) usefulSynergies |= value;
			}
			
			boolean hoboPowerUseful = false;
			boolean brimstoneUseful = false;
			boolean slimeHateUseful = false;
			{
				Modifiers mods = Modifiers.getModifiers( "_hoboPower" );
				if ( mods != null &&
					this.getScore( mods ) - nullScore > 0.0f )
				{
					hoboPowerUseful = true;
				}
			}
			{
				Modifiers mods = Modifiers.getModifiers( "_brimstone" );
				if ( mods != null &&
					this.getScore( mods ) - nullScore > 0.0f )
				{
					brimstoneUseful = true;
				}
			}
			{
				Modifiers mods = Modifiers.getModifiers( "_slimeHate" );
				if ( mods != null &&
					this.getScore( mods ) - nullScore > 0.0f )
				{
					slimeHateUseful = true;
				}
			}
				
			
			int id = 0;
			while ( (id = EquipmentDatabase.nextEquipmentItemId( id )) != -1 )
			{
				int count = InventoryManager.getAccessibleCount( id );
				if ( count <= 0 ) continue;
				if ( !EquipmentManager.canEquip( id ) ) continue;
				int slot = EquipmentManager.itemIdToEquipmentType( id );
				if ( slot < 0 || slot >= EquipmentManager.ALL_SLOTS ) continue;
				AdventureResult item = ItemPool.get( id, count );
				if ( usefulOutfits.get( EquipmentDatabase.getOutfitWithItem( id ) ) )
				{
					outfitPieces.put( item, item );
				}
				if ( KoLCharacter.hasEquipped( item ) )
				{
					automatic[ slot ].add( item );
					continue;
				}
				String name = item.getName();
				Modifiers mods = Modifiers.getModifiers( name );
				if ( mods == null )	// no enchantments
				{
					neutral[ slot ].add( item );
					continue;
				}
				if ( mods.getBoolean( Modifiers.SINGLE ) )
				{
					item = item.getInstance( 1 );
				}
				if ( hoboPowerUseful &&
					mods.get( Modifiers.HOBO_POWER ) > 0.0f )
				{
					automatic[ slot ].add( item );
					continue;
				}
				if ( brimstoneUseful &&
					mods.getRawBitmap( Modifiers.BRIMSTONE ) != 0 )
				{
					automatic[ slot ].add( item );
					continue;
				}
				if ( slimeHateUseful &&
					mods.get( Modifiers.SLIME_HATES_IT ) > 0.0f )
				{
					automatic[ slot ].add( item );
					continue;
				}
				if ( (mods.getRawBitmap( Modifiers.SYNERGETIC ) & usefulSynergies) != 0 )
				{
					automatic[ slot ].add( item );
					continue;
				}
				String intrinsic = mods.getString( Modifiers.INTRINSIC_EFFECT );
				if ( intrinsic.length() > 0 )
				{
					Modifiers newMods = new Modifiers();
					newMods.add( mods );
					newMods.add( Modifiers.getModifiers( intrinsic ) );
					mods = newMods;
				}
				float delta = this.getScore( mods ) - nullScore;
				if ( delta < 0.0f ) continue;
				if ( delta == 0.0f )
				{
					neutral[ slot ].add( item );
					continue;
				}
				if ( mods.getBoolean( Modifiers.NONSTACKABLE_WATCH ) )
				{
					automatic[ slot ].add( item );
					continue;
				}
				ranked[ slot ].add( item );
			}
			
			for ( int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot )
			{
				ArrayList list = ranked[ slot ];
				if ( list.size() == 0 )
				{
					list = neutral[ slot ];
				}
				else if ( list.size() < 3 )
				{
					list.addAll( neutral[ slot ] );
				}
				ListIterator i = list.listIterator();
				while ( i.hasNext() )
				{
					AdventureResult item = (AdventureResult) i.next();
					Spec spec = new Spec();
					spec.attachment = item;
					Arrays.fill( spec.equipment, EquipmentRequest.UNEQUIP );
					spec.equipment[ slot ] = item;
					i.set( spec );					
				}
				Collections.sort( list );
				int len = list.size();
				for ( int j = Math.max( 0, len - 3 ); j < len; ++j )
				{
					automatic[ slot ].add( 0, ((Spec) list.get( j )).attachment );
				}
				if ( this.dump )
				{
					System.out.println( "SLOT " + slot );
					System.out.println( automatic[ slot ].toString() );
				}
			}
			new Spec().tryAll( automatic );
		}
	}
	
	private static class Spec
	implements Comparable, Cloneable
	{
		private int MCD;
		public AdventureResult[] equipment;
		private ArrayList effects;
		private FamiliarData familiar;
		private boolean calculated = false;
		private boolean tiebreakered = false;
		private boolean scored = false;
		private Modifiers mods;
		private float score, tiebreaker;
		private int simplicity;
		
		public boolean failed = false;
		public AdventureResult attachment;
		
		public Spec()
		{
			this.MCD = KoLCharacter.getMindControlLevel();
			this.equipment = EquipmentManager.allEquipment();			
			this.effects = new ArrayList();
			this.effects.addAll( KoLConstants.activeEffects );
			while ( this.effects.size() > 0 )
			{	// Strip out intrinsic effects - those granted by equipment
				// will be added from Intrinsic Effect modifiers.
				// This assumes that no intrinsic that is granted by anything
				// other than equipment has any real effect.
				int pos = this.effects.size() - 1;
				if ( ((AdventureResult) this.effects.get( pos )).getCount() >
					Integer.MAX_VALUE / 2 )
				{
					this.effects.remove( pos );
				}
				else break;
			}
			this.familiar = KoLCharacter.currentFamiliar;
		}
		
		public Object clone()
		{
			try
			{
				Spec copy = (Spec) super.clone();
				copy.equipment = (AdventureResult[]) this.equipment.clone();
				return copy;
			}
			catch ( CloneNotSupportedException e )
			{
				return null;
			}
		}
		
		public void setMindControlLevel( int MCD )
		{
			this.MCD = MCD;
		}
		
		public void equip( int slot, AdventureResult item )
		{
			if ( slot < 0 || slot >= EquipmentManager.ALL_SLOTS ) return;
			this.equipment[ slot ] = item;
			if ( slot == EquipmentManager.WEAPON &&
				EquipmentDatabase.getHands( item.getItemId() ) > 1 )
			{
				this.equipment[ EquipmentManager.OFFHAND ] = EquipmentRequest.UNEQUIP;
			}
		}
		
		public void addEffect( AdventureResult effect )
		{
			this.effects.add( effect );
		}
	
		public void removeEffect( AdventureResult effect )
		{
			this.effects.remove( effect );
		}
		
		public Modifiers calculate()
		{
			this.mods = KoLCharacter.recalculateAdjustments(
				false,
				this.MCD,
				this.equipment,
				this.effects,
				this.familiar,
				true );
			this.calculated = true;
			return this.mods;
		}
	
		public float getScore()
		{
			if ( this.scored ) return this.score;
			if ( !this.calculated ) this.calculate();
			this.score = MaximizerFrame.eval.getScore( this.mods );
			this.failed = MaximizerFrame.eval.failed;
			this.scored = true;
			return this.score;
		}
		
		public float getTiebreaker()
		{
			if ( this.tiebreakered ) return this.tiebreaker;
			if ( !this.calculated ) this.calculate();
			this.tiebreaker = MaximizerFrame.eval.getTiebreaker( this.mods );
			this.tiebreakered = true;
			this.simplicity = 0;
			for ( int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot )
			{	
				AdventureResult item = this.equipment[ slot ];
				if ( item == null ) item = EquipmentRequest.UNEQUIP;
				if ( EquipmentManager.getEquipment( slot ).equals( item ) )
				{
					this.simplicity += 2;
				}
				else if ( item.equals( EquipmentRequest.UNEQUIP ) )
				{
					this.simplicity += 1;
				}
			}
			return this.tiebreaker;
		}
		
		public int compareTo( Object o )
		{
			if ( !(o instanceof Spec) ) return 1;
			Spec other = (Spec) o;
			int rv = Float.compare( this.getScore(), other.getScore() );
			if ( this.failed != other.failed ) return this.failed ? -1 : 1;
			if ( rv != 0 ) return rv;
			rv = Float.compare( this.getTiebreaker(), other.getTiebreaker() );
			if ( rv != 0 ) return rv;
			return this.simplicity - other.simplicity;
		}
		
		// Remember which equipment slots were null, so that this
		// state can be restored later.
		public Object mark()
		{
			return this.equipment.clone();
		}
		
		public void restore( Object mark )
		{
			System.arraycopy( mark, 0, this.equipment, 0, EquipmentManager.ALL_SLOTS );
		}
		
		public void tryAll( ArrayList[] possibles )
		{
			//this.equipment[ EquipmentManager.HAT ] = null;
			//this.equipment[ EquipmentManager.SHIRT ] = null;
			//this.equipment[ EquipmentManager.PANTS ] = null;
			this.equipment[ EquipmentManager.WEAPON ] = null;
			this.equipment[ EquipmentManager.ACCESSORY1 ] = null;
			this.equipment[ EquipmentManager.ACCESSORY2 ] = null;
			this.equipment[ EquipmentManager.ACCESSORY3 ] = null;
			this.tryAccessories( possibles, 0 );
		}
		
		public void tryAccessories( ArrayList[] possibles, int pos )
		{
			Object mark = this.mark();
			int free = 0;
			if ( this.equipment[ EquipmentManager.ACCESSORY1 ] == null ) ++free;
			if ( this.equipment[ EquipmentManager.ACCESSORY2 ] == null ) ++free;
			if ( this.equipment[ EquipmentManager.ACCESSORY3 ] == null ) ++free;
			if ( free > 0 )
			{
				ArrayList possible = possibles[ EquipmentManager.ACCESSORY1 ];
				for ( ; pos < possible.size(); ++pos )
				{
					AdventureResult item = (AdventureResult) possible.get( pos );
					int count = item.getCount();
					if ( item.equals( this.equipment[ EquipmentManager.ACCESSORY1 ] ) )
					{
						--count;
					}
					if ( item.equals( this.equipment[ EquipmentManager.ACCESSORY2 ] ) )
					{
						--count;
					}
					if ( item.equals( this.equipment[ EquipmentManager.ACCESSORY3 ] ) )
					{
						--count;
					}
					if ( count <= 0 ) continue;
					for ( count = Math.min( free, count ); count > 0; --count )
					{
						if ( this.equipment[ EquipmentManager.ACCESSORY1 ] == null )
						{
							this.equipment[ EquipmentManager.ACCESSORY1 ] = item;
						}
						else if ( this.equipment[ EquipmentManager.ACCESSORY2 ] == null )
						{
							this.equipment[ EquipmentManager.ACCESSORY2 ] = item;
						}
						else if ( this.equipment[ EquipmentManager.ACCESSORY3 ] == null )
						{
							this.equipment[ EquipmentManager.ACCESSORY3 ] = item;
						}
						else
						{
							System.out.println( "no room left???" );
							break;	// no room left - shouldn't happen
						}

						this.tryAccessories( possibles, pos + 1 );
					}
					this.restore( mark );
				}
			
				if ( this.equipment[ EquipmentManager.ACCESSORY1 ] == null )
				{
					this.equipment[ EquipmentManager.ACCESSORY1 ] = EquipmentRequest.UNEQUIP;
				}
				if ( this.equipment[ EquipmentManager.ACCESSORY2 ] == null )
				{
					this.equipment[ EquipmentManager.ACCESSORY2 ] = EquipmentRequest.UNEQUIP;
				}
				if ( this.equipment[ EquipmentManager.ACCESSORY3 ] == null )
				{
					this.equipment[ EquipmentManager.ACCESSORY3 ] = EquipmentRequest.UNEQUIP;
				}
			}
			
			this.trySwap( EquipmentManager.ACCESSORY1, EquipmentManager.ACCESSORY2 );
			this.trySwap( EquipmentManager.ACCESSORY2, EquipmentManager.ACCESSORY3 );
			this.trySwap( EquipmentManager.ACCESSORY3, EquipmentManager.ACCESSORY1 );

			//this.tryHats( possibles );
			this.tryWeapons( possibles );
			this.restore( mark );
		}
		
		public void tryHats( ArrayList[] possibles )
		{
			Object mark = this.mark();
			if ( this.equipment[ EquipmentManager.HAT ] == null )
			{
				ArrayList possible = possibles[ EquipmentManager.HAT ];
				for ( int pos = 0; pos < possible.size(); ++pos )
				{
					AdventureResult item = (AdventureResult) possible.get( pos );
					int count = item.getCount();
					if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
					{
						--count;
					}
					if ( count <= 0 ) continue;
					this.equipment[ EquipmentManager.HAT ] = item;
					this.tryShirts( possibles );
					this.restore( mark );
				}
			
				this.equipment[ EquipmentManager.HAT ] = EquipmentRequest.UNEQUIP;
			}
			
			this.tryShirts( possibles );
			this.restore( mark );
		}
		
		public void tryShirts( ArrayList[] possibles )
		{
			Object mark = this.mark();
			if ( this.equipment[ EquipmentManager.SHIRT ] == null &&
				KoLCharacter.hasSkill( "Torso Awaregness" ) )
			{
				ArrayList possible = possibles[ EquipmentManager.SHIRT ];
				for ( int pos = 0; pos < possible.size(); ++pos )
				{
					AdventureResult item = (AdventureResult) possible.get( pos );
					int count = item.getCount();
					//if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
					//{
					//	--count;
					//}
					//if ( count <= 0 ) continue;
					this.equipment[ EquipmentManager.SHIRT ] = item;
					this.tryPants( possibles );
					this.restore( mark );
				}
			
				this.equipment[ EquipmentManager.SHIRT ] = EquipmentRequest.UNEQUIP;
			}
			
			this.tryPants( possibles );
			this.restore( mark );
		}
		
		public void tryPants( ArrayList[] possibles )
		{
			Object mark = this.mark();
			if ( this.equipment[ EquipmentManager.PANTS ] == null )
			{
				ArrayList possible = possibles[ EquipmentManager.PANTS ];
				for ( int pos = 0; pos < possible.size(); ++pos )
				{
					AdventureResult item = (AdventureResult) possible.get( pos );
					int count = item.getCount();
					if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
					{
						--count;
					}
					if ( count <= 0 ) continue;
					this.equipment[ EquipmentManager.PANTS ] = item;
					this.tryWeapons( possibles );
					this.restore( mark );
				}
			
				this.equipment[ EquipmentManager.PANTS ] = EquipmentRequest.UNEQUIP;
			}
			
			this.tryWeapons( possibles );
			this.restore( mark );
		}
		
		public void tryWeapons( ArrayList[] possibles )
		{
			Object mark = this.mark();
			if ( this.equipment[ EquipmentManager.WEAPON ] == null )
			{
				ArrayList possible = possibles[ EquipmentManager.WEAPON ];
				for ( int pos = 0; pos < possible.size(); ++pos )
				{
					AdventureResult item = (AdventureResult) possible.get( pos );
					int count = item.getCount();
					if ( item.equals( this.equipment[ EquipmentManager.OFFHAND ] ) )
					{
						--count;
					}
					if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
					{
						--count;
					}
					if ( count <= 0 ) continue;
					this.equipment[ EquipmentManager.WEAPON ] = item;
					this.tryWeapons( possibles );
					this.restore( mark );
				}
			
				this.equipment[ EquipmentManager.WEAPON ] = EquipmentRequest.UNEQUIP;
			}
			
			// doit
			this.calculated = false;
			this.scored = false;
			this.tiebreakered = false;
			if ( this.compareTo( MaximizerFrame.best ) > 0 )
			{
				MaximizerFrame.best = (Spec) this.clone();
			}
			MaximizerFrame.bestChecked++;
			long t = System.currentTimeMillis();
			if ( t > MaximizerFrame.bestUpdate )
			{
				Spec.showProgress();
				MaximizerFrame.bestUpdate = t + 5000;
			}
			this.restore( mark );
		}
		
		private void trySwap( int slot1, int slot2 )
		{
			AdventureResult item1, item2, eq1, eq2;
			item1 = this.equipment[ slot1 ];
			if ( item1 == null ) item1 = EquipmentRequest.UNEQUIP;
			eq1 = EquipmentManager.getEquipment( slot1 );
			if ( eq1.equals( item1 ) ) return;
			item2 = this.equipment[ slot2 ];
			if ( item2 == null ) item2 = EquipmentRequest.UNEQUIP;
			eq2 = EquipmentManager.getEquipment( slot2 );
			if ( eq2.equals( item2 ) ) return;
			if ( eq1.equals( item2 ) || eq2.equals( item1 ) )
			{
				this.equipment[ slot1 ] = item2;
				this.equipment[ slot2 ] = item1;
			}
		}
		
		public static void showProgress()
		{
			String msg = MaximizerFrame.bestChecked + " combinations checked, best score " + MaximizerFrame.best.getScore();
			if ( MaximizerFrame.best.tiebreakered )
			{
				msg = msg + " / " + MaximizerFrame.best.getTiebreaker() + " / " +
					MaximizerFrame.best.simplicity;
			}
			KoLmafia.updateDisplay( msg );
		}
	}
	
	public static class Boost
	implements Comparable
	{
		private boolean isEquipment, isShrug;
		private String cmd, text;
		private int slot;
		private float boost;
		private AdventureResult item, effect;
		
		private Boost( String cmd, String text, AdventureResult item, float boost )
		{
			this.cmd = cmd;
			this.text = text;
			this.item = item;
			this.boost = boost;
			if ( cmd.length() == 0 )
			{
				this.text = "<html><font color=gray>" +
					text.replaceAll( "&", "&amp;" ) +
					"</font></html>";
			}
		}

		public Boost( String cmd, String text, AdventureResult effect, boolean isShrug, AdventureResult item, float boost )
		{
			this( cmd, text, item, boost );
			this.isEquipment = false;
			this.effect = effect;
			this.isShrug = isShrug;
		}

		public Boost( String cmd, String text, int slot, AdventureResult item, float boost )
		{
			this( cmd, text, item, boost );
			this.isEquipment = true;
			this.slot = slot;
		}
		
		public String toString()
		{
			return this.text;
		}
	
		public int compareTo( Object o )
		{
			if ( !(o instanceof Boost) ) return -1;
			Boost other = (Boost) o;
			
			if ( this.isEquipment != other.isEquipment )
			{
				return this.isEquipment ? -1 : 1;
			}
			if ( this.isEquipment ) return 0;	// preserve order of addition
			int rv = Float.compare( other.boost, this.boost );
			if ( rv == 0 ) rv = other.cmd.compareTo( this.cmd );
			if ( rv == 0 ) rv = other.text.compareTo( this.text );
			return rv;
		}
		
		public boolean execute( boolean equipOnly )
		{
			if ( equipOnly && !this.isEquipment ) return false;
			if ( this.cmd.length() == 0 ) return false;
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( this.cmd );
			return true;
		}
		
		public void addTo( Spec spec )
		{
			if ( this.isEquipment )
			{
				if ( this.slot >= 0 && this.item != null )
				{
					spec.equip( slot, this.item );
				}
			}
			else if ( this.effect != null )
			{
				if ( this.isShrug )
				{
					spec.removeEffect( this.effect );
				}
				else
				{
					spec.addEffect( this.effect );
				}
			}
		}
		
		public AdventureResult getItem()
		{
			if ( this.effect != null ) return this.effect;
			return this.item;
		}
	}

}
