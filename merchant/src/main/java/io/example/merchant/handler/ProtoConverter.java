package io.example.merchant.handler;

import com.google.protobuf.StringValue;

import io.example.common.domain.PagedResult;
import io.example.merchant.model.Merchant;
import io.example.merchant.model.MerchantDocument;
import pb.common.PaginationMeta;
import pb.merchant.Merchant.MerchantResponse;
import pb.merchant.Merchant.MerchantResponseDeleteAt;

public class ProtoConverter {

    public static MerchantResponse toResponse(Merchant merchant) {
        if (merchant == null) {
            return MerchantResponse.getDefaultInstance();
        }
        MerchantResponse.Builder builder = MerchantResponse.newBuilder()
                .setId(merchant.getMerchantId() != null ? merchant.getMerchantId() : 0)
                .setName(merchant.getName() != null ? merchant.getName() : "")
                .setApiKey(merchant.getApiKey() != null ? merchant.getApiKey() : "")
                .setStatus(merchant.getStatus() != null ? merchant.getStatus() : "")
                .setUserId(merchant.getUserId() != null ? merchant.getUserId() : 0)
                .setCreatedAt(merchant.getCreatedAt() != null ? merchant.getCreatedAt().toString() : "")
                .setUpdatedAt(merchant.getUpdatedAt() != null ? merchant.getUpdatedAt().toString() : "");
        return builder.build();
    }

    public static MerchantResponseDeleteAt toResponseDeleteAt(Merchant merchant) {
        if (merchant == null) {
            return MerchantResponseDeleteAt.getDefaultInstance();
        }
        MerchantResponseDeleteAt.Builder builder = MerchantResponseDeleteAt.newBuilder()
                .setId(merchant.getMerchantId() != null ? merchant.getMerchantId() : 0)
                .setName(merchant.getName() != null ? merchant.getName() : "")
                .setApiKey(merchant.getApiKey() != null ? merchant.getApiKey() : "")
                .setStatus(merchant.getStatus() != null ? merchant.getStatus() : "")
                .setUserId(merchant.getUserId() != null ? merchant.getUserId() : 0)
                .setCreatedAt(merchant.getCreatedAt() != null ? merchant.getCreatedAt().toString() : "")
                .setUpdatedAt(merchant.getUpdatedAt() != null ? merchant.getUpdatedAt().toString() : "");

        if (merchant.getDeletedAt() != null) {
            builder.setDeletedAt(StringValue.of(merchant.getDeletedAt().toString()));
        }
        return builder.build();
    }

    public static pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument toDocumentResponse(
            MerchantDocument doc) {
        if (doc == null) {
            return pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument.getDefaultInstance();
        }
        return pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument.newBuilder()
                .setDocumentId(doc.getDocumentId() != null ? doc.getDocumentId() : 0)
                .setMerchantId(doc.getMerchantId() != null ? doc.getMerchantId() : 0)
                .setDocumentType(doc.getDocumentType() != null ? doc.getDocumentType() : "")
                .setDocumentUrl(doc.getDocumentUrl() != null ? doc.getDocumentUrl() : "")
                .setStatus(doc.getStatus() != null ? doc.getStatus() : "")
                .setNote(doc.getNote() != null ? doc.getNote() : "")
                .setUploadedAt(doc.getUploadedAt() != null ? doc.getUploadedAt().toString() : "")
                .setUpdatedAt(doc.getUpdatedAt() != null ? doc.getUpdatedAt().toString() : "")
                .build();
    }

    public static pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt toDocumentResponseDeleteAt(
            MerchantDocument doc) {
        if (doc == null) {
            return pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt.getDefaultInstance();
        }
        pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt.Builder builder = pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt
                .newBuilder()
                .setDocumentId(doc.getDocumentId() != null ? doc.getDocumentId() : 0)
                .setMerchantId(doc.getMerchantId() != null ? doc.getMerchantId() : 0)
                .setDocumentType(doc.getDocumentType() != null ? doc.getDocumentType() : "")
                .setDocumentUrl(doc.getDocumentUrl() != null ? doc.getDocumentUrl() : "")
                .setStatus(doc.getStatus() != null ? doc.getStatus() : "")
                .setNote(doc.getNote() != null ? doc.getNote() : "")
                .setUploadedAt(doc.getUploadedAt() != null ? doc.getUploadedAt().toString() : "")
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
