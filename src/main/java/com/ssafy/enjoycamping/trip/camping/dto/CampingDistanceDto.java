package com.ssafy.enjoycamping.trip.camping.dto;

import com.ssafy.enjoycamping.trip.camping.entity.Camping;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@ToString
public class CampingDistanceDto {
    private int id;
    private String name;
    private String sidoName;
    private Integer sidoCode;
    private String gugunName;
    private Integer gugunCode;
    private String detailAddress;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String imageUrl;
    private String introduction;
    private String telephone;
    private String homepageUrl;
    private Double distance;
    private int reviewCnt;

    public static CampingDistanceDto fromEntity(Camping camping, int reviewCnt){
        return CampingDistanceDto.builder()
                .id(camping.getId())
                .name(camping.getName())
                .sidoName(camping.getSidoName())
                .sidoCode(camping.getSidoCode())
                .gugunName(camping.getGugunName())
                .gugunCode(camping.getGugunCode())
                .detailAddress(camping.getDetailAddress())
                .latitude(camping.getLatitude())
                .longitude(camping.getLongitude())
                .imageUrl(camping.getImageUrl())
                .introduction(camping.getIntroduction())
                .telephone(camping.getTelephone())
                .homepageUrl(camping.getHomepageUrl())
                .reviewCnt(reviewCnt)
                .build();
    }
}
