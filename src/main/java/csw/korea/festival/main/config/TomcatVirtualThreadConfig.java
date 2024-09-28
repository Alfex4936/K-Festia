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
            }
        });
    }
}
