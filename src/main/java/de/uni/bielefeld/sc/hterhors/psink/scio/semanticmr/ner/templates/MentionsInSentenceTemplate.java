package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.exce.DocumentLinkedAnnotationMismatchException;

/**
 * checks if two mentions belong to the same sentence
 */
public class MentionsInSentenceTemplate extends AbstractFeatureTemplate<MentionsInSentenceTemplate.MentionsInSentenceScope> {

    String punctuations = ".:!?";

	static class MentionsInSentenceScope
			extends AbstractFactorScope {


        public DocumentToken tokenOne;
        public DocumentToken tokenTwo;

        public EntityType typeOne;
        public EntityType typeTwo;



		public MentionsInSentenceScope(
                AbstractFeatureTemplate<MentionsInSentenceScope> template, DocumentToken tokenOne,
                DocumentToken tokenTwo, EntityType typeOne, EntityType typeTwo) {
			super(template);
			this.tokenOne = tokenOne;
			this.tokenTwo = tokenTwo;
			this.typeOne = typeOne;
			this.typeTwo = typeTwo;
		}

		@Override
		public int implementHashCode() {
			return 0;
		}

		@Override
		public boolean implementEquals(Object obj) {
			return false;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			MentionsInSentenceScope wbfScope = (MentionsInSentenceScope) o;
			return tokenOne == wbfScope.tokenOne &&
					tokenTwo == wbfScope.tokenTwo &&
					Objects.equals(typeOne, wbfScope.typeOne) &&
					Objects.equals(typeTwo, wbfScope.typeTwo);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), tokenOne, tokenTwo, typeOne, typeTwo);
		}

	}

	@Override
	public List<MentionsInSentenceScope> generateFactorScopes(State state) {
		List<MentionsInSentenceScope> factors = new ArrayList<>();

            Document document = state.getInstance().getDocument();

            for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
                for (DocumentLinkedAnnotation annotation2 : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)){
                    if(!annotation.equals(annotation2)) {
                        try {
                            factors.add(new MentionsInSentenceScope(this,
                                    document.getTokenByCharStartOffset(annotation.documentPosition.docCharOffset),
                                    document.getTokenByCharStartOffset(annotation2.documentPosition.docCharOffset),
                                    annotation.entityType, annotation2.entityType));
                        } catch (DocumentLinkedAnnotationMismatchException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<MentionsInSentenceScope> factor) {
        if(factor.getFactorScope().tokenOne.getSentenceIndex() ==
                factor.getFactorScope().tokenTwo.getSentenceIndex()){

            factor.getFeatureVector().set("In Same Sentence: " + factor.getFactorScope().typeOne.name +
                    ", " + factor.getFactorScope().typeTwo.name, true);
        }

	}

	public int getNextStopWord(DocumentToken token, Document document){
	    int startingPoint = token.getDocCharOffset();
	    int stoppinPoint;
	    String subText = document.getContent(token, document.tokenList.get(document.tokenList.size()-1));
	    for(int i = 0; i < subText.length(); i++){
	        if(isSentenceEnding(subText.charAt(i))){
                stoppinPoint = i;
                return startingPoint + stoppinPoint;
            }
        }
	    return 0;
    }

    private boolean isSentenceEnding(char charAt) {
        return punctuations.contains(Character.toString(charAt));
    }

}
