package heo.router;

import heo.exception.MethodNotAllowError;
import heo.exception.NotFoundError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RouterTest {
    private Router router;

    @BeforeEach
    void setup() {
        router = new Router();
    }

    @Test
    void shouldAddAndFindGetRoute() {
        router.get("/products", (req, res, next) -> {});
        router.get("/products/:id", (req, res, next) -> {});
        router.get("/products/:id/reviews", (req, res, next) -> {});
        router.get("/products/:id/reviews/:reviewId", (req, res, next) -> {});
        router.get("/products/:id/reviews/:reviewId/comments", (req, res, next) -> {});
        router.get("/products/:id/reviews/:reviewId/comments/:commentId", (req, res, next) -> {});
        router.post("/products", (req, res, next) -> {});
        router.post("/products/:id/reviews", (req, res, next) -> {});
        router.post("/products/:id/reviews/:reviewId/comments", (req, res, next) -> {});
        router.put("/products/:id", (req, res, next) -> {});
        router.put("/products/:id/reviews/:reviewId", (req, res, next) -> {});
        router.patch("/products/:id", (req, res, next) -> {});
        router.patch("/products/:id/reviews/:reviewId", (req, res, next) -> {});
        router.delete("/products/:id", (req, res, next) -> {});
        router.delete("/products/:id/reviews/:reviewId", (req, res, next) -> {});
        router.delete("/products/:id/reviews/:reviewId/comments/:commentId", (req, res, next) -> {});
        assertNotNull(router.search("/products", "GET"));
        assertNotNull(router.search("/products/123", "GET"));
        assertNotNull(router.search("/products/123/reviews", "GET"));
        assertNotNull(router.search("/products/123/reviews/456", "GET"));
        assertNotNull(router.search("/products/123/reviews/456/comments", "GET"));
        assertNotNull(router.search("/products/123/reviews/456/comments/789", "GET"));
        assertNotNull(router.search("/products", "POST"));
        assertNotNull(router.search("/products/123/reviews", "POST"));
        assertNotNull(router.search("/products/123/reviews/456/comments", "POST"));
        assertNotNull(router.search("/products/123", "PUT"));
        assertNotNull(router.search("/products/123/reviews/456", "PUT"));
        assertNotNull(router.search("/products/123", "PATCH"));
        assertNotNull(router.search("/products/123/reviews/456", "PATCH"));
        assertNotNull(router.search("/products/123", "DELETE"));
        assertNotNull(router.search("/products/123/reviews/456", "DELETE"));
        assertNotNull(router.search("/products/123/reviews/456/comments/789", "DELETE"));
    }

    @Test
    void shouldThrowExceptionForNotFound() {
        Exception notFoundError = assertThrows(NotFoundError.class, () -> {
            router.search("/nonexistent", "GET");
        });
        assertEquals("Cannot GET /nonexistent", notFoundError.getMessage());
    }

    @Test
    void shouldThrowExceptionForMethodNotAllowed() {
        router.get("/test", (req, res, next) -> {});
        Exception methodNotAllowError = assertThrows(MethodNotAllowError.class, () -> {
            router.search("/test", "POST");
        });
        assertEquals("Cannot POST /test", methodNotAllowError.getMessage());
    }

    @Test
    void shouldAddSubRouter() {
        Router subRouter = new Router();
        subRouter.get("/sub", (req, res, next) -> {});
        subRouter.post("/sub", (req, res, next) -> {});
        subRouter.put("/sub", (req, res, next) -> {});
        subRouter.patch("/sub", (req, res, next) -> {});
        subRouter.delete("/sub", (req, res, next) -> {});
        router.use("/main", subRouter);

        assertNotNull(router.search("/main/sub", "GET"));
        assertNotNull(router.search("/main/sub", "POST"));
        assertNotNull(router.search("/main/sub", "PUT"));
        assertNotNull(router.search("/main/sub", "PATCH"));
        assertNotNull(router.search("/main/sub", "DELETE"));
        Exception notFoundError = assertThrows(NotFoundError.class, () -> {
            router.search("/main/nonexistent", "GET");
        });
        assertEquals("Cannot GET /main/nonexistent", notFoundError.getMessage());
    }
}