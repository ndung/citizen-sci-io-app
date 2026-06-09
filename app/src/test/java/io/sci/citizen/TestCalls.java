package io.sci.citizen;

import java.io.IOException;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;

public final class TestCalls {
    private TestCalls() {
    }

    public static <T> Call<T> success(T body) {
        return new ImmediateCall<>(retrofit2.Response.success(body), null);
    }

    public static <T> Call<T> failure(Throwable error) {
        return new ImmediateCall<>(null, error);
    }

    private static final class ImmediateCall<T> implements Call<T> {
        private final retrofit2.Response<T> response;
        private final Throwable error;
        private boolean executed;
        private boolean canceled;

        private ImmediateCall(retrofit2.Response<T> response, Throwable error) {
            this.response = response;
            this.error = error;
        }

        @Override
        public retrofit2.Response<T> execute() throws IOException {
            executed = true;
            if (error != null) {
                throw new IOException(error);
            }
            return response;
        }

        @Override
        public void enqueue(Callback<T> callback) {
            executed = true;
            if (error != null) {
                callback.onFailure(this, error);
            } else {
                callback.onResponse(this, response);
            }
        }

        @Override
        public boolean isExecuted() {
            return executed;
        }

        @Override
        public void cancel() {
            canceled = true;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public Call<T> clone() {
            return new ImmediateCall<>(response, error);
        }

        @Override
        public Request request() {
            return new Request.Builder().url("http://localhost/").build();
        }

        @Override
        public Timeout timeout() {
            return new Timeout();
        }
    }
}
