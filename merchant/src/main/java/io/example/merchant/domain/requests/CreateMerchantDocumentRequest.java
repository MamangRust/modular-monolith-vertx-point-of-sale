package io.example.merchant.domain.requests;

import lombok.Data;

@Data
public class CreateMerchantDocumentRequest {
    private Integer merchantId;
    private String documentType;
    private String documentUrl;
}
