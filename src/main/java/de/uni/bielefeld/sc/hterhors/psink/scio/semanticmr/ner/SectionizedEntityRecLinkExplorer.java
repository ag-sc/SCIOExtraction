package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.exploration.constraints.HardConstraintsProvider;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;

/**
 * @author hterhors
 *
 */
public class SectionizedEntityRecLinkExplorer implements IExplorationStrategy {
	private static Logger log = LogManager.getFormatterLogger(SectionizedEntityRecLinkExplorer.class);

	final private HardConstraintsProvider hardConstraintsProvider;

//	public EntityRecLinkExplorer(HardConstraintsProvider hardConstraintsProvder) {
//		this.hardConstraintsProvider = hardConstraintsProvder;
//	}
	private final Map<CacheKey, Set<AbstractAnnotation>> cache = new HashMap<>();

	public SectionizedEntityRecLinkExplorer() {
		this.hardConstraintsProvider = null;
	}

	/**
	 * Average number of new explored proposal states. This variable is used as
	 * initial size of the next new proposal state list.
	 */
	private int averageNumberOfNewProposalStates = 16;

	public static int MAX_WINDOW_SIZE = 10;
	public static int MIN_WINDOW_SIZE = 1;

	@Override
	public List<State> explore(State currentState) {

		final List<State> proposalStates = new ArrayList<>(averageNumberOfNewProposalStates);

		addNewAnnotation(proposalStates, currentState);
		removeAnnotation(proposalStates, currentState);

		updateAverage(proposalStates);

		return proposalStates;

	}

	private void removeAnnotation(List<State> proposalStates, State currentState) {

		for (int annotationIndex = 0; annotationIndex < currentState.getCurrentPredictions().getAnnotations()
				.size(); annotationIndex++) {
			proposalStates.add(currentState.deepRemoveCopy(annotationIndex));
		}

	}

	private int sentenceIndex = -1;

	private void addNewAnnotation(final List<State> proposalStates, State currentState) {

//		AutomatedSectionifcation sectionifcation = AutomatedSectionifcation.getInstance(currentState.getInstance());
//		if (sectionifcation.getSection(sentenceIndex) != ESection.RESULTS)
//			return;

		final List<DocumentToken> tokens = currentState.getInstance().getDocument().getSentenceByIndex(sentenceIndex);

		for (int windowSize = MIN_WINDOW_SIZE; windowSize <= MAX_WINDOW_SIZE; windowSize++) {

			for (int runIndex = 0; runIndex < tokens.size() - windowSize; runIndex++) {

				final DocumentToken fromToken = tokens.get(runIndex); // including
				final DocumentToken toToken = tokens.get(runIndex + windowSize - 1); // including

//				if (sectionifcation.getSection(fromToken.getSentenceIndex()) != ESection.RESULTS)
//					continue;

				/*
				 * Check some basic constraints.
				 */

				if (fromToken.isStopWord())
					continue;
				if (fromToken.isPunctuation())
					continue;

				/*
				 * TODO: Might check tokens in between.
				 */

				if (toToken.isStopWord())
					continue;

				if (toToken.isPunctuation())
					continue;

				if (fromToken.getSentenceIndex() != toToken.getSentenceIndex())
					continue;

				if (fromToken == toToken && currentState.containsAnnotationOnTokens(fromToken))
					continue;
				else if (currentState.containsAnnotationOnTokens(fromToken, toToken))
					continue;
//
				final CacheKey key = new CacheKey(currentState.getInstance(), fromToken.getDocTokenIndex(),
						toToken.getDocTokenIndex());
				Set<AbstractAnnotation> annotations;
				if ((annotations = cache.get(key)) == null) {
					annotations = new HashSet<>();
					final String text = currentState.getInstance().getDocument().getContent(fromToken, toToken);

					if (text.length() == 1)
						continue;

					for (EntityType entityType : currentState.getInstance().getEntityTypeCandidates(text)) {

						try {
							AbstractAnnotation newCurrentPrediction = AnnotationBuilder.toAnnotation(
									currentState.getInstance().getDocument(), entityType, text,
									fromToken.getDocCharOffset());
							annotations.add(newCurrentPrediction);
							proposalStates.add(currentState.deepAddCopy(newCurrentPrediction));
						} catch (RuntimeException e) {
							e.printStackTrace();
						}

					}
					cache.put(key, annotations);
				} else {
					for (AbstractAnnotation newCurrentPrediction : annotations) {
						proposalStates.add(currentState.deepAddCopy(newCurrentPrediction));
					}
				}
			}
		}
	}

	static class CacheKey {
		final Instance instance;
		final int fromTokenOffset;
		final int toTokenOffset;

		public CacheKey(Instance instance, int fromTokenOffset, int toTokenOffset) {
			this.instance = instance;
			this.fromTokenOffset = fromTokenOffset;
			this.toTokenOffset = toTokenOffset;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + fromTokenOffset;
			result = prime * result + ((instance == null) ? 0 : instance.hashCode());
			result = prime * result + toTokenOffset;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CacheKey other = (CacheKey) obj;
			if (fromTokenOffset != other.fromTokenOffset)
				return false;
			if (instance == null) {
				if (other.instance != null)
					return false;
			} else if (!instance.equals(other.instance))
				return false;
			if (toTokenOffset != other.toTokenOffset)
				return false;
			return true;
		}

	}

	private void updateAverage(final List<State> proposalStates) {
		averageNumberOfNewProposalStates += proposalStates.size();
		averageNumberOfNewProposalStates /= 2;
	}

	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public State next() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void set(int sentenceIndex) {
		this.sentenceIndex = sentenceIndex;
	}

}
