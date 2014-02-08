package org.usrz.libs.httpd.starter;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.glassfish.grizzly.WriteHandler;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.io.NIOOutputStream;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.ContentType;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.http.util.MimeType;
import org.usrz.libs.crypto.codecs.HexCodec;
import org.usrz.libs.crypto.utils.KeyPairBuilder;
import org.usrz.libs.crypto.utils.StringCipherBuilder;
import org.usrz.libs.httpd.inject.SSLPasswordCallback;
import org.usrz.libs.logging.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class SSLServerStarterHandler extends HttpHandler {

    /* Static field declaration */
    private static final Log log = new Log();
    private static final Charset UTF8 = Charset.forName("UTF8");
    private static final ContentType APPLICATION_JSON = ContentType.newContentType("application/json", UTF8.name());
    private static final ContentType APPLICATION_OCTET_STREAM = ContentType.newContentType("application/octet-stream");

    /* Our injector, to stop the server when done */
    private final Injector injector;

    /* Our password callback */
    private final SSLPasswordCallback callback;

    /* NOTE: two people submitting the passwoard at the same time will fail */
    private PrivateKey privateKey;

    @Inject
    protected SSLServerStarterHandler(Injector injector, SSLPasswordCallback callback) {
        this.injector = injector;
        this.callback = callback;
    }

    @Override
    public void service(Request request, Response response)
    throws Exception {
        String pathInfo = request.getPathInfo();
        if ("/".equals(pathInfo)) pathInfo = "/index.html";

        if ("/pass".equals(pathInfo)) {
            if (Method.POST.equals(request.getMethod())) {
                new Thread(new PasswordHandler(response, request.getParameter("password"))).start();
            } else {
                response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405);
            }
            return;
        }

        /* POST is only valid for "/pass" all the rest is only GET */
        if (!Method.GET.equals(request.getMethod())) {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405);
            return;
        }

        /* If this is a key request, start a key generator */
        if ("/key".equals(pathInfo)) {
            new Thread(new KeyGenerator(response)).start();
            return;
        }

        /* All the rest is definitely a file (read from resources) */
        final URL url = this.getClass().getResource("site" + pathInfo);
        if (url == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return;
        }

        /* Get the mime type, default to binary, text is always UTF8 */
        final String mimeType = MimeType.getByFilename(pathInfo);
        response.setContentType(mimeType == null ? APPLICATION_OCTET_STREAM :
                                mimeType.startsWith("text/") ? ContentType.newContentType(mimeType, UTF8.name()) :
                                ContentType.newContentType(mimeType));

        /* Open the URL connection to the resource, set content length */
        final URLConnection connection = url.openConnection();
        final long contentLength = connection.getContentLengthLong();
        if (contentLength >= 0) response.setContentLengthLong(contentLength);

        /* Use an asynchronous writing handler to send the response */
        new WritingHandler(response, connection.getInputStream());
    }

    /* ====================================================================== */
    /* KEY GENERATOR                                                          */
    /* ====================================================================== */

    private class KeyGenerator implements Runnable {
        private final Response response;

        private KeyGenerator(Response response) {
            /* Suspend and save the response */
            response.suspend();
            this.response = response;
        }

        @Override
        public void run() {
            try {
                /* Generate our RSA private/public key pair */
                final KeyPair keyPair = new KeyPairBuilder().algorithm("RSA").keySize(2048).build();
                final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
                privateKey = keyPair.getPrivate();

                /* Create our our JSON response */
                final StringWriter writer = new StringWriter();
                final JsonGenerator generator = new JsonFactory().createGenerator(writer);
                generator.writeStartObject();
                generator.writeStringField("modulus", HexCodec.HEX.encode(publicKey.getModulus().toByteArray()));
                generator.writeStringField("exponent", HexCodec.HEX.encode(publicKey.getPublicExponent().toByteArray()));
                generator.writeEndObject();
                generator.close();

                /* Send our JSON back to the caller */
                final byte[] json = writer.toString().getBytes(UTF8);
                response.setContentType(ContentType.newContentType("application/json", UTF8.name()));
                response.setContentLength(json.length);
                response.getOutputStream().write(json);

            } catch (Throwable throwable) {
                log.error("Exception caught generating RSA keypair", throwable);
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);

            } finally {
                /* Always resume the response */
                response.resume();
            }
        }

    }

    /* ====================================================================== */
    /* PASSWORD CALLBACK                                                      */
    /* ====================================================================== */

    private class PasswordHandler implements Runnable {
        private final Response response;
        private final String password;

        private PasswordHandler(Response response, String password) {
            /* Suspend and save the response */
            response.suspend();
            this.response = response;
            this.password = password;
        }

        @Override
        public void run() {
            try {
                final StringWriter writer = new StringWriter();
                final JsonGenerator generator = new JsonFactory().createGenerator(writer);
                generator.writeStartObject();

                try {
                    final char[] decrypted = new StringCipherBuilder("RSA")
                                                .key(privateKey)
                                                .decipher()
                                                .transform(password)
                                                .toCharArray();
                    callback.setPassword(decrypted, decrypted, decrypted);
                    generator.writeStringField("result", "complete");

                    /* Shut down our server starter */
                    injector.getInstance(HttpServer.class).shutdown();

                } catch (Throwable throwable) {
                    log.error("Exception caught handling password", throwable);
                    generator.writeStringField("result", "continue");
                }

                /* Close the obhect */
                generator.writeEndObject();
                generator.close();

                /* Send our JSON back to the caller */
                final byte[] json = writer.toString().getBytes(UTF8);
                response.setContentType(ContentType.newContentType("application/json", UTF8.name()));
                response.setContentLength(json.length);
                response.getOutputStream().write(json);

            } catch (IOException exception) {
                /* Hmmm.. Some error was thrown by Jackson */
                log.error("I/O error sending confirmation", exception);
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);

            } finally {
                /* Always resume the response */
                response.resume();
            }
        }
    }

    /* ====================================================================== */
    /* WRITE RESOURCES                                                        */
    /* ====================================================================== */

    private class WritingHandler implements WriteHandler {

        private final byte[] buffer = new byte[8192];
        private final NIOOutputStream output;
        private final InputStream input;
        private final Response response;

        private WritingHandler(Response response, InputStream input) {
            /* Suspend the response */
            response.suspend();

            /* Save response and input stream */
            this.response = response;
            this.input = input;

            /* Save output stream and set up notification */
            output = response.getNIOOutputStream();
            output.notifyCanWrite(this);
        }

        @Override
        public void onWritePossible()
        throws Exception {
            final int read = input.read(buffer);
            if (read > 0) output.write(buffer, 0, read);

            if (read >= 0) {
                output.notifyCanWrite(this);
            } else try {
                input.close();
            } finally {
                response.resume();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace(System.err);
            try {
                input.close();
            } catch (Exception exception) {
                System.err.println("Closing...");
                exception.printStackTrace(System.err);
            }
        }

    }

}
