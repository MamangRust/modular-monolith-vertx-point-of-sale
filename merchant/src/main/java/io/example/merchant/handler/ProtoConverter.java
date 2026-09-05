package io.example.merchant.handler;

import com.google.protobuf.StringValue;

import io.example.common.domain.PagedResult;
import io.example.merchant.domain.response.MerchantDocumentResponse;
import io.example.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import io.example.merchant.domain.response.MerchantResponse;
import io.example.merchant.domain.response.MerchantResponseDeleteAt;
import pb.common.PaginationMeta;

public class ProtoConverter {
    public static pb.merchant.Merchant.MerchantResponse toResponse(MerchantResponse merchant) {
        if (merchant == null) {
            return pb.merchant.Merchant.MerchantResponse.getDefaultInstance();
        }
        return pb.merchant.Merchant.MerchantResponse.newBuilder()
                .setId(merchant.getId().intValue())
                .setUserId(merchant.getUserId() != null ? merchant.getUserId() : 0)
                .setName(merchant.getName() != null ? merchant.getName() : "")
                .setDescription(merchant.getDescription() != null ? merchant.getDescription() : "")
                .setAddress(merchant.getAddress() != null ? merchant.getAddress() : "")
                .setContactEmail(merchant.getContactEmail() != null ? merchant.getContactEmail() : "")
                .setContactPhone(merchant.getContactPhone() != null ? merchant.getContactPhone() : "")
                .setStatus(merchant.getStatus() != null ? merchant.getStatus() : "")
                .setCreatedAt(merchant.getCreatedAt() != null ? merchant.getCreatedAt() : "")
                .setUpdatedAt(merchant.getUpdatedAt() != null ? merchant.getUpdatedAt() : "")
                .build();
    }

    public static pb.merchant.Merchant.MerchantResponseDeleteAt toResponseDeleteAt(MerchantResponseDeleteAt merchant) {
        if (merchant == null) {
            return pb.merchant.Merchant.MerchantResponseDeleteAt.getDefaultInstance();
        }
        pb.merchant.Merchant.MerchantResponseDeleteAt.Builder builder = pb.merchant.Merchant.MerchantResponseDeleteAt
                .newBuilder()
                .setId(merchant.getId().intValue())
                .setUserId(merchant.getUserId() != null ? merchant.getUserId() : 0)
                .setName(merchant.getName() != null ? merchant.getName() : "")
                .setDescription(merchant.getDescription() != null ? merchant.getDescription() : "")
                .setAddress(merchant.getAddress() != null ? merchant.getAddress() : "")
                .setContactEmail(merchant.getContactEmail() != null ? merchant.getContactEmail() : "")
                .setContactPhone(merchant.getContactPhone() != null ? merchant.getContactPhone() : "")
                .setStatus(merchant.getStatus() != null ? merchant.getStatus() : "")
                .setCreatedAt(merchant.getCreatedAt() != null ? merchant.getCreatedAt() : "")
                .setUpdatedAt(merchant.getUpdatedAt() != null ? merchant.getUpdatedAt() : "");

        if (merchant.getDeletedAt() != null) {
            builder.setDeletedAt(StringValue.of(merchant.getDeletedAt()));
        }
        return builder.build();
    }

    public static pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument toDocumentResponse(
            MerchantDocumentResponse doc) {
        if (doc == null) {
            return pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument.getDefaultInstance();
        }
        return pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument.newBuilder()
                .setDocumentId(doc.getId())
                .setMerchantId(doc.getMerchantId() != null ? doc.getMerchantId() : 0)
                .setDocumentType(doc.getDocumentType() != null ? doc.getDocumentType() : "")
                .setDocumentUrl(doc.getDocumentUrl() != null ? doc.getDocumentUrl() : "")
                .setStatus(doc.getStatus() != null ? doc.getStatus() : "")
                .setNote(doc.getNote() != null ? doc.getNote() : "")
                .setUpdatedAt(doc.getUpdatedAt() != null ? doc.getUpdatedAt().toString() : "")
                .build();
    }

    public static pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt toDocumentResponseDeleteAt(
            MerchantDocumentResponseDeleteAt doc) {
        if (doc == null) {
            return pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt.getDefaultInstance();
        }
        pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt.Builder builder = pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt
                .newBuilder()
                .setDocumentId(doc.getId().intValue())
                .setMerchantId(doc.getMerchantId().intValue())
                .setDocumentType(doc.getDocumentType() != null ? doc.getDocumentType() : "")
                .setDocumentUrl(doc.getDocumentUrl() != null ? doc.getDocumentUrl() : "")
                .setStatus(doc.getStatus() != null ? doc.getStatus() : "")
                .setNote(doc.getNote() != null ? doc.getNote() : "")
                .setUpdatedAt(doc.getUpdatedAt() != null ? doc.getUpdatedAt().toString() : "");

        if (doc.getDeletedAt() != null) {
            builder.setDeletedAt(StringValue.of(doc.getDeletedAt().toString()));
        }
        return builder.build();
    }

    public static PaginationMeta toPaginationMeta(PagedResult<?> result, int page, int pageSize) {
        int totalRecords = result.getTotalRecords();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        return PaginationMeta.newBuilder()
                .setCurrentPage(page)
                .setPageSize(pageSize)
                .setTotalPages(totalPages)
                .setTotalRecords(totalRecords)
                .build();
    }
}