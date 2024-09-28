package csw.korea.festival.main.festival.model;

import lombok.Data;

import java.util.List;

@Data
public class FestivalPage {
    private List<Festival> content;
    private int pageNumber;
    private int pageSize;
    private int totalElements;
    private int totalPages;
}
