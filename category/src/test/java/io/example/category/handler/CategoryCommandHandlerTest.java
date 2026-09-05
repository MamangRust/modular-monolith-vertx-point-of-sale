package io.example.category.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;

import io.example.category.domain.requests.CreateCategoryRequest;
import io.example.category.domain.requests.UpdateCategoryRequest;
import io.example.category.domain.response.category.CategoryResponse;
import io.example.category.domain.response.category.CategoryResponseDeleteAt;
import io.example.category.service.CategoryCommandService;
import io.vertx.core.Future;

import pb.category.Category.ApiResponseCategory;
import pb.category.Category.ApiResponseCategoryDeleteAt;
import pb.category.Category.FindByIdCategoryRequest;
import pb.category.CategoryCommand.ApiResponseCategoryAll;
import pb.category.CategoryCommand.ApiResponseCategoryDelete;

@ExtendWith(MockitoExtension.class)
class CategoryCommandHandlerTest {

    @Mock private CategoryCommandService commandService;

    private CategoryCommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        commandHandler = new CategoryCommandHandler(commandService);
    }

    @Test
    void create_shouldCallServiceAndReturnSuccess() {
        pb.category.CategoryCommand.CreateCategoryRequest request = pb.category.CategoryCommand.CreateCategoryRequest.newBuilder()
                .setName("New Category")
                .setDescription("Description")
                .build();

        CategoryResponse responseDto = CategoryResponse.builder()
                .id(1L)
                .name("New Category")
                .description("Description")
                .build();

        when(commandService.createCategory(any(CreateCategoryRequest.class)))
                .thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseCategory> result = commandHandler.create(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getName()).isEqualTo("New Category");
        verify(commandService).createCategory(any(CreateCategoryRequest.class));
    }

    @Test
    void update_shouldCallServiceAndReturnSuccess() {
        pb.category.CategoryCommand.UpdateCategoryRequest request = pb.category.CategoryCommand.UpdateCategoryRequest.newBuilder()
                .setCategoryId(1)
                .setName("Updated Category")
                .setDescription("Updated Description")
                .build();

        CategoryResponse responseDto = CategoryResponse.builder()
                .id(1L)
                .name("Updated Category")
                .description("Updated Description")
                .build();

        when(commandService.updateCategory(any(UpdateCategoryRequest.class)))
                .thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseCategory> result = commandHandler.update(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(commandService).updateCategory(any(UpdateCategoryRequest.class));
    }

    @Test
    void trashedCategory_shouldCallServiceAndReturnSuccess() {
        FindByIdCategoryRequest request = FindByIdCategoryRequest.newBuilder()
                .setId(1)
                .build();

        CategoryResponseDeleteAt responseDto = CategoryResponseDeleteAt.builder()
                .id(1L)
                .name("Trashed Category")
                .deletedAt("2024-01-02")
                .build();

        when(commandService.trashCategory(1L)).thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseCategoryDeleteAt> result = commandHandler.trashedCategory(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getDeletedAt().getValue()).isEqualTo("2024-01-02");
        verify(commandService).trashCategory(1L);
    }

    @Test
    void restoreCategory_shouldCallServiceAndReturnSuccess() {
        FindByIdCategoryRequest request = FindByIdCategoryRequest.newBuilder()
                .setId(1)
                .build();

        CategoryResponseDeleteAt responseDto = CategoryResponseDeleteAt.builder()
                .id(1L)
                .name("Restored Category")
                .deletedAt(null)
                .build();

        when(commandService.restoreCategory(1L)).thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseCategoryDeleteAt> result = commandHandler.restoreCategory(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().hasDeletedAt()).isFalse();
        verify(commandService).restoreCategory(1L);
    }

    @Test
    void deleteCategoryPermanent_shouldCallServiceAndReturnSuccess() {
        FindByIdCategoryRequest request = FindByIdCategoryRequest.newBuilder()
                .setId(1)
                .build();

        when(commandService.deleteCategoryPermanently(1L)).thenReturn(Future.succeededFuture(true));

        Future<ApiResponseCategoryDelete> result = commandHandler.deleteCategoryPermanent(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(commandService).deleteCategoryPermanently(1L);
    }

    @Test
    void restoreAllCategory_shouldCallServiceAndReturnSuccess() {
        when(commandService.restoreAllCategories()).thenReturn(Future.succeededFuture());

        Future<ApiResponseCategoryAll> result = commandHandler.restoreAllCategory(Empty.getDefaultInstance());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(commandService).restoreAllCategories();
    }

    @Test
    void deleteAllCategoryPermanent_shouldCallServiceAndReturnSuccess() {
        when(commandService.deleteAllPermanentCategories()).thenReturn(Future.succeededFuture());

        Future<ApiResponseCategoryAll> result = commandHandler.deleteAllCategoryPermanent(Empty.getDefaultInstance());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(commandService).deleteAllPermanentCategories();
    }
}
