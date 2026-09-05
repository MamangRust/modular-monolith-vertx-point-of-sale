package io.example.transaction.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.transaction.domain.requests.transactions.FindAllTransactionRequest;
import io.example.transaction.service.TransactionQueryService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.transaction.ApiResponsePaginationTransaction;
import pb.transaction.ApiResponsePaginationTransactionDeleteAt;
import pb.transaction.ApiResponseTransaction;
import pb.transaction.FindAllTransactionMerchantRequest;
import pb.transaction.FindByIdTransactionRequest;

@RequiredArgsConstructor
public class TransactionQueryHandler
                implements pb.transaction.VertxTransactionQueryServiceGrpcServer.TransactionQueryServiceApi {

        private final TransactionQueryService service;

        private pb.common.PaginationMeta toMeta(int page, int pageSize, int totalRecords) {
                int currentPage = page > 0 ? page : 1;
                int size = pageSize > 0 ? pageSize : 10;
                int totalPages = size > 0 ? (int) Math.ceil((double) totalRecords / size) : 0;
                return pb.common.PaginationMeta.newBuilder()
                                .setCurrentPage(currentPage)
                                .setPageSize(size)
                                .setTotalPages(totalPages)
                                .setTotalRecords(totalRecords)
                                .build();
        }

        @Override
        public Future<ApiResponsePaginationTransaction> findAll(pb.transaction.FindAllTransactionRequest req) {
                var reqDomain = FindAllTransactionRequest.builder()
                                .page(req.getPage())
                                .pageSize(req.getPageSize())
                                .build();

                return service.findAllTransaction(reqDomain)
                                .map(resp -> ApiResponsePaginationTransaction.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Transactions retrieved successfully")
                                                .addAllData(resp.getData().stream()
                                                                .map(ProtoConverter::fromTransactionResponse).toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                resp.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationTransaction> findByMerchant(FindAllTransactionMerchantRequest req) {
                var domainReq = io.example.transaction.domain.requests.transactions.FindAllTransactionByMerchantRequest
                                .builder()
                                .page(req.getPage())
                                .pageSize(req.getPageSize())
                                .merchantId(req.getMerchantId())
                                .build();

                return service.findAllTransactionByMerchant(domainReq)
                                .map(resp -> ApiResponsePaginationTransaction.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Transactions by merchant retrieved successfully")
                                                .addAllData(resp.getData().stream()
                                                                .map(ProtoConverter::fromTransactionResponse).toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                resp.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseTransaction> findById(FindByIdTransactionRequest req) {
                return service.findByIdTransaction((long) req.getTransactionId())
                                .map(resp -> ApiResponseTransaction.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Transaction found successfully")
                                                .setData(ProtoConverter.fromTransactionResponse(resp))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationTransactionDeleteAt> findByActive(
                        pb.transaction.FindAllTransactionRequest req) {
                var domainReq = io.example.transaction.domain.requests.transactions.FindAllTransactionRequest.builder()
                                .page(req.getPage())
                                .pageSize(req.getPageSize())
                                .build();

                return service.findByActiveTransaction(domainReq)
                                .map(resp -> ApiResponsePaginationTransactionDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Active transactions retrieved successfully")
                                                .addAllData(resp.getData().stream()
                                                                .map(ProtoConverter::fromTransactionResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                resp.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationTransactionDeleteAt> findByTrashed(
                        pb.transaction.FindAllTransactionRequest req) {
                var domainReq = io.example.transaction.domain.requests.transactions.FindAllTransactionRequest.builder()
                                .page(req.getPage())
                                .pageSize(req.getPageSize())
                                .build();

                return service.findByTrashedTransaction(domainReq)
                                .map(resp -> ApiResponsePaginationTransactionDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Trashed transactions retrieved successfully")
                                                .addAllData(resp.getData().stream()
                                                                .map(ProtoConverter::fromTransactionResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                resp.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}