package io.example.merchant.domain.requests;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateMerchantDocumentRequest {
    private Integer merchantId;
    private String documentType;
    private String documentUrl;
    private String note;
}
