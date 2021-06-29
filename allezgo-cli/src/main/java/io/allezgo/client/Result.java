package io.allezgo.client;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.function.Function;

/**
 * A Rust-inspired result-xor-error container.
 *
 * <p>Note that one of {@code result} and {@code error} must be non-null. Callers should prefer to
 * use one of the static constructors {@link #ok(Object)} or {@link #error(Object)} rather than
 * directly initializing this record.
 */
public record Result<T, E>(T result, E error) {
    public static <T, E> Result<T, E> ok(T result) {
        return new Result<>(result, null);
    }

    public static <T, E> Result<T, E> error(E error) {
        return new Result<>(null, error);
    }

    public Result {
        Preconditions.checkArgument(
                result != null ^ error != null,
                "Result objects may contain strictly a result xor an error.");
    }

    public <U> Result<U, E> mapResult(Function<T, U> fn) {
        return !isError() ? Result.ok(fn.apply(result)) : Result.error(error);
    }

    public <F> Result<T, F> mapError(Function<E, F> fn) {
        return isError() ? Result.error(fn.apply(error)) : Result.ok(result);
    }

    public <U, F> Result<U, F> map(Function<T, U> resultFn, Function<E, F> errorFn) {
        return mapResult(resultFn).mapError(errorFn);
    }

    public boolean isError() {
        return error != null;
    }

    /** Returns an Optional containing the result if it's present or empty otherwise. */
    public Optional<T> asOptional() {
        return result != null ? Optional.of(result) : Optional.empty();
    }

    /** Returns an Optional containing the error if it's present or empty otherwise. */
    public Optional<E> asErrorOptional() {
        return error != null ? Optional.of(error) : Optional.empty();
    }

    public <X extends Exception> T orElseThrow(Function<E, X> exceptionSupplier) throws X {
        if (isError()) {
            throw exceptionSupplier.apply(error);
        }
        return result;
    }

    /**
     * Throws an exception with a string rendering of the error type. This is provided as an
     * ergonomic method to avoid boilerplate, but most callers should prefer {@link
     * #orElseThrow(Function)}.
     */
    public T orElseThrow() throws Exception {
        if (isError()) {
            throw new Exception(String.valueOf(error));
        }
        return result;
    }
}
