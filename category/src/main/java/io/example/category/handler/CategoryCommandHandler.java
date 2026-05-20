package io.example.category.handler;

import io.example.category.service.CategoryCommandService;
import io.vertx.core.Future;
import pb.category.Category.ApiResponseCategory;
import pb.category.Category.ApiResponseCategoryDeleteAt;
import pb.category.Category.FindByIdCategoryRequest;
import pb.category.CategoryCommand.CreateCategoryRequest;
import pb.category.CategoryCommand.UpdateCategoryRequest;
import pb.category.CategoryCommand.ApiResponseCategoryAll;
import pb.category.CategoryCommand.ApiResponseCategoryDelete;

public class CategoryCommandHandler implements pb.category.VertxCategoryCommandServiceGrpcServer.CategoryCommandServiceApi {
    private final CategoryCommandService commandService;

    public CategoryCommandHandler(CategoryCommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public Future<ApiResponseCategory> create(CreateCategoryRequest request) {
        io.example.category.domain.requests.CreateCategoryRequest req = new io.example.category.domain.requests.CreateCategoryRequest();
        req.setName(request.getName());
        req.setDescription(request.getDescription());

        return commandService.createCategory(req)
                .map(category -> ApiResponseCategory.newBuilder()
                        .setStatus("success")
                        .setMessage("Category created successfully")
                        .setData(ProtoConverter.toCategoryResponse(category))
                        .build());
    }

    @Override
    public Future<ApiResponseCategory> update(UpdateCategoryRequest request) {
        io.example.category.domain.requests.UpdateCategoryRequest req = new io.example.category.domain.requests.UpdateCategoryRequest();
        req.setCategoryId(request.getCategoryId());
        req.setName(request.getName());
        req.setDescription(request.getDescription());

        return commandService.updateCategory(req)
                .map(category -> ApiResponseCategory.newBuilder()
                        .setStatus("success")
                        .setMessage("Category updated successfully")
                        .setData(ProtoConverter.toCategoryResponse(category))
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryDeleteAt> trashedCategory(FindByIdCategoryRequest request) {
        return commandService.trashCategory((long) request.getId())
                .map(category -> ApiResponseCategoryDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Category trashed successfully")
                        .setData(ProtoConverter.toCategoryResponseDeleteAt(category))
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryDeleteAt> restoreCategory(FindByIdCategoryRequest request) {
        return commandService.restoreCategory((long) request.getId())
                .map(category -> ApiResponseCategoryDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Category restored successfully")
                        .setData(ProtoConverter.toCategoryResponseDeleteAt(category))
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryDelete> deleteCategoryPermanent(FindByIdCategoryRequest request) {
        return commandService.deleteCategoryPermanently((long) request.getId())
                .map(res -> ApiResponseCategoryDelete.newBuilder()
                        .setStatus("success")
                        .setMessage("Category permanently deleted successfully")
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryAll> restoreAllCategory(com.google.protobuf.Empty request) {
        return commandService.restoreAllCategories()
                .map(res -> ApiResponseCategoryAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All categories restored successfully")
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryAll> deleteAllCategoryPermanent(com.google.protobuf.Empty request) {
        return commandService.deleteAllPermanentCategories()
                .map(res -> ApiResponseCategoryAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All categories permanently deleted successfully")
                        .build());
    }
}
