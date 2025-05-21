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
    // 기본 조회 메서드 - 이미 스프링 데이터 JPA가 최적화함
    List<Festival> findByProvince(String province);
    List<Festival> findByCity(String city);
    List<Festival> findByDistrict(String district);
    List<Festival> findByProvinceAndCity(String province, String city);
    @NotNull Optional<Festival> findById(@NotNull Long id);
    List<Festival> findByFestivalIdIn(Set<String> festivalIds);

    /**
     * 특정 월의 페이징된 축제 목록을 조회합니다.
     */
    @Query(value = "SELECT * FROM festivals f WHERE substr(f.start_date, 6, 2) = :month AND f.last_updated > :lastUpdatedAfter",
            nativeQuery = true)
    List<Festival> findByMonthAndLastUpdatedAfter(
            @Param("month") String month,
            @Param("lastUpdatedAfter") LocalDateTime lastUpdatedAfter);

    /**
     * 지정된 월에 진행 중인 축제 목록을 조회합니다.
     * SQLite에 최적화된 날짜 비교 방식 사용
     */
    @Query(value = "SELECT * FROM festivals f WHERE " +
            "f.end_date >= :startOfMonth AND f.start_date <= :endOfMonth AND f.last_updated > :lastUpdatedAfter " +
            "ORDER BY f.start_date",
            nativeQuery = true)
    List<Festival> findFestivalsOverlappingMonth(
            @Param("startOfMonth") String startOfMonth,
            @Param("endOfMonth") String endOfMonth,
            @Param("lastUpdatedAfter") LocalDateTime lastUpdatedAfter);

    /**
     * 특정 지역 근처의 축제를 조회합니다.
     * 인덱스를 활용하는 위치 기반 쿼리
     */
    @Query(value = "SELECT * FROM festivals f " +
            "WHERE substr(f.start_date, 6, 2) = :month " +
            "AND f.last_updated > :lastUpdatedAfter " +
            "AND f.latitude BETWEEN :minLat AND :maxLat " +
            "AND f.longitude BETWEEN :minLon AND :maxLon " +
            "ORDER BY ((:centerLat - f.latitude) * (:centerLat - f.latitude) + " +
            "(:centerLon - f.longitude) * (:centerLon - f.longitude))",
            nativeQuery = true)
    List<Festival> findNearbyFestivals(
            @Param("month") String month,
            @Param("lastUpdatedAfter") LocalDateTime lastUpdatedAfter,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLon") double minLon,
            @Param("maxLon") double maxLon,
            @Param("centerLat") double centerLat,
            @Param("centerLon") double centerLon);

    /**
     * 날짜 범위와 카테고리로 축제를 조회합니다.
     */
    @Query("SELECT DISTINCT f FROM Festival f JOIN f.categories c " +
            "WHERE f.startDate <= :endDate AND f.endDate >= :startDate AND c IN :categories " +
            "ORDER BY f.startDate")
    List<Festival> findFestivalsByDateRangeAndCategories(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categories") List<FestivalCategory> categories);

    /**
     * 특정 시간 이후에 업데이트된 축제를 조회합니다.
     */
    @Query("SELECT f FROM Festival f WHERE f.lastUpdated > :lastUpdatedAfter ORDER BY f.lastUpdated DESC")
    List<Festival> findFestivalsUpdatedAfter(@Param("lastUpdatedAfter") LocalDateTime lastUpdatedAfter);

    /**
     * 위치 정보만으로 효율적으로 조회합니다.
     */
    @Query("SELECT f FROM Festival f WHERE " +
            "f.province = :province AND (:city IS NULL OR f.city = :city) AND " +
            "(:district IS NULL OR f.district = :district)")
    List<Festival> findByLocation(
            @Param("province") String province,
            @Param("city") String city,
            @Param("district") String district);

    /**
     * 축제 카테고리별 조회 - 특정 기간 내
     */
    @Query(value = "SELECT f.* FROM festivals f " +
            "JOIN festival_categories fc ON f.id = fc.festival_id " +
            "WHERE fc.category = :category AND f.start_date <= :endDate AND f.end_date >= :startDate " +
            "ORDER BY f.start_date",
            nativeQuery = true)
    List<Festival> findByCategoryInDateRange(
            @Param("category") String category,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);
}