package io.example.category.handler;

import io.example.category.domain.requests.CreateCategoryRequest;
import io.example.category.domain.requests.UpdateCategoryRequest;
import io.example.category.service.CategoryCommandService;
import io.example.common.grpc.GrpcExceptionMapper;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.category.Category.ApiResponseCategory;
import pb.category.Category.ApiResponseCategoryDeleteAt;
import pb.category.Category.FindByIdCategoryRequest;
import pb.category.CategoryCommand.ApiResponseCategoryAll;
import pb.category.CategoryCommand.ApiResponseCategoryDelete;

@RequiredArgsConstructor
public class CategoryCommandHandler
                implements pb.category.VertxCategoryCommandServiceGrpcServer.CategoryCommandServiceApi {
        private final CategoryCommandService commandService;

        @Override
        public Future<ApiResponseCategory> create(pb.category.CategoryCommand.CreateCategoryRequest request) {
                CreateCategoryRequest req = CreateCategoryRequest.builder()
                                .name(request.getName())
                                .description(request.getDescription())
                                .build();

                return commandService.createCategory(req)
                                .map(category -> ApiResponseCategory.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Category created successfully")
                                                .setData(ProtoConverter.toCategoryResponse(category))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategory> update(pb.category.CategoryCommand.UpdateCategoryRequest request) {
                UpdateCategoryRequest req = UpdateCategoryRequest.builder()
                                .categoryId(request.getCategoryId())
                                .name(request.getName())
                                .description(request.getDescription())
                                .build();

                return commandService.updateCategory(req)
                                .map(category -> ApiResponseCategory.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Category updated successfully")
                                                .setData(ProtoConverter.toCategoryResponse(category))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryDeleteAt> trashedCategory(FindByIdCategoryRequest request) {
                return commandService.trashCategory((long) request.getId())
                                .map(category -> ApiResponseCategoryDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Category trashed successfully")
                                                .setData(ProtoConverter.toCategoryResponseDeleteAt(category))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryDeleteAt> restoreCategory(FindByIdCategoryRequest request) {
                return commandService.restoreCategory((long) request.getId())
                                .map(category -> ApiResponseCategoryDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Category restored successfully")
                                                .setData(ProtoConverter.toCategoryResponseDeleteAt(category))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryDelete> deleteCategoryPermanent(FindByIdCategoryRequest request) {
                return commandService.deleteCategoryPermanently((long) request.getId())
                                .map(res -> ApiResponseCategoryDelete.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Category permanently deleted successfully")
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryAll> restoreAllCategory(com.google.protobuf.Empty request) {
                return commandService.restoreAllCategories()
                                .map(res -> ApiResponseCategoryAll.newBuilder()
                                                .setStatus("success")
                                                .setMessage("All categories restored successfully")
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryAll> deleteAllCategoryPermanent(com.google.protobuf.Empty request) {
                return commandService.deleteAllPermanentCategories()
                                .map(res -> ApiResponseCategoryAll.newBuilder()
                                                .setStatus("success")
                                                .setMessage("All categories permanently deleted successfully")
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}