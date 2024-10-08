package csw.korea.festival.main.festival.model;

import csw.korea.festival.main.common.dto.KWeather;
import csw.korea.festival.main.config.converter.LocalDateStringConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "festivals")
@Access(AccessType.FIELD)
@Indexed
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

    private Double latitude;

    private Double longitude;

    // Fields for English translations

    @FullTextField(analyzer = "english")
    private String nameEn;     // Festival Name (English)
    @FullTextField(analyzer = "english")
    private String summaryEn;  // Festival Summary (English)

    private String naverUrl;   // Naver Map URL

    @KeywordField
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "festival_categories", joinColumns = @JoinColumn(name = "festival_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private Set<FestivalCategory> categories;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Enumerated(EnumType.STRING)
    private FestivalUsageFeeCategory usageFeeCategory;

    @Transient
    private Double distance;   // Calculated dynamically

    @Transient
    private KWeather.WeatherRequest weather;    // current weather information
}