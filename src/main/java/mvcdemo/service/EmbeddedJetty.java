package mvcdemo.service;

import java.io.IOException;

import org.apache.jasper.servlet.JspServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class EmbeddedJetty {

    // https://wiki.eclipse.org/Jetty/Tutorial/Embedding_Jetty
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedJetty.class);
    
    private static final int PORT = 9290;
    
    private static final String CONTEXT_PATH = "/";
    private static final String CONFIG_LOCATION_PACKAGE = "com.fernandospr.example.config";
    private static final String MAPPING_URL = "/";
    private static final String WEBAPP_DIRECTORY = "webapp";
    
    public static void main(String[] args) throws Exception {
        new EmbeddedJetty().startJetty3(PORT);
    }

    private void startJetty3(int port) throws Exception {
        Server server = new Server(port);

        WebAppContext context = new WebAppContext();
        //context.setDescriptor(WEBAPP_DIRECTORY + "/WEB-INF/web.xml");
        //context.setDescriptor("/WEB-INF/web.xml");
        String webapp = new ClassPathResource(WEBAPP_DIRECTORY).getURI().toString();

        context.setDescriptor(webapp+"/WEB-INF/web.xml");
        context.setResourceBase(webapp);
        context.setContextPath("/");
        context.setParentLoaderPriority(true);

        context.setConfigurations(new Configuration[]
                {new WebXmlConfiguration(), new WebInfConfiguration()});
                //new PlusConfiguration(), new MetaInfConfiguration(), new FragmentConfiguration(), new EnvConfiguration() });

        server.setHandler(context);

        server.start();
        server.dump(System.err);
        server.join();
    }

    private void startJetty2(int port) throws Exception {
        Server server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new HelloServlet()),"/*");
        context.addServlet(new ServletHolder(new HelloServlet("Buongiorno Mondo")),"/it/*");
        context.addServlet(new ServletHolder(new HelloServlet("Bonjour le Monde")),"/fr/*");

        server.start();
        server.join();
    }
    private void startJetty1(int port) throws Exception {
        Server server = new Server(port);

        ContextHandler context = new ContextHandler();
        context.setContextPath("/hello");
        context.setResourceBase(".");
        context.setClassLoader(Thread.currentThread().getContextClassLoader());
        server.setHandler(context);

        context.setHandler(new HelloHandler());

        server.start();
        server.join();
    }

    private void xstartJetty(int port) throws Exception {
        LOGGER.debug("Starting server at port {}", port);
        Server server = new Server(port);
        
        server.setHandler(getServletContextHandler());
        
        addRuntimeShutdownHook(server);
        
        server.start();
        LOGGER.info("Server started at port {}", port);
        server.join();
    }

    private static ServletContextHandler getServletContextHandler() throws IOException {
        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS); // SESSIONS requerido para JSP 
        contextHandler.setErrorHandler(null);

        String webapp = new ClassPathResource(WEBAPP_DIRECTORY).getURI().toString();
        contextHandler.setResourceBase(webapp);
        //contextHandler.setResourceBase("file://home/rcs/opt/java/harborview/src/main/webapp");
        contextHandler.setContextPath(CONTEXT_PATH);
        
        // JSP
        //contextHandler.setClassLoader(Thread.currentThread().getContextClassLoader()); // Necesario para cargar JspServlet
        //contextHandler.addServlet(JspServlet.class, "*.jsp");
        
        // Spring
        /*
        WebApplicationContext webAppContext = getWebApplicationContext();
        DispatcherServlet dispatcherServlet = new DispatcherServlet(webAppContext);
        ServletHolder springServletHolder = new ServletHolder("mvc-dispatcher", dispatcherServlet);
        contextHandler.addServlet(springServletHolder, MAPPING_URL);
        contextHandler.addEventListener(new ContextLoaderListener(webAppContext));
        */

        return contextHandler;
    }

    private static WebApplicationContext getWebApplicationContext() {
        /*
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setConfigLocation(CONFIG_LOCATION_PACKAGE);
        return context;
        */
        XmlWebApplicationContext context = new XmlWebApplicationContext();
        context.setConfigLocation("/WEB-INF");
        return context;
    }
    
    private static void addRuntimeShutdownHook(final Server server) {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (server.isStarted()) {
                	server.setStopAtShutdown(true);
                    try {
                    	server.stop();
                    } catch (Exception e) {
                        System.out.println("Error while stopping jetty server: " + e.getMessage());
                        LOGGER.error("Error while stopping jetty server: " + e.getMessage(), e);
                    }
                }
            }
        }));
	}

}

/*
        For Example you can create VirtualHosts with ServletContextHandler and you can management context easily. That means different context handlers on different ports.

        Server server = new Server();
        ServerConnector pContext = new ServerConnector(server);
        pContext.setPort(8080);
        pContext.setName("Public");
        ServerConnector localConn = new ServerConnector(server);
        localConn.setPort(9090);
        localConn.setName("Local");

        ServletContextHandler publicContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        publicContext.setContextPath("/");
        ServletHolder sh = new ServletHolder(new HttpServletDispatcher());  sh.setInitParameter("javax.ws.rs.Application", "ServiceListPublic");
        publicContext.addServlet(sh, "/*");
        publicContext.setVirtualHosts(new String[]{"@Public"});


        ServletContextHandler localContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        localContext .setContextPath("/");
        ServletHolder shl = new ServletHolder(new HttpServletDispatcher()); shl.setInitParameter("javax.ws.rs.Application", "ServiceListLocal");
        localContext.addServlet(shl, "/*");
        localContext.setVirtualHosts(new String[]{"@Local"}); //see localConn.SetName


        HandlerCollection collection = new HandlerCollection();
        collection.addHandler(publicContext);
        collection.addHandler(localContext);
        server.setHandler(collection);
        server.addConnector(pContext);
        server.addConnector(localContext);
 */
