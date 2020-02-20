package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.groupname.templates;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.State;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * checks for all the words between two mentions
 */
public class WordsInBetweenGroupNamesTemplate extends AbstractFeatureTemplate<WordsInBetweenGroupNamesTemplate.AreWordsInBetweenScope> {

    private static final int MAX_NUMBER_OF_WORDS = 6;  //6 words seems to be the most successful

    static class AreWordsInBetweenScope
            extends AbstractFactorScope {

        DocumentLinkedAnnotation annoOne;
        DocumentLinkedAnnotation annoTwo;

        public Document document;

        public AreWordsInBetweenScope(
                AbstractFeatureTemplate<AreWordsInBetweenScope> template, DocumentLinkedAnnotation annoOne, DocumentLinkedAnnotation annoTwo,
                Document document) {
            super(template);

            this.annoOne = annoOne;
            this.annoTwo = annoTwo;

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
            return Objects.equals(annoOne, that.annoOne) &&
                    Objects.equals(annoTwo, that.annoTwo) &&
                    Objects.equals(document, that.document);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), annoOne, annoTwo, document);
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
                                annotation, annotation2,
                                document));
                }
            }
        }
        return factors;
    }

    @Override
    public void generateFeatureVector(Factor<AreWordsInBetweenScope> factor) {

        int indexTokenOne = factor.getFactorScope().annoOne.relatedTokens.get(factor.getFactorScope().
                annoOne.relatedTokens.size() - 1).getDocTokenIndex();
        int indexTokenTwo = factor.getFactorScope().annoTwo.relatedTokens.get(factor.getFactorScope().
                annoTwo.relatedTokens.size() - 1).getDocTokenIndex();

        //get all words between the mentions


        StringBuilder sbOne = new StringBuilder();
        factor.getFactorScope().annoOne.relatedTokens.forEach(p -> sbOne.append(p.getText() + " "));

        StringBuilder sbTwo = new StringBuilder();
        factor.getFactorScope().annoTwo.relatedTokens.forEach(p -> sbTwo.append(p.getText() + " "));


        if (indexTokenTwo - indexTokenOne == 1) {
            factor.getFeatureVector().set("NO WORDS BETWEEN: <\"" + sbOne.toString() + "\", \"" +
                    sbTwo.toString() + "\"> ", true);
        } else if (indexTokenTwo - indexTokenOne < MAX_NUMBER_OF_WORDS) {

            String subtext = factor.getFactorScope().document.getContent(
                    factor.getFactorScope().document.tokenList.get(indexTokenOne + 1),
                    factor.getFactorScope().document.tokenList.get(indexTokenTwo - 1)
            );

            factor.getFeatureVector().set("WORDS BETWEEN: <\"" + sbOne.toString() + "\", \""
                    + sbTwo.toString() + "\">: "
                    + subtext, true);
        }
    }
}