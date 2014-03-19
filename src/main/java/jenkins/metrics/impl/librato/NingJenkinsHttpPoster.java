package jenkins.metrics.impl.librato;

import com.librato.metrics.Authorization;
import com.librato.metrics.HttpPoster;
import com.ning.http.client.AsyncHttpClient;
import jenkins.plugins.asynchttpclient.AHC;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An HTTPPoster which uses ning's HTTP client.
 * <p/>
 *
 * Copy of the {@code com.librato.metrics.NingHttpPoster} using the {@link com.ning.http.client.AsyncHttpClient}
 * provided by the Jenkins <a href="https://wiki.jenkins-ci.org/display/JENKINS/Async+Http+Client+Plugin">
 * Jenkins Async Http Client Plugin</a>.
 *
 * @author Cyrille Le Clerc
 */
public class NingJenkinsHttpPoster implements HttpPoster {
    private final AsyncHttpClient httpClient = AHC.instance();
    private final String authHeader;

    public NingJenkinsHttpPoster(String authHeader) {
        this.authHeader = authHeader;
    }

    /**
     * Return a new poster with the correct authentication header.
     *
     * @param username the librato username
     * @param token    the librato token
     * @return a new NingHttpPoster
     */
    public static NingJenkinsHttpPoster newPoster(String username, String token) {
        return new NingJenkinsHttpPoster(Authorization.buildAuthHeader(username, token));
    }

    /**
     * Adapts a ning {@link com.ning.http.client.Response} future to a @{link Response} future
     */
    static class FutureAdapter implements Future<Response> {
        private final Future<com.ning.http.client.Response> delegate;

        FutureAdapter(Future<com.ning.http.client.Response> delegate) {
            this.delegate = delegate;
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            return delegate.cancel(mayInterruptIfRunning);
        }

        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        public boolean isDone() {
            return delegate.isDone();
        }

        public Response get() throws InterruptedException, ExecutionException {
            return adapt(delegate.get());
        }


        public Response get(long timeout, @SuppressWarnings("NullableProblems") TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return adapt(delegate.get(timeout, unit));
        }

        private Response adapt(final com.ning.http.client.Response response) {
            return new Response() {
                public int getStatusCode() {
                    return response.getStatusCode();
                }

                public String getBody() throws IOException {
                    return response.getResponseBody();
                }
            };
        }
    }

    public Future<Response> post(String userAgent, String payload) throws IOException {
        final AsyncHttpClient.BoundRequestBuilder builder = httpClient.preparePost(API_URL);
        builder.addHeader("Content-Type", "application/json");
        builder.addHeader("Authorization", authHeader);
        builder.addHeader("User-Agent", userAgent);
        builder.setBody(payload);
        return new FutureAdapter(builder.execute());
    }
}
