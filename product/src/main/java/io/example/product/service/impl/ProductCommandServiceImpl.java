package io.example.product.service.impl;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.product.domain.requests.CreateProductRequest;
import io.example.product.domain.requests.UpdateProductRequest;
import io.example.product.domain.response.ProductResponse;
import io.example.product.domain.response.ProductResponseDeleteAt;
import io.example.product.model.Product;
import io.example.product.repository.CategoryQueryRepository;
import io.example.product.repository.MerchantQueryRepository;
import io.example.product.repository.ProductCommandRepository;
import io.example.product.repository.ProductQueryRepository;
import io.example.product.service.ProductCommandService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ProductCommandServiceImpl implements ProductCommandService {
    private final ProductCommandRepository commandRepository;
    private final ProductQueryRepository queryRepository;
    private final CategoryQueryRepository categoryQueryRepository;
    private final MerchantQueryRepository merchantQueryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    private static final String CACHE_PREFIX = "product:";
    private static final String CACHE_LIST_PREFIX = "product:list:";

    @Override
    public Future<ProductResponse> create(CreateProductRequest req) {
        var ctx = tracingMetrics.startSpan("ProductCommandService.create",
                Attributes.builder().put("merchant.id", req.getMerchantId()).build());

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
                    return commandRepository.createProduct(req);
                })
                .compose(product -> invalidateListCache().<Product>map(v -> product))
                .map(ProductResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "create", "Product created successfully"))
                .onFailure(err -> {
                    log.error("Failed to create product", err);
                    tracingMetrics.completeSpanError(ctx, "create", err.getMessage());
                });
    }

    @Override
    public Future<ProductResponse> update(UpdateProductRequest req) {
        var ctx = tracingMetrics.startSpan("ProductCommandService.update",
                Attributes.builder().put("product.id", req.getProductId()).build());

        // Validate merchant/category only when provided
        Future<Void> validation = Future.succeededFuture();
        if (req.getMerchantId() != null && req.getMerchantId() > 0) {
            validation = validation.compose(v -> merchantQueryRepository.existsById(req.getMerchantId())
                    .compose(exists -> exists ? Future.succeededFuture()
                            : Future.failedFuture(new NotFoundException("Merchant not found"))));
        }
        if (req.getCategoryId() != null && req.getCategoryId() > 0) {
            validation = validation.compose(v -> categoryQueryRepository.existsById(req.getCategoryId())
                    .compose(exists -> exists ? Future.succeededFuture()
                            : Future.failedFuture(new NotFoundException("Category not found"))));
        }
        return validation
                .compose(v -> commandRepository.updateProduct(req))
                .compose(product -> {
                    if (product == null) {
                        return Future.failedFuture(new NotFoundException("Product not found"));
                    }
                    return invalidateCache(req.getProductId().longValue()).<Product>map(v -> product);
                })
                .map(ProductResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "update", "Product updated successfully"))
                .onFailure(err -> {
                    log.error("Failed to update product: {}", req.getProductId(), err);
                    tracingMetrics.completeSpanError(ctx, "update", err.getMessage());
                });
    }

    @Override
    public Future<ProductResponseDeleteAt> trash(Long id) {
        var ctx = tracingMetrics.startSpan("ProductCommandService.trash",
                Attributes.builder().put("product.id", id).build());

        return commandRepository.trashProduct(id)
                .compose(product -> {
                    if (product == null) {
                        return Future.failedFuture(new NotFoundException("Product not found"));
                    }
                    return invalidateCache(id).<Product>map(v -> product);
                })
                .map(ProductResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "trash", "Product trashed successfully"))
                .onFailure(err -> {
                    log.error("Failed to trash product: {}", id, err);
                    tracingMetrics.completeSpanError(ctx, "trash", err.getMessage());
                });
    }

    @Override
    public Future<ProductResponseDeleteAt> restore(Long id) {
        var ctx = tracingMetrics.startSpan("ProductCommandService.restore",
                Attributes.builder().put("product.id", id).build());

        return queryRepository.findByTrashedId(id)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.failedFuture(
                                new BadRequestException("Product not found or must be trashed first"));
                    }
                    return commandRepository.restoreProduct(id);
                })
                .compose(r -> {
                    if (r == null) {
                        return Future.<Product>failedFuture(new NotFoundException("Product not found"));
                    }
                    return invalidateCache(id).<Product>map(v -> r);
                })
                .map(ProductResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restore", "Product restored successfully"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "restore", e.getMessage()));
    }

    @Override
    public Future<Boolean> deletePermanent(Long id) {
        var ctx = tracingMetrics.startSpan("ProductCommandService.deletePermanent",
                Attributes.builder().put("product.id", id).build());

        return queryRepository.findByTrashedId(id)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.<Boolean>failedFuture(
                                new BadRequestException(
                                        "Product not found or must be trashed before permanent deletion"));
                    }
                    return commandRepository.deleteProductPermanently(id)
                            .compose(deleted -> {
                                if (!deleted) {
                                    return Future.<Boolean>failedFuture(
                                            new BadRequestException("Failed to delete product permanently"));
                                }
                                return invalidateCache(id).map(v -> true);
                            });
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "deletePermanent", "Product permanently deleted"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "deletePermanent", err.getMessage()));
    }

    @Override
    public Future<ProductResponse> decrementStock(Long productId, int quantity) {
        var ctx = tracingMetrics.startSpan("ProductCommandService.decrementStock",
                Attributes.builder().put("product.id", productId).build());

        return commandRepository.decrementStock(productId, quantity)
                .compose(product -> {
                    if (product == null) {
                        // Either the product does not exist or stock is insufficient.
                        return Future.failedFuture(new BadRequestException("Insufficient product stock"));
                    }
                    return invalidateCache(productId).<Product>map(v -> product);
                })
                .map(ProductResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "decrementStock", "Stock decremented"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "decrementStock", err.getMessage()));
    }

    @Override
    public Future<ProductResponse> incrementStock(Long productId, int quantity) {
        var ctx = tracingMetrics.startSpan("ProductCommandService.incrementStock",
                Attributes.builder().put("product.id", productId).build());

        return commandRepository.incrementStock(productId, quantity)
                .compose(product -> {
                    if (product == null) {
                        return Future.failedFuture(new NotFoundException("Product not found"));
                    }
                    return invalidateCache(productId).<Product>map(v -> product);
                })
                .map(ProductResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "incrementStock", "Stock incremented"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "incrementStock", err.getMessage()));
    }

    @Override
    public Future<Boolean> restoreAll() {
        var ctx = tracingMetrics.startSpan("ProductCommandService.restoreAll");

        return commandRepository.restoreAllProducts()
                .compose(count -> {
                    if (count == 0) {
                        return Future.failedFuture(new NotFoundException("No trashed products found"));
                    }
                    return invalidateListCache().map(v -> true);
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restore_all",
                        "All products restored successfully"))
                .onFailure(err -> {
                    log.error("Failed to restore all products", err);
                    tracingMetrics.completeSpanError(ctx, "restore_all", err.getMessage());
                });
    }

    @Override
    public Future<Boolean> deleteAllPermanent() {
        var ctx = tracingMetrics.startSpan("ProductCommandService.deleteAllPermanent");

        return commandRepository.deleteAllPermanentProducts()
                .compose(count -> {
                    if (count == 0) {
                        return Future.failedFuture(new NotFoundException("No trashed products found"));
                    }
                    return invalidateListCache().map(v -> true);
                })
                .onSuccess(
                        v -> tracingMetrics.completeSpanSuccess(ctx, "delete_all", "All products permanently deleted"))
                .onFailure(err -> {
                    log.error("Failed to permanently delete all products", err);
                    tracingMetrics.completeSpanError(ctx, "delete_all", err.getMessage());
                });
    }

    private Future<Void> invalidateCache(Long productId) {
        return redisService.delete(CACHE_PREFIX + productId)
                .compose(v -> redisService.deleteByPattern(CACHE_LIST_PREFIX + "*"))
                .<Void>mapEmpty();
    }

    private Future<Void> invalidateListCache() {
        return redisService.deleteByPattern(CACHE_LIST_PREFIX + "*").<Void>mapEmpty();
    }
}