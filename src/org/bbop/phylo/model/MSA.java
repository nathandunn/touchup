/* 
 * 
 * Copyright (c) 2010, Regents of the University of California 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Neither the name of the Lawrence Berkeley National Lab nor the names of its contributors may be used to endorse 
 * or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, 
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.bbop.phylo.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import owltools.gaf.Bioentity;

/**
 * Class declaration
 *
 *
 * @author
 * @version %I%, %G%
 */
public class MSA {
	private static Logger log = Logger.getLogger(MSA.class);

	private final int seq_length;

	private final Map <Bioentity, String> full_sequences;
	private final Map <Bioentity, String> condensed_sequences;
	private final Map <Bioentity, Double> weights;

	private final int    SEGMENTS = 25;

	private char [] condense_ruler;

	/**
	 * Constructor declaration
	 *
	 *
	 * @param sequences
	 * @param seq_length
	 * @param wts
	 *
	 * @see
	 */
	public MSA(Map<Bioentity, String> sequences, int seq_length, Map<Bioentity, Double> wts) {
		this.full_sequences = sequences;
		this.seq_length = seq_length;
		this.weights = wts;
		char[] full_ruler = setRuler(seq_length);
		Set<Bioentity> nodes = full_sequences.keySet();
		condensed_sequences = new HashMap<Bioentity, String>();
		condense_ruler = setCondensedSequences(nodes);
		if (seq_length < SEGMENTS) {
			condense_ruler = "Sequence".toCharArray();
		}
	}

	/**
	 * Saves information about the counts at each position of the sequence
	 */
/*	private double initColumnWeights(boolean weighted, String sequence) {
		*//* this keeps the overall totals for each count of an AA in a column *//*
		AminoAcidStats[] aminoAcidStats = new AminoAcidStats[sequence.length()];

		// Calculate total weight of sequences for all nodes
		double totalWt = 0;
		// This use to start at one, trying with 0 instead, no need to skip first node
		List<Bioentity> contents = Brush.inst().getTree().getTerminusNodes();
		for (int column = 0; column < seq_length; column++){
			AminoAcidStats alignStats = aminoAcidStats[column];
			if (alignStats == null) {
				alignStats = new AminoAcidStats();
				aminoAcidStats[column] = alignStats;
			}
			for (Bioentity node : contents) {
				if (column == 0)
					totalWt += weights.get(node).doubleValue();
				*//*
				 * this is the aligned sequence, with dashes inserted, so all of them are the same length
				 * and so we don't have to worry about which column we are counting
				 *//*
				char aa = sequence.charAt(column);
				double align_frequency = alignStats.getAAFrequency(aa);
				if (weighted) {
					align_frequency += weights.get(node).doubleValue();
				} else {
					align_frequency++;
				}
				alignStats.setAAFrequency(aa, align_frequency);
			}
		}
		return totalWt;
	}

	protected boolean haveWeights() {
		return !weights.isEmpty();
	}

	*/
	private char [] setRuler(int seqMaxLen) {
		char [] ruler;
		if (seqMaxLen < SEGMENTS){
			ruler = "Sequence".toCharArray();
		}
		else {
			ruler = new char [seqMaxLen];
			for (int i = 0; i < seqMaxLen; i++){
				if (0 == (i + 1) % SEGMENTS){
					String  s = Integer.toString(i + 1);
					for (int j = 0; j < s.length(); j++) {
						ruler[i - s.length() + j] = s.charAt(j);
					}
					ruler[i] = '|';
				}
				else {
					int SUB_SEGMENTS = 5;
					if (0 == (i + 1) % SUB_SEGMENTS) {
						ruler[i] = '\'';
					}
					else {
						ruler[i] = ' ';
					}
				}
			}
		}
		return ruler;
	}

	/**
	 * Method declaration
	 *
	 * @param nodes
	 *
	 * @return
	 *
	 * @see
	 */
	private char [] setCondensedSequences(Set<Bioentity> nodes) {
		int gap_size = 0;
		boolean column_needed;
		StringBuffer ruler = new StringBuffer();
		int hmm_length = 0;
		
		/* 
		 * Working through the primary sequence one column (amino acid) at a time
		 */
		for (int seq_position = 0; seq_position < seq_length; seq_position++) {
			column_needed = false;
			/*
			 * Find out whether there is any amino acid of significance at this column position
			 * among all the rows 
			 */
			for (Iterator<Bioentity> i = nodes.iterator(); i.hasNext();) {
				Bioentity node = i.next();
				String sequence = full_sequences.get(node);
				if (sequence != null) {
					char  c = sequence.charAt(seq_position);
					if (((c >= 'A') && (c <= 'Z')) || (c == '-')) {
						column_needed = true;
					}
				}
			}
			if (column_needed) {
				gap_size = 0;
			} else {
				gap_size++;
			}

			/*
			 * If just reentering good stuff then need to put the starting position in the ruler
			 */
			if (gap_size < 6 ) {
				if (0 == (seq_position + 1) % 10){
					String  s = Integer.toString(seq_position + 1);
					int pos = ruler.length() - s.length();
					ruler.replace(pos, pos + s.length(), s);
					ruler.append('|');
				}
				else if (0 == (seq_position + 1) % 5) {
					ruler.append('\'');
				}
				else {
					ruler.append(' ');
				}
			} else {
				if (ruler.charAt(ruler.length() - 1) != '~') {
					/*
					 * If just leaving good stuff then need to put the ending position in the ruler
					 */
					int pos = ruler.length() - 5;
					boolean digit = true;
					int end_gap = ruler.lastIndexOf("~");
					if (end_gap > 0) {
						int start_gap = end_gap - 1;
						while (start_gap > 0 && ruler.charAt(start_gap) == '~')
							start_gap--;
						if (ruler.charAt(start_gap) != '~')
							start_gap++;
						if (end_gap - start_gap < 4 && (pos - 5) < end_gap)
							ruler.replace(start_gap,  start_gap+5, "~~~~~");
					}
					for (int i = pos - 1; i >= 0 && digit; i--) {
						digit = Character.isDigit(ruler.charAt(i));
						if (digit) {
							ruler.setCharAt(i, ' ');
						}
					}
					ruler.replace(pos,  pos+5, "~~~~~");
				}
			}

			/* 
			 * Now go through every row (gene) and see what they have at this position
			 */
			for (Bioentity node : nodes) {
				switch (gap_size) {
				case 0:
				case 1:
				case 2:
				case 3:
				case 4:
				case 5:
					String sequence = full_sequences.get(node);
					char  c = sequence.charAt(seq_position);
					if (condensed_sequences.get(node) == null) {
						condensed_sequences.put(node, String.valueOf(c));
					} else {
						String prior = condensed_sequences.get(node);
						condensed_sequences.put(node, prior + c);
					}
					break;
				default:
					// assuming the gap length never goes beyond 10K
					String condensed = condensed_sequences.get(node);
					//					if (condensed.charAt(ruler.length() - 1) != '.') {
					/*
					 * If just leaving good stuff then need to put the ending position in the ruler
					 */
					int pos = ruler.length() - 5;
					condensed = condensed.substring(0, pos) + ".....";
					condensed_sequences.put(node, condensed);
					hmm_length = condensed.length();
				}
			}
		}
		condense_ruler = new char [ruler.length()];
		for (int i = 0; i < ruler.length(); i++)
			condense_ruler[i] = ruler.charAt(i);
		return condense_ruler;
	}

}