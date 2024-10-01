package csw.korea.festival.main.festival.model;

import lombok.Data;

import java.util.List;

@Data
public class FestivalResponse {
    private int totalCnt;
    private List<FestivalDTO> resultList;
}