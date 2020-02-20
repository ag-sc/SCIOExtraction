package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates;

import java.util.ArrayList;
import java.util.Arrays;
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

/**
 * checks for all the words between two mentions
 */
public class WordsInBetweenTemplate extends AbstractFeatureTemplate<WordsInBetweenTemplate.AreWordsInBetweenScope> {

    private static final int MAX_NUMBER_OF_WORDS = 6;  //6 words seems to be the most successful

    static class AreWordsInBetweenScope
            extends AbstractFactorScope {

        public String wordAfter;
        public DocumentToken tokenOne;
        public DocumentToken tokenTwo;

        public EntityType typeOne;
        public EntityType typeTwo;

        public Document document;

        public AreWordsInBetweenScope(
                AbstractFeatureTemplate<AreWordsInBetweenScope> template, DocumentToken tokenOne, DocumentToken tokenTwo,
                EntityType typeOne, EntityType typeTwo, Document document) {
            super(template);
            this.tokenOne = tokenOne;
            this.tokenTwo = tokenTwo;
            this.typeOne = typeOne;
            this.typeTwo = typeTwo;
            this.document = document;
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
            AreWordsInBetweenScope that = (AreWordsInBetweenScope) o;
            return Objects.equals(wordAfter, that.wordAfter) &&
                    Objects.equals(tokenOne, that.tokenOne) &&
                    Objects.equals(tokenTwo, that.tokenTwo) &&
                    Objects.equals(typeOne, that.typeOne) &&
                    Objects.equals(typeTwo, that.typeTwo) &&
                    Objects.equals(document, that.document);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), wordAfter, tokenOne, tokenTwo, typeOne, typeTwo, document);
        }
    }

    @Override
    public List<AreWordsInBetweenScope> generateFactorScopes(State state) {
        List<AreWordsInBetweenScope> factors = new ArrayList<>();
        Document document = state.getInstance().getDocument();

        for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
            for (DocumentLinkedAnnotation annotation2 : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
                if (!annotation.equals(annotation2)) {
                    DocumentToken firstToken = annotation.relatedTokens.get(annotation.relatedTokens.size() - 1);
                    DocumentToken secondToken = annotation2.relatedTokens.get(0);
                    if (firstToken.getDocTokenIndex() < secondToken.getDocTokenIndex())
                        factors.add(new AreWordsInBetweenScope(this,
                                firstToken, secondToken,
                                annotation.entityType, annotation2.entityType,
                                document));
                }
            }
        }
        return factors;
    }

    @Override
    public void generateFeatureVector(Factor<AreWordsInBetweenScope> factor) {

        String subtext;

        int indexTokenOne = factor.getFactorScope().tokenOne.getDocTokenIndex();
        int indexTokenTwo = factor.getFactorScope().tokenTwo.getDocTokenIndex();

        //get all words between the mentions

        if (indexTokenTwo - indexTokenOne == 1) {
            factor.getFeatureVector().set("NO WORDS BETWEEN: <" + factor.getFactorScope().typeOne.name + ", " +
                    factor.getFactorScope().typeTwo.name + "> ", true);
        } else if (indexTokenTwo - indexTokenOne < MAX_NUMBER_OF_WORDS) {
            subtext = factor.getFactorScope().document.getContent(
                    factor.getFactorScope().document.tokenList.get(indexTokenOne + 1),
                    factor.getFactorScope().document.tokenList.get(indexTokenTwo - 1)
            );

            factor.getFeatureVector().set("WORDS BETWEEN: <" + factor.getFactorScope().typeOne.name + ", "
                    + factor.getFactorScope().typeTwo.name + ">: "
                    + subtext, true);
        }
    }

    public int numberOfWords(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }

        String[] words = input.split("\\s+");
        return words.length;
    }

    public String[] tokenizeAndReduceString(String text) {
        String[] tmpArray = text.split("\\s+");
        if (tmpArray.length >= 2) {
            return Arrays.copyOfRange(tmpArray, 1, tmpArray.length - 1);
        }
        return new String[]{};
    }

    public String[] tokenizeString(String text) {
        return text.split("\\s+");
    }
}