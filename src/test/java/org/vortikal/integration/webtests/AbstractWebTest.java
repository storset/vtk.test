package org.vortikal.integration.webtests;

import java.util.Locale;
import java.util.Properties;

import junit.framework.AssertionFailedError;
import net.sourceforge.jwebunit.junit.WebTestCase;

import org.apache.commons.lang.StringUtils;

public abstract class AbstractWebTest extends WebTestCase {
    
    protected static final String rootCollection = "automatedtestresources";
    protected static final String PROP_ADMIN_URL = "admin.url";
    protected static final String PROP_ADMIN_USR = "admin.user";
    protected static final String PROP_ADMIN_PASSWORD = "admin.password";
    protected static final String PROP_VIEW_URL = "view.url";
    protected static final String PROP_WEBDAV_URL = "webdav.url";
    
    private static Properties props;
    private static final String propFile = "integration/webtests/webtests.properties";
    
    protected void setUp() throws Exception {
        super.setUp();
        
        if (props == null) {
            props = new Properties();
            props.load(this.getClass().getResourceAsStream( "/" + propFile));
        }
        
        getTestContext().setLocale(new Locale("en"));
        
        getTestContext().setBaseUrl(getBaseUrl());
        
        // Quick fix for error "Cannot call method "toLowerCase" of null" (known bug/issue)
        getTestContext().setUserAgent("Mozilla/5.0 (Windows; U; Windows NT 5.1; de; rv:1.8.1.3) " +
                "Gecko/20070309 Firefox/2.0.0.3");
        
        if (requiresAuthentication()) {
            getTestContext().setAuthorization(getProperty(PROP_ADMIN_USR), getProperty(PROP_ADMIN_PASSWORD));
            beginAt("/?vrtx=admin&authenticate");
        } else {
            beginAt("/");
        }
        
        // Make sure the rootcollection containing testresources is available
        assertLinkPresent(rootCollection);
        clickLink(rootCollection);
        assertFalse("The requested page is blank", StringUtils.isBlank(getPageSource()));
        
        prepare();
    }
    
    private void prepare() {
        String testResourceName = this.getClass().getSimpleName().toLowerCase();
        try {
            assertLinkPresent(testResourceName);
        } catch (AssertionFailedError afe) {
            throw new MissingWebTestResourceException("A required resource for this test was not found. \n" +
            		"Make sure a collection with the same name and title as the testclass " +
            		"(" + testResourceName + ") exists under the rootcollection.");
        }
        clickLink(testResourceName);
        assertFalse("The requested page is blank", StringUtils.isBlank(getPageSource()));
    }
    
    protected String getProperty(String key) throws Exception {
        if (!props.containsKey(key)) {
            throw new Exception("Missing property '" + key + "'. Make sure '" + propFile + "' is set up properly.");
        }
        return StringUtils.trim(props.getProperty(key));
    }
    
    protected abstract String getBaseUrl() throws Exception;
    
    protected abstract boolean requiresAuthentication();

}
