package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.State;

/**
 * Checks for Similar Words in tow Mentions.
 */
public class SimilarWordsTemplate extends AbstractFeatureTemplate<SimilarWordsTemplate.OverlappingScope> {


    static class OverlappingScope
            extends AbstractFactorScope {

        List<DocumentToken> firstTokens;
        List<DocumentToken> secondTokens;
        EntityType typeOne;
        EntityType typeTwo;

        public OverlappingScope(AbstractFeatureTemplate template, List<DocumentToken> firstTokens, List<DocumentToken> secondTokens, EntityType typeOne, EntityType typeTwo) {
            super(template);
            this.firstTokens = firstTokens;
            this.secondTokens = secondTokens;
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
            OverlappingScope that = (OverlappingScope) o;
            return Objects.equals(firstTokens, that.firstTokens) &&
                    Objects.equals(secondTokens, that.secondTokens) &&
                    Objects.equals(typeOne, that.typeOne) &&
                    Objects.equals(typeTwo, that.typeTwo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), firstTokens, secondTokens, typeOne, typeTwo);
        }
    }

    @Override
    public List<OverlappingScope> generateFactorScopes(State state) {
        List<OverlappingScope> factors = new ArrayList<>();

        for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
            for (DocumentLinkedAnnotation annotation2 : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
                if (!annotation.equals(annotation2)) {
                    List<DocumentToken> tokenListOne = annotation.relatedTokens;
                    List<DocumentToken> tokenListTwo = annotation2.relatedTokens;

                    factors.add(new OverlappingScope(this, tokenListOne, tokenListTwo, annotation.getEntityType(), annotation2.getEntityType()));
                }

            }
        }
        return factors;
    }

    @Override
    public void generateFeatureVector(Factor<OverlappingScope> factor) {
        List<DocumentToken> tokenListOne = factor.getFactorScope().firstTokens;
        List<DocumentToken> tokenListTwo = factor.getFactorScope().secondTokens;

        factor.getFeatureVector().set("Number of Overlapping Words for <"+ factor.getFactorScope().typeOne.name +
                ", " + factor.getFactorScope().typeTwo.name + ">: "+ getOverlapping(tokenListOne, tokenListTwo).getDegree(), true);

        factor.getFeatureVector().set("Overlapping Words for <"+ factor.getFactorScope().typeOne.name +
                ", " + factor.getFactorScope().typeTwo.name + ">: "+ getOverlapping(tokenListOne, tokenListTwo).getTokenList(), true);

    }

    private OverLappingContainer getOverlapping(List<DocumentToken> tokenListOne, List<DocumentToken> tokenListTwo) {
        int degree = 0;
        String overlappingTokens = "";
        for(DocumentToken tokenOne : tokenListOne){
           for(DocumentToken tokenTwo : tokenListTwo) {
               if(tokenOne.getText().trim().equals(tokenTwo.getText().trim())){
                   degree++;
                   overlappingTokens = overlappingTokens + tokenOne.getText() + " ";
               }
           }
        }
        return new OverLappingContainer(degree, overlappingTokens);
    }

    private class OverLappingContainer{
        int degree;
        String tokenList;

        public OverLappingContainer(int degree, String tokenList) {
            this.degree = degree;
            this.tokenList = tokenList;
        }

        public int getDegree() {
            return degree;
        }

        public String getTokenList() {
            return tokenList;
        }
    }
}



