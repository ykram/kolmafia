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

package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;

import java.util.Iterator;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

public class ForEachLoop
	extends Loop
{
	private final VariableReferenceList variableReferences;
	private final Value aggregate;

	public ForEachLoop( final Scope scope, final VariableReferenceList variableReferences,
		final Value aggregate )
	{
		super( scope );
		this.variableReferences = variableReferences;
		this.aggregate = aggregate;
	}

	public VariableReferenceList getVariableReferences()
	{
		return this.variableReferences;
	}

	public Iterator getReferences()
	{
		return this.variableReferences.iterator();
	}

	public Value getAggregate()
	{
		return this.aggregate;
	}

	public Value execute( final Interpreter interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( Interpreter.STATE_EXIT );
			return null;
		}

		interpreter.traceIndent();
		interpreter.trace( this.toString() );

		// Evaluate the aggref to get the slice
		AggregateValue slice = (AggregateValue) this.aggregate.execute( interpreter );
		interpreter.captureValue( slice );
		if ( interpreter.getState() == Interpreter.STATE_EXIT )
		{
			interpreter.traceUnindent();
			return null;
		}

		// Iterate over the slice with bound keyvar

		Iterator it = this.getReferences();
		return this.executeSlice( interpreter, slice, it, (VariableReference) it.next() );
	}

	private Value executeSlice( final Interpreter interpreter, final AggregateValue slice, final Iterator it,
		final VariableReference variable )
	{
		// Get an array of keys for the slice
		Value[] keys = slice.keys();

		// Get the next key variable
		VariableReference nextVariable = it.hasNext() ? (VariableReference) it.next() : null;

		// While there are further keys
		for ( int i = 0; i < keys.length; ++i )
		{
			// Get current key
			Value key = keys[ i ];

			// Bind variable to key
			variable.setValue( interpreter, key );

			interpreter.trace( "Key #" + i + ": " + key );

			// If there are more indices to bind, recurse
			Value result;
			if ( nextVariable != null )
			{
				Value nextSlice = slice.aref( key, interpreter );
				if ( nextSlice instanceof AggregateValue )
				{
					interpreter.traceIndent();
					result = this.executeSlice( interpreter, (AggregateValue) nextSlice, it, nextVariable );
				}
				else	// value var instead of key var
				{
					nextVariable.setValue( interpreter, nextSlice );
					result = super.execute( interpreter );
				}
			}
			else
			{
				// Otherwise, execute scope
				result = super.execute( interpreter );
			}

			if ( interpreter.getState() == Interpreter.STATE_NORMAL )
			{
				continue;
			}

			if ( interpreter.getState() == Interpreter.STATE_BREAK )
			{
				interpreter.setState( Interpreter.STATE_NORMAL );
			}

			interpreter.traceUnindent();
			return result;
		}

		interpreter.traceUnindent();
		return DataTypes.VOID_VALUE;
	}

	public String toString()
	{
		return "foreach";
	}

	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<FOREACH>" );

		Iterator it = this.getReferences();
		while ( it.hasNext() )
		{
			VariableReference current = (VariableReference) it.next();
			current.print( stream, indent + 1 );
		}

		this.getAggregate().print( stream, indent + 1 );
		this.getScope().print( stream, indent + 1 );
	}
}
