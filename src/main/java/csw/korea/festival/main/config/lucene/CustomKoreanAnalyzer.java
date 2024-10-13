package csw.korea.festival.main.config.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.FlattenGraphFilter;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.CharsRef;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * CustomKoreanAnalyzer는 한국어 텍스트를 분석하기 위한 맞춤형 Analyzer입니다.
 * 이 분석기는 동의어 처리를 포함하여 한국어 텍스트의 토큰화를 수행하며,
 * 검색의 정확성과 효율성을 높이기 위해 다양한 필터를 적용합니다.
 * </p>
 *
 * <p>
 * 주요 기능:
 * <ul>
 *   <li><b>동의어 처리:</b> 지정된 동의어 맵을 사용하여 텍스트의 동의어를 처리합니다.</li>
 *   <li><b>토큰화:</b> 단순 한국어 토크나이저(SimpleKoreanTokenizer)를 사용하여 텍스트를 토큰으로 분리합니다.</li>
 *   <li><b>소문자 변환:</b> 모든 토큰을 소문자로 변환하여 대소문자 구분 없이 검색할 수 있게 합니다.</li>
 *   <li><b>토큰 길이 필터:</b> 길이가 2 이상인 토큰만 유지하여 단일 문자 토큰을 무시합니다.</li>
 *   <li><b>그래프 평탄화:</b> 동의어 처리 후의 그래프를 평탄화하여 인덱싱을 용이하게 합니다.</li>
 * </ul>
 * </p>
 *
 * <p>
 * 이 분석기는 주로 Lucene 기반의 검색 엔진이나 Elasticsearch에서 사용되며,
 * 한국어 텍스트의 효율적인 인덱싱과 검색을 지원합니다.
 * </p>
 *
 * @author SeokWon Choi
 * @version 1.0
 */
public class CustomKoreanAnalyzer extends Analyzer {

    private final SynonymMap synonymMap;

    public CustomKoreanAnalyzer() throws IOException {
        synonymMap = buildSynonymMap();
    }

