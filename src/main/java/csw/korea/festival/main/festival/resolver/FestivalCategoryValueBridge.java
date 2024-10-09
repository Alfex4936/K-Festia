package csw.korea.festival.main.festival.resolver;

import csw.korea.festival.main.festival.model.FestivalCategory;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

// Unused
public class FestivalCategoryValueBridge implements ValueBridge<FestivalCategory, String> {

    @Override
    public String toIndexedValue(FestivalCategory value, ValueBridgeToIndexedValueContext context) {
        if (value == null) {
            return null;
        }
        // Concatenate English and Korean display names
        return STR."\{value.getDisplayNameEn()} \{value.getDisplayNameKo()}";
    }

    @Override
    public boolean isCompatibleWith(ValueBridge<?, ?> other) {
        return other != null && other.getClass() == this.getClass();
    }
}