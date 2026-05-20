package io.example.merchant.domain.requests;

import lombok.Data;

@Data
public class UpdateMerchantDocumentStatusRequest {
    private Integer documentId;
    private Integer merchantId;
    private String note;
    private String status;
}
