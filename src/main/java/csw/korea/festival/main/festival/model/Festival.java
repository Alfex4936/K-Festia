package csw.korea.festival.main.festival.model;

import csw.korea.festival.main.common.dto.KWeather;
import csw.korea.festival.main.config.converter.LocalDateStringConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Latitude;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Longitude;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@Entity
@Table(name = "festivals")
@Access(AccessType.FIELD)
@Indexed
@GeoPointBinding(fieldName = "festivalLocation", sortable = Sortable.YES)
public class Festival {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String festivalId; // Festival ID

    @FullTextField(analyzer = "korean")
    private String name;       // Festival Name

    @FullTextField(analyzer = "korean")
    @Column(length = 1000)
    private String summary;    // Festival Summary

    @Convert(converter = LocalDateStringConverter.class)
    @Column(name = "start_date")
    private LocalDate startDate;

    @Convert(converter = LocalDateStringConverter.class)
    @Column(name = "end_date")
    private LocalDate endDate;

    @FullTextField(analyzer = "korean")
    private String address;    // Address

    private String usageFeeInfo; // Festival Usage Fee Information

    private String areaName;   // Area Name

    @Latitude
    private Double latitude;
    @Longitude
    private Double longitude;

    private String imageUrl;   // Festival Image URL

    // Fields for English translations

    @FullTextField(analyzer = "english")
    private String nameEn;     // Festival Name (English)
    @FullTextField(analyzer = "english")
    private String summaryEn;  // Festival Summary (English)

    private String naverUrl;   // Naver Map URL

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "festival_categories", joinColumns = @JoinColumn(name = "festival_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private Set<FestivalCategory> categories;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Enumerated(EnumType.STRING)
    private FestivalUsageFeeCategory usageFeeCategory;

    @FullTextField(analyzer = "seok")
    private String province;

    @FullTextField(analyzer = "seok")
    private String city;

    @FullTextField(analyzer = "seok")
    private String district;

    @FullTextField(analyzer = "seok")
    private String town;

    @FullTextField(analyzer = "seok")
    private String street;

    @Transient
    private Double distance;   // Calculated dynamically

    @Transient
    private KWeather.WeatherRequest weather;    // current weather information

//    @Transient
//    public boolean isFinished() {
//        if (this.endDate != null) {
//            return this.endDate.isBefore(LocalDate.now());
//        }
//        return false;
//    }

    @Transient
    @FullTextField(analyzer = "multilingual")
    @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "categories")))
    public String getCategoryDisplayNames() {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        return categories.stream()
                .flatMap(cat -> Stream.of(cat.getDisplayNameEn(), cat.getDisplayNameKo()))
                .collect(Collectors.joining(" "));
    }
}