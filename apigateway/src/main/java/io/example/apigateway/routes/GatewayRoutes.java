package io.example.apigateway.routes;

import io.example.apigateway.handler.AuthProxyHandler;
import io.example.apigateway.handler.CashierProxyHandler;
import io.example.apigateway.handler.CategoryProxyHandler;
import io.example.apigateway.handler.MerchantProxyHandler;
import io.example.apigateway.handler.OrderItemProxyHandler;
import io.example.apigateway.handler.OrderProxyHandler;
import io.example.apigateway.handler.ProductProxyHandler;
import io.example.apigateway.handler.RoleProxyHandler;
import io.example.apigateway.handler.TransactionProxyHandler;
import io.example.apigateway.handler.UserProxyHandler;
import io.example.apigateway.middleware.JwtMiddleware;
import io.example.apigateway.middleware.RoleMiddleware;
import io.example.common.chaos.ChaosManager;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import pb.merchant.VertxMerchantQueryServiceGrpcClient;

public final class GatewayRoutes {
        private GatewayRoutes() {
        }

        public static Router register(
                        Router router,
                        JWTAuth jwtAuth,
                        VertxMerchantQueryServiceGrpcClient merchantQueryClient,
                        AuthProxyHandler auth,
                        UserProxyHandler user,
                        RoleProxyHandler role,
                        MerchantProxyHandler merchant,
                        TransactionProxyHandler transaction,
                        CashierProxyHandler cashier,
                        CategoryProxyHandler category,
                        ProductProxyHandler product,
                        OrderProxyHandler order,
                        OrderItemProxyHandler orderItem,
                        ChaosManager chaosManager) {

                // 1. Global middleware (BodyParser is required for all JSON posts)
                router.route().handler(BodyHandler.create());

                // 2. Public / Health routes
                router.get("/health").handler(ctx -> ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end("{\"status\":\"UP\",\"service\":\"gateway\"}"));

                // =========================================================================
                // AUTH ROUTES (No API prefix in monolithic routes)
                // =========================================================================
                router.post("/register").handler(auth::register);
                router.post("/login").handler(auth::login);
                router.post("/refresh-token").handler(auth::refreshToken);
                router.get("/me").handler(JwtMiddleware.jwt(jwtAuth)).handler(auth::getMe);
                router.get("/logout").handler(JwtMiddleware.jwt(jwtAuth)).handler(auth::logout);

                // =========================================================================
                // USER ROUTES (JWT guarded, Admin only for list views)
                // =========================================================================
                router.route("/users*").handler(JwtMiddleware.jwt(jwtAuth));
                router.get("/users/active").handler(RoleMiddleware.requireRole("ADMIN")).handler(user::findActive);
                router.get("/users/trashed").handler(RoleMiddleware.requireRole("ADMIN")).handler(user::findTrashed);
                router.get("/users").handler(RoleMiddleware.requireRole("ADMIN")).handler(user::findAll);
                router.get("/users/:id").handler(user::findById);
                router.post("/users/update/:id").handler(user::update);
                router.post("/users/restore/:id").handler(user::restore);
                router.post("/users/trashed/:id").handler(user::trashed);
                router.delete("/users/deletePermanent/:id").handler(user::deletePermanent);
                router.post("/users/restore-all").handler(user::restoreAllUsers);
                router.post("/users/delete-all").handler(user::deleteAllPermanentUsers);

                // =========================================================================
                // ROLE ROUTES (JWT guarded, Admin restricted)
                // =========================================================================
                router.route("/roles*").handler(JwtMiddleware.jwt(jwtAuth));
                router.get("/roles/active").handler(RoleMiddleware.requireRole("ADMIN")).handler(role::findActive);
                router.get("/roles/trashed").handler(RoleMiddleware.requireRole("ADMIN")).handler(role::findTrashed);
                router.get("/roles").handler(RoleMiddleware.requireRole("ADMIN")).handler(role::findAll);
                router.get("/roles/:id").handler(role::findById);
                router.post("/roles").handler(role::create);
                router.post("/roles/restore-all").handler(role::restoreAllRoles);
                router.post("/roles/delete-all").handler(role::deleteAllPermanentRoles);
                router.post("/roles/:id").handler(role::update);
                router.post("/roles/restore/:id").handler(role::restore);
                router.post("/roles/trashed/:id").handler(role::trashed);
                router.delete("/roles/deletePermanent/:id").handler(role::deletePermanent);

                // =========================================================================
                // MERCHANT ROUTES (/api/merchants* prefix)
                // =========================================================================
                router.route("/api/merchants*").handler(JwtMiddleware.jwt(jwtAuth));

                // Queries
                router.get("/api/merchants").handler(merchant::getAllMerchants);
                router.get("/api/merchants/active").handler(merchant::getActiveMerchants);
                router.get("/api/merchants/trashed").handler(merchant::getTrashedMerchants);
                router.get("/api/merchants/:merchantId").handler(merchant::getMerchantById);

                // Commands
                router.post("/api/merchants").handler(merchant::createMerchant);
                router.put("/api/merchants").handler(merchant::updateMerchant);
                router.patch("/api/merchants/status").handler(merchant::updateMerchantStatus);

                // Lifecycle
                router.patch("/api/merchants/trash/:merchantId").handler(merchant::trashMerchant);
                router.patch("/api/merchants/restore/:merchantId").handler(merchant::restoreMerchant);
                router.delete("/api/merchants/permanent/:merchantId").handler(merchant::deleteMerchantPermanently);
                router.post("/api/merchants/restore-all").handler(merchant::restoreAllMerchants);
                router.delete("/api/merchants/delete-all").handler(merchant::deleteAllPermanentMerchants);

                // =========================================================================
                // MERCHANT DOCUMENTS (/api/merchant-documents* prefix)
                // =========================================================================
                router.route("/api/merchant-documents*").handler(JwtMiddleware.jwt(jwtAuth));
                router.get("/api/merchant-documents").handler(merchant::getAllMerchantDocuments);
                router.get("/api/merchant-documents/active").handler(merchant::getActiveMerchantDocuments);
                router.get("/api/merchant-documents/trashed").handler(merchant::getTrashedMerchantDocuments);
                router.get("/api/merchant-documents/:documentId").handler(merchant::getMerchantDocumentById);
                router.post("/api/merchant-documents").handler(merchant::createMerchantDocument);
                router.put("/api/merchant-documents/:documentId").handler(merchant::updateMerchantDocument);
                router.patch("/api/merchant-documents/:documentId/status")
                                .handler(merchant::updateMerchantDocumentStatus);
                router.patch("/api/merchant-documents/:documentId/trash").handler(merchant::trashMerchantDocument);
                router.patch("/api/merchant-documents/:documentId/restore").handler(merchant::restoreMerchantDocument);
                router.delete("/api/merchant-documents/:documentId/permanent")
                                .handler(merchant::deleteMerchantDocumentPermanently);
                router.patch("/api/merchant-documents/restore-all").handler(merchant::restoreAllMerchantDocuments);
                router.delete("/api/merchant-documents/permanent-all")
                                .handler(merchant::deleteAllPermanentMerchantDocuments);

                // =========================================================================
                // TRANSACTION ROUTES (/transactions* prefix)
                // =========================================================================
                router.route("/transactions*").handler(JwtMiddleware.jwt(jwtAuth));
                router.get("/transactions").handler(transaction::getTransactions);
                router.get("/transactions/active").handler(transaction::getActiveTransactions);
                router.get("/transactions/trashed").handler(transaction::getTrashedTransactions);

                // Transaction stats (Status Success/Failed)
                router.get("/transactions/monthly-success").handler(transaction::getMonthTransactionStatusSuccess);
                router.get("/transactions/yearly-success").handler(transaction::getYearlyTransactionStatusSuccess);
                router.get("/transactions/monthly-failed").handler(transaction::getMonthTransactionStatusFailed);
                router.get("/transactions/yearly-failed").handler(transaction::getYearlyTransactionStatusFailed);

                // Transaction stats (Payment Methods)
                router.get("/transactions/monthly-methods").handler(transaction::getMonthlyPaymentMethods);
                router.get("/transactions/yearly-methods").handler(transaction::getYearlyPaymentMethods);

                // Transaction stats by merchant (Status)
                router.get("/transactions/monthly-success-by-merchant/:merchantId")
                                .handler(transaction::getMonthTransactionStatusSuccessByMerchant);
                router.get("/transactions/yearly-success-by-merchant/:merchantId")
                                .handler(transaction::getYearlyTransactionStatusSuccessByMerchant);
                router.get("/transactions/monthly-failed-by-merchant/:merchantId")
                                .handler(transaction::getMonthTransactionStatusFailedByMerchant);
                router.get("/transactions/yearly-failed-by-merchant/:merchantId")
                                .handler(transaction::getYearlyTransactionStatusFailedByMerchant);

                // Transaction stats by merchant (Payment Methods)
                router.get("/transactions/monthly-methods-by-merchant/:merchantId")
                                .handler(transaction::getMonthlyPaymentMethodsByMerchant);
                router.get("/transactions/yearly-methods-by-merchant/:merchantId")
                                .handler(transaction::getYearlyPaymentMethodsByMerchant);

                // Lifecycle commands
                router.post("/transactions/trash/:transactionId").handler(transaction::trashTransaction);
                router.post("/transactions/restore/:transactionId").handler(transaction::restoreTransaction);
                router.delete("/transactions/permanent/:transactionId")
                                .handler(transaction::deleteTransactionPermanently);
                router.post("/transactions/restore-all").handler(transaction::restoreAllTransactions);
                router.delete("/transactions/permanent-all").handler(transaction::deleteAllPermanentTransactions);

                router.post("/transactions/create").handler(transaction::createTransaction);
                router.post("/transactions/update").handler(transaction::updateTransaction);
                router.get("/transactions/:transactionId").handler(transaction::getTransactionById);

                // =========================================================================
                // CASHIER ROUTES (/api/cashiers* prefix)
                // =========================================================================
                router.route("/api/cashiers*").handler(JwtMiddleware.jwt(jwtAuth));
                router.get("/api/cashiers").handler(cashier::findAll);
                router.get("/api/cashiers/active").handler(cashier::findActive);
                router.get("/api/cashiers/trashed").handler(cashier::findTrashed);
                router.get("/api/cashiers/:id").handler(cashier::findById);
                router.get("/api/cashiers/by-merchant/:merchantId").handler(cashier::findByMerchant);
                router.post("/api/cashiers").handler(cashier::create);
                router.put("/api/cashiers/:id").handler(cashier::update);
                router.post("/api/cashiers/restore/:id").handler(cashier::restore);
                router.post("/api/cashiers/trashed/:id").handler(cashier::trashed);
                router.delete("/api/cashiers/deletePermanent/:id").handler(cashier::deletePermanent);
                router.post("/api/cashiers/restore-all").handler(cashier::restoreAll);
                router.post("/api/cashiers/delete-all").handler(cashier::deleteAllPermanent);
                // Cashier Stats
                router.get("/api/cashiers/stats/monthly-total-sales").handler(cashier::findMonthlyTotalSales);
                router.get("/api/cashiers/stats/yearly-total-sales").handler(cashier::findYearlyTotalSales);
                router.get("/api/cashiers/stats/monthly-total-sales-by-id/:cashierId")
                                .handler(cashier::findMonthlyTotalSalesById);
                router.get("/api/cashiers/stats/yearly-total-sales-by-id/:cashierId")
                                .handler(cashier::findYearlyTotalSalesById);
                router.get("/api/cashiers/stats/monthly-total-sales-by-merchant/:merchantId")
                                .handler(cashier::findMonthlyTotalSalesByMerchant);
                router.get("/api/cashiers/stats/yearly-total-sales-by-merchant/:merchantId")
                                .handler(cashier::findYearlyTotalSalesByMerchant);

                // =========================================================================
                // CATEGORY ROUTES (/api/categories* prefix)
                // =========================================================================
                router.route("/api/categories*").handler(JwtMiddleware.jwt(jwtAuth));
                router.get("/api/categories").handler(category::findAll);
                router.get("/api/categories/active").handler(category::findActive);
                router.get("/api/categories/trashed").handler(category::findTrashed);
                router.get("/api/categories/:id").handler(category::findById);
                router.post("/api/categories").handler(category::create);
                router.put("/api/categories/:id").handler(category::update);
                router.post("/api/categories/restore/:id").handler(category::restore);
                router.post("/api/categories/trashed/:id").handler(category::trashed);
                router.delete("/api/categories/deletePermanent/:id").handler(category::deletePermanent);
                router.post("/api/categories/restore-all").handler(category::restoreAll);
                router.post("/api/categories/delete-all").handler(category::deleteAllPermanent);

                // =========================================================================
                // PRODUCT ROUTES (/api/products* prefix)
                // =========================================================================
                router.route("/api/products*").handler(JwtMiddleware.jwt(jwtAuth));
                router.get("/api/products").handler(product::findAll);
                router.get("/api/products/active").handler(product::findActive);
                router.get("/api/products/trashed").handler(product::findTrashed);
                router.get("/api/products/:id").handler(product::findById);
                router.get("/api/products/by-merchant/:merchantId").handler(product::findByMerchant);
                router.get("/api/products/by-category/:categoryName").handler(product::findByCategory);
                router.post("/api/products").handler(product::create);
                router.put("/api/products/:id").handler(product::update);
                router.post("/api/products/restore/:id").handler(product::restore);
                router.post("/api/products/trashed/:id").handler(product::trashed);
                router.delete("/api/products/deletePermanent/:id").handler(product::deletePermanent);
                router.post("/api/products/restore-all").handler(product::restoreAll);
                router.post("/api/products/delete-all").handler(product::deleteAllPermanent);

                // =========================================================================
                // ORDER ROUTES (/api/orders* prefix)
                // =========================================================================
                router.route("/api/orders*").handler(JwtMiddleware.jwt(jwtAuth));
                router.get("/api/orders").handler(order::findAll);
                router.get("/api/orders/active").handler(order::findActive);
                router.get("/api/orders/trashed").handler(order::findTrashed);
                router.get("/api/orders/:id").handler(order::findById);
                router.get("/api/orders/by-merchant/:merchantId").handler(order::findByMerchant);
                router.post("/api/orders").handler(order::create);
                router.put("/api/orders/:id").handler(order::update);
                router.post("/api/orders/restore/:id").handler(order::restore);
                router.post("/api/orders/trashed/:id").handler(order::trashed);
                router.delete("/api/orders/deletePermanent/:id").handler(order::deletePermanent);
                router.post("/api/orders/restore-all").handler(order::restoreAll);
                router.post("/api/orders/delete-all").handler(order::deleteAllPermanent);
                // Order stats
                router.get("/api/orders/stats/monthly-total-revenue").handler(order::findMonthlyTotalRevenue);
                router.get("/api/orders/stats/yearly-total-revenue").handler(order::findYearlyTotalRevenue);
                router.get("/api/orders/stats/monthly-total-revenue-by-id/:orderId")
                                .handler(order::findMonthlyTotalRevenueById);
                router.get("/api/orders/stats/yearly-total-revenue-by-id/:orderId")
                                .handler(order::findYearlyTotalRevenueById);
                router.get("/api/orders/stats/monthly-total-revenue-by-merchant/:merchantId")
                                .handler(order::findMonthlyTotalRevenueByMerchant);
                router.get("/api/orders/stats/yearly-total-revenue-by-merchant/:merchantId")
                                .handler(order::findYearlyTotalRevenueByMerchant);
                router.get("/api/orders/stats/monthly-revenue").handler(order::findMonthlyRevenue);
                router.get("/api/orders/stats/yearly-revenue").handler(order::findYearlyRevenue);
                router.get("/api/orders/stats/monthly-revenue-by-merchant/:merchantId")
                                .handler(order::findMonthlyRevenueByMerchant);
                router.get("/api/orders/stats/yearly-revenue-by-merchant/:merchantId")
                                .handler(order::findYearlyRevenueByMerchant);

                // =========================================================================
                // ORDER ITEM ROUTES (/api/order-items* prefix)
                // =========================================================================
                router.route("/api/order-items*").handler(JwtMiddleware.jwt(jwtAuth));
                router.get("/api/order-items").handler(orderItem::findAll);
                router.get("/api/order-items/active").handler(orderItem::findActive);
                router.get("/api/order-items/trashed").handler(orderItem::findTrashed);
                router.get("/api/order-items/by-order/:orderId").handler(orderItem::findByOrder);

                // =========================================================================
                // CHAOS CONTROL PLANE ROUTES
                // =========================================================================
                router.get("/api/chaos/policies").handler(ctx -> {
                        JsonArray policiesArr = new JsonArray();
                        chaosManager.getPolicies().forEach(policy -> {
                                policiesArr.add(JsonObject.mapFrom(policy));
                        });
                        ctx.response()
                                        .putHeader("Content-Type", "application/json")
                                        .end(new JsonObject().put("policies", policiesArr).encodePrettily());
                });

                router.post("/api/chaos/halt").handler(ctx -> {
                        chaosManager.halt();
                        ctx.response()
                                        .putHeader("Content-Type", "application/json")
                                        .end(new JsonObject().put("status", "success")
                                                        .put("message", "All chaos experiments halted")
                                                        .encodePrettily());
                });

                router.post("/api/chaos/policies/reload").handler(ctx -> {
                        chaosManager.loadConfig();
                        ctx.response()
                                        .putHeader("Content-Type", "application/json")
                                        .end(new JsonObject().put("status", "success")
                                                        .put("message", "Chaos policies reloaded").encodePrettily());
                });

                return router;
        }
}
