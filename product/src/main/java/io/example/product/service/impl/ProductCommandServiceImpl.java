package io.example.product.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.exception.NotFoundException;
import io.example.common.model.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.product.domain.requests.CreateProductRequest;
import io.example.product.domain.requests.UpdateProductRequest;
import io.example.product.model.ProductResponse;
import io.example.product.model.ProductResponseDeleteAt;
import io.example.product.repository.CategoryQueryRepository;
import io.example.product.repository.MerchantQueryRepository;
import io.example.product.repository.ProductCommandRepository;
import io.example.product.service.ProductCommandService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;

public class ProductCommandServiceImpl implements ProductCommandService {
    private static final Logger logger = LoggerFactory.getLogger(ProductCommandServiceImpl.class);

    private final ProductCommandRepository repository;
    private final CategoryQueryRepository categoryQueryRepository;
    private final MerchantQueryRepository merchantQueryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    public ProductCommandServiceImpl(
            ProductCommandRepository repository,
            CategoryQueryRepository categoryQueryRepository,
            MerchantQueryRepository merchantQueryRepository,
            RedisService redisService,
            TracingMetrics tracingMetrics) {
        this.repository = repository;
        this.categoryQueryRepository = categoryQueryRepository;
        this.merchantQueryRepository = merchantQueryRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<ApiResponse<ProductResponse>> create(CreateProductRequest req) {
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("ProductCommandService.create");
        Span span = Span.fromContext(tracingContext.getContext());

        logger.info("Creating product: {}", req.getName());

        // Validate Merchant and Category exist
        return merchantQueryRepository.existsById(req.getMerchantId())
                .compose(merchantExists -> {
                    if (!merchantExists) {
                        return Future.failedFuture(new NotFoundException("Merchant not found"));
                    }
                    return categoryQueryRepository.existsById(req.getCategoryId());
                })
                .compose(categoryExists -> {
                    if (!categoryExists) {
                        return Future.failedFuture(new NotFoundException("Category not found"));
                    }
                    return repository.createProduct(req);
                })
                .map(data -> {
                    span.setAttribute("id", data.getProductId());
                    tracingMetrics.completeSpanSuccess(tracingContext, "create", "Success");
                    return ApiResponse.success("Product created", ProductResponse.from(data));
                })
                .recover(err -> {
                    logger.error("Failed to create product", err);
                    tracingMetrics.completeSpanError(tracingContext, "create", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to create: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<ProductResponse>> update(UpdateProductRequest req) {
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("ProductCommandService.update");
        Span span = Span.fromContext(tracingContext.getContext());
        span.setAttribute("id", req.getProductId() != null ? req.getProductId().longValue() : 0);

        logger.info("Updating product: {}", req.getProductId());

        // Validate Merchant and Category exist
        return merchantQueryRepository.existsById(req.getMerchantId())
                .compose(merchantExists -> {
                    if (!merchantExists) {
                        return Future.failedFuture(new NotFoundException("Merchant not found"));
                    }
                    return categoryQueryRepository.existsById(req.getCategoryId());
                })
                .compose(categoryExists -> {
                    if (!categoryExists) {
                        return Future.failedFuture(new NotFoundException("Category not found"));
                    }
                    return repository.updateProduct(req);
                })
                .compose(data -> {
                    if (data == null) {
                        return Future.failedFuture(new NotFoundException("Product not found"));
                    }
                    return redisService.delete("product:" + req.getProductId()).map(data);
                })
                .map(data -> {
                    tracingMetrics.completeSpanSuccess(tracingContext, "update", "Success");
                    return ApiResponse.success("Product updated", ProductResponse.from(data));
                })
                .recover(err -> {
                    logger.error("Failed to update product", err);
                    tracingMetrics.completeSpanError(tracingContext, "update", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to update: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<ProductResponseDeleteAt>> trash(Long id) {
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("ProductCommandService.trash");
        Span span = Span.fromContext(tracingContext.getContext());
        span.setAttribute("id", id);

        logger.info("Trashing product: {}", id);

        return repository.trashProduct(id)
                .compose(data -> {
                    if (data == null) {
                        return Future.failedFuture(new NotFoundException("Product not found"));
                    }
                    return redisService.delete("product:" + id).map(data);
                })
                .map(data -> {
                    tracingMetrics.completeSpanSuccess(tracingContext, "trash", "Success");
                    return ApiResponse.success("Product trashed", ProductResponseDeleteAt.from(data));
                })
                .recover(err -> {
                    logger.error("Failed to trash product", err);
                    tracingMetrics.completeSpanError(tracingContext, "trash", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to trash: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<ProductResponseDeleteAt>> restore(Long id) {
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("ProductCommandService.restore");
        Span span = Span.fromContext(tracingContext.getContext());
        span.setAttribute("id", id);

        logger.info("Restoring product: {}", id);

        return repository.restoreProduct(id)
                .compose(data -> {
                    if (data == null) {
                        return Future.failedFuture(new NotFoundException("Product not found"));
                    }
                    return redisService.delete("product:" + id).map(data);
                })
                .map(data -> {
                    tracingMetrics.completeSpanSuccess(tracingContext, "restore", "Success");
                    return ApiResponse.success("Product restored", ProductResponseDeleteAt.from(data));
                })
                .recover(err -> {
                    logger.error("Failed to restore product", err);
                    tracingMetrics.completeSpanError(tracingContext, "restore", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to restore: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<Boolean>> deletePermanent(Long id) {
        TracingMetrics.TracingContext tracingContext = tracingMetrics
                .startSpan("ProductCommandService.deletePermanent");
        Span span = Span.fromContext(tracingContext.getContext());
        span.setAttribute("id", id);

        logger.info("Deleting product permanently: {}", id);

        return repository.deleteProductPermanently(id)
                .compose(v -> redisService.delete("product:" + id).map(v))
                .map(v -> {
                    tracingMetrics.completeSpanSuccess(tracingContext, "delete_permanent", "Success");
                    return ApiResponse.success("Product deleted permanently", true);
                })
                .recover(err -> {
                    logger.error("Failed to delete product permanently", err);
                    tracingMetrics.completeSpanError(tracingContext, "delete_permanent", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to delete: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<Boolean>> restoreAll() {
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("ProductCommandService.restoreAll");
        logger.info("Restoring all products");
        return repository.restoreAllProducts()
                .map(v -> {
                    tracingMetrics.completeSpanSuccess(tracingContext, "restore_all", "Success");
                    return ApiResponse.success("All products restored", true);
                })
                .recover(err -> {
                    logger.error("Failed to restore all products", err);
                    tracingMetrics.completeSpanError(tracingContext, "restore_all", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to restore all: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<Boolean>> deleteAllPermanent() {
        TracingMetrics.TracingContext tracingContext = tracingMetrics
                .startSpan("ProductCommandService.deleteAllPermanent");
        logger.info("Deleting all products permanently");
        return repository.deleteAllPermanentProducts()
                .map(v -> {
                    tracingMetrics.completeSpanSuccess(tracingContext, "delete_all", "Success");
                    return ApiResponse.success("All products deleted permanently", true);
                })
                .recover(err -> {
                    logger.error("Failed to delete all products permanently", err);
                    tracingMetrics.completeSpanError(tracingContext, "delete_all", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to delete all: " + err.getMessage()));
                });
    }
}
