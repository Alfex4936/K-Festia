package csw.korea.festival.main.config;

import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

@Configuration
public class TomcatVirtualThreadConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            ProtocolHandler protocolHandler = connector.getProtocolHandler();

            if (protocolHandler instanceof Http11NioProtocol httpProtocol) {
                httpProtocol.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

                httpProtocol.setConnectionTimeout(20000); // 20 seconds
                httpProtocol.setKeepAliveTimeout(15000); // 15 seconds
                httpProtocol.setMaxConnections(10000);     // Max concurrent connections
                httpProtocol.setMaxThreads(0);             // Unlimited threads (handled by virtual threads)
                httpProtocol.setMinSpareThreads(0);        // Let virtual threads handle spares

                httpProtocol.setMaxHeaderCount(200);
                httpProtocol.setMaxHttpHeaderSize(32768);

                // Enable GZIP Compression
//                connector.setProperty("compression", "on");
//                connector.setProperty("compressableMimeType",
//                        "text/html,text/xml,text/plain,application/json,application/xml");
//                connector.setProperty("compressionMinSize", "2048"); // Compress responses larger than 2KB
            }
        });
    }
}
