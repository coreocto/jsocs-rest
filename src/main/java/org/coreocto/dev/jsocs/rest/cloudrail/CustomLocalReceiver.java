package org.coreocto.dev.jsocs.rest.cloudrail;

import com.cloudrail.si.servicecode.commands.awaitCodeRedirect.RedirectReceiver;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public class CustomLocalReceiver implements RedirectReceiver {

    private int port;
    private String answer;
    private String webDriverExe;

    public CustomLocalReceiver(int port) {
        this(port, "<h1>Please close this window!</h1>");
    }

    public CustomLocalReceiver(int port, String answer) {
        this.port = port;
        this.answer = answer;
    }

    public CustomLocalReceiver(int port, String answer, String webDriverExe) {
        this.port = port;
        this.answer = answer;
        this.webDriverExe = webDriverExe;
    }

    public String openAndAwait(String url, String currentState) {
        final AtomicReference ar = new AtomicReference();

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(this.port), 0);
            server.createContext("/", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    String uri = "http://localhost:" + CustomLocalReceiver.this.port + exchange.getRequestURI().toString();
                    exchange.sendResponseHeaders(200, (long) CustomLocalReceiver.this.answer.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(CustomLocalReceiver.this.answer.getBytes());
                    os.close();
                    AtomicReference var4 = ar;
                    synchronized (ar) {
                        ar.set(uri);
                        ar.notify();
                    }
                }
            });
            server.setExecutor((Executor) null);
            server.start();

            // basically this class is same as the LocalReceiver class from cloudrail sdk
            // the difference is this class use selenium for authentication
            // because the java.awt.Desktop is not compatible with Spring Boot applications
            System.setProperty("webdriver.chrome.driver", webDriverExe);
//            WebDriver webDriver = new FirefoxDriver();
            WebDriver webDriver = new ChromeDriver();
            webDriver.get(url);

//            Desktop desktop = Desktop.getDesktop();
//            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
//            if (desktop != null && desktop.isSupported(Action.BROWSE)) {
//                try {
//                    desktop.browse(new URI(url));
//                } catch (Exception var8) {
//                    var8.printStackTrace();
//                }
//            }

            synchronized (ar) {
                while (ar.get() == null) {
                    ar.wait();
                }
            }

            server.stop(0);

            webDriver.close();

        } catch (Exception var10) {
            var10.printStackTrace();
        }

        return (String) ar.get();
    }
}
