package iuh.fit.se.model.dto.call;

import lombok.Data;

@Data
public class OfferDto {
    private String targetUserId;
    private String offer;
    private String roomId;
}