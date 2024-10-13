package csw.korea.festival.main.config.lucene;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.cjk.CJKWidthCharFilterFactory;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.ko.KoreanNumberFilterFactory;
import org.apache.lucene.analysis.ko.KoreanPartOfSpeechStopFilterFactory;
import org.apache.lucene.analysis.ko.KoreanReadingFormFilterFactory;
import org.apache.lucene.analysis.ko.KoreanTokenizerFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class HibernateSearchConfig {

    @Bean
    public LuceneAnalysisConfigurer luceneAnalysisConfigurer() {
        return context -> {

            // English Analyzer
            context.analyzer("english").custom()
                    .tokenizer(StandardTokenizerFactory.class)
                    .charFilter(HTMLStripCharFilterFactory.class)
                    .tokenFilter(LowerCaseFilterFactory.class)
                    .tokenFilter(EnglishPossessiveFilterFactory.class)
                    .tokenFilter(StopFilterFactory.class)
                    // let it use the default EnglishAnalyzer stopwords
                    .param("ignoreCase", "true")
                    .tokenFilter(PorterStemFilterFactory.class);

            // Korean Analyzer
            context.analyzer("korean").custom()
                    .tokenizer(KoreanTokenizerFactory.class)
                    .charFilter(HTMLStripCharFilterFactory.class)
                    .charFilter(CJKWidthCharFilterFactory.class)
                    .tokenFilter(KoreanReadingFormFilterFactory.class)
                    .tokenFilter(KoreanPartOfSpeechStopFilterFactory.class)
                    // FIX ME: If adverbs are not meaningful for your searches, consider adding MAG and MAJ to the list.
                    .param("tags", "E,EP,EF,EC,ETN,ETM,IC,J,MM,SP,SSC,SSO,SC,SE,XPN,SF,SY,XSA,UNKNOWN")
                    .tokenFilter(KoreanNumberFilterFactory.class);

            // Multi-lingual Analyzer
            context.analyzer("multilingual").custom()
                    .tokenizer(StandardTokenizerFactory.class)
                    .charFilter(HTMLStripCharFilterFactory.class)
                    .tokenFilter(LowerCaseFilterFactory.class);

            try {
                context.analyzer("seok").instance(new CustomKoreanAnalyzer());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            context.analyzer("keyword").instance(new KeywordAnalyzer());

        };
    }
}

/* Korean.POS
E: Verbal endings.
EP: Pre-final endings.
EF: Final endings.
EC: Conjunctive endings.
ETN: Noun derivation endings.
ETM: Adnominal endings (modifier endings).
IC: Interjections.
J: Postpositions (particles).
MAG: General adverbs.
MAJ: Conjunctive adverbs.
MM: Determiners.
NNG: General nouns.
NNP: Proper nouns.
NNB: Dependent nouns.
NP: Pronouns.
NR: Numerals.
SC: Punctuation (comma, semicolon).
SE: Ellipsis (...).
SF: Terminal punctuation (., !, ?).
SH: Chinese characters.
SL: Foreign words.
SN: Numbers.
SP: Space.
SSC: Closing brackets.
SSO: Opening brackets.
SY: Other symbols.
VA: Adjectives.
VCN: Negative copula (not be).
VCP: Positive copula (be).
VV: Verbs.
VX: Auxiliary verbs/adjectives.
XPN: Prefixes.
XR: Root words.
XSA: Adjective suffixes.
XSN: Noun suffixes.
XSV: Verb suffixes.
UNKNOWN: Unknown POS.

Endings and Suffixes: E, EP, EF, EC, ETN, ETM, XPN, XSA, XSN, XSV
Particles and Postpositions: J
Interjections: IC
Determiners: MM
Punctuation and Symbols: SP, SSC, SSO, SC, SE, SF, SY
*/