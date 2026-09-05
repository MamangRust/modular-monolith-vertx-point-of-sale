package io.example.merchant.domain.requests;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateMerchantDocumentStatusRequest {
    private Integer documentId;
    private Integer merchantId;
    private String note;
    private String status;
}
