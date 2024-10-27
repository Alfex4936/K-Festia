package csw.korea.festival.main.festival.repository;

import csw.korea.festival.main.festival.model.Festival;
import csw.korea.festival.main.festival.model.FestivalCategory;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FestivalRepository extends JpaRepository<Festival, Long> {
    List<Festival> findByProvince(String province);

    List<Festival> findByCity(String city);

    List<Festival> findByDistrict(String district);

    List<Festival> findByProvinceAndCity(String province, String city);

    @NotNull Optional<Festival> findById(@NotNull Long id);
    List<Festival> findByFestivalIdIn(Set<String> festivalIds);

    /**
     * Retrieves a paginated list of festivals for a specific month and updated after a certain time.
     *
     * @param month            The month in "MM" format (e.g., "01" for January).
     * @param lastUpdatedAfter The cutoff LocalDateTime; festivals updated after this time will be included.
     * @return A page of festivals matching the criteria.
     */
    @Query(value = "SELECT * FROM festivals f WHERE strftime('%m', f.start_date) = :month AND f.last_updated > :lastUpdatedAfter",
            nativeQuery = true)
    List<Festival> findByMonthAndLastUpdatedAfter(@Param("month") String month,
                                                  @Param("lastUpdatedAfter") LocalDateTime lastUpdatedAfter);

    @Query(value = "SELECT * FROM festivals f WHERE date(f.end_date) >= date(:startOfMonth) AND date(f.start_date) <= date(:endOfMonth) AND f.last_updated > :lastUpdatedAfter",
            nativeQuery = true)
    List<Festival> findFestivalsOverlappingMonth(@Param("startOfMonth") String startOfMonth,
                                                 @Param("endOfMonth") String endOfMonth,
                                                 @Param("lastUpdatedAfter") LocalDateTime lastUpdatedAfter);

    @Query(value = "SELECT * FROM festivals f " +
            "WHERE strftime('%m', f.start_date) = :month " +
            "AND f.last_updated > :lastUpdatedAfter " +
            "AND f.latitude BETWEEN :minLat AND :maxLat " +
            "AND f.longitude BETWEEN :minLon AND :maxLon",
            nativeQuery = true)
    List<Festival> findNearbyFestivals(@Param("month") String month,
                                       @Param("lastUpdatedAfter") LocalDateTime lastUpdatedAfter,
                                       @Param("minLat") double minLat,
                                       @Param("maxLat") double maxLat,
                                       @Param("minLon") double minLon,
                                       @Param("maxLon") double maxLon);

    @Query("SELECT DISTINCT f FROM Festival f JOIN f.categories c WHERE f.startDate >= :startDate AND f.endDate <= :endDate AND c IN :categories")
    List<Festival> findFestivalsByDateRangeAndCategories(@Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate,
                                                         @Param("categories") List<FestivalCategory> categories);

}