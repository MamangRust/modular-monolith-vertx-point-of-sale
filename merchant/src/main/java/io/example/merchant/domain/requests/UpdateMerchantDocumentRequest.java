package io.example.merchant.domain.requests;

import lombok.Data;

@Data
public class UpdateMerchantDocumentRequest {
    private Integer documentId;
    private Integer merchantId;
    private String documentType;
    private String documentUrl;
    private String note;
    private String status;
}