    /**
     * Analyzer의 추상 메서드를 구현하여 토큰 스트림 컴포넌트를 생성합니다.
     *
     * @param fieldName 분석할 필드의 이름
     * @return 토큰 스트림 컴포넌트
     */
    @Override
    protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
        // 단순 한국어 토크나이저를 사용하여 토큰화
        Tokenizer source = new SimpleKoreanTokenizer();
        // 모든 토큰을 소문자로 변환
        TokenStream filter = new LowerCaseFilter(source);
        // 길이가 2 이상인 토큰만 유지 (단일 문자 토큰 무시)
        filter = new LengthFilter(filter, 2, Integer.MAX_VALUE);
        // 동의어 그래프 필터 적용
        filter = new SynonymGraphFilter(filter, synonymMap, true);
        // 인덱싱을 위해 그래프 평탄화
        filter = new FlattenGraphFilter(filter);
        return new Analyzer.TokenStreamComponents(source, filter);
    }

    /**
     * 동의어 맵을 빌드하는 메서드입니다.
     * 지역명에 대한 다양한 표현을 동의어로 정의하여 검색의 유연성을 높입니다.
     *
     * @return 빌드된 SynonymMap
     * @throws IOException 동의어 맵 빌드 중 발생할 수 있는 I/O 예외
     */
    private SynonymMap buildSynonymMap() throws IOException {
        SynonymMap.Builder builder = new SynonymMap.Builder(true);

        // 지역명 동의어 맵 정의
        Map<String, String> provinceMap = new HashMap<>();
        provinceMap.put("경기", "경기도");
        provinceMap.put("경기도", "경기도");
        provinceMap.put("ㄱㄱㄷ", "경기도");
        provinceMap.put("서울", "서울특별시");
        provinceMap.put("서울특별시", "서울특별시");
        provinceMap.put("ㅅㅇㅌㅂㅅ", "서울특별시");
        provinceMap.put("부산", "부산광역시");
        provinceMap.put("ㅄ", "부산광역시");
        provinceMap.put("ㅂㅅ", "부산광역시");
        provinceMap.put("ㅂㅅㄱㅇㅅ", "부산광역시");
        provinceMap.put("부산광역시", "부산광역시");
        provinceMap.put("대구", "대구광역시");
        provinceMap.put("대구광역시", "대구광역시");
        provinceMap.put("ㄷㄱㄱㅇㅅ", "대구광역시");
        provinceMap.put("인천", "인천광역시");
        provinceMap.put("인천광역시", "인천광역시");
        provinceMap.put("제주", "제주특별자치도");
        provinceMap.put("제주특별자치도", "제주특별자치도");
        provinceMap.put("ㅈㅈㄷ", "제주특별자치도");
        provinceMap.put("제주도", "제주특별자치도");
        provinceMap.put("대전", "대전광역시");
        provinceMap.put("대전광역시", "대전광역시");
        provinceMap.put("울산", "울산광역시");
        provinceMap.put("울산광역시", "울산광역시");
        provinceMap.put("광주", "광주광역시");
        provinceMap.put("광주광역시", "광주광역시");
        provinceMap.put("세종", "세종특별자치시");
        provinceMap.put("세종특별자치시", "세종특별자치시");
        provinceMap.put("ㅅㅈㅅ", "세종특별자치시");
        provinceMap.put("강원", "강원특별자치도");
        provinceMap.put("강원도", "강원특별자치도");
        provinceMap.put("강원특별자치도", "강원특별자치도");
        provinceMap.put("ㄱㅇㄷ", "강원특별자치도");
        provinceMap.put("경남", "경상남도");
        provinceMap.put("경상남도", "경상남도");
        provinceMap.put("경북", "경상북도");
        provinceMap.put("경상북도", "경상북도");
        provinceMap.put("전북", "전라북도");
        provinceMap.put("전북특별자치도", "전라북도");
        provinceMap.put("충남", "충청남도");
        provinceMap.put("충청남도", "충청남도");
        provinceMap.put("충북", "충청북도");
        provinceMap.put("충청북도", "충청북도");
        provinceMap.put("전남", "전라남도");
        provinceMap.put("전라남도", "전라남도");

        for (Map.Entry<String, String> entry : provinceMap.entrySet()) {
            builder.add(new CharsRef(entry.getKey()), new CharsRef(entry.getValue()), true);
        }

        return builder.build();
    }

    /**
     * SimpleKoreanTokenizer는 입력된 텍스트를 단순히 공백을 기준으로 토큰화하는 커스텀 토크나이저입니다.
     * 실제 한국어 토크나이저는 복잡한 형태소 분석을 필요로 하지만, 여기서는 간단한 예시로 구현되었습니다.
     */
    public static final class SimpleKoreanTokenizer extends Tokenizer {
        private final CharTermAttribute charTermAttribute = addAttribute(CharTermAttribute.class);
        private final OffsetAttribute offsetAttribute = addAttribute(OffsetAttribute.class);
        private final PositionIncrementAttribute positionIncrementAttribute = addAttribute(PositionIncrementAttribute.class);
        private final List<TokenInfo> tokens = new ArrayList<>();
        private int tokenIndex = 0;

        /**
         * 다음 토큰이 있는지 확인하고, 있으면 해당 토큰을 설정합니다.
         *
         * @return 다음 토큰이 있으면 true, 없으면 false
         */
        @Override
        public boolean incrementToken() {
            if (tokenIndex < tokens.size()) {
                clearAttributes();
                TokenInfo tokenInfo = tokens.get(tokenIndex);
                charTermAttribute.append(tokenInfo.token);
                charTermAttribute.setLength(tokenInfo.token.length());
                offsetAttribute.setOffset(correctOffset(tokenInfo.startOffset), correctOffset(tokenInfo.endOffset));
                positionIncrementAttribute.setPositionIncrement(1);
                tokenIndex++;
                return true;
            }
            return false;
        }

        @Override
        public void reset() throws IOException {
            super.reset();
            tokenIndex = 0;
            tokens.clear();
            String text = inputToString(input);
            tokenize(text);
        }

        /**
         * Reader로부터 입력을 읽어 문자열로 변환합니다.
         *
         * @param input 입력 Reader
         * @return 입력된 텍스트 문자열
         */
        private String inputToString(Reader input) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[1024];
            int length;
            try {
                while ((length = input.read(buffer)) != -1) {
                    builder.append(buffer, 0, length);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return builder.toString();
        }

        private void tokenize(String text) {
            int currentOffset = 0;
            for (String token : text.split("\\s+")) {
                int startOffset = text.indexOf(token, currentOffset);
                int endOffset = startOffset + token.length();
                tokens.add(new TokenInfo(token, startOffset, endOffset));
                currentOffset = endOffset;
            }
        }

        /**
         * Token 정보를 저장하는 내부 클래스입니다.
         */
        private static class TokenInfo {
            String token;
            int startOffset;
            int endOffset;

            TokenInfo(String token, int startOffset, int endOffset) {
                this.token = token;
                this.startOffset = startOffset;
                this.endOffset = endOffset;
            }
        }
    }
}
