package io.example.apigateway.handler;

import static io.example.apigateway.utils.GrpcGatewayUtils.sendResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.example.common.exception.api.BadRequestException;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pb.product.Product;
import pb.product.ProductCommand;
import pb.product.VertxProductCommandServiceGrpcClient;
import pb.product.VertxProductServiceGrpcClient;

@Slf4j
@RequiredArgsConstructor
public class ProductProxyHandler {
        private final VertxProductServiceGrpcClient queryClient;
        private final VertxProductCommandServiceGrpcClient commandClient;
        private static final String UPLOAD_DIRECTORY = "uploads/products/";

        public void findAll(RoutingContext ctx) {
                var req = Product.FindAllProductRequest.newBuilder()
                                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                                .build();
                queryClient.findAll(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void findActive(RoutingContext ctx) {
                var req = Product.FindAllProductRequest.newBuilder()
                                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                                .build();
                queryClient.findByActive(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void findTrashed(RoutingContext ctx) {
                var req = Product.FindAllProductRequest.newBuilder()
                                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                                .build();
                queryClient.findByTrashed(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void findById(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
                var req = Product.FindByIdProductRequest.newBuilder().setId(id).build();
                queryClient.findById(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void findByMerchant(RoutingContext ctx) {
                int merchantId = GrpcGatewayUtils.getSafePathInt(ctx, "merchantId");
                var req = Product.FindAllProductMerchantRequest.newBuilder()
                                .setMerchantId(merchantId)
                                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                                .setCategoryId(GrpcGatewayUtils.getQueryInt(ctx, "categoryId", 0))
                                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                                .setMinPrice(GrpcGatewayUtils.getQueryInt(ctx, "minPrice", 0))
                                .setMaxPrice(GrpcGatewayUtils.getQueryInt(ctx, "maxPrice", 0))
                                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                                .build();
                queryClient.findByMerchant(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void findByCategory(RoutingContext ctx) {
                String categoryName = ctx.pathParam("categoryName");
                var req = Product.FindAllProductCategoryRequest.newBuilder()
                                .setCategoryName(categoryName)
                                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                                .setMinprice(GrpcGatewayUtils.getQueryInt(ctx, "minPrice", 0))
                                .setMaxprice(GrpcGatewayUtils.getQueryInt(ctx, "maxPrice", 0))
                                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                                .build();
                queryClient.findByCategory(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void create(RoutingContext ctx) {
                // Ambil existing image URL (dari form biasa) kalau ada
                String existingImageUrl = GrpcGatewayUtils.getFormString(ctx, "image_product", "");
                String imageUrl = existingImageUrl;

                // Cek apakah ada file upload dengan field name "imageFile"
                FileUpload imageFile = GrpcGatewayUtils.getFileUpload(ctx, "imageFile");

                if (imageFile != null) {
                        try {
                                imageUrl = storeUploadedFile(imageFile);
                        } catch (IOException e) {
                                ctx.fail(new BadRequestException("Failed to process uploaded file: " + e.getMessage()));
                                return;
                        }
                }

                var req = ProductCommand.CreateProductRequest.newBuilder()
                                .setMerchantId(GrpcGatewayUtils.getFormInteger(ctx, "merchant_id", 0))
                                .setCategoryId(GrpcGatewayUtils.getFormInteger(ctx, "category_id", 0))
                                .setName(GrpcGatewayUtils.getFormString(ctx, "name", ""))
                                .setDescription(GrpcGatewayUtils.getFormString(ctx, "description", ""))
                                .setPrice(GrpcGatewayUtils.getFormInteger(ctx, "price", 0))
                                .setCountInStock(GrpcGatewayUtils.getFormInteger(ctx, "count_in_stock", 0))
                                .setBrand(GrpcGatewayUtils.getFormString(ctx, "brand", ""))
                                .setWeight(GrpcGatewayUtils.getFormInteger(ctx, "weight", 0))
                                .setImageProduct(imageUrl)
                                .build();

                commandClient.create(req)
                                .onSuccess(r -> sendResponse(ctx, r, 201))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void update(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");

                String existingImageUrl = GrpcGatewayUtils.getFormString(ctx, "image_product", "");
                String imageUrl = existingImageUrl;

                FileUpload imageFile = GrpcGatewayUtils.getFileUpload(ctx, "imageFile");

                if (imageFile != null) {
                        try {
                                imageUrl = storeUploadedFile(imageFile);
                        } catch (IOException e) {
                                ctx.fail(new BadRequestException("Failed to process uploaded file: " + e.getMessage()));
                                return;
                        }
                }

                var req = ProductCommand.UpdateProductRequest.newBuilder()
                                .setProductId(id)
                                .setMerchantId(GrpcGatewayUtils.getFormInteger(ctx, "merchant_id", 0))
                                .setCategoryId(GrpcGatewayUtils.getFormInteger(ctx, "category_id", 0))
                                .setName(GrpcGatewayUtils.getFormString(ctx, "name", ""))
                                .setDescription(GrpcGatewayUtils.getFormString(ctx, "description", ""))
                                .setPrice(GrpcGatewayUtils.getFormInteger(ctx, "price", 0))
                                .setCountInStock(GrpcGatewayUtils.getFormInteger(ctx, "count_in_stock", 0))
                                .setBrand(GrpcGatewayUtils.getFormString(ctx, "brand", ""))
                                .setWeight(GrpcGatewayUtils.getFormInteger(ctx, "weight", 0))
                                .setImageProduct(imageUrl)
                                .build();

                commandClient.update(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void trashed(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
                var req = Product.FindByIdProductRequest.newBuilder().setId(id).build();
                commandClient.trashedProduct(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void restore(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
                var req = Product.FindByIdProductRequest.newBuilder().setId(id).build();
                commandClient.restoreProduct(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void deletePermanent(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
                var req = Product.FindByIdProductRequest.newBuilder().setId(id).build();
                commandClient.deleteProductPermanent(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void restoreAll(RoutingContext ctx) {
                commandClient.restoreAllProduct(com.google.protobuf.Empty.getDefaultInstance())
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void deleteAllPermanent(RoutingContext ctx) {
                commandClient.deleteAllProductPermanent(com.google.protobuf.Empty.getDefaultInstance())
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        private String storeUploadedFile(FileUpload fileUpload) throws IOException {
                Files.createDirectories(Paths.get(UPLOAD_DIRECTORY));

                String fileName = System.currentTimeMillis() + "_" + fileUpload.fileName();
                Path source = Paths.get(fileUpload.uploadedFileName());
                Path target = Paths.get(UPLOAD_DIRECTORY + fileName);

                Files.move(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return "/downloads/" + fileName;
        }
}