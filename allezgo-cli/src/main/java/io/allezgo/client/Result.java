package io.allezgo.client;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.function.Function;

/** A Rust-inspired result-xor-error container. */
public record Result<T, E>(T result, E error, boolean isError) {
    public static <T, E> Result<T, E> ok(T result) {
        return new Result<>(result, null, false);
    }

    public static <T, E> Result<T, E> error(E error) {
        return new Result<>(null, error, true);
    }

    public Result {
        Preconditions.checkArgument(
                (!isError && result != null && error == null)
                        || (isError && result == null && error != null),
                "Result objects may contain strictly a result xor an error.");
    }

    public <U> Result<U, E> mapResult(Function<T, U> fn) {
        return !isError ? Result.ok(fn.apply(result)) : Result.error(error);
    }

    public <F> Result<T, F> mapError(Function<E, F> fn) {
        return isError ? Result.error(fn.apply(error)) : Result.ok(result);
    }

    public <U, F> Result<U, F> map(Function<T, U> resultFn, Function<E, F> errorFn) {
        return mapResult(resultFn).mapError(errorFn);
    }

    /** Returns an Optional containing the result if it's present or empty otherwise. */
    public Optional<T> asOptional() {
        return !isError ? Optional.of(result) : Optional.empty();
    }

    /** Returns an Optional containing the error if it's present or empty otherwise. */
    public Optional<E> asErrorOptional() {
        return isError ? Optional.of(error) : Optional.empty();
    }

    public <Exc extends Exception> T orElseThrow(Function<E, Exc> exceptionSupplier) throws Exc {
        if (isError) {
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
        if (isError) {
            throw new Exception(String.valueOf(error));
        }
        return result;
    }
}
